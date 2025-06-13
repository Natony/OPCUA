// File: app/src/main/java/com/example/s7opcuaapp/ui/navigation/AppNavHost.kt
package com.example.s7opcuaapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.s7opcuaapp.ui.screen.login.LoginScreen
import com.example.s7opcuaapp.viewmodel.LoginViewModel
import com.example.s7opcuaapp.ui.screen.control.ControlScreen
import com.example.s7opcuaapp.ui.screen.home.HomeScreen
import com.example.s7opcuaapp.ui.screen.alarm.AlarmScreen
import com.example.s7opcuaapp.ui.screen.config.ConfigScreen
import com.example.s7opcuaapp.ui.screen.login.LoginUiState
import com.example.s7opcuaapp.viewmodel.ControlViewModel
import com.example.s7opcuaapp.viewmodel.HomeViewModel
import com.example.s7opcuaapp.viewmodel.AlarmViewModel
import com.example.s7opcuaapp.viewmodel.ConfigViewModel

@Composable
fun AppNavHost(navController: NavHostController) {
    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(paddingValues)
        ) {
            // 1. Login Screen
            composable("login") {
                val loginViewModel: LoginViewModel = hiltViewModel()
                // Chỉ rõ kiểu LoginUiState hoặc sử dụng initial nếu cần
                val uiState: LoginUiState by loginViewModel.uiState.collectAsState()

                LoginScreen(
                    uiState = uiState,
                    onUsernameChanged = { loginViewModel.onUsernameChanged(it) },
                    onPasswordChanged = { loginViewModel.onPasswordChanged(it) },
                    onTogglePasswordVisibility = { loginViewModel.onTogglePasswordVisibility() },
                    onLoginClicked = {
                        loginViewModel.onLoginClicked {
                            // Khi login thành công, điều hướng sang "control"
                            navController.navigate("control") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    }
                )
            }

            // 2. Control Screen (chính sau khi login)
            composable("control") {
                val controlViewModel: ControlViewModel = hiltViewModel()
                val uiState by controlViewModel.uiState.collectAsState()
                ControlScreen(
                    uiState = uiState,
                    onToggleBoolean = { idx, newVal -> controlViewModel.onToggleBoolean(idx, newVal) },
                    onOpenDialog = { fieldId -> controlViewModel.onOpenDialogForField(fieldId) },
                    onConfirmNumber = { fieldId, value -> controlViewModel.onConfirmNumber(fieldId, value) }
                )
            }

            // 3. Home Screen
            composable("home") {
                val homeViewModel: HomeViewModel = hiltViewModel()
                HomeScreen()
            }

            // 4. Alarm Screen
            composable("alarm") {
                val alarmViewModel: AlarmViewModel = hiltViewModel()
                val uiState by alarmViewModel.uiState.collectAsState()
                AlarmScreen(uiState = uiState)
            }

            // 5. Config Screen
            composable("config") {
                val configViewModel: ConfigViewModel = hiltViewModel()
                val uiState by configViewModel.uiState.collectAsState()
                ConfigScreen(
                    uiState = uiState,
                    onNewDeviceNameChanged = { configViewModel.onNewDeviceNameChanged(it) },
                    onNewDeviceIpChanged = { configViewModel.onNewDeviceIpChanged(it) },
                    onNewDevicePortChanged = { configViewModel.onNewDevicePortChanged(it) },
                    onNewDeviceUsernameChanged = { configViewModel.onNewDeviceUsernameChanged(it) },
                    onNewDevicePasswordChanged = { configViewModel.onNewDevicePasswordChanged(it) },
                    onAddDevice = { configViewModel.onAddDevice() },
                    onRemoveDevice = { configViewModel.onRemoveDevice(it) },
                    onSelectDevice = { /* logic khi bấm chọn device */ }
                )
            }
        }
    }
}
