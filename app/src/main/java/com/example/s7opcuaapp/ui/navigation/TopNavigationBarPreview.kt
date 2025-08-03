package com.example.s7opcuaapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.example.s7opcuaapp.viewmodel.ControlViewModel
import com.example.s7opcuaapp.domain.connection.ConnectionState

@Preview(showBackground = true, widthDp = 1080, heightDp = 100)
@Composable
fun TopNavigationBarPreviewConnected() {
    TopNavigationBar(
        navController = rememberNavController(),
        statusValue = 1,
        batteryLevel = 75,
        deviceName = "PLC-01",
        connectionState = ConnectionState.Connected,
        onLogout = {}
    )
}

@Preview(showBackground = true, widthDp = 1080, heightDp = 100)
@Composable
fun TopNavigationBarPreviewConnecting() {
    TopNavigationBar(
        navController = rememberNavController(),
        statusValue = 2,
        batteryLevel = 50,
        deviceName = "PLC-02",
        connectionState = ConnectionState.Connecting(attempt = 2),
        onLogout = {}
    )
}

@Preview(showBackground = true, widthDp = 1080, heightDp = 100)
@Composable
fun TopNavigationBarPreviewFailed() {
    TopNavigationBar(
        navController = rememberNavController(),
        statusValue = 11,
        batteryLevel = 10,
        deviceName = "PLC-Error",
        connectionState = ConnectionState.Failed(
            error = "Timeout",
            attempt = 3
        ),
        onLogout = {}
    )
}

@Preview(showBackground = true, widthDp = 1080, heightDp = 100)
@Composable
fun TopNavigationBarPreviewOffline() {
    TopNavigationBar(
        navController = rememberNavController(),
        statusValue = 0,
        batteryLevel = 100,
        deviceName = "PLC-Offline",
        connectionState = ConnectionState.Offline,
        onLogout = {}
    )
}
