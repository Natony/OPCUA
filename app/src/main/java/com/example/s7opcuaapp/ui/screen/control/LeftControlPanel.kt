package com.example.s7opcuaapp.ui.screen.control

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.s7opcuaapp.data.model.PlcData
import com.example.s7opcuaapp.R
import com.example.s7opcuaapp.ui.components.BoolControlItem
import com.example.s7opcuaapp.ui.components.IntControlItem

@Composable
fun LeftControlPanel(
    isAuto: Boolean,
    data: PlcData,
    onToggleBoolean: (Int, Boolean) -> Unit,
    onOpenDialog: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isAuto) {
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
                    onClick = { onOpenDialog(4) }
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
                    onClick = { onToggleBoolean(6, data.bools.getOrNull(6)?.not() ?: false) }
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
                    onClick = { onToggleBoolean(8, data.bools.getOrNull(8)?.not() ?: false) }
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                BoolControlItem(
                    value = data.bools.getOrNull(0) ?: false,
                    iconOn = R.drawable.ic_shuttle_forward_on,
                    iconOff = R.drawable.ic_shuttle_forward_off,
                    onClick = { onToggleBoolean(0, data.bools.getOrNull(0)?.not() ?: false) }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                BoolControlItem(
                    value = data.bools.getOrNull(1) ?: false,
                    iconOn = R.drawable.ic_shuttle_reverse_on,
                    iconOff = R.drawable.ic_shuttle_reverse_off,
                    onClick = { onToggleBoolean(1, data.bools.getOrNull(1)?.not() ?: false) }
                )
            }
        }
    }
}