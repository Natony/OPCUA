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

@Composable
fun RightControlPanel(
    isAuto: Boolean,
    data: PlcData,
    onToggleBoolean: (Int, Boolean) -> Unit,
    onOpenDialog: (Int) -> Unit,
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
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                IntControlItem(
                    intValue = data.ints.getOrNull(3) ?: 0,
                    icons = listOf(
                        R.drawable.ic_pallets_plus_off,
                        R.drawable.ic_pallets_plus_on
                    ),
                    onClick = { onOpenDialog(3) }
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                BoolControlItem(
                    value = data.bools.getOrNull(7) ?: false,
                    iconOn = R.drawable.ic_pallet_plus_on,
                    iconOff = R.drawable.ic_pallet_plus_off,
                    onClick = { onToggleBoolean(7, data.bools.getOrNull(7)?.not() ?: false) }
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                BoolControlItem(
                    value = data.bools.getOrNull(9) ?: false,
                    iconOn = R.drawable.ic_stack_pallets_b_on,
                    iconOff = R.drawable.ic_stack_pallets_b_off,
                    onClick = { onToggleBoolean(9, data.bools.getOrNull(9)?.not() ?: false) }
                )
            }
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                BoolControlItem(
                    value = data.bools.getOrNull(2) ?: false,
                    iconOn = R.drawable.ic_shuttle_up_on,
                    iconOff = R.drawable.ic_shuttle_up_off,
                    onClick = { onToggleBoolean(2, data.bools.getOrNull(2)?.not() ?: false) }
                )
            }
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                BoolControlItem(
                    value = data.bools.getOrNull(3) ?: false,
                    iconOn = R.drawable.ic_shuttle_down_on,
                    iconOff = R.drawable.ic_shuttle_down_off,
                    onClick = { onToggleBoolean(3, data.bools.getOrNull(3)?.not() ?: false) }
                )
            }
        }
    }
}