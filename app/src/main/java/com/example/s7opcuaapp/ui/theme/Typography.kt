package com.example.s7opcuaapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Đổi tên biến thành AppTypography (hoặc tên tùy ý)
val AppTypography = Typography(
    // Bạn có thể giữ mặc định hoặc tuỳ chỉnh các TextStyle ở đây:
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    )
    // … thêm nếu cần
)
