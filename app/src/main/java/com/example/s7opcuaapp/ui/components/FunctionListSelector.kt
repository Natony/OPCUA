// File: app/src/main/java/com/example/s7opcuaapp/ui/components/FunctionListSelector.kt
package com.example.s7opcuaapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Danh sách chức năng, mỗi mục gồm label và code.
 * Chọn mục nào thì highlight, nhưng chưa gửi ngay.
 *
 * @param entries       List các cặp (label, code) của chức năng
 * @param selectedCode  Mã chức năng đang được chọn (null nếu chưa chọn)
 * @param onSelect      Callback khi chọn một mục, trả về code tương ứng
 */
@Composable
fun FunctionListSelector(
    entries: List<Pair<String, Int>>,
    selectedCode: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        entries.forEach { (label, code) ->
            val isSelected = (code == selectedCode)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onSelect(code) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else
                        MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isSelected) 4.dp else 1.dp
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
