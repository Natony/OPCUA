package com.example.s7opcuaapp.domain.connection

import android.util.Log
import com.example.s7opcuaapp.core.dispatchers.DispatcherProvider
import com.example.s7opcuaapp.data.model.DeviceEntity
import com.example.s7opcuaapp.data.repository.OptimizedOPCUARepositoryImpl
import com.example.s7opcuaapp.util.ConnectionTimeoutManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Implementation of ConnectionManager that handles PLC connection lifecycle
 */
@Singleton
class ConnectionManagerImpl @Inject constructor(
    private val repository: OptimizedOPCUARepositoryImpl,
    private val timeoutManager: ConnectionTimeoutManager,
    private val dispatchers: DispatcherProvider
) : ConnectionManager {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _loadingProgress = MutableStateFlow(0)
    override val loadingProgress: StateFlow<Int> = _loadingProgress.asStateFlow()

    private val connectionMutex = Mutex()
    // Fixed: Create scope with SupervisorJob
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)

    private var connectionJob: Job? = null
    private var monitorJob: Job? = null
    private var retryCount = 0
    private var isOfflineMode = false

    companion object {
        private const val TAG = "ConnectionManager"
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY = 2000L
        private const val MONITOR_INTERVAL = 5000L
        private const val CONNECTION_TIMEOUT = 30000L
    }

    override suspend fun connect(device: DeviceEntity): Result<Unit> = connectionMutex.withLock {
        if (_connectionState.value is ConnectionState.Connected) {
            Log.d(TAG, "Already connected")
            return Result.success(Unit)
        }

        return try {
            performConnection(device)
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed", e)
            Result.failure(e)
        }
    }

    private suspend fun performConnection(device: DeviceEntity): Result<Unit> {
        retryCount++
        _connectionState.value = ConnectionState.Connecting(retryCount)
        _loadingProgress.value = 0

        connectionJob?.cancel()
        connectionJob = scope.launch {
            try {
                // Update repository with device
                repository.updateDevice(device)

                // Start connection
                repository.start()

                // Monitor loading progress WITHOUT timeout on the flow
                launch {
                    repository.observeLoadingPercent()
                        .collect { percent ->
                            _loadingProgress.value = percent

                            when (percent) {
                                100 -> {
                                    _connectionState.value = ConnectionState.Connected
                                    retryCount = 0
                                    startConnectionMonitoring()
                                }
                                -1 -> {
                                    if (_connectionState.value !is ConnectionState.Connected) {
                                        handleConnectionError(Exception("Connection error from repository"))
                                    }
                                }
                            }
                        }
                }

                // Separate timeout monitoring
                withTimeout(CONNECTION_TIMEOUT) {
                    // Wait for connection state to change
                    while (_connectionState.value is ConnectionState.Connecting) {
                        delay(100)

                        // Check if repository is connected
                        if (repository.isConnected()) {
                            // Force update if loading tracker is slow
                            if (_loadingProgress.value < 100) {
                                _loadingProgress.value = 100
                            }
                            _connectionState.value = ConnectionState.Connected
                            break
                        }
                    }
                }

            } catch (e: TimeoutCancellationException) {
                handleTimeout()
            } catch (e: Exception) {
                handleConnectionError(e)
            }
        }

        // Wait for connection result
        return connectionJob?.let { job ->
            job.join()
            when (val state = _connectionState.value) {
                is ConnectionState.Connected -> Result.success(Unit)
                is ConnectionState.Failed -> Result.failure(Exception(state.error))
                is ConnectionState.MaxRetriesExceeded -> Result.failure(Exception(state.reason))
                else -> Result.failure(Exception("Connection failed"))
            }
        } ?: Result.failure(Exception("Connection job failed to start"))
    }

    private fun handleTimeout() {
        Log.e(TAG, "Connection timeout (attempt $retryCount/$MAX_RETRIES)")

        if (retryCount < MAX_RETRIES) {
            _connectionState.value = ConnectionState.Failed(
                "Connection timeout",
                retryCount
            )
            scheduleRetry()
        } else {
            _connectionState.value = ConnectionState.MaxRetriesExceeded(
                "Failed after $MAX_RETRIES attempts"
            )
        }
    }

    private fun handleConnectionError(error: Exception) {
        Log.e(TAG, "Connection error (attempt $retryCount/$MAX_RETRIES)", error)

        if (retryCount < MAX_RETRIES) {
            _connectionState.value = ConnectionState.Failed(
                error.message ?: "Unknown error",
                retryCount
            )
            scheduleRetry()
        } else {
            _connectionState.value = ConnectionState.MaxRetriesExceeded(
                "Failed after $MAX_RETRIES attempts: ${error.message}"
            )
        }
    }

    private fun scheduleRetry() {
        scope.launch {
            delay(RETRY_DELAY)
            if (_connectionState.value !is ConnectionState.Connected && !isOfflineMode) {
                repository.getCurrentDevice()?.let { device ->
                    connect(device)
                }
            }
        }
    }

    private fun startConnectionMonitoring() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (isActive) {
                delay(MONITOR_INTERVAL)

                if (!repository.isConnected()) {
                    Log.w(TAG, "Connection lost detected")
                    _connectionState.value = ConnectionState.Failed("Connection lost", 0)
                    _loadingProgress.value = -1

                    // Auto reconnect if not in offline mode
                    if (!isOfflineMode) {
                        repository.getCurrentDevice()?.let { device ->
                            connect(device)
                        }
                    }
                }
            }
        }
    }

    override suspend fun disconnect() {
        connectionJob?.cancelAndJoin()
        monitorJob?.cancelAndJoin()

        try {
            repository.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
        }

        _connectionState.value = ConnectionState.Idle
        _loadingProgress.value = 0
        retryCount = 0
    }

    override fun isConnected(): Boolean {
        return _connectionState.value is ConnectionState.Connected && repository.isConnected()
    }

    override suspend fun resetConnection() {
        Log.d(TAG, "Resetting connection")
        disconnect()
        delay(1000)
        retryCount = 0

        repository.getCurrentDevice()?.let { device ->
            connect(device)
        }
    }

    override fun setOfflineMode(enabled: Boolean) {
        Log.d(TAG, "Offline mode: $enabled")
        isOfflineMode = enabled

        if (enabled) {
            connectionJob?.cancel()
            monitorJob?.cancel()
            _connectionState.value = ConnectionState.Offline
            _loadingProgress.value = 0
        }
    }

    // Extension function for getting current device
    private fun OptimizedOPCUARepositoryImpl.getCurrentDevice(): DeviceEntity? {
        // This would need to be implemented in the repository
        // For now, return null
        return null
    }
}