package com.example.s7opcuaapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.s7opcuaapp.domain.connection.ConnectionState
import com.example.s7opcuaapp.presentation.coordinator.ControlCoordinator
import com.example.s7opcuaapp.ui.screen.control.ControlUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Simplified ViewModel that delegates to coordinator
 */
@HiltViewModel
class ControlViewModel @Inject constructor(
    private val coordinator: ControlCoordinator
) : ViewModel() {

    val uiState: StateFlow<ControlUiState> = coordinator.uiState
    val connectionState: StateFlow<ConnectionState> = coordinator.connectionState

    private val _dialogState = MutableStateFlow(DialogState())
    val dialogState: StateFlow<DialogState> = _dialogState.asStateFlow()

    data class DialogState(
        val openDialogForIndex: Int? = null,
        val dialogTitle: String = "",
        val selectedFunction: Int = 0,
        val intInputs: Map<Int, String> = emptyMap()
    )

    fun startConnection() {
        viewModelScope.launch {
            coordinator.startConnection()
        }
    }

    fun stopConnection() {
        viewModelScope.launch {
            coordinator.stopConnection()
        }
    }

    fun resetConnection() {
        viewModelScope.launch {
            coordinator.resetConnection()
        }
    }

    fun continueOffline() {
        coordinator.continueOffline()
    }

    fun onToggleBoolean(index: Int, value: Boolean) {
        viewModelScope.launch {
            coordinator.toggleBoolean(index, value)
        }
    }

    fun onPressButton(index: Int): Boolean {
        viewModelScope.launch {
            coordinator.pressButton(index)
        }
        return true
    }

    fun onReleaseButton(index: Int): Boolean {
        viewModelScope.launch {
            coordinator.releaseButton(index)
        }
        return true
    }

    fun openNumberDialog(title: String, index: Int) {
        _dialogState.update { it.copy(
            openDialogForIndex = index,
            dialogTitle = title
        )}
    }

    fun dismissDialog() {
        _dialogState.update { it.copy(openDialogForIndex = null) }
    }

    fun confirmNumber(index: Int, value: Int) {
        viewModelScope.launch {
            coordinator.writeInteger(index, value)
            dismissDialog()
        }
    }

    fun onFunctionSelected(code: Int) {
        _dialogState.update { it.copy(selectedFunction = code) }
    }

    fun onInlineValueChange(index: Int, text: String) {
        _dialogState.update {
            it.copy(intInputs = it.intInputs + (index to text))
        }
    }

    fun onSendAll() {
        viewModelScope.launch {
            val state = _dialogState.value
            val parameters = mutableMapOf<Int, Int>()

            // Collect coordinate values
            (5..10).forEach { idx ->
                val value = state.intInputs[idx]?.toIntOrNull()
                    ?: uiState.value.plcData.ints.getOrNull(idx)
                    ?: 0
                parameters[idx] = value
            }

            coordinator.executeFunction(state.selectedFunction, parameters)
        }
    }

    fun resetConnectionAttempts() {
        // No longer needed - handled by coordinator
    }

    fun dismissTimeoutDialog() {
        // No longer needed - handled by coordinator
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            coordinator.stopConnection()
        }
    }
}