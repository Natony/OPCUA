package com.example.s7opcuaapp.ui.screen.control

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.s7opcuaapp.viewmodel.ControlViewModel
import kotlinx.coroutines.delay
import android.util.Log
import com.example.s7opcuaapp.domain.connection.ConnectionState
import com.example.s7opcuaapp.ui.components.MainControlContent
import com.example.s7opcuaapp.ui.components.ConnectionStateOverlay
import com.example.s7opcuaapp.ui.components.TimeoutDialog
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

@Composable
fun ControlScreen(
    uiState: ControlUiState,
    connectionState: ConnectionState,
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
    var shouldMonitorTimeout by remember { mutableStateOf(true) }
    // Monitor connection state for timeout handling
    LaunchedEffect(connectionState) {
        when (connectionState) {
            is ConnectionState.Connected -> {
                // IMPORTANT: Clear timeout monitoring when connected
                shouldMonitorTimeout = false
                showTimeoutDialog = false
                Log.d("ControlScreen", "Connected - clearing timeout monitoring")
            }

            is ConnectionState.Timeout,
            is ConnectionState.MaxRetriesExceeded -> {
                if (shouldMonitorTimeout) {
                    Log.d("ControlScreen", "Connection timeout/max retries detected")
                    showTimeoutDialog = true
                    timeoutCountdown = 10
                    preventAutoDismiss = false

                    onDismissTimeoutDialog()

                    while (timeoutCountdown > 0 && showTimeoutDialog && !preventAutoDismiss) {
                        delay(1000)
                        timeoutCountdown--
                    }

                    if (showTimeoutDialog && !preventAutoDismiss) {
                        onNavigateToConfig()
                    }
                }
            }

            is ConnectionState.Failed -> {
                if (shouldMonitorTimeout &&
                    (connectionState.error.contains("timeout", ignoreCase = true) ||
                            connectionState.error.contains("max failures", ignoreCase = true))) {
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

            is ConnectionState.Connecting -> {
                // Reset monitoring when starting new connection
                shouldMonitorTimeout = true
            }

            else -> {
                showTimeoutDialog = false
            }
        }
    }

    // Safety mechanism for stuck loading
    LaunchedEffect(connectionState, uiState.loadingPercent) {
        // Only monitor if we should and if still connecting
        if (shouldMonitorTimeout &&
            connectionState is ConnectionState.Connecting &&
            uiState.loadingPercent in 1..99) {

            // Start a timeout job
            val timeoutJob = launch {
                delay(30000L) // 30 seconds timeout

                // Check again after delay - IMPORTANT!
                if (shouldMonitorTimeout &&
                    connectionState is ConnectionState.Connecting &&
                    uiState.loadingPercent in 1..99) {
                    Log.e("ControlScreen", "Loading stuck after 30 seconds")
                    showTimeoutDialog = true
                }
            }

            // Cancel timeout if state changes
            try {
                // Wait for state to change
                snapshotFlow { connectionState }
                    .filter { it !is ConnectionState.Connecting }
                    .first()

                // State changed, cancel timeout
                timeoutJob.cancel()
                Log.d("ControlScreen", "Cancelled timeout monitoring - state changed")
            } catch (e: CancellationException) {
                timeoutJob.cancel()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main UI - Show when connected and data loaded
        val showMainUI =
            (connectionState is ConnectionState.Connected &&
                    (uiState.loadingPercent == 100 || uiState.loadingPercent == 0)) ||
                    connectionState is ConnectionState.Offline


        if (showMainUI) {
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

        // Connection state overlays
        if (connectionState !is ConnectionState.Offline) {
            ConnectionStateOverlay(
                connectionState = connectionState,
                loadingPercent = uiState.loadingPercent,
                onRetryConnection = onRetryConnection
            )
        }

        // Timeout dialog
        if (showTimeoutDialog) {
            TimeoutDialog(
                timeoutCountdown = timeoutCountdown,
                onRetryConnection = {
                    showTimeoutDialog = false
                    preventAutoDismiss = false
                    (uiState as? ControlViewModel)?.dismissTimeoutDialog()
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
                    // Continue in offline mode
                    onContinueOffline()
                }
            )
        }
    }
}