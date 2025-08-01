package com.example.s7opcuaapp.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

/**
 * Navigate with single top behavior
 */
fun NavController.navigateSingleTop(route: String) {
    navigate(route) {
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Navigate and clear backstack
 */
fun NavController.navigateAndClear(route: String) {
    navigate(route) {
        popUpTo(0) { inclusive = true }
    }
}

/**
 * Navigate with pop up to
 */
fun NavController.navigateWithPopUpTo(
    route: String,
    popUpToRoute: String,
    inclusive: Boolean = false
) {
    navigate(route) {
        popUpTo(popUpToRoute) {
            this.inclusive = inclusive
        }
    }
}

/**
 * Safe navigate with try-catch
 */
fun NavController.safeNavigate(
    route: String,
    builder: NavOptionsBuilder.() -> Unit = {}
) {
    try {
        navigate(route, builder)
    } catch (e: Exception) {
        // Log error but don't crash
        android.util.Log.e("Navigation", "Failed to navigate to $route", e)
    }
}