package com.example.s7opcuaapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.s7opcuaapp.data.local.PrefsManager
import com.example.s7opcuaapp.data.repository.S7Repository
import com.example.s7opcuaapp.ui.screen.control.ControlUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.util.Log
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class ControlViewModel @Inject constructor(
    private val prefsManager: PrefsManager,
    repository: S7Repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ControlUiState())
    val uiState: StateFlow<ControlUiState> = _uiState

    private val repo = repository
    private val functionCodeNodeIndex = 14

    // Synchronization để tránh race condition khi start/stop nhanh
    private val connectionMutex = Mutex()
    private var connectionStarted = false
    private var dataObservationJob: Job? = null

    init {
        // Quan sát tỉ lệ load với proper error handling
        viewModelScope.launch {
            repo.observeLoadingPercent()
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
     * Thread-safe start connection với proper synchronization
     */
    fun startConnection() {
        viewModelScope.launch {
            connectionMutex.withLock {
                if (connectionStarted) {
                    Log.d("ControlVM", "⚠️ Connection already started/starting, skipping")
                    return@withLock
                }

                connectionStarted = true
                Log.d("ControlVM", "🚀 Starting PLC connection...")

                try {
                    // Update device info from preferences
                    prefsManager.getCurrentDevice()?.let { device ->
                        repo.updateDevice(device)
                        Log.d("ControlVM", "Updated device: ${device.name} @ ${device.ipAddress}:${device.port}")
                    }

                    // Reset loading state và error
                    _uiState.update { it.copy(loadingPercent = 0, errorMessage = null) }

                    // Start repository connection
                    repo.start()

                    // Start observing PLC data
                    startDataObservation()

                } catch (e: Exception) {
                    Log.e("ControlVM", "Failed to start connection", e)
                    _uiState.update { it.copy(errorMessage = "Không thể kết nối: ${e.message}") }
                    connectionStarted = false
                }
            }
        }
    }

    /**
     * Riêng biệt data observation để có thể restart dễ dàng
     */
    private suspend fun startDataObservation() {
        // Cancel previous observation if exists
        dataObservationJob?.cancel()

        dataObservationJob = viewModelScope.launch {
            repo.observePlcData()
                .flowOn(Dispatchers.IO)
                .catch { err ->
                    Log.e("ControlVM", "Error observing PLC data", err)
                    _uiState.update { it.copy(errorMessage = "Lỗi kết nối: ${err.message}") }
                    connectionStarted = false // Allow restart on error
                }
                .collect { data ->
                    _uiState.update { it.copy(plcData = data, errorMessage = null) }
                    Log.d("ControlVM", "Data updated: ${data.bools.size} bools, ${data.ints.size} ints")
                }
        }
    }

    /**
     * Thread-safe stop connection với proper cleanup
     */
    fun stopConnection() {
        viewModelScope.launch {
            connectionMutex.withLock {
                if (!connectionStarted) {
                    Log.d("ControlVM", "⚠️ Connection not started, nothing to stop")
                    return@withLock
                }

                Log.d("ControlVM", "🛑 Stopping PLC connection...")
                connectionStarted = false

                try {
                    // Cancel data observation first
                    dataObservationJob?.cancel()
                    dataObservationJob = null

                    // Stop repository
                    repo.stop()

                    // Reset loading state when stopped
                    _uiState.update { it.copy(loadingPercent = 0) }

                    Log.d("ControlVM", "✅ PLC connection stopped successfully")

                } catch (e: Exception) {
                    Log.e("ControlVM", "Error stopping connection", e)
                }
            }
        }
    }

    /**
     * Restart connection với proper sequencing và extended delays
     */
    fun restartConnection() {
        Log.d("ControlVM", "🔄 Restarting connection...")
        viewModelScope.launch {
            // Stop first
            stopConnection()

            // Wait longer for complete cleanup (repository cần 2s để cleanup session)
            delay(3000) // Tăng từ 1.5s lên 3s

            // Then start
            startConnection()
        }
    }

    /**
     * Check connection status
     */
    fun isConnectionActive(): Boolean {
        return connectionStarted
    }

    fun onToggleBoolean(index: Int, newValue: Boolean) {
        if (_uiState.value.isWriting || !connectionStarted) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isWriting = true, errorMessage = null) }
            try {
                repo.writeBoolean(index, newValue)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Ghi Boolean thất bại: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isWriting = false) }
            }
        }
    }

    fun onFunctionSelected(code: Int) {
        _uiState.update { it.copy(selectedFunction = code) }
    }

    fun onInlineValueChange(index: Int, text: String) {
        _uiState.update { it.copy(intInputs = it.intInputs.toMutableMap().apply { put(index, text) }) }
    }

    fun onSendAll() {
        if (_uiState.value.isWriting || !connectionStarted) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isWriting = true, errorMessage = null) }
            try {
                repo.writeInt(functionCodeNodeIndex, uiState.value.selectedFunction)
                listOf(5,6,7,8,9,10).forEach { idx ->
                    val txt = uiState.value.intInputs[idx]
                        ?: uiState.value.plcData.ints.getOrNull(idx)?.toString().orEmpty()
                    val v = txt.toIntOrNull() ?: 0
                    repo.writeInt(idx, v)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Ghi thất bại: ${e.message}") }
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
        if (_uiState.value.isWriting || !connectionStarted) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isWriting = true, openDialogForIndex = null, errorMessage = null) }
            try {
                repo.writeInt(index, value)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Ghi Integer thất bại: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isWriting = false) }
            }
        }
    }

    fun onStartPress(index: Int) {
        if (!connectionStarted) return
        viewModelScope.launch {
            try {
                repo.writeBoolean(index, true)
            } catch (e: Exception) {
                Log.e("ControlVM", "Error in onStartPress", e)
            }
        }
    }

    fun onEndPress(index: Int) {
        if (!connectionStarted) return
        viewModelScope.launch {
            try {
                repo.writeBoolean(index, false)
            } catch (e: Exception) {
                Log.e("ControlVM", "Error in onEndPress", e)
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun retryConnection() {
        Log.d("ControlVM", "🔄 Retrying connection...")
        restartConnection()
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ControlVM", "ViewModel cleared, stopping connection")
        // Cancel all jobs first
        dataObservationJob?.cancel()
        // Stop connection (launch in GlobalScope since viewModelScope is cancelled)
        kotlinx.coroutines.GlobalScope.launch {
            try {
                repo.stop()
            } catch (e: Exception) {
                Log.e("ControlVM", "Error stopping in onCleared", e)
            }
        }
    }
}