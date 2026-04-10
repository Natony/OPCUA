package com.example.s7opcuaapp.ui.screen.config

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.s7opcuaapp.data.model.DeviceEntity
import com.example.s7opcuaapp.viewmodel.ConfigUiState

@Composable
fun ConfigScreen(
    uiState: ConfigUiState,
    onNewDeviceNameChanged: (String) -> Unit,
    onNewDeviceIpChanged: (String) -> Unit,
    onNewDevicePortChanged: (String) -> Unit,
    onNewDeviceUsernameChanged: (String) -> Unit,
    onNewDevicePasswordChanged: (String) -> Unit,
    onProtocolChanged: (Boolean) -> Unit,
    onModbusSlaveIdChanged: (String) -> Unit,
    onModbusBoolRegisterAddressChanged: (String) -> Unit,
    onModbusIntRegisterAddressChanged: (String) -> Unit,
    onModbusIntRegisterCountChanged: (String) -> Unit,
    onModbusBoolCountChanged: (String) -> Unit,
    onModbusPollingIntervalChanged: (String) -> Unit,
    onAddDevice: () -> Unit,
    onRemoveDevice: (DeviceEntity) -> Unit,
    onSelectDevice: (DeviceEntity) -> Unit,
    onEditDevice: (DeviceEntity) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        item {
            Text(
                text = "Device Configuration",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // Add Device Form
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Add New Device",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Protocol Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Protocol:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(12.dp))
                        FilterChip(
                            selected = uiState.useOpcUa,
                            onClick = { onProtocolChanged(true) },
                            label = { Text("OPC UA") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = !uiState.useOpcUa,
                            onClick = { onProtocolChanged(false) },
                            label = { Text("Modbus TCP") }
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    // Common fields
                    OutlinedTextField(
                        value = uiState.newDeviceName,
                        onValueChange = onNewDeviceNameChanged,
                        label = { Text("Device Name", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = uiState.newDeviceIp,
                            onValueChange = onNewDeviceIpChanged,
                            label = { Text("IP Address", fontSize = 12.sp) },
                            modifier = Modifier.weight(2f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = uiState.newDevicePort,
                            onValueChange = onNewDevicePortChanged,
                            label = { Text("Port", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    if (uiState.useOpcUa) {
                        // OPC UA specific fields
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = uiState.newDeviceUsername,
                                onValueChange = onNewDeviceUsernameChanged,
                                label = { Text("Username", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = uiState.newDevicePassword,
                                onValueChange = onNewDevicePasswordChanged,
                                label = { Text("Password", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    } else {
                        // Modbus TCP specific fields
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = uiState.modbusSlaveId,
                                onValueChange = onModbusSlaveIdChanged,
                                label = { Text("Slave ID", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = uiState.modbusPollingIntervalMs,
                                onValueChange = onModbusPollingIntervalChanged,
                                label = { Text("Poll (ms)", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = uiState.modbusBoolRegisterAddress,
                                onValueChange = onModbusBoolRegisterAddressChanged,
                                label = { Text("Bool Reg Addr", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = uiState.modbusBoolCount,
                                onValueChange = onModbusBoolCountChanged,
                                label = { Text("Bool Count", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = uiState.modbusIntRegisterAddress,
                                onValueChange = onModbusIntRegisterAddressChanged,
                                label = { Text("Int Reg Addr", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = uiState.modbusIntRegisterCount,
                                onValueChange = onModbusIntRegisterCountChanged,
                                label = { Text("Int Count", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onAddDevice,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Add Device")
                    }

                    uiState.errorMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Device List Header
        item {
            Text(
                text = "Saved Devices (${uiState.deviceList.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Device List Content
        if (uiState.deviceList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = "No devices configured yet",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(uiState.deviceList) { device ->
                DeviceListItem(
                    device = device,
                    isSelected = (device.id == uiState.currentDevice?.id),
                    onRemove = { onRemoveDevice(device) },
                    onSelect = { onSelectDevice(device) },
                    onEdit = { onEditDevice(device) }
                )
            }
        }
    }
}
