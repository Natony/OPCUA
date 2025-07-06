package com.example.s7opcuaapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.s7opcuaapp.data.repository.UserRepository
import com.example.s7opcuaapp.ui.screen.login.LoginUiState
import com.example.s7opcuaapp.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun onUsernameChanged(newUsername: String) {
        _uiState.value = _uiState.value.copy(username = newUsername, errorMessage = null)
    }

    fun onPasswordChanged(newPassword: String) {
        _uiState.value = _uiState.value.copy(password = newPassword, errorMessage = null)
    }

    fun onTogglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            isPasswordVisible = !_uiState.value.isPasswordVisible
        )
    }

    fun onLoginClicked(onSuccess: () -> Unit) {
        val current = _uiState.value

        if (current.username.isBlank() || current.password.isBlank()) {
            _uiState.value = current.copy(errorMessage = "Vui lòng nhập đầy đủ thông tin")
            return
        }

        _uiState.value = current.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            userRepository.authenticate(current.username, current.password)
                .fold(
                    onSuccess = { user ->
                        // Create session
                        sessionManager.login(user)

                        _uiState.value = _uiState.value.copy(isLoading = false)
                        onSuccess()
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = exception.message ?: "Đăng nhập thất bại"
                        )
                    }
                )
        }
    }
}