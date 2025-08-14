package com.example.s7opcuaapp.data.model.alarm

data class AlarmStatistics(
    val totalActive: Int = 0,
    val totalUnacknowledged: Int = 0,
    val criticalCount: Int = 0,
    val highCount: Int = 0,
    val mediumCount: Int = 0,
    val lowCount: Int = 0,
    val emergencyCount: Int = 0,
    val last24Hours: Int = 0,
    val last7Days: Int = 0
)