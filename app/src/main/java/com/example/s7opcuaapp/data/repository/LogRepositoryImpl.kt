package com.example.s7opcuaapp.data.repository

import com.example.s7opcuaapp.data.local.AppDatabase
import com.example.s7opcuaapp.data.local.dao.LoginStats
import com.example.s7opcuaapp.data.model.*
import kotlinx.coroutines.flow.Flow
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogRepositoryImpl @Inject constructor(
    private val database: AppDatabase
) : LogRepository {

    private val loginHistoryDao = database.loginHistoryDao()
    private val deviceAccessLogDao = database.deviceAccessLogDao()

    override fun getAllLoginHistory(): Flow<List<LoginHistory>> {
        return loginHistoryDao.getAllLoginHistory()
    }

    override fun getLoginHistoryByUser(userId: String): Flow<List<LoginHistory>> {
        return loginHistoryDao.getLoginHistoryByUser(userId)
    }

    override fun getLoginHistoryByDateRange(startDate: Date, endDate: Date): Flow<List<LoginHistory>> {
        return loginHistoryDao.getLoginHistoryByDateRange(startDate.time, endDate.time)
    }

    override suspend fun logLogout(historyId: String, logoutTime: Long, status: LoginStatus) {
        loginHistoryDao.updateLogout(historyId, logoutTime, status)
    }

    override fun getRecentDeviceLogs(limit: Int): Flow<List<DeviceAccessLog>> {
        return deviceAccessLogDao.getRecentLogs(limit)
    }

    override fun getDeviceLogsByUser(userId: String): Flow<List<DeviceAccessLog>> {
        return deviceAccessLogDao.getLogsByUser(userId)
    }

    override fun getDeviceLogsByDevice(deviceId: String): Flow<List<DeviceAccessLog>> {
        return deviceAccessLogDao.getLogsByDevice(deviceId)
    }

    override fun getDeviceLogsByAction(action: DeviceAction): Flow<List<DeviceAccessLog>> {
        return deviceAccessLogDao.getLogsByAction(action)
    }

    override fun getDeviceLogsByDateRange(startDate: Date, endDate: Date): Flow<List<DeviceAccessLog>> {
        return deviceAccessLogDao.getLogsByDateRange(startDate.time, endDate.time)
    }

    override suspend fun logDeviceAccess(
        user: User,
        device: DeviceEntity,
        action: DeviceAction,
        details: ActionDetail?,
        success: Boolean,
        errorMessage: String?
    ) {
        val log = DeviceAccessLog(
            id = UUID.randomUUID().toString(),
            userId = user.id,
            username = user.username,
            deviceId = device.id,
            deviceName = device.name,
            action = action,
            details = details?.let { ActionDetail.toJson(it) },
            timestamp = System.currentTimeMillis(),
            success = success,
            errorMessage = errorMessage
        )
        deviceAccessLogDao.insertLog(log)
    }

    override suspend fun getLoginStats(sinceDate: Date): LoginStats {
        return loginHistoryDao.getLoginStats(sinceDate.time)
    }

    override suspend fun getDeviceActionStats(sinceDate: Date): List<ActionStat> {
        return deviceAccessLogDao.getActionStats(sinceDate.time)
    }

    override suspend fun getDeviceAccessStats(sinceDate: Date): List<DeviceAccessStat> {
        return deviceAccessLogDao.getDeviceAccessStats(sinceDate.time)
    }

    override suspend fun cleanupOldLogs(beforeDate: Date) {
        loginHistoryDao.deleteOldHistory(beforeDate.time)
        deviceAccessLogDao.deleteOldLogs(beforeDate.time)
    }
}