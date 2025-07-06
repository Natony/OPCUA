package com.example.s7opcuaapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.s7opcuaapp.data.local.PrefsManager
import com.example.s7opcuaapp.data.model.UserRole
import com.example.s7opcuaapp.data.repository.UserRepository
import com.example.s7opcuaapp.ui.navigation.RootNavHost
import com.example.s7opcuaapp.ui.theme.S7Theme
import com.example.s7opcuaapp.util.PasswordUtils
import com.example.s7opcuaapp.util.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var prefsManager: PrefsManager
    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            initializeDefaultAdmin()
        }

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

    private suspend fun initializeDefaultAdmin() {
        try {
            val adminCount = userRepository.getActiveUserCountByRole(UserRole.ADMIN)
            if (adminCount == 0) {
                println("Đang tạo tài khoản admin mặc định...")
                userRepository.createUser(
                    username = "admin",
                    password = PasswordUtils.hashPassword("123456"), // Đảm bảo mật khẩu được hash
                    role = UserRole.ADMIN,
                    createdBy = "system"
                )
                // Kiểm tra lại sau khi tạo
                val adminUser = userRepository.getUserByUsername("admin")
                if (adminUser != null) {
                    println("Tài khoản admin đã được tạo: ${adminUser.username}, ID: ${adminUser.id}")
                } else {
                    println("Lỗi: Không thể tạo tài khoản admin!")
                }
            } else {
                println("Tài khoản admin đã tồn tại, số lượng: $adminCount")
            }
        } catch (e: Exception) {
            println("Lỗi trong initializeDefaultAdmin: ${e.message}")
            e.printStackTrace()
        }
    }
}