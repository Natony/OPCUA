package com.example.s7opcuaapp.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.example.s7opcuaapp.BuildConfig
import com.example.s7opcuaapp.ui.screen.control.*
import com.example.s7opcuaapp.ui.components.unified.*

@Composable
internal fun MainControlContent(
    uiState: ControlUiState,
    isAuto: Boolean,
    onToggleAutoMode: () -> Unit,
    onToggleBoolean: (Int, Boolean) -> Unit,
    onOpenDialog: (String, Int) -> Unit,
    onConfirmNumber: (Int, Int) -> Unit,
    onDismissDialog: () -> Unit,
    onFunctionSelect: (Int) -> Unit,
    onTextChange: (Int, String) -> Unit,
    onSendAll: () -> Unit,
    onPressButton: (Int) -> Boolean,
    onReleaseButton: (Int) -> Boolean,
    onRetryConnection: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Connection lost notification at top using UnifiedOverlay
            if (uiState.errorMessage?.contains("Connection lost", ignoreCase = true) == true ||
                uiState.errorMessage?.contains("Reconnecting", ignoreCase = true) == true
            ) {
                UnifiedOverlay(
                    config = OverlayConfig(
                        type = OverlayType.CONNECTION_LOST,
                        message = "Connection lost - Reconnecting...",
                        showRetry = true,
                        onRetry = onRetryConnection
                    )
                )
            }

            // Main control panels
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
            ) {
                // Left Panel
                LeftControlPanel(
                    isAuto = isAuto,
                    data = uiState.plcData,
                    onToggleBoolean = onToggleBoolean,
                    onOpenDialog = onOpenDialog,
                    onPressButton = onPressButton,
                    onReleaseButton = onReleaseButton,
                    lockedButtons = uiState.lockedButtons,
                    busyButtons = uiState.busyButtons,
                    modifier = Modifier.weight(0.2f)
                )

                // Center Section
                CenterSection(
                    uiState = uiState,
                    isAuto = isAuto,
                    onToggleBoolean = onToggleBoolean,
                    onToggleAutoMode = onToggleAutoMode,
                    onFunctionSelect = onFunctionSelect,
                    onTextChange = onTextChange,
                    onSendAll = onSendAll,
                    modifier = Modifier.weight(0.6f)
                )

                // Right Panel
                RightControlPanel(
                    isAuto = isAuto,
                    data = uiState.plcData,
                    onToggleBoolean = onToggleBoolean,
                    onOpenDialog = onOpenDialog,
                    onPressButton = onPressButton,
                    onReleaseButton = onReleaseButton,
                    lockedButtons = uiState.lockedButtons,
                    busyButtons = uiState.busyButtons,
                    modifier = Modifier.weight(0.2f)
                )
            }
        }

        // Dialog
        uiState.openDialogForIndex?.let { idx ->
            NumberInputDialog(
                title = uiState.dialogTitle.ifBlank { "Nhập giá trị" },
                initialValue = uiState.plcData.ints.getOrNull(idx)?.toString() ?: "0",
                onConfirm = { value -> onConfirmNumber(idx, value) },
                onDismiss = onDismissDialog
            )
        }

        // Performance overlay in debug (optional)
        val isInPreview = LocalInspectionMode.current
        if (BuildConfig.DEBUG && !isInPreview) {
            // Uncomment if you want performance overlay
            // PerformanceOverlay(modifier = Modifier.align(Alignment.TopEnd))
        }
    }
}

@Composable
private fun CenterSection(
    uiState: ControlUiState,
    isAuto: Boolean,
    onToggleBoolean: (Int, Boolean) -> Unit,
    onToggleAutoMode: () -> Unit,
    onFunctionSelect: (Int) -> Unit,
    onTextChange: (Int, String) -> Unit,
    onSendAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        // Top Section
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
                modifier = Modifier.fillMaxSize()
            )
        }

        // Bottom Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(2.dp)
        ) {
            BottomControlsRow(
                isAuto = isAuto,
                data = uiState.plcData,
                onToggleBoolean = onToggleBoolean,
                onToggleAutoMode = onToggleAutoMode,
                modifier = Modifier.fillMaxSize(),
                lockedButtons = uiState.lockedButtons,
                busyButtons = uiState.busyButtons,
                isProcessing = uiState.isProcessing
            )
        }
    }
}