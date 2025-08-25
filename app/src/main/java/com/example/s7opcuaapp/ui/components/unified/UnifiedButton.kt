package com.example.s7opcuaapp.ui.components.unified

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.s7opcuaapp.ui.theme.UiConfig

enum class ButtonType {
    TOGGLE,           // Simple on/off toggle
    PRESS_RELEASE,    // Press and hold
    COUNT_DISPLAY,    // Count with button
    ACTION,           // Action button (like Send All)
    INT_CONTROL       // Integer value with icons
}

data class ButtonConfig(
    val type: ButtonType,
    val value: Any? = null,
    val iconOn: Int? = null,
    val iconOff: Int? = null,
    val icons: List<Int>? = null,
    val label: String? = null,
    val count: Int? = null,
    val size: Dp = UiConfig.Buttons.STANDARD_SIZE,
    val enabled: Boolean = true,
    val isProcessing: Boolean = false,
    val isAutoMode: Boolean = false,
    val isLocked: Boolean = false,
    val onClick: (() -> Unit)? = null,
    val onPress: (() -> Unit)? = null,
    val onRelease: (() -> Unit)? = null,
    val onValueClick: (() -> Unit)? = null
)

@Composable
fun UnifiedButton(
    config: ButtonConfig,
    modifier: Modifier = Modifier
) {
    when (config.type) {
        ButtonType.TOGGLE -> ToggleButton(config, modifier)
        ButtonType.PRESS_RELEASE -> PressReleaseButton(config, modifier)
        ButtonType.COUNT_DISPLAY -> CountDisplayButton(config, modifier)
        ButtonType.ACTION -> ActionButton(config, modifier)
        ButtonType.INT_CONTROL -> IntControlButton(config, modifier)
    }
}

@Composable
private fun ToggleButton(
    config: ButtonConfig,
    modifier: Modifier = Modifier
) {
    val value = config.value as? Boolean ?: false
    val alpha = when {
        !config.enabled -> UiConfig.Buttons.DISABLED_ALPHA
        config.isProcessing -> UiConfig.Buttons.PROCESSING_ALPHA
        else -> 1f
    }

    Box(
        modifier = modifier
            .size(config.size)
            .alpha(alpha)
            .clickable(
                enabled = config.enabled && !config.isProcessing,
                onClick = { config.onClick?.invoke() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(
                id = if (value) config.iconOn!! else config.iconOff!!
            ),
            contentDescription = null,
            modifier = Modifier.size(config.size),
            tint = Color.Unspecified
        )

        if (config.isProcessing) {
            CircularProgressIndicator(
                modifier = Modifier.size(config.size * 0.3f),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun PressReleaseButton(
    config: ButtonConfig,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val value = config.value as? Boolean ?: false

    DisposableEffect(config.enabled) {
        onDispose {
            if (isPressed) {
                config.onRelease?.invoke()
                isPressed = false
            }
        }
    }

    Box(
        modifier = modifier
            .size(config.size)
            .alpha(if (config.enabled) 1f else UiConfig.Buttons.DISABLED_ALPHA)
            .then(
                if (config.enabled) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                config.onPress?.invoke()
                                tryAwaitRelease()
                                isPressed = false
                                config.onRelease?.invoke()
                            }
                        )
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(
                id = if (value || isPressed) config.iconOn!! else config.iconOff!!
            ),
            contentDescription = null,
            modifier = Modifier.size(config.size),
            tint = if (isPressed) Color(0xFFFFEB3B).copy(alpha = 0.8f)
            else Color.Unspecified
        )
    }
}

@Composable
private fun CountDisplayButton(
    config: ButtonConfig,
    modifier: Modifier = Modifier
) {
    val isPressed = config.value as? Boolean ?: false
    val alpha = when {
        !config.isAutoMode -> UiConfig.Buttons.AUTO_MODE_ALPHA
        !config.enabled -> UiConfig.Buttons.DISABLED_ALPHA
        else -> 1f
    }

    Column(
        modifier = modifier
            .wrapContentSize()
            .padding(UiConfig.Spacing.SMALL),
        verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.SMALL),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Count display
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    config.isProcessing -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            elevation = CardDefaults.cardElevation(UiConfig.Status.CARD_ELEVATION)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = config.count?.toString() ?: "0",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                )
            }
        }

        // Button
        Box(
            modifier = Modifier
                .size(config.size)
                .alpha(alpha)
                .then(
                    if (config.isAutoMode && !config.isProcessing && config.enabled) {
                        Modifier.clickable { config.onClick?.invoke() }
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    id = if (isPressed) config.iconOn!! else config.iconOff!!
                ),
                contentDescription = null,
                modifier = Modifier.size(UiConfig.Buttons.COMPACT_SIZE),
                tint = Color.Unspecified
            )

            if (config.isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    config: ButtonConfig,
    modifier: Modifier = Modifier
) {
    val isEnabled = !config.isAutoMode && !config.isProcessing && !config.isLocked
    val alpha = when {
        config.isLocked -> UiConfig.Buttons.DISABLED_ALPHA
        config.isAutoMode -> UiConfig.Buttons.AUTO_MODE_ALPHA
        config.isProcessing -> UiConfig.Buttons.PROCESSING_ALPHA
        else -> 1f
    }

    val buttonText = when {
        config.isLocked -> "KHÓA"
        config.isProcessing -> "Đang xử lý..."
        else -> config.label ?: "CHẠY"
    }

    Button(
        onClick = { config.onClick?.invoke() },
        enabled = isEnabled,
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha),
        colors = ButtonDefaults.buttonColors(
            containerColor = when {
                config.isLocked -> MaterialTheme.colorScheme.surfaceVariant
                config.isAutoMode -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.primary
            }
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (config.isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = buttonText,
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun IntControlButton(
    config: ButtonConfig,
    modifier: Modifier = Modifier
) {
    val intValue = config.value as? Int ?: 0
    val icons = config.icons ?: emptyList()
    val iconRes = icons.getOrElse(intValue) { icons.lastOrNull() ?: 0 }

    Column(
        modifier = modifier
            .wrapContentSize()
            .clickable(
                enabled = config.enabled && !config.isProcessing,
                onClick = { config.onValueClick?.invoke() }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(UiConfig.Spacing.SMALL)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .alpha(
                        when {
                            !config.enabled -> UiConfig.Buttons.DISABLED_ALPHA
                            config.isProcessing -> UiConfig.Buttons.PROCESSING_ALPHA
                            else -> 1f
                        }
                    ),
                tint = Color.Unspecified
            )

            if (config.isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Text(
            text = intValue.toString(),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(
                alpha = when {
                    !config.enabled -> UiConfig.Buttons.DISABLED_ALPHA
                    config.isProcessing -> UiConfig.Buttons.PROCESSING_ALPHA
                    else -> 1f
                }
            )
        )
    }
}