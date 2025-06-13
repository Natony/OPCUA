package com.example.s7opcuaapp.ui.screen.control

import com.example.s7opcuaapp.data.model.PlcData

data class ControlUiState(
    val plcData: PlcData = PlcData.empty(),
    val isWriting: Boolean = false,
    val openDialogForField: String? = null,
    val errorMessage: String? = null
)
