package com.example.s7opcuaapp.ui.screen.control

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.s7opcuaapp.data.model.PlcData
import com.example.s7opcuaapp.R
import com.example.s7opcuaapp.ui.components.BoolControlItem
import com.example.s7opcuaapp.ui.components.IntControlItem
import com.example.s7opcuaapp.ui.components.PressReleaseBoolControlItem

@Composable
fun LeftControlPanel(
    isAuto: Boolean,
    data: PlcData,
    onToggleBoolean: (Int, Boolean) -> Unit,
    onOpenDialog: (Int) -> Unit,
    onStartPress: (Int) -> Unit,
    onEndPress: (Int) -> Unit,
    lockedButtons: Set<Int>,
    busyButtons: Set<Int>,
    isProcessing: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp)
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isAuto) {
            // Auto mode controls
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                IntControlItem(
                    intValue = data.ints.getOrNull(4) ?: 0,
                    icons = listOf(
                        R.drawable.ic_pallets_minus_off,
                        R.drawable.ic_pallets_minus_on
                    ),
                    onClick = { onOpenDialog(4) },
                    enabled = (4 + 200) !in lockedButtons && (4 + 200) !in busyButtons,
                    isProcessing = (4 + 200) in busyButtons
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                BoolControlItem(
                    value = data.bools.getOrNull(6) ?: false,
                    iconOn = R.drawable.ic_pallet_minus_on,
                    iconOff = R.drawable.ic_pallet_minus_off,
                    onClick = { onToggleBoolean(6, data.bools.getOrNull(6)?.not() ?: false) },
                    enabled = 6 !in lockedButtons && 6 !in busyButtons,
                    isProcessing = 6 in busyButtons
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                BoolControlItem(
                    value = data.bools.getOrNull(8) ?: false,
                    iconOn = R.drawable.ic_stack_pallets_a_on,
                    iconOff = R.drawable.ic_stack_pallets_a_off,
                    onClick = { onToggleBoolean(8, data.bools.getOrNull(8)?.not() ?: false) },
                    enabled = 8 !in lockedButtons && 8 !in busyButtons,
                    isProcessing = 8 in busyButtons
                )
            }
        } else {
            // Manual mode controls với PressReleaseBoolControlItem
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                PressReleaseBoolControlItem(
                    value = data.bools.getOrNull(0) ?: false,
                    iconOn = R.drawable.ic_shuttle_forward_on,
                    iconOff = R.drawable.ic_shuttle_forward_off,
                    onPress = { onStartPress(0) },
                    onRelease = { onEndPress(0) },
                    enabled = 0 !in lockedButtons,
                    isProcessing = isProcessing,
                    busy = 0 in busyButtons,
                )

//                BoolControlItem(
//                    value = data.bools.getOrNull(0) ?: false,
//                    iconOn = R.drawable.ic_shuttle_forward_on,
//                    iconOff = R.drawable.ic_shuttle_forward_off,
//                    onClick = { onToggleBoolean(0, data.bools.getOrNull(0)?.not() ?: false) },
//                    enabled = 0 !in lockedButtons && 0 !in busyButtons,
//                    isProcessing = 0 in busyButtons
//                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
//                PressReleaseBoolControlItem(
//                    value = data.bools.getOrNull(1) ?: false,
//                    iconOn = R.drawable.ic_shuttle_reverse_on,
//                    iconOff = R.drawable.ic_shuttle_reverse_off,
//                    onPress = { onStartPress(1) },
//                    onRelease = { onEndPress(1) },
//                    enabled = 1 !in lockedButtons,
//                    busy = 1 in busyButtons,
//                )

                BoolControlItem(
                    value = data.bools.getOrNull(1) ?: false,
                    iconOn = R.drawable.ic_shuttle_reverse_on,
                    iconOff = R.drawable.ic_shuttle_reverse_off,
                    onClick = { onToggleBoolean(1, data.bools.getOrNull(1)?.not() ?: false) },
                    enabled = 1 !in lockedButtons && 1 !in busyButtons,
                    isProcessing = 1 in busyButtons
                )
            }
        }
    }
}