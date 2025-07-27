package com.example.s7opcuaapp.ui.screen.alarm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

@Composable
fun AlarmItem(alarmText: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFCDD2))
            .padding(8.dp)
    ) {
        Text("Alarm: $alarmText")
    }
}
