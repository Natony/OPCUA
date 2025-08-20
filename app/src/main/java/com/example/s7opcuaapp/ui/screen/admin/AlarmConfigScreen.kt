// app/src/main/java/com/example/s7opcuaapp/ui/screen/admin/AlarmConfigScreen.kt
package com.example.s7opcuaapp.ui.screen.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.s7opcuaapp.data.model.*
import com.example.s7opcuaapp.data.model.alarm.AlarmConfig
import com.example.s7opcuaapp.viewmodel.AlarmConfigViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmConfigScreen(
    onBack: () -> Unit,
    viewModel: AlarmConfigViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alarm Configuration") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Reset button
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Default.Refresh, "Reset to Defaults")
                    }

                    // Add button
                    IconButton(onClick = { viewModel.showAddDialog() }) {
                        Icon(Icons.Default.Add, "Add Alarm")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = uiState.configs,
                key = { it.alarmCode }
            ) { config ->
                AlarmConfigCard(
                    config = config,
                    onEdit = { viewModel.showEditDialog(config) },
                    onDelete = { viewModel.deleteConfig(config) },
                    onToggleEnabled = { viewModel.toggleEnabled(config) }
                )
            }
        }
    }

    // Add/Edit Dialog
    if (uiState.showDialog) {
        AlarmConfigDialog(
            config = uiState.editingConfig,
            onSave = { config ->
                if (uiState.editingConfig == null) {
                    viewModel.addConfig(config)
                } else {
                    viewModel.updateConfig(config)
                }
            },
            onDismiss = { viewModel.hideDialog() }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset to Default Configurations?") },
            text = {
                Text("This will delete all custom configurations and restore default alarm settings. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetToDefaults()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AlarmConfigCard(
    config: AlarmConfig,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleEnabled: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Code: ${config.alarmCode}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = Color(config.priority.color)
                    ) {
                        Text(
                            text = config.priority.name,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    if (!config.enabled) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "DISABLED",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Text(
                    text = config.message,
                    style = MaterialTheme.typography.bodyLarge
                )

                if (config.description.isNotEmpty()) {
                    Text(
                        text = config.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row {
                Switch(
                    checked = config.enabled,
                    onCheckedChange = { onToggleEnabled() }
                )

                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Edit")
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}