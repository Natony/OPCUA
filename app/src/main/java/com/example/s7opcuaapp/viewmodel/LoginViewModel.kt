package com.example.s7opcuaapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.s7opcuaapp.data.local.PrefsManager
import com.example.s7opcuaapp.data.model.UserCredentials
import com.example.s7opcuaapp.ui.screen.login.LoginUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val prefsManager: PrefsManager
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

    /**
     * Xử lý khi người dùng bấm nút Login
     * (Ví dụ này giả định username="admin" và password="123456" để demo).
     * Khi login thành công, lưu credential và gọi callback ngoại để navigate.
     */
    fun onLoginClicked(onSuccess: () -> Unit) {
        val current = _uiState.value
        // Bắt đầu loading
        _uiState.value = current.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            // Giả lập delay để giả lập network call/compute
            delay(1000)

            // Ví dụ đơn giản: chỉ chấp nhận admin/123456
            if (current.username == "admin" && current.password == "123456") {
                // Tạo UserCredentials object và lưu vào SharedPreferences (PrefsManager)
                val credentials = UserCredentials(
                    username = current.username,
                    password = current.password
                )
                prefsManager.saveCredentials(credentials)

                // Cập nhật trạng thái, tắt loading
                _uiState.value = _uiState.value.copy(isLoading = false)
                // Gọi callback bên UI để navigate sang màn hình chính
                onSuccess()
            } else {
                // Login thất bại
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Tên đăng nhập hoặc mật khẩu không đúng"
                )
            }
        }
    }
}