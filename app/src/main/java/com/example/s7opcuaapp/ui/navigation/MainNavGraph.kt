package com.example.s7opcuaapp.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.s7opcuaapp.ui.screen.alarm.AlarmScreen
import com.example.s7opcuaapp.ui.screen.config.ConfigScreen
import com.example.s7opcuaapp.ui.screen.control.ControlScreen
import com.example.s7opcuaapp.ui.screen.home.HomeScreen
import com.example.s7opcuaapp.ui.screen.history.LoginHistoryScreen
import com.example.s7opcuaapp.ui.screen.usermanager.UserManagerScreen
import com.example.s7opcuaapp.viewmodel.AlarmViewModel
import com.example.s7opcuaapp.viewmodel.ConfigViewModel
import com.example.s7opcuaapp.viewmodel.ControlViewModel
import com.example.s7opcuaapp.viewmodel.HomeViewModel
import com.example.s7opcuaapp.viewmodel.LoginHistoryViewModel
import com.example.s7opcuaapp.viewmodel.LogoutViewModel
import com.example.s7opcuaapp.viewmodel.UserManagerViewModel

/**
 * MainNavGraph với TopNavigationBar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavGraph(rootNavController: NavHostController) {
    // Create a separate NavController for the top navigation
    val topNavController = androidx.navigation.compose.rememberNavController()
    val navBackStackEntry by topNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Create ViewModels at this level
    val controlViewModel: ControlViewModel = hiltViewModel()
    val controlUiState by controlViewModel.uiState.collectAsStateWithLifecycle()
    val logoutViewModel: LogoutViewModel = hiltViewModel()

    Scaffold(
        topBar = {
            TopNavigationBar(
                navController = topNavController,
                statusValue = controlUiState.plcData.ints.getOrNull(0) ?: 0,
                batteryLevel = controlUiState.plcData.ints.getOrNull(1) ?: 100,
                onLogout = {
                    logoutViewModel.logout {
                        rootNavController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = topNavController,
            startDestination = "control",
            modifier = Modifier.padding(paddingValues)
        ) {
            // Control Screen
            composable("control") {
                DisposableEffect(Unit) {
                    controlViewModel.startConnection()
                    onDispose {
                        // Don't stop connection when navigating away
                        // Only stop when leaving MainNavGraph
                    }
                }

                ControlScreen(
                    uiState = controlUiState,
                    onToggleBoolean = { idx, newVal ->
                        controlViewModel.onToggleBoolean(idx, newVal)
                    },
                    onOpenDialog = { index ->
                        controlViewModel.openNumberDialog(index)
                    },
                    onConfirmNumber = { index, value ->
                        controlViewModel.confirmNumber(index, value)
                    },
                    onDismissDialog = {
                        controlViewModel.dismissDialog()
                    },
                    onFunctionSelect = { code ->
                        controlViewModel.onFunctionSelected(code)
                    },
                    onTextChange = { idx, txt ->
                        controlViewModel.onInlineValueChange(idx, txt)
                    },
                    onSendAll = {
                        controlViewModel.onSendAll()
                    },
                    onStartPress = { idx ->
                        controlViewModel.onStartPress(idx)
                    },
                    onEndPress = { idx ->
                        controlViewModel.onEndPress(idx)
                    }
                )
            }

            // Home Screen
            composable("home") {
                val homeViewModel: HomeViewModel = hiltViewModel()
                HomeScreen(navController = topNavController)
            }

            // Alarm Screen
            composable("alarm") {
                val alarmViewModel: AlarmViewModel = hiltViewModel()
                val uiState by alarmViewModel.uiState.collectAsStateWithLifecycle()
                AlarmScreen(uiState = uiState)
            }

            // User Manager Screen
            composable("user_manager") {
                val userManagerViewModel: UserManagerViewModel = hiltViewModel()
                UserManagerScreen(viewModel = userManagerViewModel)
            }

            // Login History Screen
            composable("login_history") {
                val loginHistoryViewModel: LoginHistoryViewModel = hiltViewModel()
                LoginHistoryScreen(viewModel = loginHistoryViewModel)
            }

            // Config Bottom Tab
            composable("config_btm") {
                val configViewModel: ConfigViewModel = hiltViewModel()
                val uiState by configViewModel.uiState.collectAsStateWithLifecycle()
                ConfigScreen(
                    uiState = uiState,
                    onNewDeviceNameChanged = { deviceName ->
                        configViewModel.onNewDeviceNameChanged(deviceName)
                    },
                    onNewDeviceIpChanged = { deviceIp ->
                        configViewModel.onNewDeviceIpChanged(deviceIp)
                    },
                    onNewDevicePortChanged = { devicePort ->
                        configViewModel.onNewDevicePortChanged(devicePort)
                    },
                    onNewDeviceUsernameChanged = { deviceUsername ->
                        configViewModel.onNewDeviceUsernameChanged(deviceUsername)
                    },
                    onNewDevicePasswordChanged = { devicePassword ->
                        configViewModel.onNewDevicePasswordChanged(devicePassword)
                    },
                    onAddDevice = { configViewModel.onAddDevice() },
                    onRemoveDevice = { device -> configViewModel.onRemoveDevice(device) },
                    onSelectDevice = { device ->
                        configViewModel.onSelectDevice(device) {
                            // Restart connection with new device
                            controlViewModel.restartConnection()
                            // Navigate back to control
                            topNavController.navigate("control") {
                                popUpTo("control") { inclusive = true }
                            }
                        }
                    },
                    onEditDevice = { device -> configViewModel.onEditDevice(device) },
                    onCancelEdit = { configViewModel.onCancelEdit() }
                )
            }
        }
    }

    // Stop connection when leaving MainNavGraph
    DisposableEffect(Unit) {
        onDispose {
            controlViewModel.stopConnection()
        }
    }
}