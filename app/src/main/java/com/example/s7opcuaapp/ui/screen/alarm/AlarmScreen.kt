// app/src/main/java/com/example/s7opcuaapp/ui/screen/alarm/AlarmScreen.kt
package com.example.s7opcuaapp.ui.screen.alarm

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.s7opcuaapp.data.model.*
import com.example.s7opcuaapp.viewmodel.AlarmViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(
    navController: NavController,
    viewModel: AlarmViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AlarmTopBar(
                activeCount = uiState.statistics.totalActive,
                unackCount = uiState.statistics.totalUnacknowledged,
                isMuted = uiState.soundMuted,
                onToggleMute = { viewModel.toggleMute() },
                onAckAll = { viewModel.acknowledgeAll() },
                onNavigateToHistory = { navController.navigate("alarm_history") },
                onNavigateToConfig = {
                    if (currentUser?.role == UserRole.ADMIN) {
                        navController.navigate("alarm_config")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Alarm Summary Bar
            AlarmSummaryBar(statistics = uiState.statistics)

            // Active Alarms List
            if (uiState.activeAlarms.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFF4CAF50)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Active Alarms",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF4CAF50)
                        )
                        Text(
                            text = "System operating normally",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.activeAlarms,
                        key = { it.id }
                    ) { alarm ->
                        AlarmCard(
                            alarm = alarm,
                            onAcknowledge = { viewModel.acknowledgeAlarm(alarm.id) },
                            onShelve = { viewModel.showShelveDialog(alarm) },
                            onDetails = { navController.navigate("alarm_details/${alarm.id}") }
                        )
                    }
                }
            }
        }
    }

    // Shelve Dialog
    uiState.selectedAlarm?.let { selectedAlarm ->
        if (uiState.showShelveDialog) {
            ShelveAlarmDialog(
                alarm = selectedAlarm,  // No smart cast issue now
                onConfirm = { minutes ->
                    viewModel.shelveAlarm(selectedAlarm.id, minutes)
                },
                onDismiss = { viewModel.hideShelveDialog() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmTopBar(
    activeCount: Int,
    unackCount: Int,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onAckAll: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToConfig: () -> Unit
) {
    SmallTopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Alarms")

                // Active count badge
                if (activeCount > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.error
                    ) {
                        Text("$activeCount")
                    }
                }

                // Unacknowledged badge
                if (unackCount > 0) {
                    Badge(
                        containerColor = Color(0xFFFF9800)
                    ) {
                        Text("$unackCount unack")
                    }
                }
            }
        },
        actions = {
            // Mute/Unmute
            IconButton(onClick = onToggleMute) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    tint = if (isMuted) Color(0xFFFF9800) else MaterialTheme.colorScheme.onSurface
                )
            }

            // Acknowledge All
            if (unackCount > 0) {
                IconButton(onClick = onAckAll) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Acknowledge All"
                    )
                }
            }

            // History
            IconButton(onClick = onNavigateToHistory) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Alarm History"
                )
            }

            // Config (Admin only)
            IconButton(onClick = onNavigateToConfig) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Alarm Configuration"
                )
            }
        }
    )
}

@Composable
private fun AlarmSummaryBar(
    statistics: AlarmStatistics
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AlarmSummaryItem(
                count = statistics.criticalCount,
                label = "Critical",
                color = Color(0xFFF44336)
            )
            AlarmSummaryItem(
                count = statistics.highCount,
                label = "High",
                color = Color(0xFFFF9800)
            )
            AlarmSummaryItem(
                count = statistics.mediumCount,
                label = "Medium",
                color = Color(0xFFFFC107)
            )
            AlarmSummaryItem(
                count = statistics.lowCount,
                label = "Low",
                color = Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
private fun AlarmSummaryItem(
    count: Int,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (count > 0) color else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmCard(
    alarm: Alarm,
    onAcknowledge: () -> Unit,
    onShelve: () -> Unit,
    onDetails: () -> Unit
) {
    val priorityColor = Color(alarm.priority.color)
    val isBlinking = alarm.state == AlarmState.ACTIVE && alarm.priority >= AlarmPriority.HIGH

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isBlinking) 1f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        onClick = onDetails,
        colors = CardDefaults.cardColors(
            containerColor = if (isBlinking) {
                priorityColor.copy(alpha = animatedAlpha * 0.2f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = 2.dp,
            color = priorityColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Priority and Category badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Priority badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = priorityColor
                        ) {
                            Text(
                                text = alarm.priority.name,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Category
                        Text(
                            text = alarm.category.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // State
                        if (alarm.state != AlarmState.ACTIVE) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when (alarm.state) {
                                    AlarmState.ACKNOWLEDGED -> Color(0xFF2196F3)
                                    AlarmState.CLEARED -> Color(0xFF9E9E9E)
                                    else -> Color.Transparent
                                }
                            ) {
                                Text(
                                    text = alarm.state.name,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Message
                    Text(
                        text = alarm.message,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    if (alarm.description.isNotEmpty()) {
                        Text(
                            text = alarm.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Timestamp
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Occurred: ${formatTimestamp(alarm.timestamp)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (alarm.acknowledgedAt != null) {
                            Text(
                                text = "Ack: ${formatTimestamp(alarm.acknowledgedAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Action buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (alarm.state == AlarmState.ACTIVE || alarm.state == AlarmState.CLEARED) {
                        Button(
                            onClick = onAcknowledge,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = priorityColor
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ACK", fontSize = 12.sp)
                        }
                    }

                    if (alarm.state == AlarmState.ACTIVE && !alarm.shelved) {
                        OutlinedButton(
                            onClick = onShelve,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Snooze,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Shelve", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShelveAlarmDialog(
    alarm: Alarm,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMinutes by remember { mutableStateOf(30) }
    val options = listOf(5, 15, 30, 60, 120, 480)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Shelve Alarm") },
        text = {
            Column {
                Text("Shelve \"${alarm.message}\" for:")
                Spacer(modifier = Modifier.height(16.dp))

                options.forEach { minutes ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMinutes = minutes }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedMinutes == minutes,
                            onClick = { selectedMinutes = minutes }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                minutes < 60 -> "$minutes minutes"
                                minutes == 60 -> "1 hour"
                                else -> "${minutes / 60} hours"
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedMinutes) }) {
                Text("Shelve")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}