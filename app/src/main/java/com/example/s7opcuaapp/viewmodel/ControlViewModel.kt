package com.example.s7opcuaapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.s7opcuaapp.data.local.PrefsManager
import com.example.s7opcuaapp.data.model.PlcData
import com.example.s7opcuaapp.data.repository.OptimizedOPCUARepositoryImpl
import com.example.s7opcuaapp.data.repository.S7Repository
import com.example.s7opcuaapp.ui.screen.control.ControlUiState
import com.example.s7opcuaapp.util.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Refactored ControlViewModel - Simplified and more maintainable
 * Delegates responsibilities to specialized managers
 */
@HiltViewModel
class ControlViewModel @Inject constructor(
    private val prefsManager: PrefsManager,
    private val repository: S7Repository,
    private val performanceMonitor: PerformanceMonitor,
    private val buttonLockConfig: ButtonLockConfig,
    private val statusLockConfig: StatusLockConfig,
    private val connectionManager: ConnectionManager,
    private val buttonManager: ButtonOperationManager,
    private val dataManager: PlcDataManager,
) : ViewModel() {

    companion object {
        private const val TAG = "ControlViewModel"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 3000L
        private const val INT_BUTTON_OFFSET = 200
        private const val SEND_ALL_BUTTON_INDEX = 999
    }

    // Connection states
    sealed class ConnectionState {
        object Idle : ConnectionState()
        data class Connecting(val attempt: Int = 1) : ConnectionState()
        object Connected : ConnectionState()
        data class Failed(val error: String, val attempt: Int = 0) : ConnectionState()
        object Timeout : ConnectionState()
        object Offline : ConnectionState()
        data class MaxRetriesExceeded(val reason: String) : ConnectionState()
    }

    // State flows
    val uiState: StateFlow<ControlUiState> = dataManager.uiState
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Repository access
    private val repoImpl = repository as OptimizedOPCUARepositoryImpl

    // Connection management
    private var connectionAttempts = 0
    private var isOfflineMode = false
    private var isShowingTimeoutDialog = false

    // Jobs
    private var connectionJob: Job? = null
    private var dataObservationJob: Job? = null
    private var monitoringJob: Job? = null

    init {
        setupConnectionStateObserver()
        setupButtonStateObserver()
    }

    /**
     * Setup connection state observer from ConnectionManager
     */
    private fun setupConnectionStateObserver() {
        viewModelScope.launch {
            connectionManager.state.collect { state ->
                updateConnectionState(state)
            }
        }
    }

    /**
     * Setup button state observer from ButtonOperationManager
     */
    private fun setupButtonStateObserver() {
        viewModelScope.launch {
            buttonManager.busyButtons.collect { busyButtons ->
                dataManager.updateBusyButtons(busyButtons)
            }
        }
    }

    /**
     * Update connection state based on ConnectionManager state
     */
    private fun updateConnectionState(managerState: ConnectionManager.State) {
        _connectionState.value = when (managerState) {
            is ConnectionManager.State.Idle -> ConnectionState.Idle
            is ConnectionManager.State.Connecting -> ConnectionState.Connecting(managerState.attempt)
            is ConnectionManager.State.Connected -> ConnectionState.Connected
            is ConnectionManager.State.Failed -> {
                if (managerState.canRetry && connectionAttempts < MAX_RETRY_ATTEMPTS) {
                    ConnectionState.Failed(managerState.reason, connectionAttempts)
                } else {
                    ConnectionState.MaxRetriesExceeded(managerState.reason)
                }
            }
            is ConnectionManager.State.Disconnected -> ConnectionState.Idle
        }
    }

    /**
     * Start connection to PLC
     */
    internal fun startConnection() {
        if (isOfflineMode) {
            Log.d(TAG, "In offline mode, skipping connection")
            return
        }

        connectionJob?.cancel()
        connectionJob = viewModelScope.launch {
            try {
                connectionAttempts++
                Log.d(TAG, "Starting connection attempt $connectionAttempts/$MAX_RETRY_ATTEMPTS")

                _connectionState.value = ConnectionState.Connecting(connectionAttempts)
                dataManager.updateLoadingPercent(0)
                dataManager.clearError()

                // Add proper exception handling for loading monitor
                val loadingJob = launch {
                    try {
                        repoImpl.observeLoadingPercent()
                            .catch { error ->
                                // Handle loading errors gracefully
                                Log.e(TAG, "Loading monitor error", error)
                                dataManager.updateLoadingPercent(-1)
                            }
                            .collect { percent ->
                                Log.d(TAG, "Loading progress: $percent%")
                                dataManager.updateLoadingPercent(percent)

                                when (percent) {
                                    100 -> Log.d(TAG, "Loading complete")
                                    -1 -> {
                                        // Don't throw exception, handle gracefully
                                        Log.e(TAG, "Loading error detected")
                                        handleConnectionError(Exception("Loading failed"))
                                    }
                                }
                            }
                    } catch (e: Exception) {
                        Log.e(TAG, "Loading collection error", e)
                        handleConnectionError(e)
                    }
                }

                // Start repository connection with proper timeout
                val connected = withTimeoutOrNull(30000L) {
                    repoImpl.start()
                    delay(500)
                    repoImpl.isConnected()
                } ?: false

                loadingJob.cancel() // Clean up loading job

                if (connected) {
                    handleConnectionSuccess()
                } else {
                    throw Exception("Connection verification failed")
                }

            } catch (e: TimeoutCancellationException) {
                handleConnectionTimeout()
            } catch (e: Exception) {
                handleConnectionError(e)
            }
        }
    }

    /**
     * Monitor loading progress from repository
     */
    private fun monitorLoadingProgress() {
        viewModelScope.launch {
            repoImpl.observeLoadingPercent()
                .timeout(30.seconds)
                .collect { percent ->
                    Log.d(TAG, "Loading progress: $percent%")
                    dataManager.updateLoadingPercent(percent)

                    when (percent) {
                        100 -> Log.d(TAG, "Loading complete")
                        -1 -> throw Exception("Loading error")
                    }
                }
        }
    }

    /**
     * Handle successful connection
     */
    private fun handleConnectionSuccess() {
        Log.d(TAG, "✅ Connection successful")

        connectionAttempts = 0
        _connectionState.value = ConnectionState.Connected
        dataManager.updateLoadingPercent(100)

        // Start data observation
        startDataObservation()

        // Start connection monitoring
        startConnectionMonitoring()
    }

    /**
     * Handle connection timeout
     */
    private fun handleConnectionTimeout() {
        Log.w(TAG, "Connection timeout")

        _connectionState.value = ConnectionState.Timeout
        dataManager.updateError("Connection timeout - PLC not responding")
        dataManager.updateLoadingPercent(-1)

        scheduleRetryIfNeeded()
    }

    /**
     * Handle connection error
     */
    private fun handleConnectionError(error: Exception) {
        Log.e(TAG, "Connection error", error)

        val errorMessage = when {
            error.message?.contains("timeout", true) == true -> "Connection timeout"
            error.message?.contains("refused", true) == true -> "Connection refused"
            else -> error.message ?: "Unknown error"
        }

        _connectionState.value = ConnectionState.Failed(errorMessage, connectionAttempts)
        dataManager.updateError(errorMessage)
        dataManager.updateLoadingPercent(-1)

        scheduleRetryIfNeeded()
    }

    /**
     * Schedule retry if needed
     */
    private fun scheduleRetryIfNeeded() {
        if (connectionAttempts < MAX_RETRY_ATTEMPTS && !isOfflineMode && !isShowingTimeoutDialog) {
            viewModelScope.launch {
                Log.d(TAG, "Will retry in ${RETRY_DELAY_MS}ms...")
                delay(RETRY_DELAY_MS)
                startConnection()
            }
        } else if (connectionAttempts >= MAX_RETRY_ATTEMPTS) {
            handleMaxRetriesExceeded()
        }
    }

    /**
     * Handle max retries exceeded
     */
    private fun handleMaxRetriesExceeded() {
        Log.e(TAG, "Max retries exceeded")

        isShowingTimeoutDialog = true
        _connectionState.value = ConnectionState.MaxRetriesExceeded(
            "Failed after $MAX_RETRY_ATTEMPTS attempts"
        )
        dataManager.updateError("Unable to connect to PLC")
    }

    /**
     * Start observing PLC data
     */
    private fun startDataObservation() {
        dataObservationJob?.cancel()
        dataObservationJob = viewModelScope.launch {
            dataManager.startDataObservation(repository)
        }
    }

    /**
     * Start connection monitoring
     */
    private fun startConnectionMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = connectionManager.startMonitoring(
            scope = viewModelScope,
            checkConnection = { repoImpl.isConnected() },
            onConnectionLost = { handleConnectionLost() }
        )
    }

    /**
     * Handle connection lost
     */
    private suspend fun handleConnectionLost() {
        Log.w(TAG, "Connection lost")

        _connectionState.value = ConnectionState.Failed("Connection lost", connectionAttempts)
        dataManager.updateError("Connection to PLC lost")

        // Release all pressed buttons
        buttonManager.releaseAllButtons()

        // Try to reconnect
        scheduleRetryIfNeeded()
    }

    /**
     * Continue in offline mode
     */
    fun continueOffline() {
        Log.d(TAG, "Continuing in offline mode")

        stopConnection()

        isOfflineMode = true
        isShowingTimeoutDialog = false
        _connectionState.value = ConnectionState.Offline

        dataManager.setOfflineMode(true)
    }

    /**
     * Reset connection
     */
    fun resetConnection() {
        viewModelScope.launch {
            Log.d(TAG, "Resetting connection")

            stopConnection()
            delay(1000)

            connectionAttempts = 0
            isOfflineMode = false
            isShowingTimeoutDialog = false

            startConnection()
        }
    }

    /**
     * Stop connection
     */
    fun stopConnection() {
        Log.d(TAG, "Stopping connection")

        connectionJob?.cancel()
        dataObservationJob?.cancel()
        monitoringJob?.cancel()

        connectionManager.stopMonitoring()
        dataManager.stopDataObservation()

        runBlocking {
            buttonManager.releaseAllButtons()
            repoImpl.stop()
        }

        _connectionState.value = ConnectionState.Idle
        dataManager.reset()
    }

    /**
     * Refresh connection state
     */
    fun refreshConnectionState() {
        val isConnected = try {
            repoImpl.isConnected()
        } catch (e: Exception) {
            false
        }

        if (isConnected && _connectionState.value !is ConnectionState.Connected) {
            _connectionState.value = ConnectionState.Connected
        }
    }

    /**
     * Reset connection attempts
     */
    fun resetConnectionAttempts() {
        connectionAttempts = 0
        isOfflineMode = false
    }

    /**
     * Dismiss timeout dialog
     */
    fun dismissTimeoutDialog() {
        isShowingTimeoutDialog = false
    }

    // ========== BUTTON OPERATIONS ==========

    /**
     * Toggle boolean value
     */
    fun onToggleBoolean(index: Int, newValue: Boolean) {
        if (!canPerformOperation()) return

        viewModelScope.launch {
            buttonManager.queueOperation(
                ButtonOperationManager.Operation.Toggle(index, newValue)
            )

            val result = buttonManager.executeWithRepository(
                ButtonOperationManager.Operation.Toggle(index, newValue),
                repository
            )

            if (result is ButtonOperationManager.Result.Error) {
                dataManager.updateError(result.message)
            }
        }
    }

    /**
     * Press button
     */
    fun onPressButton(index: Int): Boolean {
        if (!canPerformOperation()) return false

        viewModelScope.launch {
            buttonManager.queueOperation(
                ButtonOperationManager.Operation.Press(index)
            )

            buttonManager.executeWithRepository(
                ButtonOperationManager.Operation.Press(index),
                repository
            )
        }

        return true
    }

    /**
     * Release button
     */
    fun onReleaseButton(index: Int): Boolean {
        viewModelScope.launch {
            buttonManager.queueOperation(
                ButtonOperationManager.Operation.Release(index)
            )

            buttonManager.executeWithRepository(
                ButtonOperationManager.Operation.Release(index),
                repository
            )
        }

        return true
    }

    /**
     * Open number dialog
     */
    fun openNumberDialog(title: String, index: Int) {
        dataManager.showNumberDialog(title, index)
    }

    /**
     * Confirm number input
     */
    fun confirmNumber(index: Int, value: Int) {
        if (!canPerformOperation()) return

        viewModelScope.launch {
            val buttonIndex = index + INT_BUTTON_OFFSET

            buttonManager.queueOperation(
                ButtonOperationManager.Operation.WriteInt(index, value)
            )

            val result = buttonManager.executeWithRepository(
                ButtonOperationManager.Operation.WriteInt(index, value),
                repository
            )

            if (result is ButtonOperationManager.Result.Success) {
                dataManager.dismissDialog()
            } else if (result is ButtonOperationManager.Result.Error) {
                dataManager.updateError(result.message)
            }
        }
    }

    /**
     * Dismiss dialog
     */
    fun dismissDialog() {
        dataManager.dismissDialog()
    }

    /**
     * Function selected
     */
    fun onFunctionSelected(code: Int) {
        dataManager.updateSelectedFunction(code)
    }

    /**
     * Inline value change
     */
    fun onInlineValueChange(index: Int, text: String) {
        dataManager.updateInputValue(index, text)
    }

    /**
     * Send all values
     */
    fun onSendAll() {
        if (!canPerformOperation()) return

        val state = uiState.value

        // Check if Send All button is locked
        if (SEND_ALL_BUTTON_INDEX in state.lockedButtons) {
            Log.w(TAG, "Send All button is locked")
            return
        }

        viewModelScope.launch {
            dataManager.updateWritingState(true)

            try {
                // Write function code
                repository.writeInt(14, state.selectedFunction)

                // Write coordinate values
                listOf(5, 6, 7, 8, 9, 10).forEach { idx ->
                    val value = dataManager.getInputValue(idx).toIntOrNull() ?: 0
                    repository.writeInt(idx, value)
                }

                Log.d(TAG, "Send all completed")
            } catch (e: Exception) {
                Log.e(TAG, "Send all failed", e)
                dataManager.updateError("Send failed: ${e.message}")
            } finally {
                dataManager.updateWritingState(false)
            }
        }
    }

    /**
     * Check if can perform operation
     */
    private fun canPerformOperation(): Boolean {
        return when (_connectionState.value) {
            is ConnectionState.Offline -> {
                dataManager.updateError("Controls disabled in offline mode")
                false
            }
            is ConnectionState.Connected -> true
            else -> {
                dataManager.updateError("Not connected")
                false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopConnection()
        buttonManager.cleanup()
        dataManager.cleanup()
        performanceMonitor.cleanup()
    }
}