// File: app/src/main/java/com/example/s7opcuaapp/ui/components/InlineNumberInputItem.kt
package com.example.s7opcuaapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InlineNumberInputItem(
    label: String,
    textValue: String,
    isWriting: Boolean,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.width(100.dp), fontSize = 14.sp)

        OutlinedTextField(
            value = textValue,
            onValueChange = { new ->
                if (new.all { it.isDigit() }) onTextChange(new)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
            enabled = !isWriting
        )

        if (isWriting) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        }
    }
}
