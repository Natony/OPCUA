package com.example.s7opcuaapp.data.repository

import com.example.s7opcuaapp.data.local.AppDatabase
import com.example.s7opcuaapp.data.local.PrefsManager
import com.example.s7opcuaapp.data.model.*
import com.example.s7opcuaapp.util.PasswordUtils
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val prefsManager: PrefsManager
) : BaseRepository(), UserRepository {

    private val userDao = database.userDao()
    private val loginHistoryDao = database.loginHistoryDao()

    // In-memory session storage
    private val sessions = mutableMapOf<String, Session>()

    override suspend fun createUser(
        username: String,
        password: String,
        role: UserRole,
        createdBy: String
    ): Result<User> = safeExecute("Failed to create user") {

        // Check if username exists
        getUserByUsername(username)?.let {
            throw Exception("Username already exists")
        }

        // Validate password
        if (!PasswordUtils.isValidPassword(password)) {
            throw Exception("Password does not meet requirements")
        }

        val user = User(
            id = UUID.randomUUID().toString(),
            username = username,
            passwordHash = PasswordUtils.hashPassword(password),
            role = role,
            isActive = true,
            createdAt = System.currentTimeMillis(),
            createdBy = createdBy
        )

        userDao.insertUser(user)
        logInfo("User created: ${user.username} with role ${user.role}")
        user
    }

    override suspend fun updateUser(user: User): Result<Unit> = safeExecute("Failed to update user") {
        userDao.updateUser(user.copy(modifiedAt = System.currentTimeMillis()))
        logDebug("User updated: ${user.username}")
    }

    override suspend fun deleteUser(userId: String): Result<Unit> = safeExecute("Failed to delete user") {
        val user = userDao.getUserById(userId)
            ?: throw Exception("User not found")

        // Don't delete last admin
        if (user.role == UserRole.ADMIN) {
            val adminCount = userDao.getActiveUserCountByRole(UserRole.ADMIN)
            if (adminCount <= 1) {
                throw Exception("Cannot delete the last admin")
            }
        }

        userDao.deleteUser(user)
        logInfo("User deleted: ${user.username}")
    }

    override suspend fun activateUser(userId: String, modifiedBy: String): Result<Unit> =
        safeExecute("Failed to activate user") {
            userDao.updateUserStatus(userId, true, System.currentTimeMillis(), modifiedBy)
            logDebug("User activated: $userId")
        }

    override suspend fun deactivateUser(userId: String, modifiedBy: String): Result<Unit> =
        safeExecute("Failed to deactivate user") {
            val user = userDao.getUserById(userId)
                ?: throw Exception("User not found")

            // Don't deactivate last admin
            if (user.role == UserRole.ADMIN) {
                val adminCount = userDao.getActiveUserCountByRole(UserRole.ADMIN)
                if (adminCount <= 1) {
                    throw Exception("Cannot deactivate the last admin")
                }
            }

            userDao.updateUserStatus(userId, false, System.currentTimeMillis(), modifiedBy)
            sessions.entries.removeIf { it.value.userId == userId }
            logDebug("User deactivated: ${user.username}")
        }

    override suspend fun changePassword(
        userId: String,
        newPassword: String,
        modifiedBy: String
    ): Result<Unit> = safeExecute("Failed to change password") {

        if (!PasswordUtils.isValidPassword(newPassword)) {
            throw Exception("Password does not meet requirements")
        }

        val user = userDao.getUserById(userId)
            ?: throw Exception("User not found")

        val updatedUser = user.copy(
            passwordHash = PasswordUtils.hashPassword(newPassword),
            modifiedAt = System.currentTimeMillis(),
            modifiedBy = modifiedBy
        )

        userDao.updateUser(updatedUser)
        sessions.entries.removeIf { it.value.userId == userId }
        logInfo("Password changed for user: ${user.username}")
    }

    override fun getAllUsers(): Flow<List<User>> = userDao.getAllUsers()

    override fun getActiveUsers(): Flow<List<User>> = userDao.getAllActiveUsers()

    override suspend fun getUserById(userId: String): User? = userDao.getUserById(userId)

    override suspend fun getUserByUsername(username: String): User? = userDao.getUserByUsername(username)

    override fun getUsersByRole(role: UserRole): Flow<List<User>> = userDao.getUsersByRole(role)

    override suspend fun authenticate(username: String, password: String): Result<User> =
        safeExecute("Authentication failed") {
            val historyId = UUID.randomUUID().toString()
            val loginTime = System.currentTimeMillis()

            logDebug("Authenticating user: '$username'")

            val user = userDao.getUserByUsername(username)
                ?: run {
                    logWarning("User not found: '$username'")
                    throw Exception("Invalid credentials")
                }

            if (!user.isActive) {
                logWarning("Inactive user attempted login: $username")
                loginHistoryDao.insertLoginHistory(
                    LoginHistory(
                        id = historyId,
                        userId = user.id,
                        username = username,
                        loginTime = loginTime,
                        loginStatus = LoginStatus.FAILED_ACCOUNT_DISABLED
                    )
                )
                throw Exception("Account is disabled")
            }

            if (!PasswordUtils.verifyPassword(password, user.passwordHash)) {
                logWarning("Invalid password for user: $username")
                loginHistoryDao.insertLoginHistory(
                    LoginHistory(
                        id = historyId,
                        userId = user.id,
                        username = username,
                        loginTime = loginTime,
                        loginStatus = LoginStatus.FAILED_INVALID_CREDENTIALS
                    )
                )
                throw Exception("Invalid credentials")
            }

            // Success
            userDao.updateLastLogin(user.id, loginTime)
            loginHistoryDao.insertLoginHistory(
                LoginHistory(
                    id = historyId,
                    userId = user.id,
                    username = username,
                    loginTime = loginTime,
                    loginStatus = LoginStatus.SUCCESS,
                    appVersion = "1.0.0"
                )
            )

            logInfo("User authenticated successfully: $username")
            user
        }

    override suspend fun createSession(user: User, deviceInfo: String?): Session {
        val sessionId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val session = Session(
            sessionId = sessionId,
            userId = user.id,
            user = user,
            loginTime = now,
            lastActivityTime = now,
            expiryTime = now + (8 * 60 * 60 * 1000), // 8 hours
            deviceInfo = deviceInfo
        )
        sessions[sessionId] = session
        logDebug("Session created for user: ${user.username}")
        return session
    }

    override suspend fun validateSession(sessionId: String): Session? {
        val session = sessions[sessionId] ?: return null
        val now = System.currentTimeMillis()

        if (now > session.expiryTime) {
            sessions.remove(sessionId)
            logDebug("Session expired: $sessionId")
            return null
        }

        // Update last activity
        val updatedSession = session.copy(lastActivityTime = now)
        sessions[sessionId] = updatedSession
        return updatedSession
    }

    override suspend fun endSession(sessionId: String) {
        sessions.remove(sessionId)
        logDebug("Session ended: $sessionId")
    }

    override suspend fun getActiveUserCount(): Int {
        return userDao.getAllUsers().let { flow ->
            var count = 0
            flow.collect { users ->
                count = users.count { it.isActive }
            }
            count
        }
    }

    override suspend fun getActiveUserCountByRole(role: UserRole): Int {
        return userDao.getActiveUserCountByRole(role)
    }
}