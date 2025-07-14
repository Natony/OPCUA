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
            modifier = Modifier
                .weight(1f)
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = modifier
                    .weight(1f)
                    .size(78.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                BoolControlItem(
                    value = data.bools.getOrNull(4) ?: false,
                    iconOn = R.drawable.ic_power_on,
                    iconOff = R.drawable.ic_power_off,
                    onClick = { onToggleBoolean(4, data.bools.getOrNull(4)?.not() ?: false) }
                )
            }

            Box(
                modifier = modifier
                    .weight(1f)
                    .size(78.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                BoolControlItem(
                    value = data.bools.getOrNull(11) ?: false,
                    iconOn = R.drawable.ic_lifo,
                    iconOff = R.drawable.ic_fifo,
                    onClick = { onToggleBoolean(11, data.bools.getOrNull(11)?.not() ?: false) }
                )
            }

            Box(
                modifier = modifier
                    .weight(1f)
                    .size(78.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                BoolControlItem(
                    value = isAuto,
                    iconOn = R.drawable.ic_mode_auto,
                    iconOff = R.drawable.ic_mode_manual,
                    onClick = onToggleAutoMode // Sửa thành callback
                )
            }

        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = modifier
                    .size(108.dp)
                    .fillMaxSize()
                    .weight(0.6f),
                contentAlignment = Alignment.Center
            ) {
                BooleanStatusItem(
                    label = "Shuttle",
                    value = data.bools.getOrNull(12) ?: false,
                    iconOn = R.drawable.ic_green,
                    iconOff = R.drawable.ic_red
                )
            }

            Box(
                modifier = modifier
                    .size(78.dp)
                    .weight(0.4f),
                contentAlignment = Alignment.Center
            ) {
                BoolControlItem(
                    value = data.bools.getOrNull(10) ?: false,
                    iconOn = R.drawable.ic_emergency_stop,
                    iconOff = R.drawable.ic_emergency_stop,
                    onClick = { onToggleBoolean(10, data.bools.getOrNull(10)?.not() ?: false) }
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = modifier
                    .weight(1f)
                    .size(78.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                BoolControlItem(
                    value = data.bools.getOrNull(5) ?: false,
                    iconOn = R.drawable.ic_buzzer_on,
                    iconOff = R.drawable.ic_buzzer_off,
                    onClick = { onToggleBoolean(5, data.bools.getOrNull(5)?.not() ?: false) }
                )
            }

            Box(
                modifier = modifier
                    .weight(1f)
                    .size(78.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                BoolControlItem(
                    value = data.bools.getOrNull(13) ?: false,
                    iconOn = R.drawable.ic_direction_a,
                    iconOff = R.drawable.ic_direction_b,
                    onClick = { onToggleBoolean(13, data.bools.getOrNull(13)?.not() ?: false) }
                )
            }

            Box(
                modifier = modifier
                    .weight(1f)
                    .size(78.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                CountPalletItem(
                    count = data.ints.getOrNull(2) ?: 0,
                    isPressed = data.bools.getOrNull(14) ?: false, // Sử dụng bool[13]
                    isManualMode = !isAuto, // Enable khi KHÔNG phải auto mode
                    onClick = {
                        // Chỉ cho phép nhấn khi đang ở manual mode
                        if (!isAuto) {
                            onToggleBoolean(14, data.bools.getOrNull(14)?.not() ?: false)
                        }
                    }
                )
            }
        }
    }
}