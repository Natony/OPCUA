// app/src/main/java/com/example/s7opcuaapp/data/local/dao/AlarmDao.kt
package com.example.s7opcuaapp.data.local.dao

import androidx.room.*
import com.example.s7opcuaapp.data.model.alarm.Alarm
import com.example.s7opcuaapp.data.model.alarm.AlarmConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    // Active alarms
    @Query("""
        SELECT * FROM alarms 
        WHERE state IN ('ACTIVE', 'ACKNOWLEDGED', 'CLEARED')
        ORDER BY 
            CASE priority 
                WHEN 'EMERGENCY' THEN 1
                WHEN 'CRITICAL' THEN 2
                WHEN 'HIGH' THEN 3
                WHEN 'MEDIUM' THEN 4
                WHEN 'LOW' THEN 5
            END,
            timestamp DESC
    """)
    fun getActiveAlarms(): Flow<List<Alarm>>

    @Query("SELECT COUNT(*) FROM alarms WHERE state = 'ACTIVE'")
    fun getActiveAlarmCount(): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM alarms 
        WHERE state IN ('ACTIVE', 'CLEARED') 
        AND acknowledgedAt IS NULL
    """)
    fun getUnacknowledgedCount(): Flow<Int>

    // Alarm history
    @Query("""
        SELECT * FROM alarms 
        WHERE timestamp >= :startTime AND timestamp <= :endTime
        ORDER BY timestamp DESC
    """)
    fun getAlarmHistory(startTime: Long, endTime: Long): Flow<List<Alarm>>

    // Alarm by code
    @Query("SELECT * FROM alarms WHERE alarmCode = :code AND state = 'ACTIVE' LIMIT 1")
    suspend fun getActiveAlarmByCode(code: Int): Alarm?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: Alarm)

    @Update
    suspend fun updateAlarm(alarm: Alarm)

    @Query("""
        UPDATE alarms 
        SET state = 'ACKNOWLEDGED', 
            acknowledgedBy = :userId, 
            acknowledgedAt = :timestamp
        WHERE id = :alarmId
    """)
    suspend fun acknowledgeAlarm(alarmId: String, userId: String, timestamp: Long)

    @Query("""
        UPDATE alarms 
        SET state = CASE 
                WHEN acknowledgedAt IS NOT NULL THEN 'NORMAL'
                ELSE 'CLEARED'
            END,
            clearedAt = :timestamp
        WHERE alarmCode = :code AND state IN ('ACTIVE', 'ACKNOWLEDGED')
    """)
    suspend fun clearAlarmByCode(code: Int, timestamp: Long)

    // Statistics
    @Query("""
        SELECT 
            COUNT(CASE WHEN state = 'ACTIVE' THEN 1 END) as totalActive,
            COUNT(CASE WHEN state IN ('ACTIVE', 'CLEARED') AND acknowledgedAt IS NULL THEN 1 END) as totalUnacknowledged,
            COUNT(CASE WHEN priority = 'CRITICAL' AND state = 'ACTIVE' THEN 1 END) as criticalCount,
            COUNT(CASE WHEN priority = 'HIGH' AND state = 'ACTIVE' THEN 1 END) as highCount,
            COUNT(CASE WHEN priority = 'MEDIUM' AND state = 'ACTIVE' THEN 1 END) as mediumCount,
            COUNT(CASE WHEN priority = 'LOW' AND state = 'ACTIVE' THEN 1 END) as lowCount,
            COUNT(CASE WHEN timestamp > :last24Hours THEN 1 END) as last24Hours,
            COUNT(CASE WHEN timestamp > :last7Days THEN 1 END) as last7Days
        FROM alarms
    """)
    suspend fun getAlarmStatistics(last24Hours: Long, last7Days: Long): AlarmStatisticsRaw

    // Shelving
    @Query("""
        UPDATE alarms 
        SET shelved = :shelved, shelvedUntil = :until
        WHERE id = :alarmId
    """)
    suspend fun shelveAlarm(alarmId: String, shelved: Boolean, until: Long?)
}

// Raw statistics result
data class AlarmStatisticsRaw(
    val totalActive: Int,
    val totalUnacknowledged: Int,
    val criticalCount: Int,
    val highCount: Int,
    val mediumCount: Int,
    val lowCount: Int,
    val last24Hours: Int,
    val last7Days: Int
)

@Dao
interface AlarmConfigDao {
    @Query("SELECT * FROM alarm_configs ORDER BY alarmCode")
    fun getAllConfigs(): Flow<List<AlarmConfig>>

    @Query("SELECT * FROM alarm_configs WHERE alarmCode = :code")
    suspend fun getConfigByCode(code: Int): AlarmConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: AlarmConfig)

    @Update
    suspend fun updateConfig(config: AlarmConfig)

    @Delete
    suspend fun deleteConfig(config: AlarmConfig)

    @Query("SELECT * FROM alarm_configs WHERE enabled = 1")
    suspend fun getEnabledConfigs(): List<AlarmConfig>
}