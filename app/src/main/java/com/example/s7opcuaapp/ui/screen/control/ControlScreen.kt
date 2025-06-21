package com.example.s7opcuaapp.ui.screen.control

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.s7opcuaapp.ui.components.BoolControlItem
import com.example.s7opcuaapp.ui.components. IntControlItem
import com.example.s7opcuaapp.ui.components.MultiStateStatusItem
import com.example.s7opcuaapp.ui.components.BooleanStatusItem
import com.example.s7opcuaapp.ui.components.NumberInputDialog
import com.example.s7opcuaapp.R
import com.example.s7opcuaapp.ui.components.FunctionListSelector
import com.example.s7opcuaapp.ui.components.InlineNumberInputItem
import com.example.s7opcuaapp.ui.components.NumericStatusItem

/**
 * ControlScreen sử dụng BoolControlItem thay vì BoolControlItem
 * để tạo các nút nhấn với icon thay đổi theo trạng thái
 */
@Composable
fun ControlScreen(
    uiState: ControlUiState,
    onToggleBoolean: (Int, Boolean) -> Unit,
    onOpenDialog: (Int) -> Unit,
    onConfirmNumber: (Int, Int) -> Unit,
    onDismissDialog: () -> Unit,
    onFunctionSelect: (Int) -> Unit,
    onTextChange: (Int, String) -> Unit,
    onSendAll: () -> Unit
) {
    val data = uiState.plcData
    val isWriting = uiState.isWriting

    // Hiển thị dialog nhập số khi cần
    uiState.openDialogForIndex?.let { idx ->
        NumberInputDialog(
            title = "Nhập giá trị cho Mode ${idx + 1}",
            initialValue = data.ints.getOrNull(idx)?.toString() ?: "0",
            onConfirm = { value ->
                onConfirmNumber(idx, value)
            },
            onDismiss = {
                onDismissDialog()
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Section Manual Controls
        item {
            Text(
                text = "Manual Controls",
                style = MaterialTheme.typography.titleMedium
            )
        }

        // Manual movement controls
        item {
            BoolControlItem(
                label = "Forward",
                value = data.bools.getOrNull(0) ?: false,
                iconOn = R.drawable.ic_shuttle_forward_on,
                iconOff = R.drawable.ic_shuttle_forward_off,
                isWriting = isWriting,
                onClick = {
                    onToggleBoolean(0, !(data.bools.getOrNull(0) ?: false))
                }
            )
        }

        item {
            BoolControlItem(
                label = "Reverse",
                value = data.bools.getOrNull(1) ?: false,
                iconOn = R.drawable.ic_shuttle_reverse_on,
                iconOff = R.drawable.ic_shuttle_reverse_off,
                isWriting = isWriting,
                onClick = {
                    onToggleBoolean(1, !(data.bools.getOrNull(1) ?: false))
                }
            )
        }

        item {
            BoolControlItem(
                label = "Up",
                value = data.bools.getOrNull(2) ?: false,
                iconOn = R.drawable.ic_shuttle_up_on,
                iconOff = R.drawable.ic_shuttle_up_off,
                isWriting = isWriting,
                onClick = {
                    onToggleBoolean(2, !(data.bools.getOrNull(2) ?: false))
                }
            )
        }

        item {
            BoolControlItem(
                label = "Down",
                value = data.bools.getOrNull(3) ?: false,
                iconOn = R.drawable.ic_shuttle_down_on,
                iconOff = R.drawable.ic_shuttle_down_off,
                isWriting = isWriting,
                onClick = {
                    onToggleBoolean(3, !(data.bools.getOrNull(3) ?: false))
                }
            )
        }

        // Section System Controls
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "System Controls",
                style = MaterialTheme.typography.titleMedium
            )
        }

        item {
            BoolControlItem(
                label = "Power",
                value = data.bools.getOrNull(4) ?: false,
                iconOn = R.drawable.ic_power_on,
                iconOff = R.drawable.ic_power_off,
                isWriting = isWriting,
                onClick = {
                    onToggleBoolean(4, !(data.bools.getOrNull(4) ?: false))
                }
            )
        }

        item {
            BoolControlItem(
                label = "Buzzer",
                value = data.bools.getOrNull(5) ?: false,
                iconOn = R.drawable.ic_buzzer_on,
                iconOff = R.drawable.ic_buzzer_off,
                isWriting = isWriting,
                onClick = {
                    onToggleBoolean(5, !(data.bools.getOrNull(5) ?: false))
                }
            )
        }

        // Section Pallet Operations
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Pallet Operations",
                style = MaterialTheme.typography.titleMedium
            )
        }

        item {
            BoolControlItem(
                label = "Pick Pallet",
                value = data.bools.getOrNull(6) ?: false,
                iconOn = R.drawable.ic_pallet_plus_on,
                iconOff = R.drawable.ic_pallet_plus_off,
                isWriting = isWriting,
                onClick = {
                    onToggleBoolean(6, !(data.bools.getOrNull(6) ?: false))
                }
            )
        }

        item {
            BoolControlItem(
                label = "Take Pallet",
                value = data.bools.getOrNull(7) ?: false,
                iconOn = R.drawable.ic_pallet_minus_on,
                iconOff = R.drawable.ic_pallet_minus_off,
                isWriting = isWriting,
                onClick = {
                    onToggleBoolean(7, !(data.bools.getOrNull(7) ?: false))
                }
            )
        }

        item {
            BoolControlItem(
                label = "Stack A",
                value = data.bools.getOrNull(8) ?: false,
                iconOn = R.drawable.ic_stack_pallets_a_on,
                iconOff = R.drawable.ic_stack_pallets_a_off,
                isWriting = isWriting,
                onClick = {
                    onToggleBoolean(8, !(data.bools.getOrNull(8) ?: false))
                }
            )
        }

        item {
            BoolControlItem(
                label = "Stack B",
                value = data.bools.getOrNull(9) ?: false,
                iconOn = R.drawable.ic_stack_pallets_b_on,
                iconOff = R.drawable.ic_stack_pallets_b_off,
                isWriting = isWriting,
                onClick = {
                    onToggleBoolean(9, !(data.bools.getOrNull(9) ?: false))
                }
            )
        }

        item {
            BoolControlItem(
                label = "Emergency",
                value = data.bools.getOrNull(10) ?: false,
                iconOn = R.drawable.ic_emergency_stop,
                iconOff = R.drawable.ic_emergency_stop,
                isWriting = isWriting,
                onClick = {
                    onToggleBoolean(10, !(data.bools.getOrNull(10) ?: false))
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Status Bool",
                style = MaterialTheme.typography.titleMedium
            )
        }

        item {
            BooleanStatusItem(
                label = "Shuttle",
                value = data.bools.getOrNull(11) ?: false,
                iconOn =  R.drawable.ic_green,
                iconOff =  R.drawable.ic_red,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Integer Controls",
                style = MaterialTheme.typography.titleMedium
            )
        }

        // Nút TPallets
        item {
            IntControlItem(
                label = "TPallets",
                intValue = data.ints.getOrNull(0) ?: 0,
                icons = listOf(
                    R.drawable.ic_pallets_plus_off,
                    R.drawable.ic_pallets_plus_on
                ),
                modifier = Modifier.fillMaxWidth(),
                onClick = { onOpenDialog(0) }
            )
        }

        // Nút PPallets
        item {
            IntControlItem(
                label = "PPallets",
                intValue = data.ints.getOrNull(1) ?: 0,
                icons = listOf(
                    R.drawable.ic_pallets_plus_off,
                    R.drawable.ic_pallets_plus_on
                ),
                modifier = Modifier.fillMaxWidth(),
                onClick = { onOpenDialog(1) }
            )
        }

        //Start Point X
        item {
            InlineNumberInputItem(
                label = "StartX",
                textValue = data.ints.getOrNull(2)?.toString() ?: "0",
                isWriting = isWriting,
                onTextChange = { new -> onTextChange(2, new) }
            )
        }

        //Start Point Y
        item {
            InlineNumberInputItem(
                label = "StartY",
                textValue = data.ints.getOrNull(3)?.toString() ?: "0",
                isWriting = isWriting,
                onTextChange = { new -> onTextChange(3, new) }
            )
        }

        //Start Point Z
        item {
            InlineNumberInputItem(
                label = "StartZ",
                textValue = data.ints.getOrNull(4)?.toString() ?: "0",
                isWriting = isWriting,
                onTextChange = { new -> onTextChange(4, new) }
            )
        }

        //End Point X
        item {
            InlineNumberInputItem(
                label = "EndX",
                textValue = data.ints.getOrNull(5)?.toString() ?: "0",
                isWriting = isWriting,
                onTextChange = { new -> onTextChange(5, new) }
            )
        }

        //End Point Y
        item {
            InlineNumberInputItem(
                label = "EndY",
                textValue = data.ints.getOrNull(6)?.toString() ?: "0",
                isWriting = isWriting,
                onTextChange = { new -> onTextChange(6, new) }
            )
        }

        //End Point Z
        item {
            InlineNumberInputItem(
                label = "EndZ",
                textValue = data.ints.getOrNull(7)?.toString() ?: "0",
                isWriting = isWriting,
                onTextChange = { new -> onTextChange(7, new) }
            )
        }

        // Actual Point X
        item {
            NumericStatusItem(
                label = "ActualX",
                value = data.ints.getOrNull(8) ?: 0,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Actual Point Y
        item {
            NumericStatusItem(
                label = "ActualY",
                value = data.ints.getOrNull(9) ?: 0,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Actual Point Z
        item {
            NumericStatusItem(
                label = "ActualZ",
                value = data.ints.getOrNull(10) ?: 0,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ── Function selector ──
        item {
            Text("Chọn Chức năng", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            FunctionListSelector(
                entries = listOf(
                    "Function 1" to 1,
                    "Function 2" to 2,
                    "Function 3" to 3
                ),
                selectedCode = uiState.selectedFunction,
                onSelect = onFunctionSelect
            )
        }

        item {
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onSendAll,
                enabled = !isWriting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Gửi xuống PLC")
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Position",
                style = MaterialTheme.typography.titleMedium
            )
        }

        item {
            MultiStateStatusItem(
                label = "Pos1",
                intValue = data.ints.getOrNull(13) ?: 0,
                icons = listOf(
                    R.drawable.ic_pos1_state0,
                    R.drawable.ic_pos1_state1,
                    R.drawable.ic_pos1_state2,
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            MultiStateStatusItem(
                label = "Pos2",
                intValue = data.ints.getOrNull(14) ?: 0,
                icons = listOf(
                    R.drawable.ic_pos2_state0,
                    R.drawable.ic_pos2_state1,
                    R.drawable.ic_pos2_state2,
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            MultiStateStatusItem(
                label = "Pos3",
                intValue = data.ints.getOrNull(15) ?: 0,
                icons = listOf(
                    R.drawable.ic_pos3_state0,
                    R.drawable.ic_pos3_state1,
                    R.drawable.ic_pos3_state2,
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            MultiStateStatusItem(
                label = "Pos4",
                intValue = data.ints.getOrNull(16) ?: 0,
                icons = listOf(
                    R.drawable.ic_pos4_state0,
                    R.drawable.ic_pos4_state1,
                    R.drawable.ic_pos4_state2,
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            MultiStateStatusItem(
                label = "Pos5",
                intValue = data.ints.getOrNull(17) ?: 0,
                icons = listOf(
                    R.drawable.ic_pos5_state0,
                    R.drawable.ic_pos5_state1,
                    R.drawable.ic_pos5_state2,
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            MultiStateStatusItem(
                label = "Pos6",
                intValue = data.ints.getOrNull(18) ?: 0,
                icons = listOf(
                    R.drawable.ic_pos6_state0,
                    R.drawable.ic_pos6_state1,
                    R.drawable.ic_pos6_state2,
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            MultiStateStatusItem(
                label = "Pos7",
                intValue = data.ints.getOrNull(19) ?: 0,
                icons = listOf(
                    R.drawable.ic_pos7_state0,
                    R.drawable.ic_pos7_state1,
                    R.drawable.ic_pos7_state2,
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            MultiStateStatusItem(
                label = "Pos8",
                intValue = data.ints.getOrNull(20) ?: 0,
                icons = listOf(
                    R.drawable.ic_pos8_state0,
                    R.drawable.ic_pos8_state1,
                    R.drawable.ic_pos8_state2,
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            MultiStateStatusItem(
                label = "Pos9",
                intValue = data.ints.getOrNull(21) ?: 0,
                icons = listOf(
                    R.drawable.ic_pos9_state0,
                    R.drawable.ic_pos9_state1,
                    R.drawable.ic_pos9_state2,
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            MultiStateStatusItem(
                label = "Pos10",
                intValue = data.ints.getOrNull(22) ?: 0,
                icons = listOf(
                    R.drawable.ic_pos10_state0,
                    R.drawable.ic_pos10_state1,
                    R.drawable.ic_pos10_state2,
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            MultiStateStatusItem(
                label = "Pos11",
                intValue = data.ints.getOrNull(23) ?: 0,
                icons = listOf(
                    R.drawable.ic_pos11_state0,
                    R.drawable.ic_pos11_state1,
                    R.drawable.ic_pos11_state2,
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            MultiStateStatusItem(
                label = "Pos12",
                intValue = data.ints.getOrNull(24) ?: 0,
                icons = listOf(
                    R.drawable.ic_pos12_state0,
                    R.drawable.ic_pos12_state1,
                    R.drawable.ic_pos12_state2,
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            MultiStateStatusItem(
                label = "Pos13",
                intValue = data.ints.getOrNull(25) ?: 0,
                icons = listOf(
                    R.drawable.ic_pos13_state0,
                    R.drawable.ic_pos13_state1,
                    R.drawable.ic_pos13_state2,
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}