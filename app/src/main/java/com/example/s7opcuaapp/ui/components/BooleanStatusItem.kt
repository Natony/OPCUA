// File: BooleanStatusItem.kt
package com.example.s7opcuaapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Hiển thị trạng thái Boolean (On/Off) với style giống BoolControlItem
 * ★ Read‑only, không gửi hay toggle gì
 */
@Composable
fun BooleanStatusItem(
    label: String,
    value: Boolean,
    iconOn: Int,
    iconOff: Int,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (value) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val textColor = if (value) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (value) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon lớn
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (value) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else Color.Transparent
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = if (value) iconOn else iconOff),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
            }

            Spacer(Modifier.width(12.dp))

            // Label
            Text(
                text = label,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = if (value) FontWeight.Medium else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )

            // Indicator nhỏ báo active/inactive
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (value) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
            )
        }
    }
}
