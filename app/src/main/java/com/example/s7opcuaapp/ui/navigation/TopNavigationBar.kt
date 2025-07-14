package com.example.s7opcuaapp.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.s7opcuaapp.R
import com.example.s7opcuaapp.ui.components.BatteryStatusItem
import com.example.s7opcuaapp.ui.components.TextStatusItem

/**
 * TopNavigationBar kết hợp TopStatusBar với Navigation
 * Layout: [TextStatusItem] - [Navigation Icons] - [BatteryStatusItem] - [Logout]
 */
@Composable
fun TopNavigationBar(
    navController: NavController,
    statusValue: Int = 0,
    batteryLevel: Int = 100,
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

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier.weight(0.35f),
                contentAlignment = Alignment.CenterEnd
            ) {

            }

            // TextStatusItem - chiếm khoảng 35% width
            Box(
                modifier = Modifier.weight(0.3f),
                contentAlignment = Alignment.CenterEnd
            ) {
                TextStatusItem(
                    label = "",
                    intValue = statusValue,
                    statuses = listOf(
                        "Khởi tạo",
                        "Sẵn sàng",
                        "Đang chạy",
                        "Tạm dừng",
                        "Lỗi",
                        "Bảo trì",
                        "Hoàn thành",
                        "Đang kết nối",
                        "Mất kết nối",
                        "Cảnh báo",
                        "Khẩn cấp",
                        "Đang hiệu chỉnh",
                        "Đang kiểm tra",
                        "Chờ xác nhận",
                        "Đang cập nhật"
                    ),
                    modifier = Modifier.wrapContentSize()
                )
            }

            // Navigation Icons - chiếm khoảng 30% width
            Row(
                modifier = Modifier.weight(0.3f),
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
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = item.iconRes),
                            contentDescription = item.title,
                            tint = if (currentRoute == item.route) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // BatteryStatusItem - chiếm khoảng 15% width
            Box(
                modifier = Modifier.weight(0.1f),
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
                    modifier = Modifier.size(48.dp)
                )
            }

            // Logout button - chiếm khoảng 10% width
            Box(
                modifier = Modifier.weight(0.05f),
                contentAlignment = Alignment.CenterEnd
            ) {
                IconButton(
                    onClick = onLogout,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Logout",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}