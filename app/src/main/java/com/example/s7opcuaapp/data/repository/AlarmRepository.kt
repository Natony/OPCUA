// app/src/main/java/com/example/s7opcuaapp/data/repository/AlarmRepository.kt
package com.example.s7opcuaapp.data.repository

import com.example.s7opcuaapp.data.local.AppDatabase
import com.example.s7opcuaapp.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmRepository @Inject constructor(
    private val database: AppDatabase
) {
    private val alarmDao = database.alarmDao()
    private val configDao = database.alarmConfigDao()

    fun getActiveAlarms(): Flow<List<Alarm>> = alarmDao.getActiveAlarms()

    fun getActiveAlarmCount(): Flow<Int> = alarmDao.getActiveAlarmCount()

    fun getUnacknowledgedCount(): Flow<Int> = alarmDao.getUnacknowledgedCount()

    fun getAlarmHistory(startTime: Long, endTime: Long): Flow<List<Alarm>> =
        alarmDao.getAlarmHistory(startTime, endTime)

    suspend fun getActiveAlarmByCode(code: Int): Alarm? =
        alarmDao.getActiveAlarmByCode(code)

    suspend fun insertAlarm(alarm: Alarm) = alarmDao.insertAlarm(alarm)

    suspend fun updateAlarm(alarm: Alarm) = alarmDao.updateAlarm(alarm)

    suspend fun acknowledgeAlarm(alarmId: String, userId: String) {
        alarmDao.acknowledgeAlarm(alarmId, userId, System.currentTimeMillis())
    }

    suspend fun clearAlarmByCode(code: Int) {
        alarmDao.clearAlarmByCode(code, System.currentTimeMillis())
    }

    suspend fun shelveAlarm(alarmId: String, minutes: Int) {
        val until = System.currentTimeMillis() + (minutes * 60 * 1000)
        alarmDao.shelveAlarm(alarmId, true, until)
    }

    fun getAlarmStatistics(): Flow<AlarmStatistics> {
        val now = System.currentTimeMillis()
        val last24Hours = now - (24 * 60 * 60 * 1000)
        val last7Days = now - (7 * 24 * 60 * 60 * 1000)

        return alarmDao.getActiveAlarms().map { alarms ->
            val activeCount = alarms.count { it.state == AlarmState.ACTIVE }
            val unackCount = alarms.count { it.acknowledgedAt == null }

            AlarmStatistics(
                totalActive = activeCount,
                totalUnacknowledged = unackCount,
                criticalCount = alarms.count {
                    it.state == AlarmState.ACTIVE && it.priority == AlarmPriority.CRITICAL
                },
                highCount = alarms.count {
                    it.state == AlarmState.ACTIVE && it.priority == AlarmPriority.HIGH
                },
                mediumCount = alarms.count {
                    it.state == AlarmState.ACTIVE && it.priority == AlarmPriority.MEDIUM
                },
                lowCount = alarms.count {
                    it.state == AlarmState.ACTIVE && it.priority == AlarmPriority.LOW
                }
            )
        }
    }

    // Config methods
    fun getAllConfigs(): Flow<List<AlarmConfig>> = configDao.getAllConfigs()

    suspend fun getConfigByCode(code: Int): AlarmConfig? = configDao.getConfigByCode(code)

    suspend fun insertConfig(config: AlarmConfig) = configDao.insertConfig(config)

    suspend fun updateConfig(config: AlarmConfig) = configDao.updateConfig(config)

    suspend fun deleteConfig(config: AlarmConfig) = configDao.deleteConfig(config)

    suspend fun getEnabledConfigs(): List<AlarmConfig> = configDao.getEnabledConfigs()
}