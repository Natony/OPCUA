package com.example.s7opcuaapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color

/**
 * Composable hiển thị icon và giá trị số (Int) với khả năng click,
 * không có nền.
 *
 * @param label      Nhãn hiển thị phía trước giá trị
 * @param intValue   Giá trị số cần hiển thị
 * @param icons      Danh sách icon tương ứng với các giá trị trạng thái
 * @param onClick    Callback khi nhấn vào item
 * @param modifier   Modifier tuỳ chỉnh
 */
@Composable
fun IntControlItem(
    intValue: Int,
    icons: List<Int>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Chọn icon tương ứng với intValue, fallback icon cuối
    val iconRes = icons.getOrElse(intValue) { icons.last() }

    Row(
        modifier = modifier
            .wrapContentHeight()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(4.dp)),
                tint = Color.Unspecified
        )
        Text(
            text = intValue.toString(),
            fontSize = 16.sp
        )
    }
}
