package com.example.s7opcuaapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
 * Composable hiển thị một giá trị Integer (đa trạng thái) y hệt ButtonControlItem:
 *  - Label
 *  - Icon + Text đại diện cho trạng thái (dựa vào intValue)
 *  - Background + border + indicator khi isWriting hoặc active
 *
 * Khi nhấn Row, gọi onClick() (ví dụ để mở dialog chọn giá trị).
 */
@Composable
fun IntControlItem(
    label: String,
    intValue: Int,
    icons: List<Int>,
    isWriting: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Lấy icon tương ứng với state, fallback về cuối cùng nếu vượt quá
    val iconRes = icons.getOrElse(intValue) { icons.last() }

    // Xác định màu nền khi active (hãy tự điều chỉnh alpha/color theo theme)
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    // Khi muốn nhấn mạnh active state, bạn có thể so sánh intValue với 0 hay bất kỳ điều kiện nào
    val isActive = intValue != 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable(enabled = !isWriting) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            else backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isActive) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon trạng thái
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isActive)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else Color.Transparent
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = "$label: $intValue",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Label và giá trị
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                    color = if (isActive)
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = intValue.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Indicator ghi trạng thái or loading
            if (isWriting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isActive)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}
