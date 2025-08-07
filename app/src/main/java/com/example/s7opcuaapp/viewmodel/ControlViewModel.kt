package com.example.s7opcuaapp.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.s7opcuaapp.data.local.PrefsManager
import com.example.s7opcuaapp.data.model.PlcData
import com.example.s7opcuaapp.domain.usecase.*
import com.example.s7opcuaapp.ui.screen.control.ControlUiState
import com.example.s7opcuaapp.util.StatusLockConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Simplified ControlViewModel using BaseViewModel and Use Cases
 */
@HiltViewModel
class ControlViewModel @Inject constructor(
    private val buttonControlUseCase: ButtonControlUseCase,
    private val integerWriteUseCase: IntegerWriteUseCase,
    private val plcDataSyncUseCase: PlcDataSyncUseCase,
    private val connectionStateUseCase: ConnectionStateUseCase,
    private val buttonLockCalculationUseCase: ButtonLockCalculationUseCase,
    private val statusLockConfig: StatusLockConfig,
    private val prefsManager: PrefsManager
) : BaseViewModel<ControlUiState>() {

    override val initialState = ControlUiState()

    private var dataObservationJob: Job? = null
    private var connectionJob: Job? = null

    // Connection state management
    sealed class ConnectionState {
        object Idle : ConnectionState()
        data class Connecting(val attempt: Int = 1) : ConnectionState()
        object Connected : ConnectionState()
        data class Failed(val error: String, val attempt: Int = 0) : ConnectionState()
        object Offline : ConnectionState()
        object Timeout : ConnectionState()
        data class MaxRetriesExceeded(val reason: String) : ConnectionState()
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Override base methods for loading and error
    override fun setLoading(isLoading: Boolean) {
        updateState { copy(isLoading = isLoading) }
    }

    override fun setError(message: String?) {
        updateState { copy(errorMessage = message) }
    }

    init {
        observeConnectionState()
        observePlcData()
    }

    /**
     * Start connection to PLC
     */
    fun startConnection() {
        if (connectionJob?.isActive == true) return

        connectionJob = execute {
            _connectionState.value = ConnectionState.Connecting()

            connectionStateUseCase.connect()
                .fold(
                    onSuccess = {
                        _connectionState.value = ConnectionState.Connected
                        clearError()
                    },
                    onFailure = { error ->
                        _connectionState.value = ConnectionState.Failed(
                            error.message ?: "Connection failed",
                            1
                        )
                        setError(error.message)
                    }
                )
        }
    }

    /**
     * Observe connection state changes
     */
    private fun observeConnectionState() {
        execute {
            connectionStateUseCase.observeConnectionState()
                .collect { isConnected ->
                    if (!isConnected && _connectionState.value is ConnectionState.Connected) {
                        _connectionState.value = ConnectionState.Failed("Connection lost", 0)
                        setError("Connection lost to PLC")
                    }
                }
        }
    }

    /**
     * Observe PLC data
     */
    private fun observePlcData() {
        dataObservationJob?.cancel()
        dataObservationJob = execute {
            plcDataSyncUseCase.observePlcData()
                .collect { state ->
                    when (state) {
                        is PlcDataSyncUseCase.PlcDataState.Success -> {
                            updatePlcData(state.data)
                        }
                        is PlcDataSyncUseCase.PlcDataState.Error -> {
                            setError(state.message)
                        }
                        is PlcDataSyncUseCase.PlcDataState.Loading -> {
                            setLoading(true)
                        }
                    }
                }
        }
    }

    /**
     * Update UI with PLC data
     */
    private fun updatePlcData(data: PlcData) {
        val activeButtons = buttonLockCalculationUseCase.getActiveButtons(data)
        val currentStatus = data.ints.getOrNull(0) ?: 0

        val lockedButtons = buttonLockCalculationUseCase.calculateLockedButtons(
            plcData = data,
            currentStatus = currentStatus,
            activeButtons = activeButtons,
            busyButtons = currentState.busyButtons,
            currentProcessingButton = null
        )

        updateState {
            copy(
                plcData = data,
                lockedButtons = lockedButtons,
                errorMessage = null,
                loadingPercent = 100,
                isLoading = false
            )
        }
    }

    /**
     * Toggle boolean value
     */
    fun onToggleBoolean(index: Int, newValue: Boolean) {
        if (_connectionState.value !is ConnectionState.Connected) {
            setError("Not connected to PLC")
            return
        }

        execute {
            updateState { copy(busyButtons = busyButtons + index) }

            try {
                buttonControlUseCase.toggleBoolean(index, newValue)
                    .fold(
                        onSuccess = { clearError() },
                        onFailure = { setError(it.message) }
                    )
            } finally {
                updateState { copy(busyButtons = busyButtons - index) }
            }
        }
    }

    /**
     * Press button
     */
    fun onPressButton(index: Int): Boolean {
        if (_connectionState.value !is ConnectionState.Connected) {
            setError("Not connected to PLC")
            return false
        }

        val canPress = buttonControlUseCase.canPressButton(
            buttonIndex = index,
            currentStatus = currentState.plcData.ints.getOrNull(0) ?: 0,
            lockedButtons = currentState.lockedButtons,
            busyButtons = currentState.busyButtons
        )

        if (!canPress) return false

        execute {
            updateState { copy(busyButtons = busyButtons + index) }
            buttonControlUseCase.pressButton(index)
        }

        return true
    }

    /**
     * Release button
     */
    fun onReleaseButton(index: Int): Boolean {
        execute {
            buttonControlUseCase.releaseButton(index)
                .also {
                    updateState { copy(busyButtons = busyButtons - index) }
                }
        }
        return true
    }

    /**
     * Write integer value
     */
    fun confirmNumber(index: Int, value: Int) {
        executeWithLoading {
            integerWriteUseCase.writeInteger(index, value)
                .fold(
                    onSuccess = {
                        updateState { copy(openDialogForIndex = null) }
                    },
                    onFailure = { setError(it.message) }
                )
        }
    }

    /**
     * Send all values
     */
    fun onSendAll() {
        val sendAllIndex = StatusLockConfig.SEND_ALL_BUTTON_INDEX
        if (sendAllIndex in currentState.lockedButtons) {
            setError("Send All button is locked")
            return
        }

        executeWithLoading {
            val values = mutableMapOf<Int, Int>()

            // Add function code
            values[14] = currentState.selectedFunction

            // Add coordinate values
            listOf(5, 6, 7, 8, 9, 10).forEach { idx ->
                val text = currentState.intInputs[idx]
                    ?: currentState.plcData.ints.getOrNull(idx)?.toString()
                    ?: "0"
                values[idx] = text.toIntOrNull() ?: 0
            }

            integerWriteUseCase.writeMultipleIntegers(values)
                .fold(
                    onSuccess = { clearError() },
                    onFailure = { setError(it.message) }
                )
        }
    }

    // Dialog management
    fun openNumberDialog(title: String, index: Int) {
        updateState {
            copy(
                openDialogForIndex = index,
                dialogTitle = title
            )
        }
    }

    fun dismissDialog() {
        updateState { copy(openDialogForIndex = null) }
    }

    fun dismissTimeoutDialog() {
        // Implement if needed
    }

    // Other state updates
    fun onFunctionSelected(code: Int) {
        updateState { copy(selectedFunction = code) }
    }

    fun onInlineValueChange(index: Int, text: String) {
        updateState {
            copy(intInputs = intInputs.toMutableMap().apply { put(index, text) })
        }
    }

    /**
     * Continue in offline mode
     */
    fun continueOffline() {
        _connectionState.value = ConnectionState.Offline
        updateState {
            copy(
                loadingPercent = 100,
                errorMessage = null,
                lockedButtons = (0..14).toSet() + (203..204).toSet() + setOf(999)
            )
        }
    }

    /**
     * Stop connection
     */
    fun stopConnection() {
        connectionJob?.cancel()
        dataObservationJob?.cancel()
        execute {
            connectionStateUseCase.disconnect()
        }
    }

    fun resetConnectionAttempts() {
        // Reset logic here
    }

    fun refreshConnectionState() {
        if (connectionStateUseCase.isConnected()) {
            _connectionState.value = ConnectionState.Connected
        }
    }

    /**
     * Retry connection
     */
    fun retryConnection() {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Idle
            delay(500)
            startConnection()
        }
    }

    fun resetConnection() {
        stopConnection()
        retryConnection()
    }

    override fun onCleared() {
        super.onCleared()
        dataObservationJob?.cancel()
        connectionJob?.cancel()
        execute {
            connectionStateUseCase.disconnect()
        }
    }
}