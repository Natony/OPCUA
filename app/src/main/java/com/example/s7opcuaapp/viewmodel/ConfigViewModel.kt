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
    val useOpcUa: Boolean = true,
    // Modbus-specific form fields
    val modbusSlaveId: String = "1",
    val modbusBoolRegisterAddress: String = "0",
    val modbusIntRegisterAddress: String = "1",
    val modbusIntRegisterCount: String = "28",
    val modbusBoolCount: String = "15",
    val modbusPollingIntervalMs: String = "250",
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

    fun onProtocolChanged(useOpcUa: Boolean) {
        _uiState.value = _uiState.value.copy(
            useOpcUa = useOpcUa,
            newDevicePort = if (useOpcUa) "4840" else "502"
        )
    }

    fun onModbusSlaveIdChanged(new: String) {
        _uiState.value = _uiState.value.copy(modbusSlaveId = new)
    }
    fun onModbusBoolRegisterAddressChanged(new: String) {
        _uiState.value = _uiState.value.copy(modbusBoolRegisterAddress = new)
    }
    fun onModbusIntRegisterAddressChanged(new: String) {
        _uiState.value = _uiState.value.copy(modbusIntRegisterAddress = new)
    }
    fun onModbusIntRegisterCountChanged(new: String) {
        _uiState.value = _uiState.value.copy(modbusIntRegisterCount = new)
    }
    fun onModbusBoolCountChanged(new: String) {
        _uiState.value = _uiState.value.copy(modbusBoolCount = new)
    }
    fun onModbusPollingIntervalChanged(new: String) {
        _uiState.value = _uiState.value.copy(modbusPollingIntervalMs = new)
    }

    fun onAddDevice() {
        val state = _uiState.value
        val name = state.newDeviceName.trim()
        val ip = state.newDeviceIp.trim()
        val portStr = state.newDevicePort.trim()

        if (name.isEmpty() || ip.isEmpty() || portStr.isEmpty()) {
            _uiState.value = state.copy(errorMessage = "Name, IP, Port are required")
            return
        }
        val port = portStr.toIntOrNull()
        if (port == null || port <= 0) {
            _uiState.value = state.copy(errorMessage = "Invalid port")
            return
        }

        val id = System.currentTimeMillis().toString()
        val newDevice = if (state.useOpcUa) {
            DeviceEntity(
                id = id,
                name = name,
                ipAddress = ip,
                port = port,
                opcUsername = state.newDeviceUsername.trim(),
                opcPassword = state.newDevicePassword.trim(),
                useOpcUa = true
            )
        } else {
            DeviceEntity(
                id = id,
                name = name,
                ipAddress = ip,
                port = port,
                useOpcUa = false,
                modbusSlaveId = state.modbusSlaveId.toIntOrNull() ?: 1,
                modbusBoolRegisterAddress = state.modbusBoolRegisterAddress.toIntOrNull() ?: 0,
                modbusIntRegisterAddress = state.modbusIntRegisterAddress.toIntOrNull() ?: 1,
                modbusIntRegisterCount = state.modbusIntRegisterCount.toIntOrNull() ?: 31,
                modbusBoolCount = state.modbusBoolCount.toIntOrNull() ?: 14,
                modbusPollingIntervalMs = state.modbusPollingIntervalMs.toIntOrNull() ?: 250
            )
        }

        viewModelScope.launch {
            val updatedList = _uiState.value.deviceList.toMutableList().apply { add(newDevice) }
            prefsManager.saveDeviceList(updatedList)
            _uiState.value = ConfigUiState(
                deviceList = updatedList,
                currentDevice = _uiState.value.currentDevice
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
                _uiState.value = _uiState.value.copy(currentDevice = null, deviceList = updatedList)
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
