package com.example.s7opcuaapp.ui.navigation

import com.example.s7opcuaapp.R

/**
 * Type-safe navigation routes
 */
sealed class Screen(val route: String) {
    // Root level screens
    object Login : Screen("login")
    object ConfigSelect : Screen("config_select")
    object Main : Screen("main")
    object Alarm : Screen("alarm")

    // Main navigation screens
    object Control : Screen("control")
    object Home : Screen("home")
    object UserManager : Screen("user_manager")
    object LoginHistory : Screen("login_history")
    object StatusLockConfig : Screen("status_lock_config")
    object ConfigBottom : Screen("config_btm")

    // Detail screens with parameters
    data class DeviceDetail(val deviceId: String) : Screen("device_detail/{deviceId}") {
        fun createRoute() = "device_detail/$deviceId"
    }

    data class UserDetail(val userId: String) : Screen("user_detail/{userId}") {
        fun createRoute() = "user_detail/$userId"
    }
}

/**
 * Bottom navigation items
 */
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val iconRes: Int
) {
    object Control : BottomNavItem(
        route = Screen.Control.route,
        title = "Control",
        iconRes = R.drawable.ic_control
    )

    object Home : BottomNavItem(
        route = Screen.Home.route,
        title = "Home",
        iconRes = R.drawable.ic_home
    )

    object Alarm : BottomNavItem(
        route = Screen.Alarm.route,
        title = "Alarm",
        iconRes = R.drawable.ic_alarm
    )

    object Config : BottomNavItem(
        route = Screen.ConfigBottom.route,
        title = "Config",
        iconRes = R.drawable.ic_settings
    )
}