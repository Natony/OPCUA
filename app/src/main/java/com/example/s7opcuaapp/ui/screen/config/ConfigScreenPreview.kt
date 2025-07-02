package com.example.s7opcuaapp.ui.screen.config

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import com.example.s7opcuaapp.data.model.DeviceEntity
import com.example.s7opcuaapp.viewmodel.ConfigUiState


@Preview(showBackground = true, widthDp = 1920, heightDp = 1200)
@Composable
fun ConfigScreenEmptyPreview() {
    val sampleState = ConfigUiState(
        newDeviceName = "",
        newDeviceIp = "",
        newDevicePort = "4840",
        newDeviceUsername = "",
        newDevicePassword = "",
        deviceList = emptyList(),
        currentDevice = null,
        errorMessage = null
    )

    ConfigScreen(
        uiState = sampleState,
        onNewDeviceNameChanged = {},
        onNewDeviceIpChanged = {},
        onNewDevicePortChanged = {},
        onNewDeviceUsernameChanged = {},
        onNewDevicePasswordChanged = {},
        onAddDevice = {},
        onRemoveDevice = {},
        onSelectDevice = {}
    )
}
