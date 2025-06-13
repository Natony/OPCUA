package com.example.s7opcuaapp.ui.screen.config

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.s7opcuaapp.data.model.DeviceEntity
import com.example.s7opcuaapp.viewmodel.ConfigUiState

/**
 * ConfigScreen cho phép:
 *  - Nhập tên (newDeviceName), IP (newDeviceIp), port (newDevicePort),
 *    username (newDeviceUsername), password (newDevicePassword) khi add mới.
 *  - Hiển thị danh sách Device hiện có.
 */
@Composable
fun ConfigScreen(
    uiState: ConfigUiState,
    onNewDeviceNameChanged: (String) -> Unit,
    onNewDeviceIpChanged: (String) -> Unit,
    onNewDevicePortChanged: (String) -> Unit,
    onNewDeviceUsernameChanged: (String) -> Unit,
    onNewDevicePasswordChanged: (String) -> Unit,
    onAddDevice: () -> Unit,
    onRemoveDevice: (DeviceEntity) -> Unit,
    onSelectDevice: (DeviceEntity) -> Unit
) {
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
    ) {
        Text("Config Devices", modifier = Modifier.padding(bottom = 8.dp))

        OutlinedTextField(
            value = uiState.newDeviceName,
            onValueChange = onNewDeviceNameChanged,
            label = { Text("Device Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.newDeviceIp,
            onValueChange = onNewDeviceIpChanged,
            label = { Text("IP Address") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.newDevicePort,
            onValueChange = onNewDevicePortChanged,
            label = { Text("Port (e.g. 4840)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.newDeviceUsername,
            onValueChange = onNewDeviceUsernameChanged,
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.newDevicePassword,
            onValueChange = onNewDevicePasswordChanged,
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onAddDevice, modifier = Modifier.align(Alignment.End)) {
            Text("Add Device")
        }

        uiState.errorMessage?.let { msg ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = msg, color = Color.Red)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Device List:", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            items(uiState.deviceList) { device ->
                DeviceListItem(
                    device = device,
                    isSelected = (device.id == uiState.currentDevice?.id),
                    onRemove = { onRemoveDevice(device) },
                    onSelect = { onSelectDevice(device) }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
