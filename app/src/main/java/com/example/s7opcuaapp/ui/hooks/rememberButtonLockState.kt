package com.example.s7opcuaapp.ui.hooks

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.s7opcuaapp.util.ButtonLockConfig
import com.example.s7opcuaapp.util.StatusLockConfig

/**
 * Custom hook để quản lý button lock state
 */
@Composable
fun rememberButtonLockState(
    statusValue: Int,
    statusLockConfig: StatusLockConfig,
    buttonIndex: Int,
    activeButtons: Set<Int> = emptySet(),
    busyButtons: Set<Int> = emptySet()
): ButtonLockState {
    return remember(statusValue, activeButtons, busyButtons) {
        ButtonLockState(
            isLocked = statusLockConfig.isButtonLockedInStatus(buttonIndex, statusValue) ||
                    buttonIndex in busyButtons,
            isActive = buttonIndex in activeButtons,
            canInterrupt = false // Simplify for now
        )
    }
}

data class ButtonLockState(
    val isLocked: Boolean,
    val isActive: Boolean,
    val canInterrupt: Boolean
)

/**
 * Hook để handle button press/release với lifecycle awareness
 */
@Composable
fun rememberPressReleaseHandler(
    onPress: () -> Unit,
    onRelease: () -> Unit
): PressReleaseHandler {
    var isPressed by remember { mutableStateOf(false) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && isPressed) {
                onRelease()
                isPressed = false
            }
        }
        lifecycle.addObserver(observer)

        onDispose {
            if (isPressed) {
                onRelease()
            }
            lifecycle.removeObserver(observer)
        }
    }

    return remember {
        PressReleaseHandler(
            onPressDown = {
                if (!isPressed) {
                    isPressed = true
                    onPress()
                }
            },
            onPressUp = {
                if (isPressed) {
                    isPressed = false
                    onRelease()
                }
            },
            isPressed = isPressed
        )
    }
}

data class PressReleaseHandler(
    val onPressDown: () -> Unit,
    val onPressUp: () -> Unit,
    val isPressed: Boolean
)

/**
 * Hook để debounce button actions
 */
@Composable
fun rememberDebouncedClick(
    debounceTime: Long = 300L,
    onClick: () -> Unit
): () -> Unit {
    var lastClickTime by remember { mutableStateOf(0L) }

    return remember(onClick) {
        {
            val now = System.currentTimeMillis()
            if (now - lastClickTime >= debounceTime) {
                lastClickTime = now
                onClick()
            }
        }
    }
}