package com.example.s7opcuaapp.ui.screen.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.s7opcuaapp.ui.components.CommonTextField

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onLoginClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Login", modifier = Modifier.padding(bottom = 16.dp))

        CommonTextField(
            label = "Username",
            value = uiState.username,
            onValueChange = onUsernameChanged
        )
        Spacer(modifier = Modifier.height(8.dp))
        CommonTextField(
            label = "Password",
            value = uiState.password,
            onValueChange = onPasswordChanged,
            isPassword = true,
            isPasswordVisible = uiState.isPasswordVisible,
            onTogglePasswordVisibility = onTogglePasswordVisibility
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else {
            Button(onClick = { onLoginClicked() }) {
                Text("Login")
            }
        }

        uiState.errorMessage?.let { msg ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = msg, color = androidx.compose.ui.graphics.Color.Red)
        }
    }
}
