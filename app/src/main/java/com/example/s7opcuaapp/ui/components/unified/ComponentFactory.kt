package com.example.s7opcuaapp.ui.components.unified

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.s7opcuaapp.ui.theme.UiConfig

/**
 * Factory object to simplify component creation
 */
object ComponentFactory {

    // Button creation helpers
    fun toggleButton(
        value: Boolean,
        iconOn: Int,
        iconOff: Int,
        onClick: () -> Unit,
        enabled: Boolean = true,
        isProcessing: Boolean = false,
        size: Dp = UiConfig.Buttons.STANDARD_SIZE
    ) = ButtonConfig(
        type = ButtonType.TOGGLE,
        value = value,
        iconOn = iconOn,
        iconOff = iconOff,
        onClick = onClick,
        enabled = enabled,
        isProcessing = isProcessing,
        size = size
    )

    fun pressReleaseButton(
        value: Boolean,
        iconOn: Int,
        iconOff: Int,
        onPress: () -> Unit,
        onRelease: () -> Unit,
        enabled: Boolean = true,
        size: Dp = UiConfig.Buttons.LARGE_SIZE
    ) = ButtonConfig(
        type = ButtonType.PRESS_RELEASE,
        value = value,
        iconOn = iconOn,
        iconOff = iconOff,
        onPress = onPress,
        onRelease = onRelease,
        enabled = enabled,
        size = size
    )

    fun countButton(
        count: Int,
        isPressed: Boolean,
        iconOn: Int,
        iconOff: Int,
        isManualMode: Boolean,
        onClick: () -> Unit,
        enabled: Boolean = true,
        isProcessing: Boolean = false
    ) = ButtonConfig(
        type = ButtonType.COUNT_DISPLAY,
        value = isPressed,
        iconOn = iconOn,
        iconOff = iconOff,
        count = count,
        isAutoMode = !isManualMode,
        onClick = onClick,
        enabled = enabled,
        isProcessing = isProcessing,
        size = UiConfig.Buttons.STANDARD_SIZE
    )

    fun actionButton(
        label: String,
        onClick: () -> Unit,
        isAutoMode: Boolean = false,
        isProcessing: Boolean = false,
        isLocked: Boolean = false
    ) = ButtonConfig(
        type = ButtonType.ACTION,
        label = label,
        onClick = onClick,
        isAutoMode = isAutoMode,
        isProcessing = isProcessing,
        isLocked = isLocked
    )

    // Status display creation helpers
    fun booleanStatus(
        value: Boolean,
        iconOn: Int,
        iconOff: Int,
        label: String? = null,
        isCompact: Boolean = false
    ) = StatusDisplayConfig(
        type = StatusDisplayType.BOOLEAN,
        value = value,
        icons = listOf(iconOn, iconOff),
        label = label,
        isCompact = isCompact
    )

    fun multiStateStatus(
        label: String,
        value: Int,
        icons: List<Int>,
        showIndicator: Boolean = true,
        isCompact: Boolean = false
    ) = StatusDisplayConfig(
        type = StatusDisplayType.MULTI_STATE,
        label = label,
        value = value,
        icons = icons,
        showIndicator = showIndicator,
        isCompact = isCompact
    )

    fun numericStatus(
        value: Int,
        label: String? = null
    ) = StatusDisplayConfig(
        type = StatusDisplayType.NUMERIC,
        value = value,
        label = label
    )

    fun textStatus(
        value: Int,
        statuses: List<String>,
        label: String? = null
    ) = StatusDisplayConfig(
        type = StatusDisplayType.TEXT,
        value = value,
        statuses = statuses,
        label = label
    )

    fun batteryStatus(
        level: Int,
        icons: List<Int>,
        thresholds: List<Int> = listOf(20, 80)
    ) = StatusDisplayConfig(
        type = StatusDisplayType.BATTERY,
        value = level,
        icons = icons,
        thresholds = thresholds
    )

    // Overlay creation helpers
    fun loadingOverlay(
        message: String,
        progressValue: Int? = null
    ) = OverlayConfig(
        type = OverlayType.LOADING,
        message = message,
        progressValue = progressValue,
        isIndeterminate = progressValue == null,
        showProgress = true
    )

    fun errorOverlay(
        message: String,
        onRetry: (() -> Unit)? = null
    ) = OverlayConfig(
        type = OverlayType.ERROR,
        message = message,
        showRetry = onRetry != null,
        onRetry = onRetry
    )

    fun offlineOverlay(
        onRetry: () -> Unit,
        onSettings: () -> Unit
    ) = OverlayConfig(
        type = OverlayType.OFFLINE,
        message = "View only - controls disabled",
        showRetry = true,
        showSettings = true,
        onRetry = onRetry,
        onSettings = onSettings
    )

    fun timeoutOverlay(
        message: String,
        countdown: Int,
        onRetry: () -> Unit,
        onSettings: () -> Unit,
        onOffline: () -> Unit
    ) = OverlayConfig(
        type = OverlayType.TIMEOUT,
        message = message,
        countdown = countdown,
        showRetry = true,
        showSettings = true,
        showOfflineMode = true,
        onRetry = onRetry,
        onSettings = onSettings,
        onOffline = onOffline
    )
}
