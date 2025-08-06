package com.example.s7opcuaapp.ui.screen.control

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.s7opcuaapp.viewmodel.ControlViewModel
import kotlinx.coroutines.delay
import android.util.Log
import com.example.s7opcuaapp.ui.components.MainControlContent
import com.example.s7opcuaapp.ui.components.ErrorOverlay
import com.example.s7opcuaapp.ui.components.LoadingOverlay
import com.example.s7opcuaapp.ui.components.OfflineOverlay
import com.example.s7opcuaapp.ui.components.TimeoutDialog

@Composable
fun ControlScreen(
    uiState: ControlUiState,
    connectionState: ControlViewModel.ConnectionState,
    onNavigateToConfig: () -> Unit,
    onRetryConnection: () -> Unit,
    onToggleBoolean: (Int, Boolean) -> Unit,
    onOpenDialog: (String, Int) -> Unit,
    onConfirmNumber: (Int, Int) -> Unit,
    onDismissDialog: () -> Unit,
    onFunctionSelect: (Int) -> Unit,
    onTextChange: (Int, String) -> Unit,
    onSendAll: () -> Unit,
    onPressButton: (Int) -> Boolean,
    onReleaseButton: (Int) -> Boolean,
    onDismissTimeoutDialog: () -> Unit = {},
    onContinueOffline: () -> Unit
) {
    val data = uiState.plcData
    var isAuto by remember { mutableStateOf(true) }

    // Connection timeout dialog state
    var showTimeoutDialog by remember { mutableStateOf(false) }
    var timeoutCountdown by remember { mutableStateOf(10) }

    // Monitor connection state for timeout handling - SIMPLIFIED
    LaunchedEffect(connectionState) {
        when (connectionState) {
            is ControlViewModel.ConnectionState.MaxRetriesExceeded -> {
                Log.d("ControlScreen", "Max retries exceeded - showing timeout dialog")
                showTimeoutDialog = true
                timeoutCountdown = 10

                // Start countdown
                while (timeoutCountdown > 0 && showTimeoutDialog) {
                    delay(1000)
                    timeoutCountdown--
                }

                // Navigate to config after countdown if not dismissed
                if (showTimeoutDialog) {
                    onNavigateToConfig()
                }
            }
            else -> {
                // Clear timeout dialog for other states
                showTimeoutDialog = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main content always visible
        MainControlContent(
            uiState = uiState,
            isAuto = isAuto,
            onToggleAutoMode = { isAuto = !isAuto },
            onToggleBoolean = onToggleBoolean,
            onOpenDialog = onOpenDialog,
            onConfirmNumber = onConfirmNumber,
            onDismissDialog = onDismissDialog,
            onFunctionSelect = onFunctionSelect,
            onTextChange = onTextChange,
            onSendAll = onSendAll,
            onPressButton = onPressButton,
            onReleaseButton = onReleaseButton,
            onRetryConnection = onRetryConnection
        )

        // Simplified overlays - no duplicates
        when (connectionState) {
            is ControlViewModel.ConnectionState.Offline -> {
                OfflineOverlay(
                    onRetryConnection = onRetryConnection,
                    onExitOffline = onNavigateToConfig
                )
            }

            is ControlViewModel.ConnectionState.Connecting -> {
                if (uiState.loadingPercent in 1..99) {
                    LoadingOverlay(
                        message = "Connecting to PLC... (Attempt ${connectionState.attempt})",
                        loadingPercent = uiState.loadingPercent,
                        isIndeterminate = false
                    )
                }
            }

            is ControlViewModel.ConnectionState.Failed -> {
                // Only show notification for connection lost, not full overlay
                // This prevents duplicate error dialogs
            }

            else -> { /* No overlay */ }
        }

        // Timeout dialog only for max retries
        if (showTimeoutDialog) {
            TimeoutDialog(
                timeoutCountdown = timeoutCountdown,
                onRetryConnection = {
                    showTimeoutDialog = false
                    onRetryConnection()
                },
                onNavigateToConfig = {
                    showTimeoutDialog = false
                    onNavigateToConfig()
                },
                onContinueOffline = {
                    showTimeoutDialog = false
                    onContinueOffline()
                }
            )
        }
    }
}