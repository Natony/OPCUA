package com.example.s7opcuaapp.data.local

import androidx.room.TypeConverter
import com.example.s7opcuaapp.data.model.AlarmCategory
import com.example.s7opcuaapp.data.model.AlarmPriority
import com.example.s7opcuaapp.data.model.AlarmState
import com.example.s7opcuaapp.data.model.UserRole
import com.example.s7opcuaapp.data.model.LoginStatus
import com.example.s7opcuaapp.data.model.DeviceAction

class Converters {
    @TypeConverter
    fun fromUserRole(role: UserRole): String = role.name

    @TypeConverter
    fun toUserRole(role: String): UserRole = UserRole.valueOf(role)

    @TypeConverter
    fun fromLoginStatus(status: LoginStatus): String = status.name

    @TypeConverter
    fun toLoginStatus(status: String): LoginStatus = LoginStatus.valueOf(status)

    @TypeConverter
    fun fromDeviceAction(action: DeviceAction): String = action.name

    @TypeConverter
    fun toDeviceAction(action: String): DeviceAction = DeviceAction.valueOf(action)

    @TypeConverter
    fun fromAlarmPriority(priority: AlarmPriority): String = priority.name

    @TypeConverter
    fun toAlarmPriority(priority: String): AlarmPriority = AlarmPriority.valueOf(priority)

    @TypeConverter
    fun fromAlarmCategory(category: AlarmCategory): String = category.name

    @TypeConverter
    fun toAlarmCategory(category: String): AlarmCategory = AlarmCategory.valueOf(category)

    @TypeConverter
    fun fromAlarmState(state: AlarmState): String = state.name

    @TypeConverter
    fun toAlarmState(state: String): AlarmState = AlarmState.valueOf(state)
}