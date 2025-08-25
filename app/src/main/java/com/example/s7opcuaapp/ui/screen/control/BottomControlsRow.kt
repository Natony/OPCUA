package com.example.s7opcuaapp.ui.screen.control

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.s7opcuaapp.data.model.PlcData
import com.example.s7opcuaapp.R
import com.example.s7opcuaapp.ui.components.*
import com.example.s7opcuaapp.ui.components.unified.ComponentFactory
import com.example.s7opcuaapp.ui.components.unified.UnifiedButton
import com.example.s7opcuaapp.ui.components.unified.UnifiedStatusDisplay
import com.example.s7opcuaapp.ui.components.unified.isButtonBusy
import com.example.s7opcuaapp.ui.components.unified.isButtonLocked

@Composable
fun BottomControlsRow(
    isAuto: Boolean,
    data: PlcData,
    onToggleBoolean: (Int, Boolean) -> Unit,
    onToggleAutoMode: () -> Unit,
    modifier: Modifier = Modifier,
    lockedButtons: Set<Int>,
    busyButtons: Set<Int>,
    isProcessing: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Power button - using factory helper
            Box(
                modifier = Modifier.weight(1f).size(78.dp).padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                UnifiedButton(
                    config = ComponentFactory.toggleButton(
                        value = data.bools.getOrNull(4) ?: false,
                        iconOn = R.drawable.ic_power_on,
                        iconOff = R.drawable.ic_power_off,
                        onClick = { onToggleBoolean(4, !data.bools.getOrNull(4)!!) },
                        enabled = !isButtonLocked(4, lockedButtons),
                        isProcessing = isButtonBusy(4, busyButtons)
                    )
                )
            }

            // FIFO/LIFO button
            Box(
                modifier = Modifier.weight(1f).size(78.dp).padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                UnifiedButton(
                    config = ComponentFactory.toggleButton(
                        value = data.bools.getOrNull(11) ?: false,
                        iconOn = R.drawable.ic_lifo,
                        iconOff = R.drawable.ic_fifo,
                        onClick = { onToggleBoolean(11, !data.bools.getOrNull(11)!!) },
                        enabled = !isButtonLocked(11, lockedButtons),
                        isProcessing = isButtonBusy(11, busyButtons)
                    )
                )
            }

            // Auto/Manual mode button
            Box(
                modifier = Modifier.weight(1f).size(78.dp).padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                UnifiedButton(
                    config = ComponentFactory.toggleButton(
                        value = isAuto,
                        iconOn = R.drawable.ic_mode_auto,
                        iconOff = R.drawable.ic_mode_manual,
                        onClick = onToggleAutoMode,
                        enabled = true,
                        isProcessing = false
                    )
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f).padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Shuttle status - using unified BOOLEAN status
            Box(
                modifier = Modifier.size(156.dp).fillMaxSize().weight(0.7f),
                contentAlignment = Alignment.Center
            ) {
                UnifiedStatusDisplay(
                    config = ComponentFactory.booleanStatus(
                        value = data.bools.getOrNull(12) ?: false,
                        iconOn = R.drawable.ic_green,
                        iconOff = R.drawable.ic_red,
                        label = "Shuttle",
                        isCompact = false
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Emergency stop button
            Box(
                modifier = Modifier.size(78.dp).weight(0.3f),
                contentAlignment = Alignment.Center
            ) {
                UnifiedButton(
                    config = ComponentFactory.toggleButton(
                        value = data.bools.getOrNull(10) ?: false,
                        iconOn = R.drawable.ic_emergency_stop,
                        iconOff = R.drawable.ic_emergency_stop,
                        onClick = { onToggleBoolean(10, !data.bools.getOrNull(10)!!) },
                        enabled = !isButtonLocked(10, lockedButtons),
                        isProcessing = isButtonBusy(10, busyButtons)
                    )
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f).padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Count pallet - using unified COUNT_DISPLAY button
            Box(
                modifier = Modifier.weight(1f).size(78.dp).padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                UnifiedButton(
                    config = ComponentFactory.countButton(
                        count = data.ints.getOrNull(2) ?: 0,
                        isPressed = data.bools.getOrNull(14) ?: false,
                        iconOn = R.drawable.ic_count_pallet_on,
                        iconOff = R.drawable.ic_count_pallet_off,
                        isManualMode = !isAuto,
                        onClick = {
                            if (!isAuto) {
                                onToggleBoolean(14, !data.bools.getOrNull(14)!!)
                            }
                        },
                        enabled = !isButtonLocked(14, lockedButtons),
                        isProcessing = isButtonBusy(14, busyButtons)
                    )
                )
            }
        }
    }
}