package com.example.s7opcuaapp.data.model

/**
 * Model lưu thông tin thiết bị PLC.
 * Hỗ trợ 2 protocol: OPC UA (useOpcUa=true) và Modbus TCP/IP (useOpcUa=false).
 * Các field Modbus có default values để tương thích ngược với Gson deserialization.
 */
data class DeviceEntity(
    val id: String,
    val name: String,
    val ipAddress: String,
    val port: Int = 4840,
    // OPC UA
    val opcUsername: String = "",
    val opcPassword: String = "",
    val useOpcUa: Boolean = true,
    // Modbus TCP/IP
    val modbusSlaveId: Int = 1,
    val modbusBoolRegisterAddress: Int = 0,
    val modbusIntRegisterAddress: Int = 1,
    val modbusIntRegisterCount: Int = 28,
    val modbusBoolCount: Int = 15,
    val modbusPollingIntervalMs: Int = 250
)
