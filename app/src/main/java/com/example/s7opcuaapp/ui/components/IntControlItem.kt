package com.example.s7opcuaapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.s7opcuaapp.R

/**
 * Composable hiển thị một giá trị Integer (đa trạng thái):
 *  - Label
 *  - Icon + Text đại diện cho trạng thái (dựa vào intValue)
 *  - Icon indicator On/Off (intValue != 0)
 *
 * Khi nhấn row, gọi onOpenDialog() để hiển thị dialog hoặc dropdown chọn giá trị.
 */
@Composable
fun IntControlItem(
    label: String,
    intValue: Int,
    isWriting: Boolean,
    onOpenDialog: () -> Unit
) {
    // Mapper từ code Int sang ModeState (icon + text)
    val (iconRes, modeLabel) = when (intValue) {
        0 -> R.drawable.ic_mode_idle to "Idle"
        1 -> R.drawable.ic_mode_auto to "Automatic"
        2 -> R.drawable.ic_mode_manual to "Manual"
        3 -> R.drawable.ic_mode_error to "Error"
        else -> R.drawable.ic_mode_idle to "Idle"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, modifier = Modifier.width(100.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clickable(enabled = !isWriting) {
                    onOpenDialog()
                }
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = modeLabel,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = modeLabel)
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            painter = painterResource(
                id = if (intValue != 0) R.drawable.ic_status_on else R.drawable.ic_status_off
            ),
            contentDescription = if (intValue != 0) "Active" else "Inactive",
            modifier = Modifier.size(24.dp)
        )
    }
}
