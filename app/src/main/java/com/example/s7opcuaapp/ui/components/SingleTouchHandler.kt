package com.example.s7opcuaapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.*
import kotlinx.coroutines.coroutineScope

/**
 * A composable that ensures only one touch point is active at a time.
 * Any additional touches will be ignored until the first touch is released.
 */
@Composable
fun SingleTouchHandler(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var activeTouchId by remember { mutableStateOf<Long?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                coroutineScope {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)

                            event.changes.forEach { change ->
                                when {
                                    // New touch down
                                    change.pressed && !change.previousPressed -> {
                                        if (activeTouchId == null) {
                                            // This is the first touch, allow it
                                            activeTouchId = change.id.value
                                        } else if (change.id.value != activeTouchId) {
                                            // This is an additional touch, consume it
                                            change.consume()
                                        }
                                    }

                                    // Touch up
                                    !change.pressed && change.previousPressed -> {
                                        if (change.id.value == activeTouchId) {
                                            // The active touch was released
                                            activeTouchId = null
                                        }
                                    }

                                    // Ongoing touch
                                    change.pressed -> {
                                        if (activeTouchId != null && change.id.value != activeTouchId) {
                                            // This is not the active touch, consume it
                                            change.consume()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
    ) {
        content()
    }
}

/**
 * Provides current touch state to child composables
 */
data class TouchState(
    val isEnabled: Boolean = true
)

val LocalTouchState = compositionLocalOf { TouchState() }