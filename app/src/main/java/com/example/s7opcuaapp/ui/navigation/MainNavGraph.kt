package com.example.s7opcuaapp.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
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
import com.example.s7opcuaapp.viewmodel.AlarmViewModel
import com.example.s7opcuaapp.viewmodel.ConfigViewModel
import com.example.s7opcuaapp.viewmodel.ControlViewModel
import com.example.s7opcuaapp.viewmodel.HomeViewModel

/**
 * MainNavGraph với TopNavigationBar thay thế BottomNavBar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavGraph(rootNavController: NavHostController) {
    // Create a separate NavController for the top navigation
    val topNavController = androidx.navigation.compose.rememberNavController()

    val controlViewModel: ControlViewModel = hiltViewModel()

    Scaffold(
        topBar = {
            // Lấy control data cho status bar, nhưng chỉ khi ở control tab
            val navBackStackEntry by topNavController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            if (currentRoute == "control") {
                // Chỉ lấy ControlViewModel khi ở control tab
                val controlViewModel: ControlViewModel = hiltViewModel()
                val controlUiState by controlViewModel.uiState.collectAsStateWithLifecycle()

                TopNavigationBar(
                    navController = topNavController,
                    statusValue = controlUiState.plcData.ints.getOrNull(0) ?: 0,
                    batteryLevel = controlUiState.plcData.ints.getOrNull(1) ?: 100
                )
            } else {
                // Ở tab khác thì dùng giá trị mặc định
                TopNavigationBar(
                    navController = topNavController,
                    statusValue = 0,
                    batteryLevel = 100
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = topNavController,
            startDestination = "control",
            modifier = Modifier.padding(paddingValues)
        ) {
            // Control Screen
            composable("control") {
                val uiState by controlViewModel.uiState.collectAsStateWithLifecycle()

                DisposableEffect(Unit) {
                    controlViewModel.startConnection()
                    onDispose {
                        controlViewModel.stopConnection()
                    }
                }

                ControlScreen(
                    uiState = uiState,
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
                    onFunctionSelect   = {
                            code      -> controlViewModel.onFunctionSelected(code)
                    },
                    onTextChange       = {
                            idx, txt  -> controlViewModel.onInlineValueChange(idx, txt)
                    },
                    onSendAll          = {
                        -> controlViewModel.onSendAll()
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
                    onNewDeviceNameChanged = { configViewModel.onNewDeviceNameChanged(it) },
                    onNewDeviceIpChanged = { configViewModel.onNewDeviceIpChanged(it) },
                    onNewDevicePortChanged = { configViewModel.onNewDevicePortChanged(it) },
                    onNewDeviceUsernameChanged = { configViewModel.onNewDeviceUsernameChanged(it) },
                    onNewDevicePasswordChanged = { configViewModel.onNewDevicePasswordChanged(it) },
                    onProtocolChanged = { configViewModel.onProtocolChanged(it) },
                    onModbusSlaveIdChanged = { configViewModel.onModbusSlaveIdChanged(it) },
                    onModbusBoolRegisterAddressChanged = { configViewModel.onModbusBoolRegisterAddressChanged(it) },
                    onModbusIntRegisterAddressChanged = { configViewModel.onModbusIntRegisterAddressChanged(it) },
                    onModbusIntRegisterCountChanged = { configViewModel.onModbusIntRegisterCountChanged(it) },
                    onModbusBoolCountChanged = { configViewModel.onModbusBoolCountChanged(it) },
                    onModbusPollingIntervalChanged = { configViewModel.onModbusPollingIntervalChanged(it) },
                    onAddDevice = { configViewModel.onAddDevice() },
                    onRemoveDevice = { device -> configViewModel.onRemoveDevice(device) },
                    onSelectDevice = { device ->
                        configViewModel.onSelectDevice(device) {
                            topNavController.navigate("control") {
                                popUpTo("control") { inclusive = true }
                            }
                        }
                    }
                )
            }
        }
    }
}