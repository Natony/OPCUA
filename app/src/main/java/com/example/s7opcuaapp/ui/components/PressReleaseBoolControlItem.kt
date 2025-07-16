package com.example.s7opcuaapp.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Simple Press/Release button with guaranteed release on up
 */
@Composable
fun PressReleaseBoolControlItem(
    value: Boolean,
    iconOn: Int,
    iconOff: Int,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    // Cleanup when disabled or unmounted
    DisposableEffect(enabled) {
        onDispose {
            if (isPressed) {
                onRelease()
                isPressed = false
            }
        }
    }

    Box(
        modifier = modifier
            .size(108.dp)
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                // On press down
                                isPressed = true
                                onPress()

                                // Wait for up - this is guaranteed to be called
                                val released = tryAwaitRelease()

                                // On release (always called, even on cancel)
                                isPressed = false
                                onRelease()
                            }
                        )
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = if (value || isPressed) iconOn else iconOff),
            contentDescription = null,
            modifier = Modifier.size(108.dp),
            tint = when {
                !enabled -> Color.Gray
                isPressed -> Color.Yellow
                else -> Color.Unspecified
            }
        )
    }
}