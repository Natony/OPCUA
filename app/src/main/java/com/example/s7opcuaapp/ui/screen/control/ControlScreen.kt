package com.example.s7opcuaapp.ui.screen.control

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.s7opcuaapp.ui.components.NumberInputDialog
import com.example.s7opcuaapp.BuildConfig
import com.example.s7opcuaapp.ui.components.PerformanceOverlay

@Composable
fun ControlScreen(
    uiState: ControlUiState,
    onToggleBoolean: (Int, Boolean) -> Unit,
    onOpenDialog: (Int) -> Unit,
    onConfirmNumber: (Int, Int) -> Unit,
    onDismissDialog: () -> Unit,
    onFunctionSelect: (Int) -> Unit,
    onTextChange: (Int, String) -> Unit,
    onSendAll: () -> Unit,
    onStartPress: (Int) -> Unit,
    onEndPress: (Int) -> Unit
) {
    val data = uiState.plcData
    var isAuto by remember { mutableStateOf(true) }

    // Dialog nhập số
    uiState.openDialogForIndex?.let { idx ->
        NumberInputDialog(
            title = "Nhập giá trị cho index $idx",
            initialValue = data.ints.getOrNull(idx)?.toString() ?: "0",
            onConfirm = { value -> onConfirmNumber(idx, value) },
            onDismiss = onDismissDialog
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Nội dung chính - bỏ TopStatusBar vì đã chuyển lên TopNavigationBar
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
                onStartPress = onStartPress,
                onEndPress = onEndPress,
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
                        modifier = Modifier.weight(0.7f)
                    )
                }
            }

            RightControlPanel(
                isAuto = isAuto,
                data = data,
                onToggleBoolean = onToggleBoolean,
                onOpenDialog = onOpenDialog,
                modifier = Modifier.weight(0.2f)
            )
        }

        // Overlay chặn tương tác khi đang load
        if (uiState.loadingPercent < 100) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .pointerInput(Unit) {
                        awaitEachGesture { /* tiêu thụ mọi gesture */ }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        progress = uiState.loadingPercent / 100f,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${uiState.loadingPercent}%",
                        color = Color.White
                    )
                }
            }
        }
        if (BuildConfig.DEBUG) {
            PerformanceOverlay(
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}