package com.example.s7opcuaapp.ui.screen.alarm

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AlarmScreen(uiState: AlarmUiState) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Alarm List", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        uiState.errorMessage?.let { msg ->
            Text(text = msg, color = androidx.compose.ui.graphics.Color.Red)
            Spacer(modifier = Modifier.height(8.dp))
        }

        LazyColumn {
            items(uiState.alarmList) { alarmText ->
                AlarmItem(alarmText)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}