package com.example.s7opcuaapp.ui.screen.alarm

data class AlarmUiState(
    val alarmList: List<String> = emptyList(),
    val errorMessage: String? = null
)