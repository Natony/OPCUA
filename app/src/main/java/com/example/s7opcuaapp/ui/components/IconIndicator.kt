package com.example.s7opcuaapp.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * Hiển thị icon ON/OFF:
 * - isOn: nếu true dùng iconOn, else iconOff
 */
@Composable
fun IconIndicator(
    isOn: Boolean,
    iconOn: Int,
    iconOff: Int,
    modifier: Modifier = Modifier
) {
    val resId = if (isOn) iconOn else iconOff
    Icon(
        painter = painterResource(id = resId),
        contentDescription = null,
        tint = if (isOn) Color.Green else Color.Gray,
        modifier = modifier.size(24.dp)
    )
}
