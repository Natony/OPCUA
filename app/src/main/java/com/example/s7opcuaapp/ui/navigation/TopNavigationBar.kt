package com.example.s7opcuaapp.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.s7opcuaapp.R
import com.example.s7opcuaapp.ui.components.unified.StatusDisplayConfig
import com.example.s7opcuaapp.ui.components.unified.StatusDisplayType
import com.example.s7opcuaapp.ui.components.unified.UnifiedStatusDisplay
import com.example.s7opcuaapp.util.StatusLockConfig
import com.example.s7opcuaapp.viewmodel.ControlViewModel

/**
 * Top Navigation Bar with connection status, device info, navigation and battery
 * Fixed version without unified components to avoid crashes
 */
@Composable
fun TopNavigationBar(
    navController: NavController,
    statusValue: Int = 0,
    batteryLevel: Int = 100,
    deviceName: String = "No Device",
    connectionState: ControlViewModel.ConnectionState,
    onLogout: () -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Connection state configuration
    val connectionConfig = getConnectionConfig(connectionState)

    // Animated color for connection
    val animatedColor by animateColorAsState(
        targetValue = connectionConfig.color,
        animationSpec = tween(300),
        label = "ConnectionColor"
    )

    // Blink animation for connecting state
    val infiniteTransition = rememberInfiniteTransition(label = "BlinkAnimation")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BlinkAlpha"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Connection status + device (30%)
            Row(
                modifier = Modifier
                    .weight(0.30f)
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = connectionConfig.icon,
                    contentDescription = "Connection",
                    modifier = Modifier.size(20.dp),
                    tint = if (connectionState is ControlViewModel.ConnectionState.Connecting) {
                        animatedColor.copy(alpha = alpha)
                    } else {
                        animatedColor
                    }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = deviceName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp
                    )
                    Text(
                        text = connectionConfig.text,
                        style = MaterialTheme.typography.labelSmall,
                        color = animatedColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp
                    )
                }
            }

            // Status display (35%)
            Box(
                modifier = Modifier.weight(0.35f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = getStatusBackgroundColor(statusValue)
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (statusValue == 11) 2.dp else 1.dp
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Status indicator dot
                        if (statusValue != 0) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        color = getStatusIndicatorColor(statusValue),
                                        shape = MaterialTheme.shapes.small
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        // Status text
                        Text(
                            text = StatusLockConfig.DEFAULT_STATUS_DESCRIPTIONS[statusValue]
                                ?: "Unknown Status",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = getStatusTextColor(statusValue),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Navigation icons (20%)
            Row(
                modifier = Modifier.weight(0.20f),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val navigationItems = listOf(
                    BottomNavItem.Control,
                    BottomNavItem.Home,
                    BottomNavItem.Alarm,
                    BottomNavItem.Config
                )

                navigationItems.forEach { item ->
                    IconButton(
                        onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = item.iconRes),
                            contentDescription = item.title,
                            tint = if (currentRoute == item.route) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Battery (7%)
            Box(
                modifier = Modifier.weight(0.07f),
                contentAlignment = Alignment.Center
            ) {
                UnifiedStatusDisplay(
                    config = StatusDisplayConfig(
                        type = StatusDisplayType.BATTERY,
                        value = batteryLevel,
                        icons = listOf(
                            R.drawable.ic_battery_low,
                            R.drawable.ic_battery_medium,
                            R.drawable.ic_battery_full
                        ),
                        thresholds = listOf(20, 80)
                    ),
                    modifier = Modifier.size(28.dp)
                )
            }

            // Logout (8%)
            Box(
                modifier = Modifier.weight(0.08f),
                contentAlignment = Alignment.CenterEnd
            ) {
                IconButton(
                    onClick = onLogout,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Logout",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// ========== HELPER FUNCTIONS ==========

/**
 * Get connection configuration based on state
 */
private fun getConnectionConfig(state: ControlViewModel.ConnectionState): ConnectionConfig {
    return when (state) {
        is ControlViewModel.ConnectionState.Connected -> ConnectionConfig(
            text = "Connected",
            color = Color(0xFF4CAF50),
            icon = Icons.Default.Wifi
        )
        is ControlViewModel.ConnectionState.Connecting -> ConnectionConfig(
            text = "Connecting (${state.attempt}/3)",
            color = Color(0xFFFFC107),
            icon = Icons.Default.Sync
        )
        is ControlViewModel.ConnectionState.Failed -> ConnectionConfig(
            text = "Failed (${state.attempt}/3)",
            color = Color(0xFFF44336),
            icon = Icons.Default.ErrorOutline
        )
        is ControlViewModel.ConnectionState.MaxRetriesExceeded -> ConnectionConfig(
            text = "Max Retries",
            color = Color(0xFF9C27B0),
            icon = Icons.Default.Error
        )
        is ControlViewModel.ConnectionState.Timeout -> ConnectionConfig(
            text = "Timeout",
            color = Color(0xFFFF5722),
            icon = Icons.Default.Timer
        )
        is ControlViewModel.ConnectionState.Offline -> ConnectionConfig(
            text = "Offline Mode",
            color = Color(0xFF607D8B),
            icon = Icons.Default.WifiOff
        )
        else -> ConnectionConfig(
            text = "Disconnected",
            color = Color(0xFF9E9E9E),
            icon = Icons.Default.SignalWifiOff
        )
    }
}

/**
 * Get status background color
 */
private fun getStatusBackgroundColor(statusValue: Int): Color {
    return when (statusValue) {
        0 -> Color(0xFFE0E0E0)      // Idle - Gray
        1 -> Color(0xFFC8E6C9)      // Running - Light Green
        in 2..6 -> Color(0xFFFFF9C4) // Processing - Light Yellow
        7 -> Color(0xFFA5D6A7)      // Complete - Green
        11 -> Color(0xFFFFCDD2)     // Error - Light Red
        else -> Color(0xFFF5F5F5)   // Default - Light Gray
    }
}

/**
 * Get status indicator color
 */
private fun getStatusIndicatorColor(statusValue: Int): Color {
    return when (statusValue) {
        1 -> Color(0xFF4CAF50)      // Running - Green
        in 2..6 -> Color(0xFFFFC107) // Processing - Amber
        7 -> Color(0xFF8BC34A)      // Complete - Light Green
        11 -> Color(0xFFF44336)     // Error - Red
        else -> Color(0xFF9E9E9E)   // Default - Gray
    }
}

/**
 * Get status text color
 */
private fun getStatusTextColor(statusValue: Int): Color {
    return when (statusValue) {
        11 -> Color(0xFFB71C1C)     // Error - Dark Red
        1, 7 -> Color(0xFF2E7D32)   // Running/Complete - Dark Green
        in 2..6 -> Color(0xFFF57C00) // Processing - Dark Orange
        else -> Color(0xFF424242)    // Default - Dark Gray
    }
}

/**
 * Data class for connection configuration
 */
private data class ConnectionConfig(
    val text: String,
    val color: Color,
    val icon: ImageVector
)