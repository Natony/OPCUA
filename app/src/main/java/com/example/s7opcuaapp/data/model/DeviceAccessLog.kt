package com.example.s7opcuaapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "device_access_logs",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["deviceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index("deviceId"), Index("timestamp")]
)
data class DeviceAccessLog(
    @PrimaryKey
    val id: String,
    val userId: String,
    val username: String, // Denormalized
    val deviceId: String,
    val deviceName: String, // Denormalized
    val action: DeviceAction,
    val details: String? = null, // JSON string với chi tiết action
    val timestamp: Long,
    val success: Boolean = true,
    val errorMessage: String? = null
)

enum class DeviceAction {
    CONNECT,
    DISCONNECT,
    READ_DATA,
    WRITE_BOOL,
    WRITE_INT,
    EMERGENCY_STOP,
    MODE_CHANGE,
    FUNCTION_EXECUTE,
    CONFIG_CHANGE
}