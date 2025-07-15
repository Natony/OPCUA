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
import com.example.s7opcuaapp.util.PerformanceMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class ControlViewModel @Inject constructor(
    private val prefsManager: PrefsManager,
    repository: S7Repository,
    private val performanceMonitor: PerformanceMonitor,
    private val buttonLockConfig: ButtonLockConfig
) : ViewModel() {

    private val _uiState = MutableStateFlow(ControlUiState())
    val uiState: StateFlow<ControlUiState> = _uiState.asStateFlow()

    private val repoImpl = repository as OptimizedOPCUARepositoryImpl
    private val functionCodeNodeIndex = 14

    // Connection management
    private val connectionMutex = Mutex()
    private var connectionStarted = false
    private var dataObservationJob: Job? = null

    // UI update throttling
    private val uiUpdateThrottle = 200L
    private var lastUiUpdateTime = 0L

    // Global processing lock - chỉ cho phép 1 operation tại 1 thời điểm
    private val globalProcessingLock = Mutex()
    private var currentProcessingButton: Int? = null

    companion object {
        const val BOOL_OFFSET = 0
        const val INT_OFFSET = 200
    }

    init {
        // Monitor UI state changes
        viewModelScope.launch {
            snapshotFlow { uiState.value }
                .collect {
                    performanceMonitor.recordUiRecomposition()
                }
        }

        // Observe loading percent
        viewModelScope.launch {
            repoImpl.observeLoadingPercent()
                .catch { err ->
                    Log.e("ControlVM", "Error observing loading percent", err)
                    _uiState.update { it.copy(errorMessage = "Loading error: ${err.message}") }
                }
                .collect { pct ->
                    Log.d("ControlVM", "Loading percent = $pct")
                    _uiState.update { it.copy(loadingPercent = pct) }
                }
        }
    }

    fun startConnection() {
        viewModelScope.launch {
            connectionMutex.withLock {
                if (connectionStarted) {
                    Log.d("ControlVM", "⚠️ Connection already started")
                    return@withLock
                }

                connectionStarted = true
                Log.d("ControlVM", "🚀 Starting connection...")

                try {
                    prefsManager.getCurrentDevice()?.let { device ->
                        repoImpl.updateDevice(device)
                    }

                    _uiState.update { it.copy(loadingPercent = 0, errorMessage = null) }
                    repoImpl.start()
                    startOptimizedDataObservation()

                } catch (e: Exception) {
                    Log.e("ControlVM", "Failed to start connection", e)
                    _uiState.update { it.copy(errorMessage = "Connection failed: ${e.message}") }
                    connectionStarted = false
                }
            }
        }
    }

    private suspend fun startOptimizedDataObservation() {
        dataObservationJob?.cancel()

        dataObservationJob = viewModelScope.launch {
            repoImpl.observePlcData()
                .flowOn(Dispatchers.Default)
                .distinctUntilChanged()
                .sample(uiUpdateThrottle)
                .catch { err ->
                    Log.e("ControlVM", "Data observation error", err)
                    _uiState.update { it.copy(errorMessage = "Data error: ${err.message}") }
                    connectionStarted = false
                }
                .collect { data ->
                    val now = System.currentTimeMillis()
                    if (now - lastUiUpdateTime >= uiUpdateThrottle) {
                        lastUiUpdateTime = now
                        updateUIWithPlcData(data)
                    }
                }
        }
    }

    private suspend fun updateUIWithPlcData(data: PlcData) {
        // Lấy active buttons từ PLC data
        val activeButtons = getActiveButtons(data)

        // Tính toán locked buttons
        val lockedButtons = if (currentProcessingButton != null) {
            // Nếu đang xử lý, khóa tất cả nút trừ nút đang xử lý
            val allButtons = (0..14).toSet() + (203..230).toSet()
            currentProcessingButton?.let {
                allButtons - it
            } ?: allButtons
        } else {
            // Nếu không đang xử lý, sử dụng button lock config bình thường
            val busyButtons = if (currentProcessingButton != null) {
                setOf(currentProcessingButton!!)
            } else {
                emptySet()
            }
            buttonLockConfig.getLockedButtons(activeButtons, busyButtons)
        }

        _uiState.update {
            it.copy(
                plcData = data,
                errorMessage = null,
                lockedButtons = lockedButtons,
                busyButtons = if (currentProcessingButton != null) setOf(currentProcessingButton!!) else emptySet()
            )
        }
    }

    private fun getActiveButtons(data: PlcData): Set<Int> {
        val active = mutableSetOf<Int>()

        // Check bool buttons
        data.bools.forEachIndexed { index, value ->
            if (value) active.add(index)
        }

        // Check int buttons (3-4) for non-zero values
        listOf(3, 4).forEach { index ->
            if ((data.ints.getOrNull(index) ?: 0) != 0) {
                active.add(index + INT_OFFSET)
            }
        }

        return active
    }

    /**
     * Generic button action với global lock
     */
    private suspend fun executeButtonAction(
        buttonIndex: Int,
        actionName: String,
        action: suspend () -> Unit
    ): Boolean {
        // Try to acquire global lock
        if (!globalProcessingLock.tryLock()) {
            Log.d("ControlVM", "❌ Cannot $actionName button $buttonIndex - another operation in progress")
            return false
        }

        try {
            val state = _uiState.value

            // Check basic conditions
            if (!connectionStarted) {
                Log.d("ControlVM", "❌ Cannot $actionName - not connected")
                return false
            }

            // Check if button is already locked
            if (buttonIndex in state.lockedButtons) {
                Log.d("ControlVM", "❌ Button $buttonIndex is locked")
                return false
            }

            // Mark this button as processing
            currentProcessingButton = buttonIndex

            // Update UI immediately to show all other buttons as locked
            _uiState.update { currentState ->
                val allButtons = (0..14).toSet() + (203..230).toSet()
                currentState.copy(
                    isWriting = true,
                    busyButtons = setOf(buttonIndex),
                    lockedButtons = allButtons - buttonIndex // Lock all except current
                )
            }

            Log.d("ControlVM", "🔄 Starting $actionName for button $buttonIndex")

            // Execute the action
            action()

            Log.d("ControlVM", "✅ Completed $actionName for button $buttonIndex")
            return true

        } catch (e: Exception) {
            Log.e("ControlVM", "❌ Error in $actionName for button $buttonIndex", e)
            _uiState.update { it.copy(errorMessage = "Operation failed: ${e.message}") }
            return false

        } finally {
            // Clear processing state
            currentProcessingButton = null

            // Update UI state
            _uiState.update { it.copy(isWriting = false) }

            // Release global lock
            globalProcessingLock.unlock()

            // Force immediate UI update
            updateUIWithPlcData(_uiState.value.plcData)
        }
    }

    fun onToggleBoolean(index: Int, newValue: Boolean) {
        viewModelScope.launch {
            // Xử lý đặc biệt cho Emergency Stop
            if (index == 10) { // Emergency stop
                executeEmergencyStop(newValue)
            } else {
                executeButtonAction(index, "toggle") {
                    repoImpl.writeBoolean(index, newValue)
                }
            }
        }
    }

    // Thêm hàm mới để xử lý Emergency Stop
    private suspend fun executeEmergencyStop(activate: Boolean) {
        try {
            // Không lock emergency stop button
            _uiState.update { currentState ->
                currentState.copy(
                    isWriting = true,
                    busyButtons = setOf(10)
                )
            }

            // Write to PLC
            repoImpl.writeBoolean(10, activate)

            // Delay nhỏ để PLC xử lý
            delay(100)

        } catch (e: Exception) {
            Log.e("ControlVM", "Error in emergency stop", e)
            _uiState.update { it.copy(errorMessage = "Emergency stop failed: ${e.message}") }
        } finally {
            _uiState.update { it.copy(isWriting = false) }
        }
    }
    fun onStartPress(index: Int) {
        viewModelScope.launch {
            try {
                // Kiểm tra điều kiện cơ bản
                val state = _uiState.value
                if (!connectionStarted || index in state.lockedButtons) {
                    Log.d("ControlVM", "❌ Cannot press button $index - not ready or locked")
                    return@launch
                }

                // Kiểm tra xem có operation nào đang chạy không
                if (currentProcessingButton != null) {
                    Log.d("ControlVM", "❌ Cannot press button $index - button $currentProcessingButton is processing")
                    return@launch
                }

                // Mark as processing
                currentProcessingButton = index

                // Update UI ngay lập tức
                _uiState.update { currentState ->
                    val allButtons = (0..14).toSet() + (203..230).toSet()
                    currentState.copy(
                        isWriting = true,
                        busyButtons = setOf(index),
                        lockedButtons = allButtons - index
                    )
                }

                // Write true với timeout
                withTimeout(3000) { // 3 giây timeout
                    repoImpl.writeBoolean(index, true)
                }

                Log.d("ControlVM", "✅ Button $index pressed")

            } catch (e: Exception) {
                Log.e("ControlVM", "❌ Error in onStartPress for button $index", e)

                // QUAN TRỌNG: Clear state khi có lỗi
                if (currentProcessingButton == index) {
                    currentProcessingButton = null
                    _uiState.update {
                        it.copy(
                            isWriting = false,
                            busyButtons = emptySet(),
                            errorMessage = "Press failed: ${e.message}"
                        )
                    }

                    // Force update UI
                    updateUIWithPlcData(_uiState.value.plcData)
                }
            }
        }
    }

    fun onEndPress(index: Int) {
        viewModelScope.launch {
            try {
                // Chỉ xử lý nếu đây là button đang được press
                if (currentProcessingButton == index && connectionStarted) {
                    // Write false với timeout
                    withTimeout(3000) { // 3 giây timeout
                        repoImpl.writeBoolean(index, false)
                    }
                    Log.d("ControlVM", "✅ Button $index released")
                }
            } catch (e: Exception) {
                Log.e("ControlVM", "❌ Error in onEndPress for button $index", e)
            } finally {
                // LUÔN LUÔN clear state khi release
                if (currentProcessingButton == index) {
                    currentProcessingButton = null
                    _uiState.update {
                        it.copy(
                            isWriting = false,
                            busyButtons = emptySet()
                        )
                    }

                    // Force update UI
                    updateUIWithPlcData(_uiState.value.plcData)
                }
            }
        }
    }

    fun resetProcessingState() {
        viewModelScope.launch {
            Log.d("ControlVM", "🔄 Resetting processing state")

            currentProcessingButton = null

            // Unlock global lock nếu đang bị lock
            if (globalProcessingLock.isLocked) {
                try {
                    globalProcessingLock.unlock()
                } catch (e: Exception) {
                    // Ignore
                }
            }

            _uiState.update {
                it.copy(
                    isWriting = false,
                    busyButtons = emptySet(),
                    lockedButtons = emptySet()
                )
            }

            // Force UI update
            updateUIWithPlcData(_uiState.value.plcData)
        }
    }

    fun confirmNumber(index: Int, value: Int) {
        viewModelScope.launch {
            val buttonIndex = index + INT_OFFSET
            executeButtonAction(buttonIndex, "write int") {
                repoImpl.writeInt(index, value)
            }
            _uiState.update { it.copy(openDialogForIndex = null) }
        }
    }

    fun onSendAll() {
        viewModelScope.launch {
            // Use a special index for "send all" operation
            executeButtonAction(999, "send all") {
                // Write function code
                repoImpl.writeInt(functionCodeNodeIndex, uiState.value.selectedFunction)

                // Write coordinate values
                listOf(5, 6, 7, 8, 9, 10).forEach { idx ->
                    val txt = uiState.value.intInputs[idx]
                        ?: uiState.value.plcData.ints.getOrNull(idx)?.toString() ?: "0"
                    val value = txt.toIntOrNull() ?: 0
                    repoImpl.writeInt(idx, value)
                }
            }
        }
    }

    fun onFunctionSelected(code: Int) {
        _uiState.update { it.copy(selectedFunction = code) }
    }

    fun onInlineValueChange(index: Int, text: String) {
        _uiState.update {
            it.copy(intInputs = it.intInputs.toMutableMap().apply { put(index, text) })
        }
    }

    fun openNumberDialog(index: Int) {
        // Check if we can open dialog
        if (currentProcessingButton != null) {
            Log.d("ControlVM", "Cannot open dialog - operation in progress")
            return
        }
        _uiState.update { it.copy(openDialogForIndex = index) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(openDialogForIndex = null) }
    }

    fun stopConnection() {
        viewModelScope.launch {
            connectionMutex.withLock {
                if (!connectionStarted) return@withLock

                Log.d("ControlVM", "🛑 Stopping connection...")
                connectionStarted = false

                try {
                    dataObservationJob?.cancel()
                    dataObservationJob = null
                    repoImpl.stop()
                    currentProcessingButton = null
                    _uiState.update { it.copy(loadingPercent = 0) }
                } catch (e: Exception) {
                    Log.e("ControlVM", "Error stopping connection", e)
                }
            }
        }
    }

    fun restartConnection() {
        viewModelScope.launch {
            stopConnection()
            delay(3000)
            startConnection()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        dataObservationJob?.cancel()
        GlobalScope.launch {
            try {
                repoImpl.stop()
            } catch (e: Exception) {
                Log.e("ControlVM", "Error stopping in onCleared", e)
            }
        }
    }
}