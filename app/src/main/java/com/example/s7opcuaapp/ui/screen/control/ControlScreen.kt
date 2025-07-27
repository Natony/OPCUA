package com.example.s7opcuaapp.ui.screen.control

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.s7opcuaapp.ui.components.NumberInputDialog
import com.example.s7opcuaapp.ui.components.PerformanceOverlay
import com.example.s7opcuaapp.ui.components.SingleTouchHandler
import com.example.s7opcuaapp.BuildConfig
import androidx.compose.ui.platform.LocalInspectionMode
import com.example.s7opcuaapp.ui.components.ConnectionOverlay
import com.example.s7opcuaapp.viewmodel.ControlViewModel.ConnectionState
import kotlinx.coroutines.delay
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class ConnectionLifecycleObserver(
    private val onStop: () -> Unit
) : DefaultLifecycleObserver {

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        onStop()
    }
}

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
        if (loadingPercent == -1) {
            // Wait 3 seconds then navigate to config
            delay(3000)
            onNavigateToConfig()
        }
    }

    // Dialog nhập

    Box(modifier = Modifier.fillMaxSize()) {
        SingleTouchHandler(modifier = Modifier.fillMaxSize()) {
            if (loadingPercent == 100) {
                uiState.openDialogForIndex?.let { idx ->
                    NumberInputDialog(
                        title = uiState.dialogTitle.ifBlank { "Nhập giá trị" },
                        initialValue = data.ints.getOrNull(idx)?.toString() ?: "0",
                        onConfirm = { value -> onConfirmNumber(idx, value) },
                        onDismiss = onDismissDialog
                    )
                }

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
            }

            // Performance overlay only in debug
            val isInPreview = LocalInspectionMode.current
            if (BuildConfig.DEBUG && !isInPreview) {
                PerformanceOverlay(modifier = Modifier.padding(16.dp))
            }
        }

        // Connection/Loading overlay based on loadingPercent
        when (loadingPercent) {
            -1 -> {
                // Connection failed/timeout
                ConnectionOverlay(
                    message = uiState.errorMessage ?: "Unable to connect to PLC",
                    showProgress = false,
                    showRetry = true,
                    onRetry = onRetryConnection
                )
            }

            0 -> {
                // Connecting
                ConnectionOverlay(
                    message = "Connecting to PLC...",
                    showProgress = true,
                    showRetry = false
                )
            }

            in 1..99 -> {
                // Loading nodes
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .pointerInput(Unit) { awaitEachGesture { } },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            progress = loadingPercent / 100f,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Loading data... $loadingPercent%",
                            color = Color.White
                        )
                    }
                }
            }

            // 100 -> UI đã hiển thị ở trên
        }
    }
}
