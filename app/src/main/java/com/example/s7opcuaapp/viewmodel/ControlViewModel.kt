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
    private val performanceMonitor: PerformanceMonitor // ADD THIS
) : ViewModel() {

    private val _uiState = MutableStateFlow(ControlUiState())
    val uiState: StateFlow<ControlUiState> = _uiState.asStateFlow()

    // CHANGE THIS LINE
    private val repoImpl = repository as OptimizedOPCUARepositoryImpl
    private val functionCodeNodeIndex = 14

    // Connection management
    private val connectionMutex = Mutex()
    private var connectionStarted = false
    private var dataObservationJob: Job? = null

    // Thêm throttling cho UI updates
    private val uiUpdateThrottle = 500L // 500ms between UI updates
    private var lastUiUpdateTime = 0L

    companion object {
        // Offset để phân biệt các loại button
        const val BOOL_OFFSET = 0
        const val INT_OFFSET = 200
    }
    // Track UI recomposition
    init {
        // Monitor UI state changes for recomposition tracking
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

    /**
     * Start connection with optimized data flow
     */
    fun startConnection() {
        viewModelScope.launch {
            connectionMutex.withLock {
                if (connectionStarted) {
                    Log.d("ControlVM", "⚠️ Connection already started")
                    return@withLock
                }

                connectionStarted = true
                Log.d("ControlVM", "🚀 Starting optimized connection...")

                try {
                    // Update device info
                    prefsManager.getCurrentDevice()?.let { device ->
                        repoImpl.updateDevice(device)
                        Log.d("ControlVM", "Updated device: ${device.name}")
                    }

                    // Reset state
                    _uiState.update { it.copy(loadingPercent = 0, errorMessage = null) }

                    // Start repository
                    repoImpl.start()

                    // Start observing optimized data flow
                    startOptimizedDataObservation()

                } catch (e: Exception) {
                    Log.e("ControlVM", "Failed to start connection", e)
                    _uiState.update { it.copy(errorMessage = "Connection failed: ${e.message}") }
                    connectionStarted = false
                }
            }
        }
    }

    /**
     * Optimized data observation with selective updates
     */
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

                        // Tính toán locked buttons
                        val currentBusy = _uiState.value.busyButtons
                        val lockedButtons = calculateLockedButtons(data, currentBusy)

                        _uiState.update {
                            it.copy(
                                plcData = data,
                                errorMessage = null,
                                lockedButtons = lockedButtons
                            )
                        }
                        Log.v("ControlVM", "UI updated with PLC data")
                    }
                }
        }
    }    /**
     * Stop connection
     */
    fun stopConnection() {
        viewModelScope.launch {
            connectionMutex.withLock {
                if (!connectionStarted) {
                    Log.d("ControlVM", "⚠️ Connection not started")
                    return@withLock
                }

                Log.d("ControlVM", "🛑 Stopping connection...")
                connectionStarted = false

                try {
                    dataObservationJob?.cancel()
                    dataObservationJob = null
                    repoImpl.stop()
                    _uiState.update { it.copy(loadingPercent = 0) }
                    Log.d("ControlVM", "✅ Connection stopped")
                } catch (e: Exception) {
                    Log.e("ControlVM", "Error stopping connection", e)
                }
            }
        }
    }

    /**
     * Restart connection
     */
    fun restartConnection() {
        Log.d("ControlVM", "🔄 Restarting connection...")
        viewModelScope.launch {
            stopConnection()
            delay(3000) // Wait for cleanup
            startConnection()
        }
    }

    private fun calculateLockedButtons(data: PlcData, busyButtons: Set<Int>): Set<Int> {
        val locked = mutableSetOf<Int>()

        // Tìm nút đang active
        var hasActiveButton = false
        var activeButtonIndex: Int? = null

        // Check manual mode buttons (0-3)
        for (i in 0..3) {
            if (data.bools.getOrNull(i) == true || i in busyButtons) {
                hasActiveButton = true
                activeButtonIndex = i
                break
            }
        }

        // Check auto mode bool buttons (6-9)
        if (!hasActiveButton) {
            for (i in 6..9) {
                if (data.bools.getOrNull(i) == true || i in busyButtons) {
                    hasActiveButton = true
                    activeButtonIndex = i
                    break
                }
            }
        }

        // Check int buttons (3-4) - cần map với INT_OFFSET
        if (!hasActiveButton) {
            for (i in 3..4) {
                if ((data.ints.getOrNull(i) ?: 0) != 0 || (i + INT_OFFSET) in busyButtons) {
                    hasActiveButton = true
                    activeButtonIndex = i + INT_OFFSET
                    break
                }
            }
        }

        // Nếu có nút active, khóa tất cả trừ nút đó
        if (hasActiveButton && activeButtonIndex != null) {
            // Khóa tất cả bool buttons
            locked.addAll(0..14)
            // Khóa tất cả int buttons (với offset)
            locked.addAll((0..30).map { it + INT_OFFSET })

            // Mở khóa nút đang active
            locked.remove(activeButtonIndex)
        }

        return locked
    }

    // UI Actions with performance tracking
    fun onToggleBoolean(index: Int, newValue: Boolean) {
        val state = _uiState.value
        if (state.isWriting || !connectionStarted) return

        // Check if button is locked
        if (index in state.lockedButtons) {
            Log.d("ControlVM", "Button $index is locked")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            // Mark button as busy
            _uiState.update {
                it.copy(
                    isWriting = true,
                    errorMessage = null,
                    busyButtons = it.busyButtons + index
                )
            }

            try {
                repoImpl.writeBoolean(index, newValue)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Write failed: ${e.message}") }
            } finally {
                _uiState.update {
                    it.copy(
                        isWriting = false,
                        busyButtons = it.busyButtons - index
                    )
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

    fun onSendAll() {
        if (_uiState.value.isWriting || !connectionStarted) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isWriting = true, errorMessage = null) }
            try {
                // Write function code
                repoImpl.writeInt(functionCodeNodeIndex, uiState.value.selectedFunction)

                // Write coordinate values
                listOf(5, 6, 7, 8, 9, 10).forEach { idx ->
                    val txt = uiState.value.intInputs[idx]
                        ?: uiState.value.plcData.ints.getOrNull(idx)?.toString() ?: "0"
                    val value = txt.toIntOrNull() ?: 0
                    repoImpl.writeInt(idx, value)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Send failed: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isWriting = false) }
            }
        }
    }

    fun openNumberDialog(index: Int) {
        _uiState.update { it.copy(openDialogForIndex = index) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(openDialogForIndex = null) }
    }

    fun confirmNumber(index: Int, value: Int) {
        val state = _uiState.value
        if (state.isWriting || !connectionStarted) return

        // Check if int button is locked (với offset)
        if ((index + INT_OFFSET) in state.lockedButtons) {
            Log.d("ControlVM", "Int button $index is locked")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    isWriting = true,
                    openDialogForIndex = null,
                    errorMessage = null,
                    busyButtons = it.busyButtons + (index + INT_OFFSET)
                )
            }

            try {
                repoImpl.writeInt(index, value)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Write failed: ${e.message}") }
            } finally {
                _uiState.update {
                    it.copy(
                        isWriting = false,
                        busyButtons = it.busyButtons - (index + INT_OFFSET)
                    )
                }
            }
        }
    }

    fun onStartPress(index: Int) {
        if (!connectionStarted) return
        viewModelScope.launch {
            try {
                repoImpl.writeBoolean(index, true)
            } catch (e: Exception) {
                Log.e("ControlVM", "Error in onStartPress", e)
            }
        }
    }

    fun onEndPress(index: Int) {
        if (!connectionStarted) return
        viewModelScope.launch {
            try {
                repoImpl.writeBoolean(index, false)
            } catch (e: Exception) {
                Log.e("ControlVM", "Error in onEndPress", e)
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ControlVM", "ViewModel cleared")
        dataObservationJob?.cancel()

        // Stop in GlobalScope since viewModelScope is cancelled
        GlobalScope.launch {
            try {
                repoImpl.stop()
            } catch (e: Exception) {
                Log.e("ControlVM", "Error stopping in onCleared", e)
            }
        }
    }
}