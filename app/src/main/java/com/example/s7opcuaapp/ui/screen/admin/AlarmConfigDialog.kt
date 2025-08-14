// app/src/main/java/com/example/s7opcuaapp/ui/screen/admin/AlarmConfigDialog.kt
package com.example.s7opcuaapp.ui.screen.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.s7opcuaapp.data.model.*
import com.example.s7opcuaapp.data.model.alarm.AlarmCategory
import com.example.s7opcuaapp.data.model.alarm.AlarmConfig
import com.example.s7opcuaapp.data.model.alarm.AlarmPriority

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmConfigDialog(
    config: AlarmConfig?,
    onSave: (AlarmConfig) -> Unit,

    onDismiss: () -> Unit
) {
    var alarmCode by remember { mutableStateOf(config?.alarmCode?.toString() ?: "") }
    var message by remember { mutableStateOf(config?.message ?: "") }
    var description by remember { mutableStateOf(config?.description ?: "") }
    var priority by remember { mutableStateOf(config?.priority ?: AlarmPriority.MEDIUM) }
    var category by remember { mutableStateOf(config?.category ?: AlarmCategory.PROCESS) }
    var enabled by remember { mutableStateOf(config?.enabled ?: true) }
    var soundEnabled by remember { mutableStateOf(config?.soundEnabled ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (config == null) "Add Alarm Configuration" else "Edit Alarm Configuration")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Alarm Code
                OutlinedTextField(
                    value = alarmCode,
                    onValueChange = { alarmCode = it },
                    label = { Text("Alarm Code") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = config == null // Only editable for new configs
                )

                // Message
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                // Priority Dropdown
                PriorityDropdown(
                    selectedPriority = priority,
                    onPrioritySelected = { priority = it }
                )

                // Category Dropdown
                CategoryDropdown(
                    selectedCategory = category,
                    onCategorySelected = { category = it }
                )

                // Enabled Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enabled", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it }
                    )
                }

                // Sound Enabled Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Sound Alert", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { soundEnabled = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val code = alarmCode.toIntOrNull()
                    if (code != null && message.isNotBlank()) {
                        val newConfig = AlarmConfig(
                            alarmCode = code,
                            message = message,
                            description = description,
                            priority = priority,
                            category = category,
                            enabled = enabled,
                            soundEnabled = soundEnabled,
                            createdBy = config?.createdBy ?: "admin" // Will be updated in viewmodel
                        )
                        onSave(newConfig)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PriorityDropdown(
    selectedPriority: AlarmPriority,
    onPrioritySelected: (AlarmPriority) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedPriority.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Priority") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedLabelColor = Color(selectedPriority.color),
                focusedBorderColor = Color(selectedPriority.color)
            ),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AlarmPriority.values().forEach { priority ->
                DropdownMenuItem(
                    text = {
                        Row {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = Color(priority.color),
                                modifier = Modifier.size(16.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(priority.name)
                        }
                    },
                    onClick = {
                        onPrioritySelected(priority)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selectedCategory: AlarmCategory,
    onCategorySelected: (AlarmCategory) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedCategory.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AlarmCategory.values().forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}