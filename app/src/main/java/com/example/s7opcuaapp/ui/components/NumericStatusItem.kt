// File: NumericStatusItem.kt
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Hiển thị chỉ số số (Int) dạng read‑only,
 * không có icon, giữ style giống MultiStateStatusItem.
 */
@Composable
fun NumericStatusItem(
    label: String,
    value: Int,
    modifier: Modifier = Modifier
) {
    val isActive = value != 0
    val backgroundColor = if (isActive)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    else
        MaterialTheme.colorScheme.surface

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = value.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Indicator nhỏ báo active/inactive
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isActive)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
            )
        }
    }
}
