package com.example.s7opcuaapp.ui.screen.control

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.s7opcuaapp.viewmodel.ControlViewModel
import kotlinx.coroutines.delay
import android.util.Log
import androidx.compose.material.icons.*
import androidx.compose.material.icons.filled.*
import com.example.s7opcuaapp.ui.components.ConnectionLostNotification
import com.example.s7opcuaapp.ui.components.MainControlContent
import com.example.s7opcuaapp.ui.components.LoadingOverlay
import com.example.s7opcuaapp.ui.components.OfflineOverlay
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.*

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
    var timeoutCountdown by remember { mutableStateOf(30) }
    var countdownJob by remember { mutableStateOf<Job?>(null) }

    // Monitor connection state for timeout handling - SIMPLIFIED
    LaunchedEffect(connectionState) {
        when (connectionState) {
            is ControlViewModel.ConnectionState.MaxRetriesExceeded -> {
                Log.d("ControlScreen", "Max retries exceeded - showing timeout dialog")

                // Cancel any existing countdown
                countdownJob?.cancel()

                // Show dialog with fresh countdown
                showTimeoutDialog = true
                timeoutCountdown = 30 // 30 seconds instead of 10

                // Start countdown in separate job
                countdownJob = launch {
                    try {
                        while (timeoutCountdown > 0 && showTimeoutDialog) {
                            delay(1000)
                            timeoutCountdown--

                            // Log countdown progress
                            if (timeoutCountdown % 5 == 0) {
                                Log.d("ControlScreen", "Timeout countdown: ${timeoutCountdown}s")
                            }
                        }

                        // Only navigate if dialog is still showing and countdown reached 0
                        if (showTimeoutDialog && timeoutCountdown == 0) {
                            Log.d("ControlScreen", "Timeout expired, navigating to config")
                            showTimeoutDialog = false
                            onNavigateToConfig()
                        }
                    } catch (e: CancellationException) {
                        Log.d("ControlScreen", "Countdown cancelled")
                    }
                }
            }

            is ControlViewModel.ConnectionState.Connected -> {
                // Clear timeout dialog if connection succeeds
                if (showTimeoutDialog) {
                    Log.d("ControlScreen", "Connection restored, dismissing timeout dialog")
                    showTimeoutDialog = false
                    countdownJob?.cancel()
                    countdownJob = null
                }
            }

            else -> {
                // Don't auto-dismiss for other states
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            countdownJob?.cancel()
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

        // Connection state overlays
        when (connectionState) {
            is ControlViewModel.ConnectionState.Offline -> {
                OfflineOverlay(
                    onRetryConnection = onRetryConnection,
                    onExitOffline = onNavigateToConfig
                )
            }

            is ControlViewModel.ConnectionState.Connecting -> {
                // Show loading overlay during connection
                if (uiState.loadingPercent in 1..99) {
                    LoadingOverlay(
                        message = "Connecting to PLC... (Attempt ${connectionState.attempt}/3)",
                        loadingPercent = uiState.loadingPercent,
                        isIndeterminate = false
                    )
                }
            }

            is ControlViewModel.ConnectionState.Failed -> {
                // Show connection lost notification instead of full overlay
                if (connectionState.error.contains("Connection lost", ignoreCase = true)) {
                    ConnectionLostNotification(
                        onRetryConnection = onRetryConnection
                    )
                }
            }

            else -> { /* No overlay */
            }
        }

        // Timeout dialog with improved UX
        if (showTimeoutDialog) {
            // Custom dialog with better visibility
            AlertDialog(
                onDismissRequest = { /* Can't dismiss by tapping outside */ },
                icon = {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                },
                title = {
                    Text(
                        "Connection Failed",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Unable to connect to PLC after 3 attempts.",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        // Countdown with progress indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = timeoutCountdown / 30f,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = if (timeoutCountdown > 0) {
                                    "Auto-returning to config in ${timeoutCountdown}s..."
                                } else {
                                    "Returning to config..."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Divider()

                        Text(
                            "What would you like to do?",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Options with descriptions
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "• Retry: Try connecting again",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "• Config: Change connection settings",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "• Offline: View interface without PLC",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Retry button (primary action)
                        Button(
                            onClick = {
                                Log.d("ControlScreen", "User chose to retry connection")
                                showTimeoutDialog = false
                                countdownJob?.cancel()
                                countdownJob = null
                                onDismissTimeoutDialog()
                                onRetryConnection()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Retry")
                        }

                        // Config button
                        OutlinedButton(
                            onClick = {
                                Log.d("ControlScreen", "User chose to go to config")
                                showTimeoutDialog = false
                                countdownJob?.cancel()
                                countdownJob = null
                                onNavigateToConfig()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Config")
                        }
                    }
                },
                dismissButton = {
                    // Offline button (text button style)
                    TextButton(
                        onClick = {
                            Log.d("ControlScreen", "User chose offline mode")
                            showTimeoutDialog = false
                            countdownJob?.cancel()
                            countdownJob = null
                            onDismissTimeoutDialog()
                            onContinueOffline()
                        }
                    ) {
                        Text("Continue Offline")
                    }
                }
            )
        }
    }
}