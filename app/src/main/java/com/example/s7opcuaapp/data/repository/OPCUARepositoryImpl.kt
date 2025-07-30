package com.example.s7opcuaapp.data.repository

import android.util.Log
import com.example.s7opcuaapp.data.local.AppDatabase
import com.example.s7opcuaapp.data.model.DeviceEntity
import com.example.s7opcuaapp.data.model.PlcData
import com.example.s7opcuaapp.data.opcua.OPCUAClientManager
import com.example.s7opcuaapp.util.LoadingTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Job
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Fixed OPC UA Repository với proper authentication handling và session cleanup:
 * - Detect server authentication requirements và sử dụng đúng method ngay từ đầu
 * - Extended delays để đảm bảo session cleanup hoàn toàn
 * - Better error handling và connection limiting
 */
class OPCUARepositoryImpl(
    private var device: DeviceEntity,
    private val database: AppDatabase
) : S7Repository {

    private val _plcDataFlow = MutableStateFlow(PlcData.empty())
    override fun observePlcData(): Flow<PlcData> = _plcDataFlow

    // Coroutine scope riêng cho repository
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Mutex để đồng bộ start/stop operations
    private val connectionMutex = Mutex()

    // State management
    private val isStarted = AtomicBoolean(false)
    private val isConnected = AtomicBoolean(false)
    private var connectionJob: Job? = null

    // Connection management
    private var lastConnectionAttempt = 0L
    private val minConnectionInterval = 2000L // Minimum 2s between attempts

    // Node IDs (chính phải trùng với phần 'Published Variables' trong PLC)
    private val boolNodeIds = (3..16).map { "ns=4;i=$it" }
    private val intNodeIds = (17..47).map { "ns=4;i=$it" }

    // Thread-safe data holders
    private val boolValues = mutableMapOf<Int, Boolean>()
    private val intValues = mutableMapOf<Int, Int>()
    private var lastUpdateTime = 0L
    private var batchUpdateJob: Job? = null

    private val totalNodes = boolNodeIds.size + intNodeIds.size
    private val loadingTracker = LoadingTracker<String>(totalNodes)

    /**
     * Thread-safe start với proper synchronization
     */
    suspend fun start() = connectionMutex.withLock {
        if (isStarted.get()) {
            Log.d("OPCUARepo", "⚠️ Repository already started, skipping")
            return@withLock
        }

        Log.d("OPCUARepo", "🚀 Starting OPC UA Repository...")
        isStarted.set(true)

        // Reset loading tracker khi bắt đầu kết nối mới
        loadingTracker.reset()
        Log.d("OPCUARepo", "🔄 Loading tracker reset - starting fresh")

        // Cancel previous connection job if exists
        connectionJob?.cancel()

        // Bắt đầu connection loop với auto-reconnect
        connectionJob = repositoryScope.launch {
            startConnectionLoop()
        }
    }

    /**
     * Connection loop với better session management
     */
    private suspend fun startConnectionLoop() {
        var consecutiveFailures = 0
        val maxConsecutiveFailures = 5 // Tăng từ 3 lên 5

        while (isStarted.get() && repositoryScope.isActive) {
            try {
                if (!isConnected.get()) {
                    // Rate limiting - đợi ít nhất 2s giữa các attempts
                    val timeSinceLastAttempt = System.currentTimeMillis() - lastConnectionAttempt
                    if (timeSinceLastAttempt < minConnectionInterval) {
                        delay(minConnectionInterval - timeSinceLastAttempt)
                    }

                    Log.d("OPCUARepo", "🔄 Attempting to connect... (failures: $consecutiveFailures)")
                    lastConnectionAttempt = System.currentTimeMillis()

                    // Aggressive cleanup trước khi kết nối
                    try {
                        Log.d("OPCUARepo", "🧹 Performing aggressive cleanup...")
                        OPCUAClientManager.disconnect()

                        // Extended delay để server cleanup hoàn toàn
                        val cleanupDelay = when {
                            consecutiveFailures == 0 -> 1000L  // Lần đầu: 1s
                            consecutiveFailures <= 2 -> 3000L  // Lần 2-3: 3s
                            else -> 5000L                      // Lần 4+: 5s
                        }

                        Log.d("OPCUARepo", "⏳ Waiting ${cleanupDelay}ms for server cleanup...")
                        delay(cleanupDelay)
                    } catch (e: Exception) {
                        Log.w("OPCUARepo", "Error during pre-connect cleanup", e)
                    }

                    val connected = connectToServer()

                    if (connected) {
                        consecutiveFailures = 0
                        isConnected.set(true)

                        // Reset loading tracker khi kết nối thành công
                        loadingTracker.reset()
                        Log.d("OPCUARepo", "🔄 Connection successful, loading tracker reset")

                        subscribeToAllNodes()
                        Log.d("OPCUARepo", "✅ Successfully connected and subscribed")
                    } else {
                        consecutiveFailures++

                        if (consecutiveFailures >= maxConsecutiveFailures) {
                            Log.e("OPCUARepo", "💥 Too many consecutive failures ($consecutiveFailures), stopping connection attempts")
                            break
                        }

                        // Exponential backoff với cap
                        val retryDelay = minOf(5000L * consecutiveFailures, 30000L)
                        Log.w("OPCUARepo", "❌ Connection failed (attempt $consecutiveFailures), retrying in ${retryDelay/1000}s...")
                        delay(retryDelay)
                    }
                } else {
                    // Kiểm tra kết nối mỗi 15s (tăng từ 10s)
                    if (!OPCUAClientManager.isConnected()) {
                        Log.w("OPCUARepo", "📡 Connection lost, attempting reconnect...")
                        isConnected.set(false)
                        consecutiveFailures = 0 // Reset failures counter khi connection lost
                        loadingTracker.reset()
                        Log.d("OPCUARepo", "🔄 Connection lost, loading tracker reset")
                    }
                    delay(15000)
                }
            } catch (e: Exception) {
                Log.e("OPCUARepo", "💥 Error in connection loop", e)
                isConnected.set(false)
                loadingTracker.reset()
                consecutiveFailures++
                delay(5000)
            }
        }

        Log.d("OPCUARepo", "🔄 Connection loop ended")
    }

    /**
     * Smart connection với proper authentication detection
     */
    private suspend fun connectToServer(): Boolean {
        return try {
            // Nếu device có username, dùng ngay thay vì thử anonymous trước
            val useCredentials = !device.opcUsername.isNullOrBlank()

            if (useCredentials) {
                Log.d("OPCUARepo", "🔐 Using Username authentication (${device.opcUsername})")
                OPCUAClientManager.connect(
                    ipAddress = device.ipAddress,
                    port = device.port,
                    username = device.opcUsername,
                    password = device.opcPassword
                )
            } else {
                Log.d("OPCUARepo", "🔓 Attempting Anonymous authentication")
                val anonymousSuccess = OPCUAClientManager.connect(
                    ipAddress = device.ipAddress,
                    port = device.port,
                    username = null,
                    password = null
                )

                if (!anonymousSuccess) {
                    Log.w("OPCUARepo", "❌ Anonymous failed and no credentials available")
                }

                anonymousSuccess
            }
        } catch (e: Exception) {
            when {
                e.message?.contains("no anonymous token policy found") == true -> {
                    Log.e("OPCUARepo", "❌ Server requires authentication but no credentials provided")
                }
                e.message?.contains("Bad_TooManySessions") == true -> {
                    Log.e("OPCUARepo", "❌ Too many sessions - server needs more time to cleanup")
                }
                else -> {
                    Log.e("OPCUARepo", "❌ Connect error: ${e.message}", e)
                }
            }
            false
        }
    }

    /**
     * Subscribe tất cả nodes với proper error handling
     */
    private suspend fun subscribeToAllNodes() {
        // Khởi tạo giá trị mặc định
        boolValues.clear()
        intValues.clear()
        repeat(boolNodeIds.size) { boolValues[it] = false }
        repeat(intNodeIds.size) { intValues[it] = 0 }

        Log.d("OPCUARepo", "📊 Starting subscription for ${boolNodeIds.size} bools + ${intNodeIds.size} ints")

        var successfulSubscriptions = 0
        val totalSubscriptions = boolNodeIds.size + intNodeIds.size

        // Subscribe boolean nodes
        boolNodeIds.forEachIndexed { idx, nodeId ->
            try {
                OPCUAClientManager.createSubscription(
                    nodeIdString = nodeId,
                    samplingInterval = UInteger.valueOf(250)
                ) { dataValue ->
                    updateBoolValue(idx, dataValue)
                }

                loadingTracker.markLoaded(nodeId)
                successfulSubscriptions++
                Log.d("OPCUARepo", "📈 Bool[$idx] subscription created, progress: ${loadingTracker.loadedCount}/${totalNodes}")

            } catch (e: Exception) {
                Log.e("OPCUARepo", "❌ Failed to subscribe bool[$idx]: $nodeId", e)
            }
        }

        // Subscribe integer nodes
        intNodeIds.forEachIndexed { idx, nodeId ->
            try {
                OPCUAClientManager.createSubscription(
                    nodeIdString = nodeId,
                    samplingInterval = UInteger.valueOf(250)
                ) { dataValue ->
                    updateIntValue(idx, dataValue)
                }

                loadingTracker.markLoaded(nodeId)
                successfulSubscriptions++
                Log.d("OPCUARepo", "📈 Int[$idx] subscription created, progress: ${loadingTracker.loadedCount}/${totalNodes}")

            } catch (e: Exception) {
                Log.e("OPCUARepo", "❌ Failed to subscribe int[$idx]: $nodeId", e)
            }
        }

        // Log final loading state
        Log.d("OPCUARepo", "🎯 Subscriptions completed: $successfulSubscriptions/$totalSubscriptions successful")
        Log.d("OPCUARepo", "🎯 Loading tracker: ${loadingTracker.loadedCount}/${totalNodes} = ${loadingTracker.percent.value}%")

        // Bắt đầu batch update timer
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

    fun observeLoadingPercent(): StateFlow<Int> = loadingTracker.percent

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
     * Timer để update UI định kỳ với proper job management
     */
    private fun startBatchUpdateTimer() {
        // Cancel previous timer if exists
        batchUpdateJob?.cancel()

        batchUpdateJob = repositoryScope.launch {
            while (isConnected.get() && repositoryScope.isActive) {
                publishUpdate()
                delay(250) // Update UI mỗi 250ms
            }
        }
    }

    /**
     * Thread-safe stop với extended cleanup delays
     */
    override fun stop() {
        repositoryScope.launch {
            connectionMutex.withLock {
                if (!isStarted.get()) {
                    Log.d("OPCUARepo", "⚠️ Repository already stopped, skipping")
                    return@withLock
                }

                Log.d("OPCUARepo", "🛑 Stopping OPC UA Repository...")
                isStarted.set(false)
                isConnected.set(false)

                try {
                    // Cancel all running jobs
                    connectionJob?.cancel()
                    batchUpdateJob?.cancel()

                    // Extended wait for jobs to finish
                    Log.d("OPCUARepo", "⏳ Waiting for jobs to finish...")
                    delay(500)

                    // Disconnect from OPC UA server
                    Log.d("OPCUARepo", "🔌 Disconnecting from server...")
                    OPCUAClientManager.disconnect()

                    // Additional delay để server có thời gian cleanup session
                    Log.d("OPCUARepo", "⏳ Waiting for server session cleanup...")
                    delay(2000) // 2s để server cleanup session hoàn toàn

                    Log.d("OPCUARepo", "✅ Repository stopped and session cleaned up")

                } catch (e: Exception) {
                    Log.e("OPCUARepo", "Error during stop", e)
                }
            }
        }
    }

    /**
     * Ghi một Boolean xuống PLC với error handling
     */
    override suspend fun writeBoolean(index: Int, value: Boolean) = withContext(Dispatchers.IO) {
        if (!isConnected.get()) {
            throw Exception("Not connected to PLC")
        }

        if (index in boolNodeIds.indices) {
            val nodeIdString = boolNodeIds[index]
            val accessLevel = OPCUAClientManager.   readAccessLevel(nodeIdString)
            if (accessLevel != null && (accessLevel.toInt() and 0x02) != 0) {
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
     * Ghi một Int xuống PLC với error handling
     */
    override suspend fun writeInt(index: Int, value: Int) = withContext(Dispatchers.IO) {
        if (!isConnected.get()) {
            throw Exception("Not connected to PLC")
        }

        if (index in intNodeIds.indices) {
            val nodeIdString = intNodeIds[index]
            val shortValue = value.toShort()
            val accessLevel = OPCUAClientManager.readAccessLevel(nodeIdString)
            if (accessLevel != null && (accessLevel.toInt() and 0x02) != 0) {
                val status = OPCUAClientManager.writeNode(nodeIdString, shortValue)
                if (status == null || !status.isGood) {
                    Log.e("OPCUARepo", "❌ WriteInt failed for $nodeIdString: $status")
                    throw Exception("WriteInt failed: $status")
                } else {
                    Log.d("OPCUARepo", "✅ WriteInt succeeded for $nodeIdString = $value")
                }
            } else {
                Log.e("OPCUARepo", "❌ Node $nodeIdString is not writable")
                throw Exception("Node $nodeIdString is not writable")
            }
        }
    }

    override fun updateDevice(device: DeviceEntity) {
        this.device = device
    }
}