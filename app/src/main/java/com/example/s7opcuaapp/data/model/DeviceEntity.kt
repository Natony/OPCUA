package com.example.s7opcuaapp.data.model

/**
 * Model lưu thông tin thiết bị PLC:
 *  - id: duy nhất
 *  - name: tên thiết bị
 *  - ipAddress: IP
 *  - port: cổng OPC UA (mặc định 4840)
 *  - opcUsername/opcPassword: credential cho OPC UA server
 *  - useOpcUa: true nếu dùng OPC UA
 */
data class DeviceEntity(
    val id: String,
    val name: String,
    val ipAddress: String,
    val port: Int = 4840,
    val opcUsername: String = "",
    val opcPassword: String = "",
    val useOpcUa: Boolean = true
)
