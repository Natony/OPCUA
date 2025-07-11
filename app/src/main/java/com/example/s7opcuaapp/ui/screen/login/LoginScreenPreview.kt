package com.example.s7opcuaapp.ui.screen.login

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable

@Preview(showBackground = true, widthDp = 1920, heightDp = 1200)
@Composable
fun LoginScreenPreview() {
    val sampleState = LoginUiState(
        username = "",
        password = "",
        isPasswordVisible = false,
        isLoading = false,
        errorMessage = null
    )

    LoginScreen(
        uiState = sampleState,
        onUsernameChanged = {},
        onPasswordChanged = {},
        onTogglePasswordVisibility = {},
        onLoginClicked = {}
    )
}