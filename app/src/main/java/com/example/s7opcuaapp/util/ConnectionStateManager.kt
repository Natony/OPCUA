package com.example.s7opcuaapp.util

import android.util.Log
import com.example.s7opcuaapp.data.repository.OptimizedOPCUARepositoryImpl
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages connection state and retry logic
 * Extracted from ControlViewModel to reduce complexity
 */
@Singleton
class ConnectionStateManager @Inject constructor() {

    companion object {
        private const val TAG = "ConnectionStateManager"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 3000L
        private const val CONNECTION_TIMEOUT_MS = 30000L
        private const val HEALTH_CHECK_INTERVAL_MS = 5000L
    }

    // Connection states
    sealed class State {
        object Idle : State()
        data class Connecting(val attempt: Int = 1) : State()
        object Connected : State()
        data class Failed(val error: String, val attempt: Int = 0) : State()
        object Timeout : State()
        object Offline : State()
        data class MaxRetriesExceeded(val reason: String) : State()
    }

    // State management
    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    // Loading progress
    private val _loadingPercent = MutableStateFlow(0)
    val loadingPercent: StateFlow<Int> = _loadingPercent.asStateFlow()

    // Connection control
    private val connectionMutex = Mutex()
    @Volatile
    private var isConnectionActive = false
    @Volatile
    private var isOfflineMode = false
    private var connectionAttempts = 0

    // Monitoring
    private var healthCheckJob: Job? = null
    private val monitorScope = CoroutineScope(
        Dispatchers.IO + SupervisorJob() + CoroutineName("ConnectionMonitor")
    )

    // Callbacks
    private var onConnectionLost: (() -> Unit)? = null
    private var onConnectionRestored: (() -> Unit)? = null

    /**
     * Set connection callbacks
     */
    fun setCallbacks(
        onLost: (() -> Unit)? = null,
        onRestored: (() -> Unit)? = null
    ) {
        onConnectionLost = onLost
        onConnectionRestored = onRestored
    }

    /**
     * Start connection with retry logic
     */
    suspend fun connect(
        repository: OptimizedOPCUARepositoryImpl
    ): Boolean = connectionMutex.withLock {

        if (isConnectionActive) {
            Log.d(TAG, "Connection already in progress")
            return@withLock false
        }

        if (isOfflineMode) {
            Log.d(TAG, "In offline mode, skipping connection")
            return@withLock false
        }

        isConnectionActive = true
        connectionAttempts++

        Log.d(TAG, "Starting connection attempt $connectionAttempts/$MAX_RETRY_ATTEMPTS")

        _state.value = State.Connecting(connectionAttempts)
        _loadingPercent.value = 0

        try {
            // Monitor loading progress
            val loadingJob = monitorScope.launch {
                repository.observeLoadingPercent().collect { percent ->
                    Log.d(TAG, "Loading progress: $percent%")
                    _loadingPercent.value = percent

                    when (percent) {
                        100 -> {
                            Log.d(TAG, "Loading complete")
                            handleConnectionSuccess()
                        }
                        -1 -> {
                            Log.e(TAG, "Loading error")
                            handleConnectionError("Loading failed")
                        }
                    }
                }
            }

            // Start repository with timeout
            val connected = withTimeoutOrNull(CONNECTION_TIMEOUT_MS) {
                repository.start()

                // Wait for connection to establish
                delay(500)

                // Verify connection
                repository.isConnected()
            } ?: false

            loadingJob.cancel()

            if (connected) {
                handleConnectionSuccess()
                startHealthMonitoring(repository)
                return@withLock true
            } else {
                throw Exception("Connection verification failed")
            }

        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Connection timeout", e)
            handleConnectionTimeout()
            return@withLock false

        } catch (e: Exception) {
            Log.e(TAG, "Connection failed", e)
            handleConnectionError(e.message ?: "Unknown error")

            // Auto retry if not exceeded max attempts
            if (connectionAttempts < MAX_RETRY_ATTEMPTS && !isOfflineMode) {
                Log.d(TAG, "Will retry in ${RETRY_DELAY_MS}ms...")
                monitorScope.launch {
                    delay(RETRY_DELAY_MS)
                    connect(repository)
                }
            } else if (connectionAttempts >= MAX_RETRY_ATTEMPTS) {
                handleMaxRetriesExceeded()
            }

            return@withLock false

        } finally {
            isConnectionActive = false
        }
    }

