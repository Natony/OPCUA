package com.example.s7opcuaapp.ui.screen.control

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.example.s7opcuaapp.ui.components.BoolControlItem
import com.example.s7opcuaapp.ui.components.IntControlItem
import com.example.s7opcuaapp.ui.components.NumberInputDialog

/**
 * ControlScreen hiển thị 2 section:
 *  1. Booleans (15 items)
 *  2. Integers/Mode (10 items)
 *
 * Sử dụng LazyColumn để chỉ render các item cần thiết.
 * Mỗi khi giá trị thay đổi, Compose sẽ recompose các item tương ứng.
 */
@Composable
fun ControlScreen(
    uiState: ControlUiState,
    onToggleBoolean: (Int, Boolean) -> Unit,
    onOpenDialog: (String) -> Unit,
    onConfirmNumber: (String, Int) -> Unit
) {
    val data = uiState.plcData
    val isWriting = uiState.isWriting

    // Hiển thị dialog nhập số khi cần
    if (uiState.openDialogForField != null) {
        NumberInputDialog(
            title = "Nhập giá trị cho ${uiState.openDialogForField}",
            initialValue = "",
            onConfirm = { value ->
                onConfirmNumber(uiState.openDialogForField!!, value)
            },
            onDismiss = {
                // Gọi ViewModel.onDismissDialog()
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Section Boolean
        item {
            Text(
                text = "Booleans",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        itemsIndexed(data.bools) { idx, boolVal ->
            BoolControlItem(
                label = "Bool ${idx + 1}",
                value = boolVal,
                isWriting = isWriting,
                onToggle = { newVal ->
                    onToggleBoolean(idx, newVal)
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Section Integer (Mode hoặc giá trị khác)
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Integers / Modes",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        itemsIndexed(data.ints) { idx, intVal ->
            IntControlItem(
                label = "Int ${idx + 1}",
                intValue = intVal,
                isWriting = isWriting,
                onOpenDialog = { onOpenDialog("Int $idx") }
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
