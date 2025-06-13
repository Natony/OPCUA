package com.example.s7opcuaapp.ui.screen.control

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.example.s7opcuaapp.ui.components.IconIndicator
import com.example.s7opcuaapp.R

@Composable
fun ControlItem(
    label: String,
    isBool: Boolean = true,
    boolValue: Boolean = false,
    onToggle: (Boolean) -> Unit = {},
    intValue: Int = 0,
    onOpenDialog: () -> Unit = {},
    isWriting: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, modifier = Modifier.width(80.dp))

        if (isBool) {
            Switch(
                checked = boolValue,
                onCheckedChange = { newVal ->
                    if (!isWriting) onToggle(newVal)
                },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconIndicator(
                isOn = boolValue,
                iconOn = R.drawable.ic_status_on,
                iconOff = R.drawable.ic_status_off
            )
        } else {
            Text(
                text = intValue.toString(),
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = !isWriting) { onOpenDialog() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconIndicator(
                isOn = (intValue != 0),
                iconOn = R.drawable.ic_status_on,
                iconOff = R.drawable.ic_status_off
            )
        }
    }
}
