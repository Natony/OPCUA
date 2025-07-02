package com.example.s7opcuaapp.ui.screen.control

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.s7opcuaapp.data.model.PlcData
import com.example.s7opcuaapp.R
import com.example.s7opcuaapp.ui.components.*

@Composable
fun BottomControlsRow(
    isAuto: Boolean,
    data: PlcData,
    onToggleBoolean: (Int, Boolean) -> Unit,
    onToggleAutoMode: () -> Unit, // Thêm parameter này
    modifier: Modifier = Modifier
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BoolControlItem(
                value = data.bools.getOrNull(4) ?: false,
                iconOn = R.drawable.ic_power_on,
                iconOff = R.drawable.ic_power_off,
                onClick = { onToggleBoolean(4, data.bools.getOrNull(4)?.not() ?: false) }
            )
            BoolControlItem(
                value = isAuto,
                iconOn = R.drawable.ic_mode_auto,
                iconOff = R.drawable.ic_mode_manual,
                onClick = onToggleAutoMode // Sửa thành callback
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BooleanStatusItem(
                label = "Shuttle",
                value = data.bools.getOrNull(12) ?: false,
                iconOn = R.drawable.ic_green,
                iconOff = R.drawable.ic_red
            )
            BoolControlItem(
                value = data.bools.getOrNull(10) ?: false,
                iconOn = R.drawable.ic_emergency_stop,
                iconOff = R.drawable.ic_emergency_stop,
                onClick = { onToggleBoolean(10, data.bools.getOrNull(10)?.not() ?: false) }
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BoolControlItem(
                value = data.bools.getOrNull(5) ?: false,
                iconOn = R.drawable.ic_buzzer_on,
                iconOff = R.drawable.ic_buzzer_off,
                onClick = { onToggleBoolean(5, data.bools.getOrNull(5)?.not() ?: false) }
            )
            BoolControlItem(
                value = data.bools.getOrNull(11) ?: false,
                iconOn = R.drawable.ic_lifo,
                iconOff = R.drawable.ic_fifo,
                onClick = { onToggleBoolean(11, data.bools.getOrNull(11)?.not() ?: false) }
            )
        }
    }
}