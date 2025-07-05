package com.example.s7opcuaapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String,
    val username: String,
    val passwordHash: String,
    val role: UserRole,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long? = null,
    val createdBy: String? = null,
    val modifiedAt: Long? = null,
    val modifiedBy: String? = null
)

enum class UserRole {
    ADMIN,    // Full quyền: quản lý user, xem tất cả log, config
    OPERATOR, // Điều khiển: control PLC, xem log của mình
    VIEWER    // Chỉ xem: không điều khiển, chỉ xem data
}