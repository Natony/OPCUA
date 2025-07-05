package com.example.s7opcuaapp.data.repository

import com.example.s7opcuaapp.data.model.*
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    // User management
    suspend fun createUser(username: String, password: String, role: UserRole, createdBy: String): Result<User>
    suspend fun updateUser(user: User): Result<Unit>
    suspend fun deleteUser(userId: String): Result<Unit>
    suspend fun activateUser(userId: String, modifiedBy: String): Result<Unit>
    suspend fun deactivateUser(userId: String, modifiedBy: String): Result<Unit>
    suspend fun changePassword(userId: String, newPassword: String, modifiedBy: String): Result<Unit>

    // User queries
    fun getAllUsers(): Flow<List<User>>
    fun getActiveUsers(): Flow<List<User>>
    suspend fun getUserById(userId: String): User?
    suspend fun getUserByUsername(username: String): User?
    fun getUsersByRole(role: UserRole): Flow<List<User>>

    // Authentication
    suspend fun authenticate(username: String, password: String): Result<User>
    suspend fun createSession(user: User, deviceInfo: String?): Session
    suspend fun validateSession(sessionId: String): Session?
    suspend fun endSession(sessionId: String)

    // Stats
    suspend fun getActiveUserCount(): Int
    suspend fun getActiveUserCountByRole(role: UserRole): Int
}