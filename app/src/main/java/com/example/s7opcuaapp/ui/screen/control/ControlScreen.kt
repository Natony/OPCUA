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
    var preventAutoDismiss by remember { mutableStateOf(false) }

    // Monitor connection state for timeout handling
    LaunchedEffect(connectionState) {
        when (connectionState) {
            is ControlViewModel.ConnectionState.Timeout,
            is ControlViewModel.ConnectionState.MaxRetriesExceeded -> {
                Log.d("ControlScreen", "Connection timeout/max retries detected")
                showTimeoutDialog = true
                timeoutCountdown = 10
                preventAutoDismiss = false

                // Notify ViewModel that timeout dialog is showing
                onDismissTimeoutDialog() // This will disable auto-retry

                // Start countdown
                while (timeoutCountdown > 0 && showTimeoutDialog && !preventAutoDismiss) {
                    delay(1000)
                    timeoutCountdown--
                }

                // Navigate to config after countdown if not dismissed
                if (showTimeoutDialog && !preventAutoDismiss) {
                    onNavigateToConfig()
                }
            }
            is ControlViewModel.ConnectionState.Failed -> {
                // For critical failures, show timeout dialog
                if (connectionState.error.contains("timeout", ignoreCase = true) ||
                    connectionState.error.contains("max failures", ignoreCase = true)) {
                    showTimeoutDialog = true
                    timeoutCountdown = 10
                    preventAutoDismiss = true

                    while (timeoutCountdown > 0 && showTimeoutDialog) {
                        delay(1000)
                        timeoutCountdown--
                    }

                    if (showTimeoutDialog) {
                        onNavigateToConfig()
                    }
                }
            }
            else -> {
                // Clear timeout dialog for other states
                showTimeoutDialog = false
            }
        }
    }

    // Safety mechanism for stuck loading
    LaunchedEffect(connectionState, uiState.loadingPercent) {
        if (connectionState is ControlViewModel.ConnectionState.Connecting &&
            uiState.loadingPercent in 1..99) {
            delay(30000) // 30 seconds timeout
            if (connectionState is ControlViewModel.ConnectionState.Connecting &&
                uiState.loadingPercent in 1..99) {
                Log.e("ControlScreen", "Loading stuck after 30 seconds")
                showTimeoutDialog = true
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        // Main UI - Show when connected and data loaded
//        val showMainUI = when (connectionState) {
//            is ControlViewModel.ConnectionState.Offline -> true
//            is ControlViewModel.ConnectionState.Connected -> true
//            is ControlViewModel.ConnectionState.Connecting ->
//                uiState.loadingPercent > 0 // Show UI if loading started
//            else -> false
//        }

//        if (showMainUI) {
//
//            val contentAlpha = if (connectionState is ControlViewModel.ConnectionState.Offline)
//                0.8f else 1f

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
        }

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
                        message = "Connecting to PLC...",
                        loadingPercent = uiState.loadingPercent
                    )
                }
            }

            is ControlViewModel.ConnectionState.Failed -> {
                // Chỉ hiện overlay cho critical errors
                if (!connectionState.error.contains("Connection lost")) {
                    ErrorOverlay(
                        errorMessage = connectionState.error,
                        onRetryConnection = onRetryConnection
                    )
                }
            }
            else -> {
                // No overlay for Connected, Idle states
            }
        }
        // Timeout dialog
        if (showTimeoutDialog) {
            TimeoutDialog(
                timeoutCountdown = timeoutCountdown,
                onRetryConnection = {
                    showTimeoutDialog = false
                    preventAutoDismiss = false
                    onRetryConnection()
                },
                onNavigateToConfig = {
                    showTimeoutDialog = false
                    preventAutoDismiss = false
                    onNavigateToConfig()
                },
                onContinueOffline = {
                    showTimeoutDialog = false
                    preventAutoDismiss = false
                    onContinueOffline()
                }
            )
        }
//    }
}