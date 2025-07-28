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
import com.example.s7opcuaapp.ui.components.ConnectionOverlay
import kotlinx.coroutines.delay
import android.util.Log

@Composable
fun ControlScreen(
    uiState: ControlUiState,
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

    // Handle connection timeout/error
    LaunchedEffect(loadingPercent) {
        Log.d("ControlScreen", "Loading percent changed: $loadingPercent")
        if (loadingPercent == -1) {
            Log.e("ControlScreen", "Connection failed, navigating to config in 1 second...")
            delay(1000)
            Log.d("ControlScreen", "Navigating to config now")
            onNavigateToConfig()
        }
    }

    // Safety mechanism: If stuck in loading for too long
    LaunchedEffect(loadingPercent) {
        if (loadingPercent in 1..99) {
            delay(30000) // 30 seconds timeout
            if (uiState.loadingPercent in 1..99) {
                Log.e("ControlScreen", "Loading timeout after 30 seconds")
                onNavigateToConfig()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main UI - ONLY show when fully connected
        if (loadingPercent == 100) {
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

                // Main control UI
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

                // Performance overlay only in debug
                val isInPreview = LocalInspectionMode.current
                if (BuildConfig.DEBUG && !isInPreview) {
                    PerformanceOverlay(modifier = Modifier.padding(16.dp))
                }
            }

            // Connection lost notification - shows on top of UI when connection is lost
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

        // Overlay states - Always on top
        when (loadingPercent) {
            -1 -> {
                // Connection failed - Full screen blocking overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    // Consume all events
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
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
                                text = uiState.errorMessage ?: "Unable to connect to PLC",
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

            0 -> {
                // Connecting - Full screen blocking overlay
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
                    contentAlignment = Alignment.Center
                ) {
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
                    }
                }
            }

            in 1..99 -> {
                // Loading nodes - Full screen blocking overlay
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
                    contentAlignment = Alignment.Center
                ) {
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
    }
}