package com.example.s7opcuaapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.s7opcuaapp.data.local.PrefsManager
import com.example.s7opcuaapp.ui.navigation.RootNavHost
import com.example.s7opcuaapp.ui.theme.S7Theme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var prefsManager: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cold-start: xóa credential cũ và device cũ
        prefsManager.clearCredentials()
        prefsManager.clearCurrentDevice()

        setContent {
            S7Theme {
                val navController = rememberNavController()
                RootNavHost(navController = navController)
            }
        }
    }
}
