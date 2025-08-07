package com.example.s7opcuaapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.s7opcuaapp.R
import com.example.s7opcuaapp.data.model.PlcData

/**
 * Reusable grid component for control buttons
 */
@Composable
fun ControlButtonGrid(
    items: List<ControlButtonItem>,
    columns: Int = 3,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { item ->
            when (item) {
                is ControlButtonItem.BoolButton -> {
                    BoolControlItem(
                        value = item.value,
                        iconOn = item.iconOn,
                        iconOff = item.iconOff,
                        onClick = item.onClick,
                        enabled = item.enabled,
                        isProcessing = item.isProcessing
                    )
                }
                is ControlButtonItem.IntButton -> {
                    IntControlItem(
                        intValue = item.value,
                        icons = item.icons,
                        onClick = item.onClick,
                        enabled = item.enabled,
                        isProcessing = item.isProcessing
                    )
                }
                is ControlButtonItem.PressReleaseButton -> {
                    PressReleaseBoolControlItem(
                        value = item.value,
                        iconOn = item.iconOn,
                        iconOff = item.iconOff,
                        onPress = item.onPress,
                        onRelease = item.onRelease,
                        enabled = item.enabled
                    )
                }
            }
        }
    }
}

/**
 * Sealed class cho các loại button
 */
sealed class ControlButtonItem {
    data class BoolButton(
        val index: Int,
        val value: Boolean,
        val iconOn: Int,
        val iconOff: Int,
        val onClick: () -> Unit,
        val enabled: Boolean = true,
        val isProcessing: Boolean = false
    ) : ControlButtonItem()

    data class IntButton(
        val index: Int,
        val value: Int,
        val icons: List<Int>,
        val onClick: () -> Unit,
        val enabled: Boolean = true,
        val isProcessing: Boolean = false
    ) : ControlButtonItem()

    data class PressReleaseButton(
        val index: Int,
        val value: Boolean,
        val iconOn: Int,
        val iconOff: Int,
        val onPress: () -> Unit,
        val onRelease: () -> Unit,
        val enabled: Boolean = true
    ) : ControlButtonItem()
}

/**
 * Helper function để tạo button items từ PlcData
 */
fun createManualButtons(
    data: PlcData,
    onPress: (Int) -> Unit,
    onRelease: (Int) -> Unit,
    lockedButtons: Set<Int>
): List<ControlButtonItem> {
    return listOf(
        ControlButtonItem.PressReleaseButton(
            index = 0,
            value = data.bools.getOrNull(0) ?: false,
            iconOn = R.drawable.ic_shuttle_forward_on,
            iconOff = R.drawable.ic_shuttle_forward_off,
            onPress = { onPress(0) },
            onRelease = { onRelease(0) },
            enabled = 0 !in lockedButtons
        ),
        ControlButtonItem.PressReleaseButton(
            index = 1,
            value = data.bools.getOrNull(1) ?: false,
            iconOn = R.drawable.ic_shuttle_reverse_on,
            iconOff = R.drawable.ic_shuttle_reverse_off,
            onPress = { onPress(1) },
            onRelease = { onRelease(1) },
            enabled = 1 !in lockedButtons
        ),
        ControlButtonItem.PressReleaseButton(
            index = 2,
            value = data.bools.getOrNull(2) ?: false,
            iconOn = R.drawable.ic_shuttle_up_on,
            iconOff = R.drawable.ic_shuttle_up_off,
            onPress = { onPress(2) },
            onRelease = { onRelease(2) },
            enabled = 2 !in lockedButtons
        ),
        ControlButtonItem.PressReleaseButton(
            index = 3,
            value = data.bools.getOrNull(3) ?: false,
            iconOn = R.drawable.ic_shuttle_down_on,
            iconOff = R.drawable.ic_shuttle_down_off,
            onPress = { onPress(3) },
            onRelease = { onRelease(3) },
            enabled = 3 !in lockedButtons
        )
    )
}