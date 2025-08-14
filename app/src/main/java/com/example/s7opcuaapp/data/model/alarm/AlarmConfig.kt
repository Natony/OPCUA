package com.example.s7opcuaapp.data.model.alarm

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarm_configs")
data class AlarmConfig(
    @PrimaryKey
    val alarmCode: Int,
    val priority: AlarmPriority = AlarmPriority.MEDIUM,
    val category: AlarmCategory = AlarmCategory.PROCESS,
    val message: String,
    val description: String = "",
    val enabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val soundFile: String = "default",
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedBy: String? = null,
    val modifiedAt: Long? = null
)