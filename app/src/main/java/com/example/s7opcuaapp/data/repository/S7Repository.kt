package com.example.s7opcuaapp.data.repository

import com.example.s7opcuaapp.data.model.DeviceEntity
import com.example.s7opcuaapp.data.model.PlcData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Giao diện chung cho repository:
 * - OPC UA: implement qua OPCUARepositoryImpl
 * - Modbus TCP/IP: implement qua ModbusRepositoryImpl
 */
interface S7Repository {
    /** Flow phát ra PlcData mỗi khi có thay đổi từ PLC */
    fun observePlcData(): Flow<PlcData>

    /** Phần trăm loading (0..100) */
    fun observeLoadingPercent(): StateFlow<Int>

    /** Bắt đầu kết nối và đọc dữ liệu từ PLC */
    suspend fun start()

    /** Ghi một Boolean (bit) xuống PLC */
    suspend fun writeBoolean(index: Int, value: Boolean)

    /** Ghi một Int (DInt) xuống PLC */
    suspend fun writeInt(index: Int, value: Int)

    /** Dừng mọi kết nối/subscription */
    fun stop()

    /** Cập nhật thông tin Device (IP, port, credentials…) */
    fun updateDevice(device: DeviceEntity)
}
