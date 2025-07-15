package com.example.s7opcuaapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun PressReleaseBoolControlItem(
    value: Boolean,
    iconOn: Int,
    iconOff: Int,
    onPress: () -> Unit,
    onRelease: () -> Unit,
    enabled: Boolean = true,
    busy: Boolean = false,
    isProcessing: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    // Cleanup khi component bị unmount hoặc disable
    DisposableEffect(enabled) {
        onDispose {
            if (isPressed) {
                isPressed = false
                onRelease()
            }
        }
    }

    Box(
        modifier = modifier
            .size(108.dp)
            .then(
                if (enabled && !busy && !isProcessing) {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            try {
                                // Chờ down event
                                val down = awaitFirstDown()

                                // Gọi onPress ngay khi nhấn xuống
                                isPressed = true
                                onPress()

                                // Chờ up event hoặc cancel
                                val up = waitForUpOrCancellation()

                                // Luôn gọi onRelease dù có up hay cancel
                                isPressed = false
                                onRelease()

                            } catch (e: Exception) {
                                // Đảm bảo release nếu có lỗi
                                if (isPressed) {
                                    isPressed = false
                                    onRelease()
                                }
                            }
                        }
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
                isProcessing || isPressed -> Color.Yellow
                !enabled || busy -> Color.Gray
                else -> Color.Unspecified
            }
        )

        if (busy || isProcessing || isPressed) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}