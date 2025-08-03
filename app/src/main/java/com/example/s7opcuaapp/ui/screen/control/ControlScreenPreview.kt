package com.example.s7opcuaapp.ui.screen.control

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.s7opcuaapp.data.model.PlcData
import com.example.s7opcuaapp.domain.connection.ConnectionState
import com.example.s7opcuaapp.ui.screen.control.ControlUiState
import com.example.s7opcuaapp.viewmodel.ControlViewModel

@Preview(showBackground = true, widthDp = 1080, heightDp = 720)
@Composable
fun ControlScreenPreview() {
    // Sample data for preview
    val samplePlcData = PlcData(
        bools = List(15) { it % 2 == 0 },
        ints = List(31) { it }
    )

    val sampleState = ControlUiState(
        plcData = samplePlcData,
        isWriting = false,
        openDialogForIndex = null,
        selectedFunction = 1,
        intInputs = mapOf(
            2 to "10",
            3 to "20",
            4 to "30",
            5 to "40",
            6 to "50",
            7 to "60"
        ),
        loadingPercent = 100,
        lockedButtons = emptySet(),
        busyButtons = emptySet(),
        isProcessing = false,
        errorMessage = null
    )

    // Obtain a ViewModel for preview
    val previewViewModel: ControlViewModel = viewModel()

    ControlScreen(
        uiState = sampleState,
        connectionState = ConnectionState.Connected,
        onNavigateToConfig = {},
        onRetryConnection = {},
        onToggleBoolean = { _, _ -> },
        onOpenDialog = { _, _ -> },
        onConfirmNumber = { _, _ -> },
        onDismissDialog = {},
        onFunctionSelect = {},
        onTextChange = { _, _ -> },
        onSendAll = {},
        onPressButton = { _ -> false },
        onReleaseButton = { _ -> false },
        onDismissTimeoutDialog = {},
        onContinueOffline = {}
    )
}

@Preview(showBackground = true, widthDp = 1080, heightDp = 720)
@Composable
fun ControlScreenConnectingPreview() {
    val sampleState = ControlUiState(
        plcData = PlcData.empty(),
        loadingPercent = 45
    )

    val previewViewModel: ControlViewModel = viewModel()

    ControlScreen(
        uiState = sampleState,
        connectionState = ConnectionState.Connecting(attempt = 2),
        onNavigateToConfig = {},
        onRetryConnection = {},
        onToggleBoolean = { _, _ -> },
        onOpenDialog = { _, _ -> },
        onConfirmNumber = { _, _ -> },
        onDismissDialog = {},
        onFunctionSelect = {},
        onTextChange = { _, _ -> },
        onSendAll = {},
        onPressButton = { _ -> false },
        onReleaseButton = { _ -> false },
        onDismissTimeoutDialog = {},
        onContinueOffline = {}
    )
}

@Preview(showBackground = true, widthDp = 1080, heightDp = 720)
@Composable
fun ControlScreenFailedPreview() {
    val sampleState = ControlUiState(
        plcData = PlcData.empty(),
        errorMessage = "Connection to PLC failed"
    )

    val previewViewModel: ControlViewModel = viewModel()

    ControlScreen(
        uiState = sampleState,
        connectionState = ConnectionState.Failed(
            error = "Connection timeout",
            attempt = 3
        ),
        onNavigateToConfig = {},
        onRetryConnection = {},
        onToggleBoolean = { _, _ -> },
        onOpenDialog = { _, _ -> },
        onConfirmNumber = { _, _ -> },
        onDismissDialog = {},
        onFunctionSelect = {},
        onTextChange = { _, _ -> },
        onSendAll = {},
        onPressButton = { _ -> false },
        onReleaseButton = { _ -> false },
        onDismissTimeoutDialog = {},
        onContinueOffline = {}
    )
}

@Preview(showBackground = true, widthDp = 1080, heightDp = 720)
@Composable
fun ControlScreenOfflinePreview() {
    val samplePlcData = PlcData(
        bools = List(15) { false },
        ints = List(31) { 0 }
    )

    val sampleState = ControlUiState(
        plcData = samplePlcData,
        errorMessage = "Working offline",
        lockedButtons = (0..14).toSet()
    )

    val previewViewModel: ControlViewModel = viewModel()

    ControlScreen(
        uiState = sampleState,
        connectionState = ConnectionState.Offline,
        onNavigateToConfig = {},
        onRetryConnection = {},
        onToggleBoolean = { _, _ -> },
        onOpenDialog = { _, _ -> },
        onConfirmNumber = { _, _ -> },
        onDismissDialog = {},
        onFunctionSelect = {},
        onTextChange = { _, _ -> },
        onSendAll = {},
        onPressButton = { _ -> false },
        onReleaseButton = { _ -> false },
        onDismissTimeoutDialog = {},
        onContinueOffline = {}
    )
}
