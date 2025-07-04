package com.example.s7opcuaapp.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
 * Layout: [TextStatusItem] - [Navigation Icons] - [BatteryStatusItem]
 */
@Composable
fun TopNavigationBar(
    navController: NavController,
    statusValue: Int = 0,
    batteryLevel: Int = 100
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
            // TextStatusItem - chiếm khoảng 40% width
            Box(
                modifier = Modifier.weight(0.4f),
                contentAlignment = Alignment.CenterStart
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

            // Navigation Icons - chiếm khoảng 40% width
            Row(
                modifier = Modifier.weight(0.4f),
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

            // BatteryStatusItem - chiếm khoảng 20% width
            Box(
                modifier = Modifier.weight(0.2f),
                contentAlignment = Alignment.CenterEnd
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
        }
    }
}