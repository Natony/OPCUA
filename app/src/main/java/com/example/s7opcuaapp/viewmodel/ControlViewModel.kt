package com.example.s7opcuaapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.s7opcuaapp.data.local.PrefsManager
import com.example.s7opcuaapp.data.repository.OPCUARepositoryImpl
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

    private val repoImpl = repository as OPCUARepositoryImpl
    private val functionCodeNodeIndex = 14

    // track connection state to prevent duplicate sessions
    private var connectionStarted = false

    init {
        // Quan sát tỉ lệ load, nhưng không tự động start kết nối
        viewModelScope.launch {
            repoImpl.observeLoadingPercent()
                .catch { err -> Log.e("ControlVM", "Error loading percent", err) }
                .collect { pct ->
                    Log.d("ControlVM", "Loading percent = $pct")
                    _uiState.update { it.copy(loadingPercent = pct) }
                }
        }
    }

    /**
     * Bắt đầu hoặc khởi động lại kết nối OPC UA với thiết bị hiện tại
     */
    fun startConnection() {
        if (connectionStarted) return
        connectionStarted = true

        viewModelScope.launch(Dispatchers.IO) {

            // Tiếp tục khởi kết nối mới
            prefsManager.getCurrentDevice()?.let { repoImpl.updateDevice(it) }
            try {
                Log.d("ControlVM", "Starting OPC UA connection...")
                repoImpl.start()
                observePlcData()
            } catch (e: Exception) {
                Log.e("ControlVM", "Failed to start connection", e)
                _uiState.update { it.copy(errorMessage = "Không thể kết nối: ${e.message}") }
                connectionStarted = false
            }
        }
    }

    private suspend fun observePlcData() {
        repoImpl.observePlcData()
            .flowOn(Dispatchers.IO)
            .catch { err ->
                Log.e("ControlVM", "Error observing PLC data", err)
                _uiState.update { it.copy(errorMessage = "Lỗi kết nối: ${err.message}") }
            }
            .collect { data ->
                _uiState.update { it.copy(plcData = data, errorMessage = null) }
                Log.d("ControlVM", "Data updated: ${data.bools.size} bools, ${data.ints.size} ints")
            }
    }

    /**
     * Dừng kết nối và hủy subscription
     */
    fun stopConnection() {
        if (!connectionStarted) return
        connectionStarted = false
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Chờ repository dừng hẳn
//                repoImpl.stopAndAwait()
//                Log.d("ControlVM", "Repository fully stopped")

                repoImpl.stop()
                Log.d("ControlVM", "Stopped OPC UA connection")
            } catch (e: Exception) {
                Log.e("ControlVM", "Error waiting for repository stop", e)
            }
        }
    }

    fun onToggleBoolean(index: Int, newValue: Boolean) {
        if (_uiState.value.isWriting) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isWriting = true, errorMessage = null) }
            try {
                repoImpl.writeBoolean(index, newValue)
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
        if (_uiState.value.isWriting) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isWriting = true, errorMessage = null) }
            try {
                repoImpl.writeInt(functionCodeNodeIndex, uiState.value.selectedFunction)
                listOf(5,6,7,8,9,10).forEach { idx ->
                    val txt = uiState.value.intInputs[idx]
                        ?: uiState.value.plcData.ints.getOrNull(idx)?.toString().orEmpty()
                    val v = txt.toIntOrNull() ?: 0
                    repoImpl.writeInt(idx, v)
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
        if (_uiState.value.isWriting) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isWriting = true, openDialogForIndex = null, errorMessage = null) }
            try {
                repoImpl.writeInt(index, value)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Ghi Integer thất bại: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isWriting = false) }
            }
        }
    }

    fun onStartPress(index: Int) {
        viewModelScope.launch {
            repoImpl.writeBoolean(index, true)
        }
    }

    fun onEndPress(index: Int) {
        // phát tín hiệu write Boolean false lên OPC UA
        viewModelScope.launch {
            repoImpl.writeBoolean(index, false)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun retryConnection() {
        viewModelScope.launch(Dispatchers.IO) {
            stopConnection()
            kotlinx.coroutines.delay(1000)
            startConnection()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopConnection()
    }

}
