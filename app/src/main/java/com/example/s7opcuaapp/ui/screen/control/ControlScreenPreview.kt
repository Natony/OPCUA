import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import com.example.s7opcuaapp.data.model.PlcData
import com.example.s7opcuaapp.ui.screen.control.ControlScreen
import com.example.s7opcuaapp.ui.screen.control.ControlUiState

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
        )
    )
    ControlScreen(
        uiState = sampleState,
        onToggleBoolean = { _, _ -> },
        onOpenDialog = { _, _ -> },
        onConfirmNumber = { _, _ -> },
        onDismissDialog = {},
        onFunctionSelect = {},
        onTextChange = { _, _ -> },
        onSendAll = {},
        onPressButton = { _ -> false },
        onReleaseButton = { _ -> false }
    )
}
