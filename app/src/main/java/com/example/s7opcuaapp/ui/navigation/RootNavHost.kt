package com.example.s7opcuaapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.s7opcuaapp.ui.screen.config.ConfigScreen
import com.example.s7opcuaapp.ui.screen.login.LoginScreen
import com.example.s7opcuaapp.viewmodel.ConfigViewModel
import com.example.s7opcuaapp.viewmodel.LoginViewModel

/**
 * RootNavHost tách biệt 3 mức:
 *  1. "login"
 *  2. "config_select" (chọn device lần đầu)
 *  3. "main" (MainNavGraph chứa BottomNavBar)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        // 1. Login Screen
        composable("login") {
            val loginViewModel: LoginViewModel = hiltViewModel()
            val uiState by loginViewModel.uiState.collectAsStateWithLifecycle()

            LoginScreen(
                uiState = uiState,
                onUsernameChanged = { username -> loginViewModel.onUsernameChanged(username) },
                onPasswordChanged = { password -> loginViewModel.onPasswordChanged(password) },
                onTogglePasswordVisibility = { loginViewModel.onTogglePasswordVisibility() },
                onLoginClicked = {
                    loginViewModel.onLoginClicked {
                        // Sau khi login thành công, đi đến config_select
                        navController.navigate("config_select") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            )
        }

        // 2. ConfigSelect Screen (chọn device lần đầu)
        composable("config_select") {
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
                    configViewModel.onSelectDevice(device) {
                        // Sau khi chọn device, dẫn vào main (MainNavGraph)
                        navController.navigate("main") {
                            popUpTo("config_select") { inclusive = true }
                        }
                    }
                }
            )
        }

        // 3. MainNavGraph (có BottomNavBar)
        composable("main") {
            MainNavGraph(rootNavController = navController)
        }
    }
}