package com.example.s7opcuaapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.s7opcuaapp.R

/**
 * Composable hiển thị một giá trị Boolean:
 *  - Label
 *  - Switch để toggle
 *  - Icon On/Off tương ứng
 *
 * onToggle chỉ gọi khi isWriting=false.
 */
@Composable
fun BoolControlItem(
    label: String,
    value: Boolean,
    isWriting: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, modifier = Modifier.width(100.dp))

        Switch(
            checked = value,
            onCheckedChange = { newVal ->
                if (!isWriting) {
                    onToggle(newVal)
                }
            },
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            painter = painterResource(id = if (value) R.drawable.ic_status_on else R.drawable.ic_status_off),
            contentDescription = if (value) "On" else "Off",
            modifier = Modifier.size(24.dp)
        )
    }
}
