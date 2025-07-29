package com.example.s7opcuaapp.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.s7opcuaapp.R
import com.example.s7opcuaapp.ui.components.BatteryStatusItem
import com.example.s7opcuaapp.util.StatusLockConfig
import com.example.s7opcuaapp.viewmodel.ControlViewModel

@Composable
fun TopNavigationBar(
    navController: NavController,
    statusValue: Int = 0,
    batteryLevel: Int = 100,
    deviceName: String = "No Device",
    connectionState: ControlViewModel.ConnectionState,
    onLogout: () -> Unit = {}
) {
    val items = listOf(
        BottomNavItem.Control,
        BottomNavItem.Home,
        BottomNavItem.Alarm,
        BottomNavItem.Config
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Connection state color animation
    val connectionColor by animateColorAsState(
        targetValue = when (connectionState) {
            is ControlViewModel.ConnectionState.Connected -> Color(0xFF4CAF50) // Green
            is ControlViewModel.ConnectionState.Connecting -> Color(0xFFFFC107) // Amber
            is ControlViewModel.ConnectionState.Failed -> Color(0xFFF44336) // Red
            is ControlViewModel.ConnectionState.MaxRetriesExceeded -> Color(0xFF9C27B0) // Purple
            is ControlViewModel.ConnectionState.Timeout -> Color(0xFFFF5722) // Deep Orange
            is ControlViewModel.ConnectionState.Offline -> Color(0xFF607D8B) // Blue Grey
            else -> Color(0xFF9E9E9E) // Gray
        },
        animationSpec = tween(300)
    )

    val connectionText = when (connectionState) {
        is ControlViewModel.ConnectionState.Connected -> "Connected"
        is ControlViewModel.ConnectionState.Connecting -> "Connecting... (${connectionState.attempt}/3)"
        is ControlViewModel.ConnectionState.Failed -> "Failed (${connectionState.attempt}/3)"
        is ControlViewModel.ConnectionState.MaxRetriesExceeded -> "Connection Failed"
        is ControlViewModel.ConnectionState.Timeout -> "Timeout"
        is ControlViewModel.ConnectionState.Offline -> "Offline Mode"
        else -> "Disconnected"
    }

    val connectionIcon = when (connectionState) {
        is ControlViewModel.ConnectionState.Connected -> Icons.Default.Wifi
        is ControlViewModel.ConnectionState.Offline -> Icons.Default.WifiOff
        is ControlViewModel.ConnectionState.Failed,
        is ControlViewModel.ConnectionState.MaxRetriesExceeded -> Icons.Default.ErrorOutline
        else -> Icons.Default.WifiOff
    }

    // Blinking animation for connecting state
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp), // Slightly taller for 2 rows
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // First row - Device and Connection Status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(
                        when (connectionState) {
                            is ControlViewModel.ConnectionState.Connected ->
                                MaterialTheme.colorScheme.surfaceVariant
                            is ControlViewModel.ConnectionState.Offline ->
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                            is ControlViewModel.ConnectionState.Failed,
                            is ControlViewModel.ConnectionState.MaxRetriesExceeded ->
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Device name
                Row(
                    modifier = Modifier
                        .weight(0.6f)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_device),
                        contentDescription = "Device",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = deviceName,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Connection status
                Row(
                    modifier = Modifier
                        .weight(0.4f)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Icon(
                        imageVector = connectionIcon,
                        contentDescription = "Connection",
                        modifier = Modifier.size(16.dp),
                        tint = if (connectionState is ControlViewModel.ConnectionState.Connecting) {
                            connectionColor.copy(alpha = alpha)
                        } else {
                            connectionColor
                        }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = connectionText,
                        style = MaterialTheme.typography.labelSmall,
                        color = connectionColor,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Second row - Status, Navigation, Battery, Logout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status display - 25% width
                Box(
                    modifier = Modifier
                        .weight(0.25f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (statusValue) {
                                0 -> Color(0xFFE0E0E0)  // Gray for not ready
                                1 -> Color(0xFFC8E6C9)  // Light green for ready
                                in 2..6 -> Color(0xFFFFF9C4)  // Light yellow for executing
                                7 -> Color(0xFFA5D6A7)  // Green for complete
                                11 -> Color(0xFFFFCDD2) // Light red for emergency
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Text(
                            text = StatusLockConfig.DEFAULT_STATUS_DESCRIPTIONS[statusValue]
                                ?: "Unknown",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = when (statusValue) {
                                11 -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Navigation Icons - 50% width
                Row(
                    modifier = Modifier.weight(0.5f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEach { item ->
                        IconButton(
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = item.iconRes),
                                contentDescription = item.title,
                                tint = if (currentRoute == item.route) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Battery - 15% width
                Box(
                    modifier = Modifier.weight(0.15f),
                    contentAlignment = Alignment.Center
                ) {
                    BatteryStatusItem(
                        level = batteryLevel,
                        thresholds = listOf(20, 80),
                        icons = listOf(
                            R.drawable.ic_battery_low,
                            R.drawable.ic_battery_medium,
                            R.drawable.ic_battery_full
                        ),
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Logout button - 10% width
                Box(
                    modifier = Modifier.weight(0.1f),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}