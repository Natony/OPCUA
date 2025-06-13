package com.example.s7opcuaapp.ui.screen.config

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.example.s7opcuaapp.R
import com.example.s7opcuaapp.data.model.DeviceEntity

@Composable
fun DeviceListItem(
    device: DeviceEntity,
    isSelected: Boolean,
    onRemove: () -> Unit,
    onSelect: () -> Unit
) {
    val bgColor = if (isSelected) Color(0xFFE0F7FA) else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable { onSelect() }
            .padding(8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = device.name)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = "${device.ipAddress}:${device.port}", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(2.dp))
            if (device.opcUsername.isNotEmpty()) {
                Text(text = "User: ${device.opcUsername}", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }
        }
        IconButton(onClick = onRemove) {
            Icon(painter = painterResource(id = R.drawable.ic_delete), contentDescription = "Remove")
        }
    }
}
