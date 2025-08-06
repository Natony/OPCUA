package com.example.s7opcuaapp.data.opcua

import android.util.Log
import com.example.s7opcuaapp.data.model.DeviceEntity
import com.example.s7opcuaapp.util.PerformanceMonitor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages OPC UA connection lifecycle and health monitoring
 */
@Singleton
class OpcUaConnectionManager @Inject constructor(
    private val performanceMonitor: PerformanceMonitor
) {

    companion object {
        private const val TAG = "OpcUaConnectionManager"
        private const val CONNECTION_TIMEOUT = 20000L
        private const val HEALTH_CHECK_INTERVAL = 5000L
        private const val MAX_CONSECUTIVE_FAILURES = 3
    }

    // Connection state
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    // Connection lifecycle
    private val isStarted = AtomicBoolean(false)
    private var healthCheckJob: Job? = null
    private val connectionScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Callbacks
    private var onConnectionLost: (() -> Unit)? = null
    private var onConnectionRestored: (() -> Unit)? = null

    /**
     * Connect to OPC UA server
     */
    suspend fun connect(device: DeviceEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (isStarted.get()) {
                Log.w(TAG, "Already connected or connecting")
                return@withContext Result.success(Unit)
            }

            Log.d(TAG, "🔌 Connecting to ${device.name} at ${device.ipAddress}:${device.port}")
            isStarted.set(true)
            _connectionError.value = null

            val startTime = System.currentTimeMillis()

            // Connect with timeout
            withTimeout(CONNECTION_TIMEOUT) {
                val connected = if (!device.opcUsername.isNullOrBlank()) {
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

                if (!connected) {
                    throw Exception("Failed to connect to OPC UA server")
                }
            }

            val connectTime = System.currentTimeMillis() - startTime
            performanceMonitor.recordNetworkLatency(connectTime)

            _isConnected.value = true
            Log.d(TAG, "✅ Connected successfully in ${connectTime}ms")

            // Start health monitoring
            startHealthMonitoring()

            // Set connection callbacks
            OPCUAClientManager.setConnectionCallbacks(
                onLost = { handleConnectionLost() },
                onRestored = { handleConnectionRestored() }
            )

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Connection failed: ${e.message}", e)
            _connectionError.value = e.message
            _isConnected.value = false
            isStarted.set(false)
            Result.failure(e)
        }
    }

    /**
     * Disconnect from OPC UA server
     */
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔌 Disconnecting...")

            // Stop health monitoring
            healthCheckJob?.cancel()
            healthCheckJob = null

            // Disconnect OPC UA
            OPCUAClientManager.disconnect()

            // Reset state
            _isConnected.value = false
            _connectionError.value = null
            isStarted.set(false)

            Log.d(TAG, "✅ Disconnected successfully")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during disconnect", e)
        }
    }

    /**
     * Create subscription for a node
     */
    suspend fun createSubscription(
        nodeId: String,
        samplingInterval: Int = 250,
        onValueChange: (DataValue) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!_isConnected.value) {
                return@withContext Result.failure(Exception("Not connected"))
            }

            OPCUAClientManager.createSubscription(
                nodeIdString = nodeId,
                samplingInterval = UInteger.valueOf(samplingInterval),
                onValueChange = onValueChange
            )

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create subscription for $nodeId", e)
            Result.failure(e)
        }
    }

    /**
     * Write value to node
     */
    suspend fun writeNode(nodeId: String, value: Any): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!_isConnected.value) {
                return@withContext Result.failure(Exception("Not connected"))
            }

            val status = OPCUAClientManager.writeNode(nodeId, value)

            if (status == null || !status.isGood) {
                return@withContext Result.failure(Exception("Write failed: $status"))
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to write to $nodeId", e)

            // Check if connection lost
            if (e.message?.contains("connection", ignoreCase = true) == true ||
                e.message?.contains("timeout", ignoreCase = true) == true) {
                handleConnectionLost()
            }

            Result.failure(e)
        }
    }

    /**
     * Start health monitoring
     */
    private fun startHealthMonitoring() {
        healthCheckJob?.cancel()
        healthCheckJob = connectionScope.launch {
            var consecutiveFailures = 0

            while (isActive && isStarted.get()) {
                delay(HEALTH_CHECK_INTERVAL)

                try {
                    val healthy = withTimeoutOrNull(2000L) {
                        OPCUAClientManager.checkConnectionHealth()
                    } ?: false

                    if (!healthy) {
                        consecutiveFailures++
                        Log.w(TAG, "Health check failed ($consecutiveFailures/$MAX_CONSECUTIVE_FAILURES)")

                        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                            handleConnectionLost()
                            break
                        }
                    } else {
                        if (consecutiveFailures > 0) {
                            Log.d(TAG, "Connection recovered")
                        }
                        consecutiveFailures = 0
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Health check error", e)
                    consecutiveFailures++
                }
            }
        }
    }

    /**
     * Handle connection lost
     */
    private fun handleConnectionLost() {
        Log.w(TAG, "🔌 Connection lost!")
        _isConnected.value = false
        _connectionError.value = "Connection lost"
        onConnectionLost?.invoke()
    }

    /**
     * Handle connection restored
     */
    private fun handleConnectionRestored() {
        Log.d(TAG, "✅ Connection restored!")
        _isConnected.value = true
        _connectionError.value = null
        onConnectionRestored?.invoke()
    }

    /**
     * Set connection event callbacks
     */
    fun setConnectionCallbacks(
        onLost: (() -> Unit)? = null,
        onRestored: (() -> Unit)? = null
    ) {
        onConnectionLost = onLost
        onConnectionRestored = onRestored
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        connectionScope.cancel()
        healthCheckJob?.cancel()
        runBlocking {
            disconnect()
        }
    }
}