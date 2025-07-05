package com.example.s7opcuaapp.data.local.dao

import androidx.room.*
import com.example.s7opcuaapp.data.model.DeviceAccessLog
import com.example.s7opcuaapp.data.model.DeviceAction
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceAccessLogDao {
    @Query("SELECT * FROM device_access_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 500): Flow<List<DeviceAccessLog>>

    @Query("""
        SELECT * FROM device_access_logs 
        WHERE userId = :userId 
        ORDER BY timestamp DESC
    """)
    fun getLogsByUser(userId: String): Flow<List<DeviceAccessLog>>

    @Query("""
        SELECT * FROM device_access_logs 
        WHERE deviceId = :deviceId 
        ORDER BY timestamp DESC
    """)
    fun getLogsByDevice(deviceId: String): Flow<List<DeviceAccessLog>>

    @Query("""
        SELECT * FROM device_access_logs 
        WHERE action = :action
        ORDER BY timestamp DESC
    """)
    fun getLogsByAction(action: DeviceAction): Flow<List<DeviceAccessLog>>

    @Query("""
        SELECT * FROM device_access_logs 
        WHERE timestamp >= :startTime AND timestamp <= :endTime 
        ORDER BY timestamp DESC
    """)
    fun getLogsByDateRange(startTime: Long, endTime: Long): Flow<List<DeviceAccessLog>>

    @Query("""
        SELECT * FROM device_access_logs 
        WHERE userId = :userId AND deviceId = :deviceId 
              AND timestamp >= :startTime AND timestamp <= :endTime
        ORDER BY timestamp DESC
    """)
    fun getFilteredLogs(
        userId: String?,
        deviceId: String?,
        startTime: Long,
        endTime: Long
    ): Flow<List<DeviceAccessLog>>

    @Insert
    suspend fun insertLog(log: DeviceAccessLog)

    @Insert
    suspend fun insertLogs(logs: List<DeviceAccessLog>)

    @Query("DELETE FROM device_access_logs WHERE timestamp < :beforeTime")
    suspend fun deleteOldLogs(beforeTime: Long)

    @Query("""
        SELECT action, COUNT(*) as count 
        FROM device_access_logs 
        WHERE timestamp >= :startTime 
        GROUP BY action
    """)
    suspend fun getActionStats(startTime: Long): List<ActionStat>

    @Query("""
        SELECT deviceId, deviceName, COUNT(*) as accessCount 
        FROM device_access_logs 
        WHERE timestamp >= :startTime 
        GROUP BY deviceId, deviceName 
        ORDER BY accessCount DESC
    """)
    suspend fun getDeviceAccessStats(startTime: Long): List<DeviceAccessStat>
}

data class ActionStat(
    val action: DeviceAction,
    val count: Int
)

data class DeviceAccessStat(
    val deviceId: String,
    val deviceName: String,
    val accessCount: Int
)