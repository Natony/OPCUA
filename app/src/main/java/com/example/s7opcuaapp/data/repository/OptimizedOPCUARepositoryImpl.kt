package com.example.s7opcuaapp.data.repository

import android.util.Log
import com.example.s7opcuaapp.data.buffer.PlcDataBuffer
import com.example.s7opcuaapp.data.local.AppDatabase
import com.example.s7opcuaapp.data.model.DeviceEntity
import com.example.s7opcuaapp.data.model.PlcData
import com.example.s7opcuaapp.data.opcua.OPCUAClientManager
import com.example.s7opcuaapp.util.LoadingTracker
import com.example.s7opcuaapp.util.NetworkConnectivity
import com.example.s7opcuaapp.util.PerformanceMonitor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * Optimized OPC UA Repository with buffer and performance improvements
 */
class OptimizedOPCUARepositoryImpl @Inject constructor(
    private var device: DeviceEntity,
    private val database: AppDatabase,
    private val dataBuffer: PlcDataBuffer,
    private val performanceMonitor: PerformanceMonitor,
    private val networkConnectivity: NetworkConnectivity
) : S7Repository {

    // Use buffer's flow instead of creating our own
    override fun observePlcData(): Flow<PlcData> = dataBuffer.dataFlow

    // Repository scope for coroutines
    private val repositoryScope = CoroutineScope(
        Dispatchers.IO + SupervisorJob() +
                CoroutineExceptionHandler { _, throwable ->
                    Log.e("OPCUARepo", "Repository coroutine error", throwable)
                    isConnected.set(false)
                }
    )
    // Connection management
    private val connectionMutex = Mutex()
    private val isStarted = AtomicBoolean(false)
    private val isConnected = AtomicBoolean(false)
    private var connectionJob: Job? = null

    // Connection parameters
    private var lastConnectionAttempt = 0L
    private val minConnectionInterval = 2000L
    private var connectionAttempts = 0
    private val maxConnectionAttempts = 3

    private var lastUpdateLogTime = 0L
    private var updateCounter = 0

    // Node IDs configuration
    private val boolNodeIds = (3..17).map { "ns=4;i=$it" }
    private val intNodeIds = (18..45).map { "ns=4;i=$it" }

    // Loading tracker
    private val totalNodes = boolNodeIds.size + intNodeIds.size
    private val loadingTracker = LoadingTracker<String>(totalNodes)


    // Subscription groups for optimized polling
    private val subscriptionGroups = mapOf(
        "critical" to SubscriptionGroup(
            nodeIds = listOf("ns=4;i=7", "ns=4;i=14"), // Power, E-Stop
            samplingInterval = 250  // Tăng từ 100ms lên 250ms
        ),
        "movement" to SubscriptionGroup(
            nodeIds = listOf("ns=4;i=3", "ns=4;i=4", "ns=4;i=5", "ns=4;i=6"),
            samplingInterval = 500  // Tăng từ 250ms lên 500ms
        ),
        "status" to SubscriptionGroup(
            nodeIds = boolNodeIds.filter { it !in listOf("ns=4;i=7", "ns=4;i=14", "ns=4;i=3", "ns=4;i=4", "ns=4;i=5", "ns=4;i=6") } + intNodeIds,
            samplingInterval = 1000  // Tăng từ 500ms lên 1000ms
        )
    )

    data class SubscriptionGroup(
        val nodeIds: List<String>,
        val samplingInterval: Int
    )

    /**
     * Start connection with improved error handling
     */
    suspend fun start() = connectionMutex.withLock {
        if (isStarted.get()) {
            Log.d("OPCUARepo", "⚠️ Repository already started")
            return@withLock
        }

        Log.d("OPCUARepo", "🚀 Starting Optimized OPC UA Repository...")
        isStarted.set(true)

        // Reset state
        loadingTracker.reset()
        dataBuffer.clear()

        // Start connection loop
        connectionJob?.cancel()
        connectionJob = repositoryScope.launch {
            startConnectionLoop()
        }
    }

    /**
     * Connection loop with smart retry logic
     */
    private suspend fun startConnectionLoop() {
        var consecutiveFailures = 0
        val maxConsecutiveFailures = 3 // Giảm từ 5 xuống 3

        while (isStarted.get() && repositoryScope.isActive) {
            try {
                // Check if we should stop trying
                if (connectionAttempts >= maxConnectionAttempts) {
                    Log.e("OPCUARepo", "Max connection attempts reached, stopping")
                    break
                }

                if (!isConnected.get()) {
                    // Rate limiting với exponential backoff
                    val backoffDelay = minOf(
                        minConnectionInterval * (1L shl consecutiveFailures),
                        30000L // Max 30 seconds
                    )

                    val timeSinceLastAttempt = System.currentTimeMillis() - lastConnectionAttempt
                    if (timeSinceLastAttempt < backoffDelay) {
                        delay(backoffDelay - timeSinceLastAttempt)
                    }

                    Log.d("OPCUARepo", "🔄 Connection attempt ${connectionAttempts + 1}/$maxConnectionAttempts")
                    lastConnectionAttempt = System.currentTimeMillis()
                    connectionAttempts++

                    // Cleanup trước khi connect
                    performCleanup(consecutiveFailures)

                    val connected = try {
                        withTimeout(10000L) { // 10 second timeout
                            connectToServer()
                        }
                    } catch (e: TimeoutCancellationException) {
                        Log.e("OPCUARepo", "Connection timeout")
                        false
                    }

                    if (connected) {
                        consecutiveFailures = 0
                        connectionAttempts = 0 // Reset on success
                        isConnected.set(true)

                        loadingTracker.reset()
                        subscribeWithGroups()

                        Log.d("OPCUARepo", "✅ Connected successfully")
                    } else {
                        consecutiveFailures++

                        if (consecutiveFailures >= maxConsecutiveFailures) {
                            Log.e("OPCUARepo", "💥 Max failures reached, stopping")
                            break
                        }
                    }
                } else {
                    // Health check với longer interval
                    delay(30000) // 30s instead of 15s

                    if (!OPCUAClientManager.isConnected()) {
                        Log.w("OPCUARepo", "📡 Connection lost")
                        isConnected.set(false)
                        connectionAttempts = 0 // Reset for reconnection
                    }
                }
            } catch (e: CancellationException) {
                Log.d("OPCUARepo", "Connection loop cancelled")
                break
            } catch (e: Exception) {
                Log.e("OPCUARepo", "💥 Connection loop error", e)
                isConnected.set(false)
                consecutiveFailures++

                if (consecutiveFailures >= maxConsecutiveFailures) {
                    break
                }

                delay(5000)
            }
        }

        Log.d("OPCUARepo", "🔄 Connection loop ended")
        isConnected.set(false)
    }

    /**
     * Cleanup with progressive delays
     */
    private suspend fun performCleanup(failureCount: Int) {
        try {
            Log.d("OPCUARepo", "🧹 Performing cleanup...")
            OPCUAClientManager.disconnect()

            val cleanupDelay = when {
                failureCount == 0 -> 1000L
                failureCount <= 2 -> 3000L
                else -> 5000L
            }

            Log.d("OPCUARepo", "⏳ Waiting ${cleanupDelay}ms for cleanup...")
            delay(cleanupDelay)
        } catch (e: Exception) {
            Log.w("OPCUARepo", "Cleanup error", e)
        }
    }

    /**
     * Connect to server
     */
    private suspend fun connectToServer(): Boolean {
        // Check network first
        if (!networkConnectivity.isNetworkAvailable()) {
            Log.w("OPCUARepo", "No network connection available")
            return false
        }
        return try {

            val startTime = System.currentTimeMillis()

            withContext(Dispatchers.IO) {
                val result = if (!device.opcUsername.isNullOrBlank()) {
                    OPCUAClientManager.connect(
                        ipAddress = device.ipAddress,
                        port = device.port,
                        username = device.opcUsername,
                        password = device.opcPassword
                    )
                } else {
                    OPCUAClientManager.connect(
                        ipAddress = device.ipAddress,
                        port = device.port
                    )
                }

                if (result) {
                    val connectTime = System.currentTimeMillis() - startTime
                    performanceMonitor.recordNetworkLatency(connectTime)
                    Log.d("OPCUARepo", "✅ Connected in ${connectTime}ms")
                }

                result
            }
        } catch (e: CancellationException) {
            throw e // Re-throw to maintain cancellation
        } catch (e: Exception) {
            when {
                e.message?.contains("No route to host") == true -> {
                    Log.e("OPCUARepo", "❌ Network unreachable: ${device.ipAddress}")
                }
                e.message?.contains("Connection refused") == true -> {
                    Log.e("OPCUARepo", "❌ Connection refused by server")
                }
                else -> {
                    Log.e("OPCUARepo", "❌ Connection error: ${e.message}")
                }
            }
            false
        }
    }

    /**
     * Subscribe with grouped sampling intervals
     */
    private suspend fun subscribeWithGroups() {
        var totalSubscriptions = 0

        subscriptionGroups.forEach { (groupName, group) ->
            Log.d("OPCUARepo", "📊 Subscribing $groupName group (${group.nodeIds.size} nodes, ${group.samplingInterval}ms)")

            group.nodeIds.forEach { nodeId ->
                try {
                    // Calculate index based on nodeId
                    val nodeIndex = nodeId.substringAfter("i=").toInt()
                    val index = when {
                        nodeIndex in 3..17 -> nodeIndex - 3  // Bool indices 0-14
                        nodeIndex in 18..45 -> nodeIndex - 18 // Int indices 0-27
                        else -> -1
                    }

                    if (index >= 0) {
                        OPCUAClientManager.createSubscription(
                            nodeIdString = nodeId,
                            samplingInterval = UInteger.valueOf(group.samplingInterval)
                        ) { dataValue ->
                            handleDataUpdate(nodeId, index, dataValue)
                        }

                        totalSubscriptions++
                        loadingTracker.markLoaded(nodeId)
                    }
                } catch (e: Exception) {
                    Log.e("OPCUARepo", "Failed to subscribe $nodeId", e)
                }
            }
        }

        performanceMonitor.updateSubscriptionCount(totalSubscriptions)
        Log.d("OPCUARepo", "🎯 Created $totalSubscriptions subscriptions")
    }

    /**
     * Handle data updates through buffer
     */
    private fun handleDataUpdate(nodeId: String, index: Int, dataValue: DataValue) {
        try {
            // Log update frequency for debugging
            updateCounter++
            val now = System.currentTimeMillis()
            if (now - lastUpdateLogTime >= 5000) { // Log every 5 seconds
                val rate = updateCounter * 1000.0 / (now - lastUpdateLogTime)
                Log.d("OPCUARepo", "OPC UA update rate: ${String.format("%.1f", rate)}/s")
                updateCounter = 0
                lastUpdateLogTime = now
            }

            val nodeIndex = nodeId.substringAfter("i=").toInt()
            when {
                nodeIndex in 3..17 -> {
                    val value = dataValue.value.value as? Boolean ?: false
                    dataBuffer.updateBool(index, value)
                }
                nodeIndex in 18..45 -> {
                    val raw = dataValue.value.value
                    val value = when (raw) {
                        is Int -> raw
                        is Long -> raw.toInt()
                        is Short -> raw.toInt()
                        is UInteger -> raw.toInt()
                        else -> 0
                    }
                    dataBuffer.updateInt(index, value)
                }
            }
        } catch (e: Exception) {
            Log.w("OPCUARepo", "Error handling update for $nodeId", e)
        }
    }
    /**
     * Write boolean with performance tracking
     */
    override suspend fun writeBoolean(index: Int, value: Boolean) = withContext(Dispatchers.IO) {
        if (!isConnected.get()) {
            throw Exception("Not connected to PLC")
        }

        performanceMonitor.recordWriteCommand()

        val startTime = System.currentTimeMillis()
        try {
            if (index in boolNodeIds.indices) {
                val nodeIdString = boolNodeIds[index]
                val status = OPCUAClientManager.writeNode(nodeIdString, value)

                if (status == null || !status.isGood) {
                    throw Exception("Write failed: $status")
                }

                val writeTime = System.currentTimeMillis() - startTime
                performanceMonitor.recordNetworkLatency(writeTime)

                Log.d("OPCUARepo", "✅ WriteBoolean[$index]=$value in ${writeTime}ms")
            }
        } catch (e: Exception) {
            Log.e("OPCUARepo", "❌ WriteBoolean failed", e)
            throw e
        }
    }

    /**
     * Write integer with performance tracking
     */
    override suspend fun writeInt(index: Int, value: Int) = withContext(Dispatchers.IO) {
        if (!isConnected.get()) {
            throw Exception("Not connected to PLC")
        }

        performanceMonitor.recordWriteCommand()

        val startTime = System.currentTimeMillis()
        try {
            if (index in intNodeIds.indices) {
                val nodeIdString = intNodeIds[index]
                val shortValue = value.toShort()
                val status = OPCUAClientManager.writeNode(nodeIdString, shortValue)

                if (status == null || !status.isGood) {
                    throw Exception("Write failed: $status")
                }

                val writeTime = System.currentTimeMillis() - startTime
                performanceMonitor.recordNetworkLatency(writeTime)

                Log.d("OPCUARepo", "✅ WriteInt[$index]=$value in ${writeTime}ms")
            }
        } catch (e: Exception) {
            Log.e("OPCUARepo", "❌ WriteInt failed", e)
            throw e
        }
    }

    /**
     * Stop repository
     */
    override fun stop() {
        repositoryScope.launch {
            connectionMutex.withLock {
                if (!isStarted.get()) return@withLock

                Log.d("OPCUARepo", "🛑 Stopping repository...")
                isStarted.set(false)
                isConnected.set(false)

                // Cancel jobs first
                connectionJob?.cancel()

                // Join to ensure cancellation completes
                runCatching {
                    connectionJob?.join()
                }

                // Then cleanup
                try {
                    withTimeout(5000L) { // 5 second timeout
                        OPCUAClientManager.disconnect()
                    }
                } catch (e: Exception) {
                    Log.e("OPCUARepo", "Error during disconnect", e)
                }

                // Clear buffer
                dataBuffer.clear()

                Log.d("OPCUARepo", "✅ Repository stopped")
            }
        }
    }
    /**
     * Update device configuration
     */
    override fun updateDevice(device: DeviceEntity) {
        this.device = device

        // Restart connection if active
        if (isStarted.get()) {
            repositoryScope.launch {
                stop()
                delay(1000)
                start()
            }
        }
    }

    fun observeLoadingPercent(): StateFlow<Int> = loadingTracker.percent
}