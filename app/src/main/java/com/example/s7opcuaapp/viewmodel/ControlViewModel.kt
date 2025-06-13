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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

@HiltViewModel
class ControlViewModel @Inject constructor(
    private val prefsManager: PrefsManager,
    repository: S7Repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ControlUiState())
    val uiState: StateFlow<ControlUiState> = _uiState

    private val repoImpl = repository as OPCUARepositoryImpl

    init {
        initializeConnection()
    }

    /**
     * Initialize connection trên background thread
     */
    private fun initializeConnection() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("ControlVM", "🚀 Initializing OPC UA connection...")

                // Start repository (kết nối và subscribe)
                repoImpl.start()

                // Observe data trên background thread với throttling
                repoImpl.observePlcData()
                    .distinctUntilChanged() // Chỉ emit khi có thay đổi thực sự
                    .flowOn(Dispatchers.IO)
                    .catch { error ->
                        Log.e("ControlVM", "❌ Error observing PLC data", error)
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Lỗi kết nối: ${error.message}"
                        )
                    }
                    .collect { newData ->
                        // Update UI state
                        _uiState.value = _uiState.value.copy(
                            plcData = newData,
                            errorMessage = null // Clear error khi có data
                        )
                        Log.d("ControlVM", "📊 Data updated: ${newData.bools.size} bools, ${newData.ints.size} ints")
                    }

            } catch (e: Exception) {
                Log.e("ControlVM", "💥 Failed to initialize connection", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Không thể kết nối: ${e.message}"
                )
            }
        }
    }

    /**
     * Toggle Boolean với error handling tốt hơn
     */
    fun onToggleBoolean(index: Int, newValue: Boolean) {
        // Prevent double-tap
        if (_uiState.value.isWriting) {
            Log.w("ControlVM", "⚠️ Write operation already in progress")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isWriting = true, errorMessage = null)

            try {
                Log.d("ControlVM", "✏️ Writing Boolean[$index] = $newValue")
                repoImpl.writeBoolean(index, newValue)
                Log.d("ControlVM", "✅ Boolean write successful")

            } catch (e: Exception) {
                Log.e("ControlVM", "❌ Boolean write failed", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Ghi Boolean thất bại: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isWriting = false)
            }
        }
    }

    /**
     * Dialog management
     */
    fun onOpenDialogForField(fieldId: String) {
        _uiState.value = _uiState.value.copy(openDialogForField = fieldId)
        Log.d("ControlVM", "📝 Opening dialog for $fieldId")
    }

    fun onDismissDialog() {
        _uiState.value = _uiState.value.copy(openDialogForField = null)
        Log.d("ControlVM", "❌ Dialog dismissed")
    }

    /**
     * Write Integer với validation
     */
    fun onConfirmNumber(fieldId: String, value: Int) {
        val index = fieldId.split(" ").lastOrNull()?.toIntOrNull()
        if (index == null) {
            Log.e("ControlVM", "❌ Invalid field ID: $fieldId")
            _uiState.value = _uiState.value.copy(
                errorMessage = "ID field không hợp lệ: $fieldId",
                openDialogForField = null
            )
            return
        }

        // Prevent double-tap
        if (_uiState.value.isWriting) {
            Log.w("ControlVM", "⚠️ Write operation already in progress")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(
                isWriting = true,
                openDialogForField = null,
                errorMessage = null
            )

            try {
                Log.d("ControlVM", "✏️ Writing Int[$index] = $value")
                repoImpl.writeInt(index, value)
                Log.d("ControlVM", "✅ Integer write successful")

            } catch (e: Exception) {
                Log.e("ControlVM", "❌ Integer write failed", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Ghi Integer thất bại: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isWriting = false)
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Retry connection
     */
    fun retryConnection() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("ControlVM", "🔄 Retrying connection...")
                _uiState.value = _uiState.value.copy(errorMessage = null)

                // Stop và start lại
                repoImpl.stop()
                kotlinx.coroutines.delay(1000)
                repoImpl.start()

            } catch (e: Exception) {
                Log.e("ControlVM", "❌ Retry failed", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Retry thất bại: ${e.message}"
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("ControlVM", "🧹 ViewModel cleared, stopping repository...")
        repoImpl.stop()
    }
}