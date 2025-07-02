package com.example.s7opcuaapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.s7opcuaapp.data.local.PrefsManager
import com.example.s7opcuaapp.data.model.DeviceEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConfigUiState(
    val deviceList: List<DeviceEntity> = emptyList(),
    val currentDevice: DeviceEntity? = null,
    val newDeviceName: String = "",
    val newDeviceIp: String = "",
    val newDevicePort: String = "4840",
    val newDeviceUsername: String = "",
    val newDevicePassword: String = "",
    val errorMessage: String? = null
)

@HiltViewModel
class ConfigViewModel @Inject constructor(
    private val prefsManager: PrefsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfigUiState())
    val uiState: StateFlow<ConfigUiState> = _uiState

    init {
        loadDevices()
    }

    private fun loadDevices() {
        viewModelScope.launch {
            val list = prefsManager.getAllDevices()
            val current = prefsManager.getCurrentDevice()
            _uiState.value = _uiState.value.copy(
                deviceList = list,
                currentDevice = current
            )
        }
    }

    fun onNewDeviceNameChanged(new: String) {
        _uiState.value = _uiState.value.copy(newDeviceName = new)
    }
    fun onNewDeviceIpChanged(new: String) {
        _uiState.value = _uiState.value.copy(newDeviceIp = new)
    }
    fun onNewDevicePortChanged(new: String) {
        _uiState.value = _uiState.value.copy(newDevicePort = new)
    }
    fun onNewDeviceUsernameChanged(new: String) {
        _uiState.value = _uiState.value.copy(newDeviceUsername = new)
    }
    fun onNewDevicePasswordChanged(new: String) {
        _uiState.value = _uiState.value.copy(newDevicePassword = new)
    }

    fun onAddDevice() {
        val name = _uiState.value.newDeviceName.trim()
        val ip = _uiState.value.newDeviceIp.trim()
        val portStr = _uiState.value.newDevicePort.trim()
        val username = _uiState.value.newDeviceUsername.trim()
        val password = _uiState.value.newDevicePassword.trim()

        if (name.isEmpty() || ip.isEmpty() || portStr.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Tên, IP, Port không được để trống")
            return
        }
        val port = portStr.toIntOrNull()
        if (port == null || port <= 0) {
            _uiState.value = _uiState.value.copy(errorMessage = "Port không hợp lệ")
            return
        }
        val id = System.currentTimeMillis().toString()
        val newDevice = DeviceEntity(
            id = id,
            name = name,
            ipAddress = ip,
            port = port,
            opcUsername = username,
            opcPassword = password,
            useOpcUa = true
        )
        viewModelScope.launch {
            val updatedList = _uiState.value.deviceList.toMutableList().apply { add(newDevice) }
            prefsManager.saveDeviceList(updatedList)
            _uiState.value = _uiState.value.copy(
                deviceList = updatedList,
                newDeviceName = "",
                newDeviceIp = "",
                newDevicePort = "4840",
                newDeviceUsername = "",
                newDevicePassword = "",
                errorMessage = null
            )
        }
    }

    fun onRemoveDevice(device: DeviceEntity) {
        viewModelScope.launch {
            val updatedList = _uiState.value.deviceList.filterNot { it.id == device.id }
            prefsManager.saveDeviceList(updatedList)
            val current = prefsManager.getCurrentDevice()
            if (current?.id == device.id) {
                prefsManager.clearCurrentDevice()
                _uiState.value = _uiState.value.copy(currentDevice = null)
            } else {
                _uiState.value = _uiState.value.copy(deviceList = updatedList)
            }
        }
    }

    fun onSelectDevice(device: DeviceEntity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            prefsManager.setCurrentDevice(device)
            _uiState.value = _uiState.value.copy(currentDevice = device)
            onSuccess()
        }
    }

}
