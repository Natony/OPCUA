package com.example.s7opcuaapp.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import com.example.s7opcuaapp.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavGraph(rootNavController: NavHostController) {
    val topNavController = androidx.navigation.compose.rememberNavController()
    val navBackStackEntry by topNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

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
            composable("control") {

                val controlUiState by controlViewModel.uiState.collectAsStateWithLifecycle()
                val connectionState by controlViewModel.connectionState.collectAsStateWithLifecycle()
                DisposableEffect(Unit) {
                    controlViewModel.startConnection()
                    onDispose { /* keep connection until leaving MainNavGraph */ }
                }

                ControlScreen(
                    uiState = controlUiState,
                    onNavigateToConfig = {
                        // Navigate to config when timeout
                        topNavController.navigate("config_btm") {
                            popUpTo("control") { inclusive = true }
                        }
                    },
                    onRetryConnection = {
                        // Retry connection
                        controlViewModel.resetConnection()
                    },
                    onToggleBoolean = { idx, newVal ->
                        controlViewModel.onToggleBoolean(idx, newVal)
                    },
                    onOpenDialog = { title, index ->
                        controlViewModel.openNumberDialog(title, index)
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
                    onPressButton = { index ->
                        controlViewModel.onPressButton(index)
                        true
                    },
                    onReleaseButton = { index ->
                        controlViewModel.onReleaseButton(index)
                        true
                    }
                )
            }

            composable("home") {
                val homeViewModel: HomeViewModel = hiltViewModel()
                HomeScreen(navController = topNavController)
            }

            composable("alarm") {
                val alarmViewModel: AlarmViewModel = hiltViewModel()
                val uiState by alarmViewModel.uiState.collectAsStateWithLifecycle()
                AlarmScreen(uiState = uiState)
            }

            composable("user_manager") {
                val userManagerViewModel: UserManagerViewModel = hiltViewModel()
                UserManagerScreen(viewModel = userManagerViewModel)
            }

            composable("login_history") {
                val loginHistoryViewModel: LoginHistoryViewModel = hiltViewModel()
                LoginHistoryScreen(viewModel = loginHistoryViewModel)
            }

            composable("status_lock_config") {
                val statusLockConfigViewModel: StatusLockConfigViewModel = hiltViewModel()
                com.example.s7opcuaapp.ui.screen.admin.StatusLockConfigScreen(
                    viewModel = statusLockConfigViewModel,
                    onBack = { topNavController.popBackStack() }
                )
            }

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
                            controlViewModel.restartConnection()
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

    DisposableEffect(Unit) {
        onDispose {
            controlViewModel.stopConnection()
        }
    }
}
