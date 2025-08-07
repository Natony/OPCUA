package com.example.s7opcuaapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.s7opcuaapp.viewmodel.ControlViewModel

@Composable
fun ConnectionStatusBar(
    connectionState: ControlViewModel.ConnectionState,
    onRetryConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = connectionState !is ControlViewModel.ConnectionState.Connected,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (connectionState) {
                    is ControlViewModel.ConnectionState.Connecting -> Color(0xFFFFF9C4)
                    is ControlViewModel.ConnectionState.Failed -> Color(0xFFFFEBEE)
                    is ControlViewModel.ConnectionState.Offline -> Color(0xFFE0E0E0)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = when (connectionState) {
                            is ControlViewModel.ConnectionState.Offline -> Icons.Default.WifiOff
                            is ControlViewModel.ConnectionState.Failed -> Icons.Default.ErrorOutline
                            is ControlViewModel.ConnectionState.Connecting -> Icons.Default.Sync
                            else -> Icons.Default.Info
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = getConnectionMessage(connectionState),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (connectionState is ControlViewModel.ConnectionState.Failed ||
                    connectionState is ControlViewModel.ConnectionState.Offline) {
                    TextButton(onClick = onRetryConnection) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

private fun getConnectionMessage(state: ControlViewModel.ConnectionState): String {
    return when (state) {
        is ControlViewModel.ConnectionState.Connecting ->
            "Connecting... (Attempt ${state.attempt}/3)"
        is ControlViewModel.ConnectionState.Failed ->
            "Connection failed: ${state.error}"
        is ControlViewModel.ConnectionState.Offline ->
            "Offline Mode - View Only"
        is ControlViewModel.ConnectionState.Timeout ->
            "Connection timeout"
        is ControlViewModel.ConnectionState.MaxRetriesExceeded ->
            "Max retries exceeded"
        else -> ""
    }
}