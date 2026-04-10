package com.example.s7opcuaapp.data.repository

import android.util.Log
import com.example.s7opcuaapp.data.model.DeviceEntity
import com.example.s7opcuaapp.data.model.PlcData
import com.example.s7opcuaapp.data.modbus.ModbusTcpClientManager
import com.example.s7opcuaapp.util.LoadingTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean

/**
 * S7Repository implementation cho Modbus TCP/IP.
 * Đọc Holding Registers theo polling interval, unpack bits cho booleans.
 */
class ModbusRepositoryImpl(
    private var device: DeviceEntity
) : S7Repository {

    private val _plcDataFlow = MutableStateFlow(PlcData.empty())
    override fun observePlcData(): Flow<PlcData> = _plcDataFlow

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val connectionMutex = Mutex()
    private val writeMutex = Mutex() // Serialize write operations (read-modify-write cho bools)

    private val isStarted = AtomicBoolean(false)
    private val isConnected = AtomicBoolean(false)
    private var connectionJob: Job? = null

    private var lastConnectionAttempt = 0L
    private val minConnectionInterval = 2000L

    // Loading tracker: 2 steps - "bools" + "ints"
    private val loadingTracker = LoadingTracker<String>(2)

    override fun observeLoadingPercent(): StateFlow<Int> = loadingTracker.percent

    override suspend fun start() = connectionMutex.withLock {
        if (isStarted.get()) {
            Log.d("ModbusRepo", "Already started, skipping")
            return@withLock
        }

        Log.d("ModbusRepo", "Starting Modbus Repository...")
        isStarted.set(true)
        loadingTracker.reset()

        connectionJob?.cancel()
        connectionJob = repositoryScope.launch {
            startConnectionLoop()
        }
    }

    private suspend fun startConnectionLoop() {
        var consecutiveFailures = 0
        val maxConsecutiveFailures = 5

        while (isStarted.get() && repositoryScope.isActive) {
            try {
                if (!isConnected.get()) {
                    val timeSinceLastAttempt = System.currentTimeMillis() - lastConnectionAttempt
                    if (timeSinceLastAttempt < minConnectionInterval) {
                        delay(minConnectionInterval - timeSinceLastAttempt)
                    }

                    Log.d("ModbusRepo", "Connecting... (failures: $consecutiveFailures)")
                    lastConnectionAttempt = System.currentTimeMillis()

                    try {
                        ModbusTcpClientManager.disconnect()
                        delay(500)
                    } catch (_: Exception) {}

                    val connected = ModbusTcpClientManager.connect(
                        ipAddress = device.ipAddress,
                        port = device.port
                    )

                    if (connected) {
                        consecutiveFailures = 0
                        isConnected.set(true)
                        loadingTracker.reset()
                        Log.d("ModbusRepo", "Connected, starting poll loop")
                        pollLoop()
                        // pollLoop exits when disconnected or stopped
                    } else {
                        consecutiveFailures++
                        if (consecutiveFailures >= maxConsecutiveFailures) {
                            Log.e("ModbusRepo", "Too many failures ($consecutiveFailures), stopping")
                            break
                        }
                        val retryDelay = minOf(5000L * consecutiveFailures, 30000L)
                        Log.w("ModbusRepo", "Connection failed, retrying in ${retryDelay / 1000}s...")
                        delay(retryDelay)
                    }
                } else {
                    if (!ModbusTcpClientManager.isConnected()) {
                        Log.w("ModbusRepo", "Connection lost, reconnecting...")
                        isConnected.set(false)
                        loadingTracker.reset()
                    }
                    delay(5000)
                }
            } catch (e: Exception) {
                Log.e("ModbusRepo", "Error in connection loop", e)
                isConnected.set(false)
                loadingTracker.reset()
                consecutiveFailures++
                delay(5000)
            }
        }

        Log.d("ModbusRepo", "Connection loop ended")
    }

    /**
     * Polling loop: đọc registers theo interval, cập nhật PlcData.
     */
    private suspend fun pollLoop() {
        while (isStarted.get() && isConnected.get() && repositoryScope.isActive) {
            try {
                // Đọc bool register(s)
                val boolRegCount = ceilDiv(device.modbusBoolCount, 16)
                val boolRegisters = ModbusTcpClientManager.readHoldingRegisters(
                    slaveId = device.modbusSlaveId,
                    startAddress = device.modbusBoolRegisterAddress,
                    count = boolRegCount
                )
                val bools = unpackBools(boolRegisters, device.modbusBoolCount)
                loadingTracker.markLoaded("bools")

                // Đọc int registers
                val intRegisters = ModbusTcpClientManager.readHoldingRegisters(
                    slaveId = device.modbusSlaveId,
                    startAddress = device.modbusIntRegisterAddress,
                    count = device.modbusIntRegisterCount
                )
                val ints = intRegisters.toList()
                loadingTracker.markLoaded("ints")

                _plcDataFlow.value = PlcData(bools = bools, ints = ints)

                delay(device.modbusPollingIntervalMs.toLong())

            } catch (e: Exception) {
                Log.e("ModbusRepo", "Poll error", e)
                isConnected.set(false)
                return // Exit poll loop, connection loop will handle reconnect
            }
        }
    }

    /**
     * Unpack bits từ holding register(s) thành List<Boolean>.
     * Bit 0 của register đầu tiên = bool[0], bit 1 = bool[1], ...
     */
    private fun unpackBools(registerValues: IntArray, count: Int): List<Boolean> {
        val result = mutableListOf<Boolean>()
        for (i in 0 until count) {
            val regIndex = i / 16
            val bitIndex = i % 16
            if (regIndex < registerValues.size) {
                val bit = (registerValues[regIndex] shr bitIndex) and 1
                result.add(bit == 1)
            } else {
                result.add(false)
            }
        }
        return result
    }

    /**
     * Ghi Boolean: read-modify-write pattern trên holding register.
     * Mutex đảm bảo không bị race condition giữa các write đồng thời.
     */
    override suspend fun writeBoolean(index: Int, value: Boolean) = withContext(Dispatchers.IO) {
        if (!isConnected.get()) throw Exception("Not connected to PLC")

        writeMutex.withLock {
            val regIndex = index / 16
            val bitIndex = index % 16
            val address = device.modbusBoolRegisterAddress + regIndex

            // Read current register value
            val current = ModbusTcpClientManager.readHoldingRegisters(
                device.modbusSlaveId, address, 1
            )
            var regValue = current[0]

            // Modify the target bit
            regValue = if (value) {
                regValue or (1 shl bitIndex)
            } else {
                regValue and (1 shl bitIndex).inv()
            }

            // Write back
            ModbusTcpClientManager.writeSingleRegister(
                device.modbusSlaveId, address, regValue
            )
            Log.d("ModbusRepo", "WriteBool OK: index=$index value=$value (reg=$address bit=$bitIndex)")
        }
    }

    /**
     * Ghi Int: write single holding register.
     */
    override suspend fun writeInt(index: Int, value: Int) = withContext(Dispatchers.IO) {
        if (!isConnected.get()) throw Exception("Not connected to PLC")

        val address = device.modbusIntRegisterAddress + index
        ModbusTcpClientManager.writeSingleRegister(
            device.modbusSlaveId, address, value and 0xFFFF
        )
        Log.d("ModbusRepo", "WriteInt OK: index=$index value=$value (addr=$address)")
    }

    override fun stop() {
        repositoryScope.launch {
            connectionMutex.withLock {
                if (!isStarted.get()) return@withLock

                Log.d("ModbusRepo", "Stopping Modbus Repository...")
                isStarted.set(false)
                isConnected.set(false)

                try {
                    connectionJob?.cancel()
                    delay(300)
                    ModbusTcpClientManager.disconnect()
                    Log.d("ModbusRepo", "Stopped and disconnected")
                } catch (e: Exception) {
                    Log.e("ModbusRepo", "Error during stop", e)
                }
            }
        }
    }

    override fun updateDevice(device: DeviceEntity) {
        this.device = device
    }

    private fun ceilDiv(a: Int, b: Int): Int = (a + b - 1) / b
}
