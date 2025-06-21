package com.example.s7opcuaapp.data.repository

import android.util.Log
import com.example.s7opcuaapp.data.model.DeviceEntity
import com.example.s7opcuaapp.data.model.PlcData
import com.example.s7opcuaapp.data.opcua.OPCUAClientManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Optimized OPC UA Repository:
 * - Chạy hoàn toàn trên background thread
 * - Batch update để tránh spam UI
 * - Auto-reconnect khi mất kết nối
 */
class OPCUARepositoryImpl(
    private val device: DeviceEntity
) : S7Repository {

    private val _plcDataFlow = MutableStateFlow(PlcData.empty())
    override fun observePlcData(): Flow<PlcData> = _plcDataFlow

    // Coroutine scope riêng cho repository
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isStarted = AtomicBoolean(false)
    private val isConnected = AtomicBoolean(false)

    // Node IDs (chính phải trùng với phần 'Published Variables' trong PLC)
    private val boolNodeIds = (3..16).map { "ns=4;i=$it" }
    private val intNodeIds = (17..44).map { "ns=4;i=$it" }

    // Thread-safe data holders
    private val boolValues = mutableMapOf<Int, Boolean>()
    private val intValues = mutableMapOf<Int, Int>()
    private var lastUpdateTime = 0L

    /**
     * Bắt đầu kết nối và subscribe – chạy hoàn toàn background
     */
    suspend fun start() = withContext(Dispatchers.IO) {
        if (isStarted.compareAndSet(false, true)) {
            Log.d("OPCUARepo", "🚀 Starting OPC UA Repository...")

            // Bắt đầu connection loop với auto-reconnect
            repositoryScope.launch {
                startConnectionLoop()
            }
        }
    }

    /**
     * Connection loop với auto-reconnect
     */
    private suspend fun startConnectionLoop() {
        while (isStarted.get() && repositoryScope.isActive) {
            try {
                if (!isConnected.get()) {
                    Log.d("OPCUARepo", "🔄 Attempting to connect...")
                    val connected = connectToServer()

                    if (connected) {
                        isConnected.set(true)
                        subscribeToAllNodes()
                        Log.d("OPCUARepo", "✅ Successfully connected and subscribed")
                    } else {
                        Log.w("OPCUARepo", "❌ Connection failed, retrying in 5s...")
                        delay(5000)
                    }
                } else {
                    // Kiểm tra kết nối mỗi 10s
                    if (!OPCUAClientManager.isConnected()) {
                        Log.w("OPCUARepo", "📡 Connection lost, attempting reconnect...")
                        isConnected.set(false)
                    }
                    delay(10000)
                }
            } catch (e: Exception) {
                Log.e("OPCUARepo", "💥 Error in connection loop", e)
                isConnected.set(false)
                delay(5000)
            }
        }
    }

    /**
     * Kết nối đến server với anonymous auth trước (có thể chuyển sang username nếu anonymous không được phép).
     */
    private suspend fun connectToServer(): Boolean {
        return try {
            // Thử kết nối với anonymous trước (vì server chỉ hỗ trợ "No security")
            val success = OPCUAClientManager.connect(
                ipAddress = device.ipAddress,
                port = device.port,
                username = null, // Dùng anonymous
                password = null
            )

            if (!success && !device.opcUsername.isNullOrBlank()) {
                // Nếu anonymous thất bại, thử với username
                Log.d("OPCUARepo", "🔄 Anonymous failed, trying with username...")
                OPCUAClientManager.connect(
                    ipAddress = device.ipAddress,
                    port = device.port,
                    username = device.opcUsername,
                    password = device.opcPassword
                )
            } else {
                success
            }
        } catch (e: Exception) {
            Log.e("OPCUARepo", "❌ Connect error", e)
            false
        }
    }

    /**
     * Subscribe tất cả nodes với batched update
     */
    private suspend fun subscribeToAllNodes() {
        // Khởi tạo giá trị mặc định
        boolValues.clear()
        intValues.clear()
        repeat(boolNodeIds.size) { boolValues[it] = false }
        repeat(intNodeIds.size) { intValues[it] = 0 }

        // **Chỉ gọi createSubscription (dùng chung subscription internally)**
        boolNodeIds.forEachIndexed { idx, nodeId ->
            // samplingInterval = 1000ms (1s) để giảm tải
            OPCUAClientManager.createSubscription(
                nodeIdString = nodeId,
                samplingInterval = UInteger.valueOf(250)
            ) { dataValue ->
                updateBoolValue(idx, dataValue)
            }
        }

        intNodeIds.forEachIndexed { idx, nodeId ->
            OPCUAClientManager.createSubscription(
                nodeIdString = nodeId,
                samplingInterval = UInteger.valueOf(250)
            ) { dataValue ->
                updateIntValue(idx, dataValue)
            }
        }

        // Bắt đầu batch update timer (nếu có)
        startBatchUpdateTimer()
    }

    /**
     * Update Boolean value với thread safety
     */
    private fun updateBoolValue(index: Int, dataValue: DataValue) {
        try {
            val newValue = dataValue.value.value as? Boolean ?: false
            synchronized(boolValues) {
                boolValues[index] = newValue
            }
            scheduleUpdate()
        } catch (e: Exception) {
            Log.w("OPCUARepo", "Error updating bool[$index]", e)
        }
    }

    /**
     * Update Integer value với thread safety
     */
    private fun updateIntValue(index: Int, dataValue: DataValue) {
        try {
            val raw = dataValue.value.value
            val newValue = when (raw) {
                is Int      -> raw
                is Long     -> raw.toInt()
                is Short    -> raw.toInt()
                is UInteger -> raw.toInt()
                else        -> 0
            }
            synchronized(intValues) {
                intValues[index] = newValue
            }
            scheduleUpdate()
        } catch (e: Exception) {
            Log.w("OPCUARepo", "Error updating int[$index]", e)
        }
    }

    /**
     * Schedule batch update (throttle để tránh spam UI)
     */
    private fun scheduleUpdate() {
        val now = System.currentTimeMillis()
        lastUpdateTime = now

        // Delay 100ms rồi mới update UI
        repositoryScope.launch {
            delay(100)
            if (System.currentTimeMillis() - lastUpdateTime >= 100) {
                publishUpdate()
            }
        }
    }

    /**
     * Publish update lên UI thread
     */
    private fun publishUpdate() {
        val boolList: List<Boolean>
        val intList: List<Int>

        synchronized(boolValues) {
            boolList = (0 until boolNodeIds.size).map { boolValues[it] ?: false }
        }
        synchronized(intValues) {
            intList = (0 until intNodeIds.size).map { intValues[it] ?: 0 }
        }

        val newData = PlcData(bools = boolList, ints = intList)
        _plcDataFlow.value = newData
    }

    /**
     * Timer để update UI định kỳ
     */
    private fun startBatchUpdateTimer() {
        repositoryScope.launch {
            while (isConnected.get() && repositoryScope.isActive) {
                publishUpdate()
                delay(250) // Update UI mỗi 300ms
            }
        }
    }

    /**
     * Ghi một Boolean xuống PLC. Sử dụng writeNode(...) của ClientManager.
     */
    override suspend fun writeBoolean(index: Int, value: Boolean) = withContext(Dispatchers.IO) {
        if (index in boolNodeIds.indices) {
            val nodeIdString = boolNodeIds[index]
            val accessLevel = OPCUAClientManager.readAccessLevel(nodeIdString)
            if (accessLevel != null && (accessLevel.toInt() and 0x02) != 0) { // Bit 1 = writable
                val status = OPCUAClientManager.writeNode(nodeIdString, value)
                if (status == null || !status.isGood) {
                    Log.e("OPCUARepo", "❌ WriteBoolean failed for $nodeIdString: $status")
                    throw Exception("WriteBoolean failed: $status")
                } else {
                    Log.d("OPCUARepo", "✅ WriteBoolean succeeded for $nodeIdString = $value")
                }
            } else {
                Log.e("OPCUARepo", "❌ Node $nodeIdString is not writable")
                throw Exception("Node $nodeIdString is not writable")
            }
        }
    }

    /**
     * Ghi một Int xuống PLC. Sử dụng writeNode(...) của ClientManager.
     */
    override suspend fun writeInt(index: Int, value: Int) = withContext(Dispatchers.IO) {
        if (index in intNodeIds.indices) {
            val nodeIdString = intNodeIds[index]
            val shortValue = value.toShort()
            val accessLevel = OPCUAClientManager.readAccessLevel(nodeIdString)
            if (accessLevel != null && (accessLevel.toInt() and 0x02) != 0) { // Bit 1 = writable
                val status = OPCUAClientManager.writeNode(nodeIdString, shortValue)
                if (status == null || !status.isGood) {
                    Log.e("OPCUARepo", "❌ WriteBoolean failed for $nodeIdString: $status")
                    throw Exception("WriteBoolean failed: $status")
                } else {
                    Log.d("OPCUARepo", "✅ WriteBoolean succeeded for $nodeIdString = $value")
                }
            } else {
                Log.e("OPCUARepo", "❌ Node $nodeIdString is not writable")
                throw Exception("Node $nodeIdString is not writable")
            }
        }
    }

    override fun stop() {
        if (isStarted.compareAndSet(true, false)) {
            Log.d("OPCUARepo", "🛑 Stopping OPC UA Repository...")
            isConnected.set(false)

            repositoryScope.launch {
                try {
                    OPCUAClientManager.disconnect()
                } catch (e: Exception) {
                    Log.e("OPCUARepo", "Error during stop", e)
                }
            }
        }
    }
}
