package com.example.s7opcuaapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.ColorFilter
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

    Box(
        modifier = modifier
            .size(108.dp)
            .then(
                if (enabled && !busy && !isProcessing) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { offset ->
                                isPressed = true
                                onPress()

                                val released = try {
                                    tryAwaitRelease()
                                } catch (e: Exception) {
                                    false
                                }

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
                isProcessing || isPressed -> Color.Yellow
                !enabled -> Color.Gray
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