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
 * Count Pallet component hiển thị số lượng và nút đếm
 * @param count Số lượng pallet (từ ints[2])
 * @param isPressed Trạng thái nút (từ bools[13])
 * @param isManualMode True khi đang ở chế độ manual
 * @param onClick Callback khi nhấn nút (chỉ hoạt động ở manual mode)
 */
@Composable
fun CountPalletItem(
    count: Int,
    isPressed: Boolean,
    isManualMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .wrapContentSize()
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Số lượng - luôn hiển thị
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Nút đếm - chỉ enable khi manual mode
        Box(
            modifier = Modifier
                .size(78.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (!isManualMode) Color.Gray.copy(alpha = 0.3f)
                    else if (isPressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else Color.Transparent
                )
                .then(
                    if (isManualMode) {
                        Modifier.clickable { onClick() }
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    id = if (isPressed) com.example.s7opcuaapp.R.drawable.ic_count_pallet_on
                    else com.example.s7opcuaapp.R.drawable.ic_count_pallet_off
                ),
                contentDescription = "Count Pallet",
                modifier = Modifier.size(56.dp),
                tint = if (isManualMode) {
                    Color.Unspecified
                } else {
                    Color.Gray
                }
            )
        }
    }
}