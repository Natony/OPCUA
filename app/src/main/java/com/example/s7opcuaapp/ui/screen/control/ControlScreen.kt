package com.example.s7opcuaapp.ui.screen.control

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.unit.dp
import com.example.s7opcuaapp.ui.components.NumberInputDialog
import com.example.s7opcuaapp.ui.components.PerformanceOverlay
import com.example.s7opcuaapp.ui.components.SingleTouchHandler
import com.example.s7opcuaapp.BuildConfig
import androidx.compose.ui.platform.LocalInspectionMode
import com.example.s7opcuaapp.viewmodel.ControlViewModel
import kotlinx.coroutines.delay
import android.util.Log

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
    onReleaseButton: (Int) -> Boolean
) {
    val data = uiState.plcData
    var isAuto by remember { mutableStateOf(true) }
    val lockedButtons = uiState.lockedButtons
    val busyButtons = uiState.busyButtons
    val isProcessing = uiState.isProcessing
    val loadingPercent = uiState.loadingPercent

    // Connection timeout dialog state
    var showTimeoutDialog by remember { mutableStateOf(false) }
    var timeoutCountdown by remember { mutableStateOf(10) }

    // Monitor connection state for timeout handling
    LaunchedEffect(connectionState) {
        when (connectionState) {
            is ControlViewModel.ConnectionState.Timeout -> {
                Log.d("ControlScreen", "Connection timeout detected")
                showTimeoutDialog = true
                timeoutCountdown = 10

                // Start countdown
                while (timeoutCountdown > 0 && showTimeoutDialog) {
                    delay(1000)
                    timeoutCountdown--
                }

                // Navigate to config after countdown
                if (showTimeoutDialog) {
                    onNavigateToConfig()
                }
            }
            is ControlViewModel.ConnectionState.Failed -> {
                // For critical failures, show timeout dialog
                if (connectionState.error.contains("timeout", ignoreCase = true) ||
                    connectionState.error.contains("max failures", ignoreCase = true)) {
                    showTimeoutDialog = true
                    timeoutCountdown = 10

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
    LaunchedEffect(connectionState, loadingPercent) {
        if (connectionState is ControlViewModel.ConnectionState.Connecting && loadingPercent in 1..99) {
            delay(30000) // 30 seconds timeout
            if (connectionState is ControlViewModel.ConnectionState.Connecting && loadingPercent in 1..99) {
                Log.e("ControlScreen", "Loading stuck after 30 seconds")
                showTimeoutDialog = true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main UI - Show when connected and data loaded
        val showMainUI = connectionState is ControlViewModel.ConnectionState.Connected &&
                (loadingPercent == 100 || loadingPercent == 0) // 0 for restart case

        if (showMainUI) {
            SingleTouchHandler(modifier = Modifier.fillMaxSize()) {
                // Dialog
                uiState.openDialogForIndex?.let { idx ->
                    NumberInputDialog(
                        title = uiState.dialogTitle.ifBlank { "Nhập giá trị" },
                        initialValue = data.ints.getOrNull(idx)?.toString() ?: "0",
                        onConfirm = { value -> onConfirmNumber(idx, value) },
                        onDismiss = onDismissDialog
                    )
                }

                // Main control panels
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp)
                ) {
                    LeftControlPanel(
                        isAuto = isAuto,
                        data = data,
                        onToggleBoolean = onToggleBoolean,
                        onOpenDialog = onOpenDialog,
                        onPressButton = onPressButton,
                        onReleaseButton = onReleaseButton,
                        lockedButtons = lockedButtons,
                        busyButtons = busyButtons,
                        modifier = Modifier.weight(0.2f)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(0.6f)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(2.dp)
                        ) {
                            CenterPanel(
                                uiState = uiState,
                                isAuto = isAuto,
                                onToggleBoolean = onToggleBoolean,
                                onFunctionSelect = onFunctionSelect,
                                onTextChange = onTextChange,
                                onSendAll = onSendAll,
                                modifier = Modifier.weight(0.3f)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(2.dp)
                        ) {
                            BottomControlsRow(
                                isAuto = isAuto,
                                data = data,
                                onToggleBoolean = onToggleBoolean,
                                onToggleAutoMode = { isAuto = !isAuto },
                                modifier = Modifier.weight(0.7f),
                                lockedButtons = lockedButtons,
                                busyButtons = busyButtons,
                                isProcessing = isProcessing
                            )
                        }
                    }

                    RightControlPanel(
                        isAuto = isAuto,
                        data = data,
                        onToggleBoolean = onToggleBoolean,
                        onOpenDialog = onOpenDialog,
                        onPressButton = onPressButton,
                        onReleaseButton = onReleaseButton,
                        lockedButtons = lockedButtons,
                        busyButtons = busyButtons,
                        modifier = Modifier.weight(0.2f)
                    )
                }

                // Performance overlay in debug
                val isInPreview = LocalInspectionMode.current
                if (BuildConfig.DEBUG && !isInPreview) {
                    PerformanceOverlay(modifier = Modifier.padding(16.dp))
                }
            }

            // Connection lost notification - non-blocking
            if (uiState.errorMessage?.contains("Connection lost") == true) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .align(Alignment.TopCenter)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Connection to PLC lost",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            TextButton(
                                onClick = onRetryConnection,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }

        // Overlay states based on connection state
        when (connectionState) {
            is ControlViewModel.ConnectionState.Idle -> {
                // No overlay for idle state
            }

            is ControlViewModel.ConnectionState.Connecting -> {
                FullScreenOverlay {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(64.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Connecting to PLC...",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (loadingPercent > 0) {
                            Text(
                                text = "Loading: $loadingPercent%",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            is ControlViewModel.ConnectionState.Connected -> {
                // Show loading overlay only if actively loading
                if (loadingPercent in 1..99) {
                    FullScreenOverlay {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = loadingPercent / 100f,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "Loading data... $loadingPercent%",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            is ControlViewModel.ConnectionState.Failed -> {
                // Only show full overlay for critical errors
                if (!connectionState.error.contains("Connection lost")) {
                    FullScreenOverlay {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .wrapContentHeight()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = connectionState.error,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Button(onClick = onRetryConnection) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
            }

            is ControlViewModel.ConnectionState.Timeout -> {
                // Timeout handled by LaunchedEffect above
            }
        }

        // Timeout dialog
        if (showTimeoutDialog) {
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
                    TextButton(
                        onClick = {
                            showTimeoutDialog = false
                            onRetryConnection()
                        }
                    ) {
                        Text("Retry Connection")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showTimeoutDialog = false
                            onNavigateToConfig()
                        }
                    ) {
                        Text("Go to Config")
                    }
                }
            )
        }
    }
}

// Helper composable for full screen overlays
@Composable
private fun FullScreenOverlay(
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .pointerInput(Unit) {
                awaitEachGesture {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        event.changes.forEach { it.consume() }
                    }
                }
            },
        contentAlignment = Alignment.Center,
        content = content
    )
}