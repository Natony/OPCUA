package com.example.s7opcuaapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color

/**
 * Composable hiển thị icon và giá trị số (Int) với khả năng click,
 * không có nền.
 *
 * @param intValue   Giá trị số cần hiển thị
 * @param icons      Danh sách icon tương ứng với các giá trị trạng thái
 * @param onClick    Callback khi nhấn vào item
 * @param enabled    Cho phép nhấn
 * @param isProcessing Hiển thị trạng thái đang xử lý
 */
@Composable
fun IntControlItem(
    intValue: Int,
    icons: List<Int>,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isProcessing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val iconRes = icons.getOrElse(intValue) { icons.last() }

    Column(
        modifier = modifier
            .wrapContentSize()
            .clickable(enabled = enabled && !isProcessing) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)  // giảm khoảng cách
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)  // giảm kích thước để phù hợp
                    .clip(RoundedCornerShape(4.dp)),
                tint = when {
                    isProcessing -> Color.Yellow
                    !enabled -> Color.Gray
                    else -> Color.Unspecified
                }
            )

            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.Center),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Text(
            text = intValue.toString(),
            fontSize = 14.sp,
            color = when {
                isProcessing -> MaterialTheme.colorScheme.primary
                !enabled -> Color.Gray
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
