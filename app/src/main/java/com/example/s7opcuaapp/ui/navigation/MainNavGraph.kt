package com.example.s7opcuaapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.s7opcuaapp.ui.screen.alarm.AlarmScreen
import com.example.s7opcuaapp.ui.screen.config.ConfigScreen
import com.example.s7opcuaapp.ui.screen.control.ControlScreen
import com.example.s7opcuaapp.ui.screen.home.HomeScreen
import com.example.s7opcuaapp.viewmodel.AlarmViewModel
import com.example.s7opcuaapp.viewmodel.ConfigViewModel
import com.example.s7opcuaapp.viewmodel.ControlViewModel
import com.example.s7opcuaapp.viewmodel.HomeViewModel

/**
 * MainNavGraph chứa BottomNavBar, gồm 4 tab:
 *  - control
 *  - home
 *  - alarm
 *  - config_btm (để đổi device khi cần)
 *
 * Note: This is now a simple screen with bottom navigation,
 * not a NavHost since it's already inside RootNavHost
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavGraph(rootNavController: NavHostController) {
    // Create a separate NavController for the bottom navigation
    val bottomNavController = androidx.navigation.compose.rememberNavController()

    Scaffold(
        bottomBar = { BottomNavBar(bottomNavController) }
    ) { paddingValues ->
        NavHost(
            navController = bottomNavController,
            startDestination = "control",
            modifier = Modifier.padding(paddingValues)
        ) {
            // Control Screen
            composable("control") {
                val controlViewModel: ControlViewModel = hiltViewModel()
                val uiState by controlViewModel.uiState.collectAsStateWithLifecycle()
                ControlScreen(
                    uiState = uiState,
                    onToggleBoolean = { idx, newVal -> controlViewModel.onToggleBoolean(idx, newVal) },
                    onOpenDialog = { fieldId -> controlViewModel.onOpenDialogForField(fieldId) },
                    onConfirmNumber = { fieldId, value -> controlViewModel.onConfirmNumber(fieldId, value) }
                )
            }

            // Home Screen
            composable("home") {
                val homeViewModel: HomeViewModel = hiltViewModel()
                HomeScreen()
            }

            // Alarm Screen
            composable("alarm") {
                val alarmViewModel: AlarmViewModel = hiltViewModel()
                val uiState by alarmViewModel.uiState.collectAsStateWithLifecycle()
                AlarmScreen(uiState = uiState)
            }

            // Config Bottom Tab (đổi device)
            composable("config_btm") {
                val configViewModel: ConfigViewModel = hiltViewModel()
                val uiState by configViewModel.uiState.collectAsStateWithLifecycle()
                ConfigScreen(
                    uiState = uiState,
                    onNewDeviceNameChanged = { deviceName -> configViewModel.onNewDeviceNameChanged(deviceName) },
                    onNewDeviceIpChanged = { deviceIp -> configViewModel.onNewDeviceIpChanged(deviceIp) },
                    onNewDevicePortChanged = { devicePort -> configViewModel.onNewDevicePortChanged(devicePort) },
                    onNewDeviceUsernameChanged = { deviceUsername -> configViewModel.onNewDeviceUsernameChanged(deviceUsername) },
                    onNewDevicePasswordChanged = { devicePassword -> configViewModel.onNewDevicePasswordChanged(devicePassword) },
                    onAddDevice = { configViewModel.onAddDevice() },
                    onRemoveDevice = { device -> configViewModel.onRemoveDevice(device) },
                    onSelectDevice = { device ->
                        // Khi chọn device ở đây, ta sẽ connect lại ControlViewModel
                        configViewModel.onSelectDevice(device) {
                            // Sau khi đổi device thành công, quay về Control tab
                            bottomNavController.navigate("control") {
                                popUpTo("control") { inclusive = true }
                            }
                        }
                    }
                )
            }
        }
    }
}