// app/src/main/java/com/example/s7opcuaapp/viewmodel/ControlViewModel.kt

package com.example.s7opcuaapp.viewmodel

import android.util.Log
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.s7opcuaapp.data.local.PrefsManager
import com.example.s7opcuaapp.data.model.PlcData
import com.example.s7opcuaapp.data.repository.OptimizedOPCUARepositoryImpl
import com.example.s7opcuaapp.data.repository.S7Repository
import com.example.s7opcuaapp.ui.screen.control.ControlUiState
import com.example.s7opcuaapp.util.ButtonLockConfig
import com.example.s7opcuaapp.util.NetworkConnectivity
import com.example.s7opcuaapp.util.PerformanceMonitor
import com.example.s7opcuaapp.util.StatusLockConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class ControlViewModel @Inject constructor(
    private val prefsManager: PrefsManager,
    repository: S7Repository,
    private val performanceMonitor: PerformanceMonitor,
    private val buttonLockConfig: ButtonLockConfig,
    private val statusLockConfig: StatusLockConfig,
    private val networkConnectivity: NetworkConnectivity
) : ViewModel() {

    // ==================== STATE MANAGEMENT ====================
    private val _uiState = MutableStateFlow(ControlUiState())
    val uiState: StateFlow<ControlUiState> = _uiState.asStateFlow()

    // Repository reference
    private val repoImpl = repository as OptimizedOPCUARepositoryImpl
    private val functionCodeNodeIndex = 14

    // ==================== CONNECTION MANAGEMENT ====================
    private val connectionMutex = Mutex()
    private val connectionState = AtomicBoolean(false)
    private var dataObservationJob: Job? = null
    private val monitoringJobs = mutableListOf<Job>()

    // Connection retry configuration
    private var connectionAttempts = 0
    private val maxConnectionAttempts = 3
    private var lastConnectionAttempt = 0L
    private val connectionRetryDelay = 5000L // 5 seconds

    // ==================== RATE LIMITING ====================
    private val stateUpdateLimiter = RateLimiter(100)      // General state updates
    private val loadingUpdateLimiter = RateLimiter(200)    // Loading percent updates
    private val errorMessageLimiter = RateLimiter(1000)    // Error messages
    private val dataUpdateLimiter = RateLimiter(300)       // PLC data updates

    // ==================== BUTTON STATE MANAGEMENT ====================
    private val buttonStates = ConcurrentHashMap<Int, ButtonState>()
    private val globalProcessingMutex = Mutex()
    private val buttonOperationMutex = Mutex()

    @Volatile
    private var currentProcessingButton: Int? = null
    private val pressedButtons = Collections.synchronizedSet(mutableSetOf<Int>())

    // ==================== CONSTANTS ====================
    companion object {
        private const val TAG = "ControlVM"
        const val BOOL_OFFSET = 0
        const val INT_OFFSET = 200
        const val MIN_BUTTON_ACTION_INTERVAL = 100L
        const val SEND_ALL_BUTTON_INDEX = 999
        const val CONNECTION_TIMEOUT = 30000L // 30 seconds
        const val DATA_OBSERVATION_TIMEOUT = 60000L // 60 seconds
    }

    // ==================== DATA CLASSES ====================
    private data class ButtonState(
        val index: Int,
        val isPressed: Boolean,
        val lastActionTime: Long,
        val operationJob: Job? = null
    )

    private class RateLimiter(private val minInterval: Long) {
        @Volatile
        private var lastUpdateTime = 0L

        fun tryUpdate(action: () -> Unit): Boolean {
            val now = System.currentTimeMillis()
            if (now - lastUpdateTime >= minInterval) {
                lastUpdateTime = now
                try {
                    action()
                    return true
                } catch (e: Exception) {
                    Log.e(TAG, "Error in rate limited action", e)
                    return false
                }
            }
            return false
        }

        fun reset() {
            lastUpdateTime = 0L
        }
    }

    // ==================== INITIALIZATION ====================
    init {
        delay(100)
        initializeMonitoring()
        observeLoadingProgress()
        observeNetworkState()
    }

    private fun observeNetworkState() {
        viewModelScope.launch {
            networkConnectivity.observeNetworkAvailability()
                .collect { isAvailable ->
                    if (!isAvailable && connectionState.get()) {
                        Log.w(TAG, "Network lost, stopping connection")
                        stopConnection()
                    }
                }
        }
    }

    private fun initializeMonitoring() {
        // Monitor UI state changes with proper error handling
        monitoringJobs += viewModelScope.launch {
            try {
                snapshotFlow { uiState.value }
                    .distinctUntilChanged()
                    .sample(500) // Max 2 updates per second
                    .catch { e ->
                        Log.e(TAG, "Error monitoring UI state", e)
                    }
                    .collect {
                        try {
                            performanceMonitor.recordUiRecomposition()
                        } catch (e: Exception) {
                            Log.w(TAG, "Error recording UI recomposition", e)
                        }
                    }
            } catch (e: CancellationException) {
                throw e // Re-throw to properly cancel
            } catch (e: Exception) {
                Log.e(TAG, "Fatal error in UI monitoring", e)
            }
        }
    }

    private fun observeLoadingProgress() {
        monitoringJobs += viewModelScope.launch {
            try {
                withTimeout(DATA_OBSERVATION_TIMEOUT) {
                    repoImpl.observeLoadingPercent()
                        // Bỏ distinctUntilChanged() vì StateFlow đã tự động distinct
                        // vì StateFlow/SharedFlow đã có những tính năng này sẵn
                        .catch { err ->
                            Log.e(TAG, "Error observing loading percent", err)
                            updateLoadingError("Loading error: ${err.message}")
                            emit(0) // Reset to 0 on error
                        }
                        .collect { pct ->
                            updateLoadingPercent(pct)
                        }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Loading observation timeout")
                updateLoadingError("Loading timeout - please check connection")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Fatal error observing loading", e)
                updateLoadingError("Loading failed: ${e.message}")
            }
        }
    }

    // ==================== CONNECTION METHODS ====================
    fun startConnection() {
        viewModelScope.launch(Dispatchers.Default) {
            connectionMutex.withLock {
                if (connectionState.get()) {
                    Log.d(TAG, "Connection already active")
                    return@withLock
                }

                // Check network connectivity first
                if (!networkConnectivity.isNetworkAvailable()) {
                    withContext(Dispatchers.Main) {
                        updateConnectionError("No network connection available")
                    }
                    return@withLock
                }

                // Check max attempts
                if (connectionAttempts >= maxConnectionAttempts) {
                    updateConnectionError("Max connection attempts reached. Please restart the app.")
                    return@withLock
                }

                connectionAttempts++
                connectionState.set(true)
                lastConnectionAttempt = System.currentTimeMillis()

                Log.d(TAG, "Starting connection attempt $connectionAttempts/$maxConnectionAttempts")

                try {
                    withTimeout(CONNECTION_TIMEOUT) {
                        // Update device configuration
                        prefsManager.getCurrentDevice()?.let { device ->
                            repoImpl.updateDevice(device)
                        }

                        // Reset UI state
                        _uiState.safeUpdate {
                            it.copy(
                                loadingPercent = 0,
                                errorMessage = null,
                                isWriting = false,
                                busyButtons = emptySet()
                            )
                        }

                        // Start repository
                        repoImpl.start()

                        // Start data observation
                        startOptimizedDataObservation()

                        // Reset attempts on success
                        connectionAttempts = 0

                        Log.d(TAG, "Connection established successfully")
                    }
                } catch (e: TimeoutCancellationException) {
                    Log.e(TAG, "Connection timeout")
                    handleConnectionFailure("Connection timeout. Please check your network.")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Connection failed", e)
                    handleConnectionFailure("Connection failed: ${e.message}")
                }
            }
        }
    }

    private fun handleConnectionFailure(errorMessage: String) {
        connectionState.set(false)
        updateConnectionError(errorMessage)

        // Schedule retry if under max attempts
        if (connectionAttempts < maxConnectionAttempts) {
            viewModelScope.launch {
                delay(connectionRetryDelay)
                if (!connectionState.get()) {
                    startConnection()
                }
            }
        }
    }

    private suspend fun startOptimizedDataObservation() {
        dataObservationJob?.cancel()

        dataObservationJob = viewModelScope.launch {
            repoImpl.observePlcData()
                .flowOn(Dispatchers.Default)
                // Bỏ distinctUntilChanged() nếu observePlcData() return StateFlow
                .sample(300) // Throttle to ~3 updates per second
                .catch { err ->
                    Log.e(TAG, "Data observation error", err)
                    updateDataError("Data error: ${err.message}")
                    connectionState.set(false)
                }
                .collect { data ->
                    updatePlcData(data)
                }
        }
    }

    // ==================== STATE UPDATE METHODS ====================
    private fun updatePlcData(data: PlcData) {
        dataUpdateLimiter.tryUpdate {
            val activeButtons = getActiveButtons(data)
            val currentStatus = data.ints.getOrNull(0) ?: 0

            // Calculate locked buttons based on current state
            val lockedButtons = when {
                currentProcessingButton != null -> {
                    // Lock all buttons except the one being processed
                    val allButtons = (0..14).toSet() + (203..230).toSet() + setOf(SEND_ALL_BUTTON_INDEX)
                    currentProcessingButton?.let { allButtons - it } ?: allButtons
                }
                else -> {
                    // Use status-based locking
                    statusLockConfig.getLockedButtonsForStatus(currentStatus)
                }
            }

            _uiState.safeUpdate { currentState ->
                currentState.copy(
                    plcData = data,
                    errorMessage = if (currentState.errorMessage?.contains("Data error") == true) null
                    else currentState.errorMessage,
                    lockedButtons = lockedButtons,
                    busyButtons = currentProcessingButton?.let { setOf(it) } ?: emptySet()
                )
            }
        }
    }

    private fun updateLoadingPercent(percent: Int) {
        loadingUpdateLimiter.tryUpdate {
            _uiState.safeUpdate { currentState ->
                // Chỉ update nếu giá trị thực sự khác
                if (currentState.loadingPercent != percent) {
                    Log.d(TAG, "Loading progress: $percent%")
                    currentState.copy(loadingPercent = percent)
                } else {
                    currentState // Không thay đổi state nếu giá trị giống nhau
                }
            }

            // Clear loading errors when complete
            if (percent >= 100) {
                clearLoadingError()
            }
        }
    }

    private fun updateLoadingError(message: String?) {
        errorMessageLimiter.tryUpdate {
            _uiState.safeUpdate { it.copy(errorMessage = message, loadingPercent = 0) }
        }
    }

    private fun updateConnectionError(message: String) {
        errorMessageLimiter.tryUpdate {
            _uiState.safeUpdate {
                it.copy(
                    errorMessage = message,
                    loadingPercent = 0,
                    isWriting = false
                )
            }
        }
    }

    private fun updateDataError(message: String) {
        errorMessageLimiter.tryUpdate {
            _uiState.safeUpdate { it.copy(errorMessage = message) }
        }
    }

    private fun clearLoadingError() {
        _uiState.safeUpdate { currentState ->
            if (currentState.errorMessage?.contains("Loading") == true ||
                currentState.errorMessage?.contains("loading") == true) {
                currentState.copy(errorMessage = null)
            } else {
                currentState
            }
        }
    }

    // ==================== BUTTON OPERATIONS ====================
    fun onToggleBoolean(index: Int, newValue: Boolean) {
        viewModelScope.launch {
            // Special handling for emergency stop
            if (index == 10) { // Emergency stop
                executeEmergencyStop(newValue)
                return@launch
            }

            // Normal button toggle
            executeButtonAction(index, "toggle boolean") {
                repoImpl.writeBoolean(index, newValue)
            }
        }
    }

    fun onPressButton(index: Int) {
        viewModelScope.launch {
            buttonOperationMutex.withLock {
                performButtonPress(index)
            }
        }
    }

    fun onReleaseButton(index: Int) {
        viewModelScope.launch {
            buttonOperationMutex.withLock {
                performButtonRelease(index)
            }
        }
    }

    private suspend fun performButtonPress(index: Int): Boolean {
        // Check if already pressed
        if (pressedButtons.contains(index)) {
            Log.w(TAG, "Button $index already pressed")
            return false
        }

        // Check if locked
        if (index in _uiState.value.lockedButtons) {
            Log.w(TAG, "Button $index is locked")
            return false
        }

        // Rate limiting check
        val currentState = buttonStates[index]
        val now = System.currentTimeMillis()
        if (currentState != null && (now - currentState.lastActionTime) < MIN_BUTTON_ACTION_INTERVAL) {
            Log.w(TAG, "Button $index action too fast")
            return false
        }

        return try {
            // Add to pressed set
            pressedButtons.add(index)

            // Update UI
            _uiState.safeUpdate { it.copy(busyButtons = it.busyButtons + index) }

            // Write to PLC
            repoImpl.writeBoolean(index, true)

            // Update button state
            buttonStates[index] = ButtonState(
                index = index,
                isPressed = true,
                lastActionTime = now,
                operationJob = null
            )

            Log.d(TAG, "Button $index pressed successfully")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error pressing button $index", e)
            pressedButtons.remove(index)
            _uiState.safeUpdate { it.copy(busyButtons = it.busyButtons - index) }
            false
        }
    }

    private suspend fun performButtonRelease(index: Int): Boolean {
        if (!pressedButtons.contains(index)) {
            Log.w(TAG, "Button $index not pressed, ignoring release")
            return false
        }

        return try {
            // Remove from pressed set
            pressedButtons.remove(index)

            // Update UI
            _uiState.safeUpdate { it.copy(busyButtons = it.busyButtons - index) }

            // Write to PLC
            repoImpl.writeBoolean(index, false)

            // Update button state
            val currentState = buttonStates[index]
            if (currentState != null) {
                buttonStates[index] = currentState.copy(
                    isPressed = false,
                    lastActionTime = System.currentTimeMillis()
                )
            }

            Log.d(TAG, "Button $index released successfully")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Error releasing button $index", e)
            false
        }
    }

    private suspend fun executeButtonAction(
        buttonIndex: Int,
        actionName: String,
        action: suspend () -> Unit
    ): Boolean {
        // Try to acquire global processing lock
        if (!globalProcessingMutex.tryLock()) {
            Log.d(TAG, "Another operation in progress, cannot $actionName for button $buttonIndex")
            return false
        }

        try {
            // Check connection
            if (!connectionState.get()) {
                showError("Not connected to PLC")
                return false
            }

            // Check if button is locked
            if (buttonIndex in _uiState.value.lockedButtons) {
                Log.d(TAG, "Button $buttonIndex is locked")
                return false
            }

            // Set current processing button
            currentProcessingButton = buttonIndex

            // Update UI to show processing
            _uiState.safeUpdate { currentState ->
                val allButtons = (0..14).toSet() + (203..230).toSet() + setOf(SEND_ALL_BUTTON_INDEX)
                currentState.copy(
                    isWriting = true,
                    busyButtons = setOf(buttonIndex),
                    lockedButtons = allButtons - buttonIndex
                )
            }

            Log.d(TAG, "Executing $actionName for button $buttonIndex")

            // Execute the action with timeout
            withTimeout(5000L) { // 5 second timeout
                action()
            }

            Log.d(TAG, "Successfully completed $actionName for button $buttonIndex")
            return true

        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Timeout executing $actionName for button $buttonIndex")
            showError("Operation timeout")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error executing $actionName for button $buttonIndex", e)
            showError("Operation failed: ${e.message}")
            return false
        } finally {
            // Clear processing state
            currentProcessingButton = null

            // Update UI
            _uiState.safeUpdate { it.copy(isWriting = false) }

            // Release lock
            globalProcessingMutex.unlock()

            // Force UI update
            updatePlcData(_uiState.value.plcData)
        }
    }

    private suspend fun executeEmergencyStop(activate: Boolean) {
        try {
            _uiState.safeUpdate { it.copy(isWriting = true, busyButtons = setOf(10)) }

            // Emergency stop doesn't need lock - always allowed
            repoImpl.writeBoolean(10, activate)

            // Small delay for PLC to process
            delay(100)

        } catch (e: Exception) {
            Log.e(TAG, "Emergency stop failed", e)
            showError("Emergency stop failed!")
        } finally {
            _uiState.safeUpdate { it.copy(isWriting = false, busyButtons = it.busyButtons - 10) }
        }
    }

    // ==================== INT VALUE OPERATIONS ====================
    fun openNumberDialog(title: String, index: Int) {
        if (currentProcessingButton != null) {
            Log.d(TAG, "Cannot open dialog - operation in progress")
            return
        }

        _uiState.safeUpdate {
            it.copy(
                openDialogForIndex = index,
                dialogTitle = title
            )
        }
    }

    fun dismissDialog() {
        _uiState.safeUpdate { it.copy(openDialogForIndex = null) }
    }

    fun confirmNumber(index: Int, value: Int) {
        viewModelScope.launch {
            val buttonIndex = index + INT_OFFSET
            executeButtonAction(buttonIndex, "write int") {
                repoImpl.writeInt(index, value)
            }
            dismissDialog()
        }
    }

    fun onInlineValueChange(index: Int, text: String) {
        stateUpdateLimiter.tryUpdate {
            _uiState.safeUpdate { currentState ->
                currentState.copy(
                    intInputs = currentState.intInputs.toMutableMap().apply {
                        put(index, text)
                    }
                )
            }
        }
    }

    fun onFunctionSelected(code: Int) {
        _uiState.safeUpdate { it.copy(selectedFunction = code) }
    }

    fun onSendAll() {
        viewModelScope.launch {
            // Check if Send All is locked by status
            if (SEND_ALL_BUTTON_INDEX in _uiState.value.lockedButtons) {
                Log.w(TAG, "Send All button is locked by current status")
                showError("Send All is not available in current status")
                return@launch
            }

            executeButtonAction(SEND_ALL_BUTTON_INDEX, "send all") {
                // Write function code
                repoImpl.writeInt(functionCodeNodeIndex, _uiState.value.selectedFunction)

                // Write coordinate values
                val state = _uiState.value
                listOf(5, 6, 7, 8, 9, 10).forEach { idx ->
                    val textValue = state.intInputs[idx]
                        ?: state.plcData.ints.getOrNull(idx)?.toString()
                        ?: "0"
                    val intValue = textValue.toIntOrNull() ?: 0
                    repoImpl.writeInt(idx, intValue)
                }
            }
        }
    }

    // ==================== UTILITY METHODS ====================
    private fun getActiveButtons(data: PlcData): Set<Int> {
        val active = mutableSetOf<Int>()

        // Check bool buttons
        data.bools.forEachIndexed { index, value ->
            if (value) active.add(index)
        }

        // Check int buttons
        listOf(3, 4).forEach { index ->
            if ((data.ints.getOrNull(index) ?: 0) != 0) {
                active.add(index + INT_OFFSET)
            }
        }

        return active.toSet()
    }

    private fun showError(message: String) {
        errorMessageLimiter.tryUpdate {
            _uiState.safeUpdate { it.copy(errorMessage = message) }

            // Auto-clear error after 5 seconds
            viewModelScope.launch {
                delay(5000)
                _uiState.safeUpdate { currentState ->
                    if (currentState.errorMessage == message) {
                        currentState.copy(errorMessage = null)
                    } else {
                        currentState
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.safeUpdate { it.copy(errorMessage = null) }
    }

    // ==================== CONNECTION CONTROL ====================
    fun stopConnection() {
        viewModelScope.launch {
            connectionMutex.withLock {
                if (!connectionState.get()) {
                    Log.d(TAG, "Connection already stopped")
                    return@withLock
                }

                Log.d(TAG, "Stopping connection...")
                connectionState.set(false)

                try {
                    // Release all pressed buttons
                    releaseAllButtons()

                    // Cancel observation job
                    dataObservationJob?.cancel()
                    dataObservationJob = null

                    // Stop repository
                    withTimeout(5000L) {
                        repoImpl.stop()
                    }

                    // Reset UI state
                    _uiState.safeUpdate {
                        it.copy(
                            loadingPercent = 0,
                            isWriting = false,
                            busyButtons = emptySet(),
                            lockedButtons = emptySet()
                        )
                    }

                    // Reset connection attempts
                    connectionAttempts = 0
                    currentProcessingButton = null

                    Log.d(TAG, "Connection stopped successfully")

                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping connection", e)
                }
            }
        }
    }

    fun restartConnection() {
        viewModelScope.launch {
            stopConnection()
            delay(3000) // Wait 3 seconds before restart
            startConnection()
        }
    }

    fun releaseAllButtons() {
        viewModelScope.launch {
            buttonOperationMutex.withLock {
                Log.d(TAG, "Releasing all pressed buttons")

                val buttonsCopy = pressedButtons.toList()

                coroutineScope {
                    buttonsCopy.map { index ->
                        async {
                            try {
                                performButtonRelease(index)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error releasing button $index", e)
                            }
                        }
                    }.awaitAll()
                }

                // Clear all states
                buttonStates.clear()
                pressedButtons.clear()
                currentProcessingButton = null
            }
        }
    }

    // ==================== LIFECYCLE ====================
    override fun onCleared() {
        Log.d(TAG, "ViewModel clearing...")

        // Cancel all monitoring jobs
        monitoringJobs.forEach { it.cancel() }
        monitoringJobs.clear()

        // Cancel view model scope
        viewModelScope.cancel()

        // Stop connection with GlobalScope for cleanup
        GlobalScope.launch {
            try {
                withTimeout(3000L) {
                    connectionMutex.withLock {
                        connectionState.set(false)
                        dataObservationJob?.cancel()
                        repoImpl.stop()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during cleanup", e)
            }
        }

        super.onCleared()
        Log.d(TAG, "ViewModel cleared")
    }
}

// ==================== EXTENSION FUNCTIONS ====================
private inline fun <T> MutableStateFlow<T>.safeUpdate(transform: (T) -> T) {
    val currentValue = value
    value = try {
        transform(currentValue)
    } catch (e: Exception) {
        Log.e("ControlVM", "Error updating state", e)
        currentValue
    }
}