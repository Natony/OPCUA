package com.example.s7opcuaapp.ui.screen.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.s7opcuaapp.util.StatusLockConfig.StatusLockRule
import com.example.s7opcuaapp.viewmodel.StatusLockConfigViewModel
import com.example.s7opcuaapp.viewmodel.StatusLockConfigUiState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusLockConfigScreen(
    viewModel: StatusLockConfigViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cấu hình khóa theo trạng thái") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Emergency override toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "Override",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = uiState.overrideActive,
                            onCheckedChange = { viewModel.toggleOverride() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Red,
                                checkedTrackColor = Color.Red.copy(alpha = 0.5f)
                            )
                        )
                    }

                    // Reset button
                    TextButton(
                        onClick = { viewModel.showResetConfirmation() }
                    ) {
                        Text("Reset", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Warning banner if override is active
            if (uiState.overrideActive) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Red.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "OVERRIDE ĐANG BẬT",
                                fontWeight = FontWeight.Bold,
                                color = Color.Red
                            )
                            Text(
                                text = "Tất cả khóa trạng thái đã bị vô hiệu hóa!",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Red
                            )
                        }
                    }
                }
            }

            // Status rules list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val sortedRules = uiState.statusRules.toList().sortedBy { it.first }
                items(
                    items = sortedRules,
                    key = { it.first }
                ) { (status, rule) ->
                    StatusRuleCard(
                        rule = rule,
                        onToggleEnabled = { viewModel.toggleRuleEnabled(status) },
                        onEditExemptButtons = { viewModel.showExemptButtonsDialog(status) }
                    )
                }
            }
        }
    }

    // Dialogs
    if (uiState.showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.hideResetConfirmation() },
            title = { Text("Xác nhận reset") },
            text = { Text("Bạn có chắc muốn reset về cấu hình mặc định?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetToDefaults()
                        viewModel.hideResetConfirmation()
                    }
                ) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideResetConfirmation() }) {
                    Text("Hủy")
                }
            }
        )
    }

    uiState.editingStatus?.let { editingStatus ->
        ExemptButtonsDialog(
            statusValue = editingStatus,
            statusDescription = uiState.statusRules[editingStatus]?.description ?: "",
            exemptButtons = uiState.statusRules[editingStatus]?.exemptButtons ?: emptySet(),
            onToggleButton = { button -> viewModel.toggleExemptButton(button) },
            onDismiss = { viewModel.hideExemptButtonsDialog() }
        )
    }
}

@Composable
private fun StatusRuleCard(
    rule: StatusLockRule,
    onToggleEnabled: () -> Unit,
    onEditExemptButtons: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (rule.isEnabled) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Trạng thái ${rule.statusValue}",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = rule.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = { onToggleEnabled() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (rule.lockAllButtons) {
                        "Khóa tất cả nút (trừ ngoại lệ)"
                    } else {
                        "Không khóa"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (rule.isEnabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    }
                )

                if (rule.lockAllButtons) {
                    TextButton(
                        onClick = onEditExemptButtons,
                        enabled = rule.isEnabled
                    ) {
                        Text("Ngoại lệ (${rule.exemptButtons.size})")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExemptButtonsDialog(
    statusValue: Int,
    statusDescription: String,
    exemptButtons: Set<Int>,
    onToggleButton: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Nút ngoại lệ")
                Text(
                    text = "Trạng thái $statusValue: $statusDescription",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Chọn các nút KHÔNG bị khóa trong trạng thái này:",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Common buttons
                Text(
                    text = "Nút điều khiển:",
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                val commonButtons = listOf(
                    0 to "Tiến",
                    1 to "Lùi",
                    2 to "Lên",
                    3 to "Xuống",
                    4 to "Power",
                    5 to "Buzzer",
                    6 to "Pallet -",
                    7 to "Pallet +",
                    8 to "Stack A",
                    9 to "Stack B",
                    10 to "Emergency Stop",
                    11 to "FIFO/LIFO",
                    13 to "Direction",
                    14 to "Count"
                )

                commonButtons.forEach { (index, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = index in exemptButtons,
                            onCheckedChange = { onToggleButton(index) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("$index - $name")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}