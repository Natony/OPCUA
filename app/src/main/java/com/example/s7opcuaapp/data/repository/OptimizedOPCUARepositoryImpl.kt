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

        Log.d(TAG, "Starting connection loop...")

        while (isStarted.get() && repositoryScope.isActive) {
            try {
                when {
                    // Case 1: Not connected - try to establish connection
                    !isConnected.get() -> {
                        handleNotConnected(consecutiveFailures)?.let { failures ->
                            consecutiveFailures = failures
                        } ?: break // Max failures reached, exit loop
                    }

                    // Case 2: Connected - perform health check
                    else -> {
                        val isHealthy = performHealthCheck()
                        if (!isHealthy) {
                            Log.w(TAG, "Health check failed, marking as disconnected")
                            handleDisconnection()
                            consecutiveFailures = 1 // Start counting failures again
                        } else {
                            consecutiveFailures = 0 // Reset on successful health check
                        }

                        // Wait before next health check
                        delay(HEALTH_CHECK_INTERVAL)
                    }
                }

            } catch (e: CancellationException) {
                // Normal cancellation - exit gracefully
                Log.d(TAG, "Connection loop cancelled")
                break

            } catch (e: Exception) {
                // Unexpected error
                Log.e(TAG, "Unexpected error in connection loop", e)
                consecutiveFailures++

                if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                    Log.e(TAG, "Max consecutive failures reached due to errors")
                    handleMaxFailuresReached()
                    break
                }

                delay(calculateRetryDelay(consecutiveFailures))
            }
        }

        // Cleanup when loop exits
        Log.d(TAG, "Connection loop ended")
        handleLoopExit()
    }

    /**
     * Handle not connected state - attempt to connect
     * @return Updated failure count or null if max failures reached
     */
    private suspend fun handleNotConnected(currentFailures: Int): Int? {
        // Check if we should give up
        if (currentFailures >= MAX_CONSECUTIVE_FAILURES) {
            Log.e(TAG, "Max consecutive failures reached ($currentFailures/$MAX_CONSECUTIVE_FAILURES)")
            handleMaxFailuresReached()
            return null
        }

        // Apply rate limiting
        if (currentFailures > 0) {
            val retryDelay = calculateRetryDelay(currentFailures)
            Log.d(TAG, "Waiting ${retryDelay}ms before retry attempt ${currentFailures + 1}")
            delay(retryDelay)

            // Clean up previous connection if needed
            cleanupPreviousConnection()
        }

        // Enforce minimum interval between connection attempts
        enforceRateLimit()

        // Attempt connection
        Log.d(TAG, "Attempting connection (attempt ${currentFailures + 1}/$MAX_CONSECUTIVE_FAILURES)")

        val connected = attemptConnection()

        return if (connected) {
            Log.d(TAG, "✅ Connection successful")
            handleConnectionSuccess()
            0 // Reset failure count
        } else {
            Log.w(TAG, "❌ Connection failed")
            handleConnectionFailure()
            currentFailures + 1 // Increment failure count
        }
    }

    /**
     * Attempt to establish connection
     */
    private suspend fun attemptConnection(): Boolean {
        return try {
            // Set callbacks before connecting
            OPCUAClientManager.setConnectionCallbacks(
                onLost = {
                    connectionLostScope.launch {
                        handleConnectionLost()
                    }
                },
                onRestored = {
                    Log.d(TAG, "Connection restored callback triggered")
                }
            )

            // Try to connect
            val connected = OPCUAClientManager.connect(
                ipAddress = device.ipAddress,
                port = device.port,
                username = device.opcUsername.takeIf { it.isNotBlank() },
                password = device.opcPassword.takeIf { it.isNotBlank() }
            )

            if (connected) {
                // Setup subscriptions after successful connection
                try {
                    setupSubscriptions()
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to setup subscriptions", e)
                    OPCUAClientManager.disconnect()
                    false
                }
            } else {
                false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Connection attempt failed with exception", e)
            false
        }
    }

    /**
     * Perform health check on existing connection
     */
    private suspend fun performHealthCheck(): Boolean {
        return try {
            withTimeoutOrNull(3000L) {
                OPCUAClientManager.checkConnectionHealth()
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Health check exception", e)
            false
        }
    }

    /**
     * Handle successful connection
     */
    private fun handleConnectionSuccess() {
        isConnected.set(true)
        loadingTracker.reset() // Reset for next connection if needed
        performanceMonitor.recordNetworkLatency(System.currentTimeMillis() - lastConnectionAttempt)

        // Notify success through loading tracker
        repeat(44) { // Mark all nodes as loaded
            loadingTracker.markLoaded("node_$it")
        }
    }

    /**
     * Handle connection failure
     */
    private fun handleConnectionFailure() {
        isConnected.set(false)
        loadingTracker.setError()
    }

    /**
     * Handle disconnection detected during health check
     */
    private fun handleDisconnection() {
        isConnected.set(false)
        loadingTracker.setError()

        // Trigger connection lost callback
        connectionLostScope.launch {
            handleConnectionLost()
        }
    }

    /**
     * Handle max failures reached
     */
    private fun handleMaxFailuresReached() {
        isConnected.set(false)
        loadingTracker.setError()

        // Could emit a special state or throw exception if needed
        Log.e(TAG, "Connection loop stopping due to max failures")
    }

    /**
     * Cleanup when connection loop exits
     */
    private fun handleLoopExit() {
        isConnected.set(false)
        loadingTracker.setError()

        // Cancel any pending operations
        connectionLostScope.cancel()
    }

    /**
     * Calculate retry delay based on failure count (exponential backoff)
     */
    private fun calculateRetryDelay(failureCount: Int): Long {
        val baseDelay = 2000L // 2 seconds
        val maxDelay = 30000L // 30 seconds

        val delay = baseDelay * (1 shl (failureCount - 1)) // Exponential backoff
        return minOf(delay, maxDelay)
    }

    /**
     * Cleanup previous connection attempt
     */
    private suspend fun cleanupPreviousConnection() {
        try {
            OPCUAClientManager.disconnect()
        } catch (e: Exception) {
            Log.w(TAG, "Error during connection cleanup", e)
        }
    }

    /**
     * Enforce minimum interval between connection attempts
     */
    private suspend fun enforceRateLimit() {
        val timeSinceLastAttempt = System.currentTimeMillis() - lastConnectionAttempt
        if (timeSinceLastAttempt < MIN_CONNECTION_INTERVAL) {
            delay(MIN_CONNECTION_INTERVAL - timeSinceLastAttempt)
        }
        lastConnectionAttempt = System.currentTimeMillis()
    }


    private suspend fun connect(): Boolean = try {
        OPCUAClientManager.setConnectionCallbacks(
            onLost = {
                connectionLostScope.launch {
                    handleConnectionLost()
                }
            },
            onRestored = null  // Không cần xử lý khi connection restored
        )

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