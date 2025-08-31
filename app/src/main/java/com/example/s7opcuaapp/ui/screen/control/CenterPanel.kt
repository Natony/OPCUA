package com.example.s7opcuaapp.ui.screen.control

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.s7opcuaapp.R
import com.example.s7opcuaapp.ui.components.*

import com.example.s7opcuaapp.ui.components.unified.ComponentFactory
import com.example.s7opcuaapp.ui.components.unified.StatusDisplayConfig
import com.example.s7opcuaapp.ui.components.unified.StatusDisplayType
import com.example.s7opcuaapp.ui.components.unified.UnifiedButton
import com.example.s7opcuaapp.ui.components.unified.UnifiedStatusDisplay
import com.example.s7opcuaapp.ui.theme.UiConfig

@Composable
fun CenterPanel(
    uiState: ControlUiState,
    isAuto: Boolean,
    onToggleBoolean: (Int, Boolean) -> Unit,
    onFunctionSelect: (Int) -> Unit,
    onTextChange: (Int, String) -> Unit,
    onSendAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val data = uiState.plcData
    val SEND_ALL_BUTTON_INDEX = 999
    val isSendAllLocked = SEND_ALL_BUTTON_INDEX in uiState.lockedButtons

    Column(
        modifier = modifier.fillMaxHeight().padding(1.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Positions row - using unified MULTI_STATE status displays
        Row(
            modifier = Modifier.weight(0.25f),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Positions row - Each position has its own unique icons
            Row(
                modifier = Modifier.weight(0.25f),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Position 1
                MultiStateStatusItem(
                    label = "Pos1",
                    intValue = data.ints.getOrNull(15) ?: 0,
                    icons = listOf(
                        R.drawable.ic_pos1_state0,
                        R.drawable.ic_pos1_state1,
                        R.drawable.ic_pos1_state2
                    ),
                    modifier = Modifier.size(36.dp)
                )

                // Position 2
                MultiStateStatusItem(
                    label = "Pos2",
                    intValue = data.ints.getOrNull(16) ?: 0,
                    icons = listOf(
                        R.drawable.ic_pos2_state0,
                        R.drawable.ic_pos2_state1,
                        R.drawable.ic_pos2_state2
                    ),
                    modifier = Modifier.size(36.dp)
                )

                // Position 3
                MultiStateStatusItem(
                    label = "Pos3",
                    intValue = data.ints.getOrNull(17) ?: 0,
                    icons = listOf(
                        R.drawable.ic_pos3_state0,
                        R.drawable.ic_pos3_state1,
                        R.drawable.ic_pos3_state2
                    ),
                    modifier = Modifier.size(36.dp)
                )

                // Position 4
                MultiStateStatusItem(
                    label = "Pos4",
                    intValue = data.ints.getOrNull(18) ?: 0,
                    icons = listOf(
                        R.drawable.ic_pos4_state0,
                        R.drawable.ic_pos4_state1,
                        R.drawable.ic_pos4_state2
                    ),
                    modifier = Modifier.size(36.dp)
                )

                // Position 5
                MultiStateStatusItem(
                    label = "Pos5",
                    intValue = data.ints.getOrNull(19) ?: 0,
                    icons = listOf(
                        R.drawable.ic_pos5_state0,
                        R.drawable.ic_pos5_state1,
                        R.drawable.ic_pos5_state2
                    ),
                    modifier = Modifier.size(36.dp)
                )

                // Position 6
                MultiStateStatusItem(
                    label = "Pos6",
                    intValue = data.ints.getOrNull(20) ?: 0,
                    icons = listOf(
                        R.drawable.ic_pos6_state0,
                        R.drawable.ic_pos6_state1,
                        R.drawable.ic_pos6_state2
                    ),
                    modifier = Modifier.size(36.dp)
                )

                // Position 7
                MultiStateStatusItem(
                    label = "Pos7",
                    intValue = data.ints.getOrNull(21) ?: 0,
                    icons = listOf(
                        R.drawable.ic_pos7_state0,
                        R.drawable.ic_pos7_state1,
                        R.drawable.ic_pos7_state2
                    ),
                    modifier = Modifier.size(36.dp)
                )

                // Position 8
                MultiStateStatusItem(
                    label = "Pos8",
                    intValue = data.ints.getOrNull(22) ?: 0,
                    icons = listOf(
                        R.drawable.ic_pos8_state0,
                        R.drawable.ic_pos8_state1,
                        R.drawable.ic_pos8_state2
                    ),
                    modifier = Modifier.size(36.dp)
                )

                // Position 9
                MultiStateStatusItem(
                    label = "Pos9",
                    intValue = data.ints.getOrNull(23) ?: 0,
                    icons = listOf(
                        R.drawable.ic_pos9_state0,
                        R.drawable.ic_pos9_state1,
                        R.drawable.ic_pos9_state2
                    ),
                    modifier = Modifier.size(36.dp)
                )

                // Position 10
                MultiStateStatusItem(
                    label = "Pos10",
                    intValue = data.ints.getOrNull(24) ?: 0,
                    icons = listOf(
                        R.drawable.ic_pos10_state0,
                        R.drawable.ic_pos10_state1,
                        R.drawable.ic_pos10_state2
                    ),
                    modifier = Modifier.size(36.dp)
                )

                // Position 11
                MultiStateStatusItem(
                    label = "Pos11",
                    intValue = data.ints.getOrNull(25) ?: 0,
                    icons = listOf(
                        R.drawable.ic_pos11_state0,
                        R.drawable.ic_pos11_state1,
                        R.drawable.ic_pos11_state2
                    ),
                    modifier = Modifier.size(36.dp)
                )

                // Position 12
                MultiStateStatusItem(
                    label = "Pos12",
                    intValue = data.ints.getOrNull(26) ?: 0,
                    icons = listOf(
                        R.drawable.ic_pos12_state0,
                        R.drawable.ic_pos12_state1,
                        R.drawable.ic_pos12_state2
                    ),
                    modifier = Modifier.size(36.dp)
                )

                // Position 13
                MultiStateStatusItem(
                    label = "Pos13",
                    intValue = data.ints.getOrNull(27) ?: 0,
                    icons = listOf(
                        R.drawable.ic_pos13_state0,
                        R.drawable.ic_pos13_state1,
                        R.drawable.ic_pos13_state2
                    ),
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Start/End/Actual/Function/Send row
        Row(
            modifier = Modifier.weight(0.5f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Start point cluster - using unified NUMERIC status
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Điểm bắt đầu", style = MaterialTheme.typography.bodySmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(3) { index ->
                        val fieldIndex = 5 + index  // XS=5, YS=6, ZS=7
                        UnifiedStatusDisplay(
                            config = StatusDisplayConfig(
                                type = StatusDisplayType.INLINE_NUMBER,
                                value = uiState.intInputs[fieldIndex]
                                    ?: (data.ints.getOrNull(fieldIndex)?.toString() ?: "0")
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Điểm kết thúc", style = MaterialTheme.typography.bodySmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(3) { index ->
                        val fieldIndex = 8 + index  // XE=8, YE=9, ZE=10
                        UnifiedStatusDisplay(
                            config = StatusDisplayConfig(
                                type = StatusDisplayType.INLINE_NUMBER,
                                value = uiState.intInputs[fieldIndex]
                                    ?: (data.ints.getOrNull(fieldIndex)?.toString() ?: "0")
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Điểm thực tế", style = MaterialTheme.typography.bodySmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(3) { index ->
                        val fieldIndex = 11 + index  // XE=11, YE=12, ZE=13
                        UnifiedStatusDisplay(
                            config = StatusDisplayConfig(
                                type = StatusDisplayType.INLINE_NUMBER,
                                value = uiState.intInputs[fieldIndex]
                                    ?: (data.ints.getOrNull(fieldIndex)?.toString() ?: "0")
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Send All button - using unified ACTION button
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                UnifiedButton(
                    config = ComponentFactory.actionButton(
                        label = "CHẠY",
                        onClick = onSendAll,
                        isAutoMode = isAuto,
                        isProcessing = uiState.isWriting,
                        isLocked = isSendAllLocked
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}