package com.example.s7opcuaapp.util

import com.example.s7opcuaapp.data.model.Session
import com.example.s7opcuaapp.data.model.User
import com.example.s7opcuaapp.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val userRepository: UserRepository
) {
    private val _currentSession = MutableStateFlow<Session?>(null)
    val currentSession: StateFlow<Session?> = _currentSession

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    suspend fun login(username: String, password: String, deviceInfo: String? = null): Result<Session> {
        return try {
            val result = userRepository.authenticate(username, password)
            result.fold(
                onSuccess = { user ->
                    val session = userRepository.createSession(user, deviceInfo)
                    _currentSession.value = session
                    _isLoggedIn.value = true
                    Result.success(session)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        _currentSession.value?.let { session ->
            userRepository.endSession(session.sessionId)
        }
        _currentSession.value = null
        _isLoggedIn.value = false
    }

    suspend fun validateSession(): Boolean {
        val sessionId = _currentSession.value?.sessionId ?: return false
        val validSession = userRepository.validateSession(sessionId)

        if (validSession != null) {
            _currentSession.value = validSession
            return true
        } else {
            _currentSession.value = null
            _isLoggedIn.value = false
            return false
        }
    }

    fun getCurrentUser(): User? = _currentSession.value?.user

    fun getSessionId(): String? = _currentSession.value?.sessionId

    fun hasRole(role: String): Boolean {
        return _currentSession.value?.user?.role?.name == role
    }

    fun isAdmin(): Boolean = hasRole("ADMIN")

    fun isOperator(): Boolean = hasRole("OPERATOR")

    fun isViewer(): Boolean = hasRole("VIEWER")
}