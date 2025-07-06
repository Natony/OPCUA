package com.example.s7opcuaapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.example.s7opcuaapp.data.local.PrefsManager
import com.example.s7opcuaapp.data.repository.UserRepository
import com.example.s7opcuaapp.ui.navigation.RootNavHost
import com.example.s7opcuaapp.ui.theme.S7Theme
import com.example.s7opcuaapp.util.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var prefsManager: PrefsManager
    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize default admin user if needed
        initializeDefaultAdmin()

        setContent {
            S7Theme {
                val navController = rememberNavController()

                // Check for existing session
                LaunchedEffect(Unit) {
                    if (sessionManager.validateSession()) {
                        // Session valid, check if device selected
                        if (prefsManager.getCurrentDevice() != null) {
                            navController.navigate("main") {
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            navController.navigate("config_select") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    }
                }

                RootNavHost(navController = navController)
            }
        }
    }

    private fun initializeDefaultAdmin() {
        runBlocking {
            try {
                // Check if any admin exists
                val adminCount = userRepository.getActiveUserCountByRole(com.example.s7opcuaapp.data.model.UserRole.ADMIN)
                if (adminCount == 0) {
                    // Create default admin
                    userRepository.createUser(
                        username = "admin",
                        password = "123456",
                        role = com.example.s7opcuaapp.data.model.UserRole.ADMIN,
                        createdBy = "system"
                    )
                }
            } catch (e: Exception) {
                // Log error but don't crash
                e.printStackTrace()
            }
        }
    }
}