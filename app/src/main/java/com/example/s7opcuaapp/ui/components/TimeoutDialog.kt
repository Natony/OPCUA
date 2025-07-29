package com.example.s7opcuaapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun TimeoutDialog(
    timeoutCountdown: Int,
    onRetryConnection: () -> Unit,
    onNavigateToConfig: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Can't dismiss */ },
        title = { Text("Connection Timeout") },
        text = {
            Column {
                Text("Unable to connect to PLC.")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Returning to config in $timeoutCountdown seconds...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onRetryConnection) {
                Text("Retry Connection")
            }
        },
        dismissButton = {
            TextButton(onClick = onNavigateToConfig) {
                Text("Go to Config")
            }
        }
    )
}