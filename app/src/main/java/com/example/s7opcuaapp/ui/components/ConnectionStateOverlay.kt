package com.example.s7opcuaapp.ui.components

import androidx.compose.runtime.Composable
import com.example.s7opcuaapp.viewmodel.ControlViewModel
import com.example.s7opcuaapp.domain.connection.ConnectionState
@Composable
internal fun ConnectionStateOverlay(
    connectionState: ConnectionState,
    loadingPercent: Int,
    onRetryConnection: () -> Unit
) {
    when (connectionState) {
        is ConnectionState.Connecting -> {
            LoadingOverlay(
                message = "Connecting to PLC...",
                loadingPercent = if (loadingPercent > 0) loadingPercent else null
            )
        }

        is ConnectionState.Connected -> {
            // Show loading overlay only if actively loading
            if (loadingPercent in 1..99) {
                LoadingOverlay(
                    message = "Loading data...",
                    loadingPercent = loadingPercent,
                    isIndeterminate = false
                )
            }
        }

        is ConnectionState.Failed -> {
            // Only show full overlay for critical errors
            if (!connectionState.error.contains("Connection lost")) {
                ErrorOverlay(
                    errorMessage = connectionState.error,
                    onRetryConnection = onRetryConnection
                )
            }
        }

        else -> {
            // No overlay for other states
        }
    }
}