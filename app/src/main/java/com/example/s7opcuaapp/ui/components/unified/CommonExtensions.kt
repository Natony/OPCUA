// ============================================
// 6. Common Extensions & Utilities
// ============================================

// File: ui/components/unified/CommonExtensions.kt
package com.example.s7opcuaapp.ui.components.unified

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.example.s7opcuaapp.ui.theme.UiConfig

/**
 * Extension functions for common UI operations
 */
@Composable
fun Modifier.conditionalAlpha(
    enabled: Boolean,
    isProcessing: Boolean = false,
    isAutoMode: Boolean = false
): Modifier = this.alpha(
    when {
        !enabled -> UiConfig.Buttons.DISABLED_ALPHA
        isProcessing -> UiConfig.Buttons.PROCESSING_ALPHA
        isAutoMode -> UiConfig.Buttons.AUTO_MODE_ALPHA
        else -> 1f
    }
)

@Composable
fun Modifier.buttonSize(type: ButtonSize = ButtonSize.STANDARD): Modifier =
    this.then(
        when (type) {
            ButtonSize.LARGE -> Modifier.size(UiConfig.Buttons.LARGE_SIZE)
            ButtonSize.STANDARD -> Modifier.size(UiConfig.Buttons.STANDARD_SIZE)
            ButtonSize.COMPACT -> Modifier.size(UiConfig.Buttons.COMPACT_SIZE)
            ButtonSize.SMALL -> Modifier.size(UiConfig.Buttons.SMALL_SIZE)
        }
    )

enum class ButtonSize {
    LARGE,
    STANDARD,
    COMPACT,
    SMALL
}

// Helper functions for common UI patterns
fun isButtonLocked(
    buttonIndex: Int,
    lockedButtons: Set<Int>,
    isAutoMode: Boolean = false
): Boolean {
    return buttonIndex in lockedButtons || (isAutoMode && buttonIndex < 100)
}

fun isButtonBusy(
    buttonIndex: Int,
    busyButtons: Set<Int>
): Boolean {
    return buttonIndex in busyButtons
}