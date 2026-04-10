package com.example.s7opcuaapp.data.modbus

import android.util.Log
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster
import com.ghgande.j2mod.modbus.procimg.SimpleRegister
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Singleton wrapper cho j2mod ModbusTCPMaster.
 * Cung cấp các method đọc/ghi Holding Registers qua Modbus TCP/IP.
 */
object ModbusTcpClientManager {

    private var master: ModbusTCPMaster? = null

    suspend fun connect(ipAddress: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d("ModbusClient", "Connecting to $ipAddress:$port...")
            disconnect() // Cleanup trước khi kết nối mới
            val m = ModbusTCPMaster(ipAddress, port)
            m.connect()
            master = m
            Log.d("ModbusClient", "Connected to $ipAddress:$port")
            true
        } catch (e: Exception) {
            Log.e("ModbusClient", "Connection failed to $ipAddress:$port", e)
            master = null
            false
        }
    }

    fun isConnected(): Boolean {
        return master?.isConnected ?: false
    }

    /**
     * Đọc Holding Registers (Function Code 03).
     * @return IntArray chứa giá trị unsigned 16-bit của mỗi register.
     */
    suspend fun readHoldingRegisters(
        slaveId: Int,
        startAddress: Int,
        count: Int
    ): IntArray = withContext(Dispatchers.IO) {
        val m = master ?: throw IllegalStateException("Not connected")
        try {
            val registers = m.readMultipleRegisters(slaveId, startAddress, count)
            IntArray(registers.size) { i -> registers[i].value and 0xFFFF }
        } catch (e: Exception) {
            Log.e("ModbusClient", "Read failed: slave=$slaveId addr=$startAddress count=$count", e)
            throw e
        }
    }

    /**
     * Ghi một Holding Register (Function Code 06).
     */
    suspend fun writeSingleRegister(
        slaveId: Int,
        address: Int,
        value: Int
    ) = withContext(Dispatchers.IO) {
        val m = master ?: throw IllegalStateException("Not connected")
        try {
            val register = SimpleRegister(value and 0xFFFF)
            m.writeSingleRegister(slaveId, address, register)
            Log.d("ModbusClient", "Write OK: slave=$slaveId addr=$address value=$value")
        } catch (e: Exception) {
            Log.e("ModbusClient", "Write failed: slave=$slaveId addr=$address", e)
            throw e
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            master?.disconnect()
            Log.d("ModbusClient", "Disconnected")
        } catch (e: Exception) {
            Log.w("ModbusClient", "Error during disconnect", e)
        } finally {
            master = null
        }
    }
}