    /**
     * Handle successful connection
     */
    private fun handleConnectionSuccess() {
        Log.d(TAG, "✅ Connection successful")

        connectionAttempts = 0
        _state.value = State.Connected
        _loadingPercent.value = 100

        onConnectionRestored?.invoke()
    }

    /**
     * Handle connection error
     */
    private fun handleConnectionError(error: String) {
        Log.e(TAG, "❌ Connection error: $error")

        _state.value = State.Failed(error, connectionAttempts)
        _loadingPercent.value = -1
    }

    /**
     * Handle connection timeout
     */
    private fun handleConnectionTimeout() {
        Log.w(TAG, "⏰ Connection timeout")

        _state.value = State.Timeout
        _loadingPercent.value = -1
    }

    /**
     * Handle max retries exceeded
     */
    private fun handleMaxRetriesExceeded() {
        Log.e(TAG, "Max retries exceeded")

        _state.value = State.MaxRetriesExceeded(
            "Failed after $MAX_RETRY_ATTEMPTS attempts"
        )
        _loadingPercent.value = -1
    }

    /**
     * Start health monitoring
     */
    private fun startHealthMonitoring(repository: OptimizedOPCUARepositoryImpl) {
        healthCheckJob?.cancel()

        healthCheckJob = monitorScope.launch {
            Log.d(TAG, "Starting health monitoring")

            var consecutiveFailures = 0
            val maxFailures = 3

            while (isActive && _state.value is State.Connected) {
                delay(HEALTH_CHECK_INTERVAL_MS)

                try {
                    val isHealthy = withTimeoutOrNull(2000L) {
                        repository.isConnected()
                    } ?: false

                    if (!isHealthy) {
                        consecutiveFailures++
                        Log.w(TAG, "Health check failed ($consecutiveFailures/$maxFailures)")

                        if (consecutiveFailures >= maxFailures) {
                            handleConnectionLost()
                            break
                        }
                    } else {
                        consecutiveFailures = 0
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Health check error", e)
                    consecutiveFailures++

                    if (consecutiveFailures >= maxFailures) {
                        handleConnectionLost()
                        break
                    }
                }
            }

            Log.d(TAG, "Health monitoring stopped")
        }
    }

    /**
     * Handle connection lost
     */
    private fun handleConnectionLost() {
        Log.w(TAG, "Connection lost")

        _state.value = State.Failed("Connection lost", 0)
        _loadingPercent.value = -1

        onConnectionLost?.invoke()

        // Reset for potential reconnection
        isConnectionActive = false
        connectionAttempts = 0
    }

    /**
     * Disconnect and cleanup
     */
    suspend fun disconnect() = connectionMutex.withLock {
        Log.d(TAG, "Disconnecting...")

        healthCheckJob?.cancel()
        healthCheckJob = null

        isConnectionActive = false
        connectionAttempts = 0
        isOfflineMode = false

        _state.value = State.Idle
        _loadingPercent.value = 0

        Log.d(TAG, "Disconnected")
    }

    /**
     * Enter offline mode
     */
    fun enterOfflineMode() {
        Log.d(TAG, "Entering offline mode")

        isOfflineMode = true
        isConnectionActive = false
        connectionAttempts = 0

        _state.value = State.Offline
        _loadingPercent.value = 100 // Show UI
    }

    /**
     * Exit offline mode
     */
    fun exitOfflineMode() {
        Log.d(TAG, "Exiting offline mode")

        isOfflineMode = false
        _state.value = State.Idle
    }

    /**
     * Reset connection state
     */
    fun reset() {
        Log.d(TAG, "Resetting connection state")

        healthCheckJob?.cancel()
        isConnectionActive = false
        connectionAttempts = 0
        isOfflineMode = false

        _state.value = State.Idle
        _loadingPercent.value = 0
    }

    /**
     * Check if can perform operations
     */
    fun canOperate(): Boolean {
        return _state.value is State.Connected && !isOfflineMode
    }

    /**
     * Get current attempt number
     */
    fun getCurrentAttempt(): Int = connectionAttempts

    /**
     * Get max attempts
     */
    fun getMaxAttempts(): Int = MAX_RETRY_ATTEMPTS

    /**
     * Cleanup resources
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up")

        healthCheckJob?.cancel()
        monitorScope.cancel()

        onConnectionLost = null
        onConnectionRestored = null
    }
}