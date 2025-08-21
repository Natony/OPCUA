package com.example.s7opcuaapp.data.repository

import android.util.Log
import com.example.s7opcuaapp.data.buffer.PlcDataBuffer
import com.example.s7opcuaapp.data.local.AppDatabase
import com.example.s7opcuaapp.data.model.DeviceEntity
import com.example.s7opcuaapp.data.model.PlcData
import com.example.s7opcuaapp.data.opcua.OPCUAClientManager
import com.example.s7opcuaapp.util.ConnectionManager
import com.example.s7opcuaapp.util.LoadingTracker
import com.example.s7opcuaapp.util.PerformanceMonitor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class OptimizedOPCUARepositoryImpl @Inject constructor(
    private var device: DeviceEntity,
    private val database: AppDatabase,
    private val dataBuffer: PlcDataBuffer,
    private val performanceMonitor: PerformanceMonitor,
    private val connectionManager: ConnectionManager
) : S7Repository {

    companion object {
        @Volatile
        private var activeInstance: OptimizedOPCUARepositoryImpl? = null
        private val instanceLock = Any()

        private const val TAG = "OPCUARepo"
        private const val MIN_CONNECTION_INTERVAL = 2000L
        private const val HEALTH_CHECK_INTERVAL = 2000L
        private const val MAX_CONSECUTIVE_FAILURES = 3
    }

    // Flow & State Management
    override fun observePlcData(): Flow<PlcData> = dataBuffer.dataFlow
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val connectionLostScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val connectionMutex = Mutex()
    private val loadingTracker = LoadingTracker<String>(44) // 15 bool + 29 int nodes

    // Connection State
    private val isStarted = AtomicBoolean(false)
    private val isConnected = AtomicBoolean(false)
    private var connectionJob: Job? = null
    private var lastConnectionAttempt = 0L
    private var updateCounter = 0
    private var lastUpdateLogTime = 0L

    // Node Configuration
    private val boolNodeIds = (3..17).map { "ns=4;i=$it" }
    private val intNodeIds = (18..46).map { "ns=4;i=$it" }

    // Subscription Groups for optimized polling
    private val subscriptionGroups = mapOf(
        "critical" to Pair(listOf("ns=4;i=7", "ns=4;i=14"), 250),
        "movement" to Pair((3..6).map { "ns=4;i=$it" }, 500),
        "status" to Pair(
            boolNodeIds.filterNot { it in listOf("ns=4;i=7", "ns=4;i=14") + (3..6).map { "ns=4;i=$it" } } + intNodeIds,
            1000
        )
    )

    init {
        synchronized(instanceLock) {
            activeInstance?.let { existingInstance ->
                Log.w(TAG, "Forcefully stopping existing instance")
                runBlocking {
                    existingInstance.forceStop()
                    delay(1000)  // Wait for cleanup
                }
            }
            activeInstance = this
        }
    }

    fun isConnected(): Boolean = isConnected.get() && OPCUAClientManager.isConnected()

    fun observeLoadingPercent(): StateFlow<Int> = loadingTracker.percent

    suspend fun start() = connectionMutex.withLock {
        if (isStarted.get()) return@withLock

        Log.d(TAG, "Starting repository...")
        isStarted.set(true)
        loadingTracker.reset()
        dataBuffer.clear()

        connectionJob?.cancel()
        delay(500)

        connectionJob = repositoryScope.launch {
            connectionLoop()
        }
    }

    private suspend fun connectionLoop() {
        var consecutiveFailures = 0

        while (isStarted.get() && repositoryScope.isActive) {
            try {
                if (!isConnected.get()) {
                    // Rate limiting
                    enforceRateLimit()

                    // Cleanup before connection
                    if (consecutiveFailures > 0) {
                        try {
                            OPCUAClientManager.disconnect()
                        } catch (e: Exception) {
                            Log.w(TAG, "Error during cleanup", e)
                        }
                        delay(minOf(2000L * consecutiveFailures, 10000L))
                    }

                    // Connect with proper error handling
                    val connected = try {
                        connect()
                    } catch (e: Exception) {
                        Log.e(TAG, "Connection attempt failed", e)
                        false
                    }

                    if (connected) {
                        consecutiveFailures = 0
                        isConnected.set(true)

                        try {
                            setupSubscriptions()
                            Log.d(TAG, "Connected successfully")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to setup subscriptions", e)
                            isConnected.set(false)
                            loadingTracker.setError()
                            throw e
                        }
                    } else {
                        consecutiveFailures++
                        loadingTracker.setError()

                        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                            Log.e(TAG, "Max consecutive failures reached")
                            break
                        }

                        delay(minOf(5000L * consecutiveFailures, 30000L))
                    }
                } else {
                    // Health check with error handling
                    delay(HEALTH_CHECK_INTERVAL)

                    try {
                        if (!OPCUAClientManager.checkConnectionHealth()) {
                            Log.w(TAG, "Connection unhealthy")
                            isConnected.set(false)
                            loadingTracker.setError()
                            OPCUAClientManager.connectionLostCallback?.invoke()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Health check error", e)
                        isConnected.set(false)
                        loadingTracker.setError()
                    }
                }
            } catch (e: CancellationException) {
                // Normal cancellation
                Log.d(TAG, "Connection loop cancelled")
                break
            } catch (e: Exception) {
                Log.e(TAG, "Connection loop error", e)
                isConnected.set(false)
                consecutiveFailures++

                if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                    loadingTracker.setError()
                    break
                }

                delay(5000)
            }
        }

        // Cleanup on exit
        Log.d(TAG, "Connection loop ended")
        isConnected.set(false)
        loadingTracker.setError()
    }

    private suspend fun enforceRateLimit() {
        val timeSinceLastAttempt = System.currentTimeMillis() - lastConnectionAttempt
        if (timeSinceLastAttempt < MIN_CONNECTION_INTERVAL) {
            delay(MIN_CONNECTION_INTERVAL - timeSinceLastAttempt)
        }
        lastConnectionAttempt = System.currentTimeMillis()
    }

    private suspend fun connect(): Boolean = try {
        OPCUAClientManager.setConnectionLostCallback {
            connectionLostScope.launch {
                handleConnectionLost()
            }
        }

        val result = OPCUAClientManager.connect(
            ipAddress = device.ipAddress,
            port = device.port,
            username = device.opcUsername.takeIf { it.isNotBlank() },
            password = device.opcPassword.takeIf { it.isNotBlank() }
        )

        if (result) {
            performanceMonitor.recordNetworkLatency(System.currentTimeMillis() - lastConnectionAttempt)
        }
        result
    } catch (e: Exception) {
        Log.e(TAG, "Connection error: ${e.message}")
        false
    }

    private suspend fun setupSubscriptions() {
        val subscribedNodes = mutableSetOf<String>()  // Track already subscribed nodes
        var totalSubscriptions = 0

        subscriptionGroups.forEach { (groupName, config) ->
            val (nodeIds, interval) = config
            Log.d(TAG, "Subscribing $groupName group (${nodeIds.size} nodes, ${interval}ms)")

            nodeIds.forEach { nodeId ->
                if (subscribedNodes.contains(nodeId)) {
                    Log.w(TAG, "Node $nodeId already subscribed, skipping")
                    return@forEach
                }

                try {
                    val index = getNodeIndex(nodeId)
                    if (index >= 0) {
                        OPCUAClientManager.createSubscription(
                            nodeIdString = nodeId,
                            samplingInterval = UInteger.valueOf(interval)
                        ) { dataValue ->
                            handleDataUpdate(nodeId, index, dataValue)
                        }

                        subscribedNodes.add(nodeId)
                        totalSubscriptions++
                        loadingTracker.markLoaded(nodeId)  // Only mark once
                    }
                    delay(20)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to subscribe $nodeId", e)
                }
            }
        }

        performanceMonitor.updateSubscriptionCount(totalSubscriptions)
        Log.d(TAG, "Created $totalSubscriptions subscriptions")
    }

    private fun getNodeIndex(nodeId: String): Int = try {
        val nodeIndex = nodeId.substringAfter("i=").toInt()
        when (nodeIndex) {
            in 3..17 -> nodeIndex - 3   // Bool indices 0-14
            in 18..46 -> nodeIndex - 18  // Int indices 0-27
            else -> -1
        }
    } catch (e: Exception) {
        -1
    }

    private fun handleDataUpdate(nodeId: String, index: Int, dataValue: DataValue) {
        if (index < 0) return

        try {
            // Log update frequency for debugging
            updateCounter++
            val now = System.currentTimeMillis()
            if (now - lastUpdateLogTime >= 5000) { // Log every 5 seconds
                val rate = updateCounter * 1000.0 / (now - lastUpdateLogTime)
                Log.d(TAG, "OPC UA update rate: ${String.format("%.1f", rate)}/s")
                updateCounter = 0
                lastUpdateLogTime = now
            }

            val nodeIndex = nodeId.substringAfter("i=").toIntOrNull() ?: return

            when (nodeIndex) {
                in 3..17 -> {
                    val value = dataValue.value.value as? Boolean ?: false
                    dataBuffer.updateBool(index, value)
                }
                in 18..46 -> {
                    val value = when (val raw = dataValue.value.value) {
                        is Int -> raw
                        is Long -> raw.toInt()
                        is Short -> raw.toInt()
                        is org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger -> raw.toInt()
                        else -> 0
                    }
                    dataBuffer.updateInt(index, value)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error handling update for $nodeId", e)
        }
    }

    private fun handleConnectionLost() {
        Log.w(TAG, "Connection lost")
        isConnected.set(false)
        loadingTracker.setError()
        // Callback will be handled by OPCUAClientManager's connectionLostCallback
    }

    override suspend fun writeBoolean(index: Int, value: Boolean): Unit = withContext(Dispatchers.IO) {
        ensureConnected()
        performanceMonitor.recordWriteCommand()

        val startTime = System.currentTimeMillis()
        try {
            val nodeId = boolNodeIds.getOrNull(index) ?: throw IndexOutOfBoundsException("Invalid bool index: $index")
            val status = OPCUAClientManager.writeNode(nodeId, value)

            if (status == null || !status.isGood) {
                throw Exception("Write failed: $status")
            }

            val writeTime = System.currentTimeMillis() - startTime
            performanceMonitor.recordNetworkLatency(writeTime)
            Log.d(TAG, "WriteBoolean[$index]=$value in ${writeTime}ms")
        } catch (e: Exception) {
            Log.e(TAG, "WriteBoolean failed", e)
            // Check if connection lost
            if (e.message?.contains("connection", ignoreCase = true) == true ||
                e.message?.contains("timeout", ignoreCase = true) == true ||
                e.message?.contains("Bad_NotConnected", ignoreCase = true) == true) {
                handleConnectionLost()
            }
            throw e
        }
    }

    override suspend fun writeInt(index: Int, value: Int): Unit = withContext(Dispatchers.IO) {
        ensureConnected()
        performanceMonitor.recordWriteCommand()

        val startTime = System.currentTimeMillis()
        try {
            val nodeId = intNodeIds.getOrNull(index) ?: throw IndexOutOfBoundsException("Invalid int index: $index")
            val status = OPCUAClientManager.writeNode(nodeId, value.toShort())

            if (status == null || !status.isGood) {
                throw Exception("Write failed: $status")
            }

            val writeTime = System.currentTimeMillis() - startTime
            performanceMonitor.recordNetworkLatency(writeTime)
            Log.d(TAG, "WriteInt[$index]=$value in ${writeTime}ms")
        } catch (e: Exception) {
            Log.e(TAG, "WriteInt failed", e)
            // Check if connection lost
            if (e.message?.contains("connection", ignoreCase = true) == true ||
                e.message?.contains("timeout", ignoreCase = true) == true ||
                e.message?.contains("Bad_NotConnected", ignoreCase = true) == true) {
                handleConnectionLost()
            }
            throw e
        }
    }

    private fun ensureConnected() {
        if (!isConnected.get()) {
            throw Exception("Not connected to PLC")
        }
    }

    override fun stop() {
        runBlocking {
            forceStop()
            synchronized(instanceLock) {
                if (activeInstance == this@OptimizedOPCUARepositoryImpl) {
                    activeInstance = null
                }
            }
        }
    }

    private suspend fun forceStop() {
        try {
            Log.d(TAG, "Force stopping repository")

            isStarted.set(false)
            isConnected.set(false)

            connectionJob?.cancel()
            connectionJob = null

            connectionLostScope.cancel()

            OPCUAClientManager.disconnect()
            dataBuffer.clear()
            loadingTracker.reset()

            Log.d(TAG, "Force stop completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error in force stop", e)
        }
    }

    override fun updateDevice(device: DeviceEntity) {
        Log.d(TAG, "Updating device: ${device.name}")
        this.device = device
    }
}