package com.example.s7opcuaapp.data.buffer

import android.util.Log
import com.example.s7opcuaapp.data.model.PlcData
import com.example.s7opcuaapp.util.PerformanceMonitor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow

/**
 * Smart buffer system to batch PLC data updates and reduce UI update frequency
 * FIXED: No more blocking main thread
 */
@Singleton
class PlcDataBuffer @Inject constructor(
    private val performanceMonitor: PerformanceMonitor
) {

    // Thread-safe data storage
    private val boolBuffer = ConcurrentHashMap<Int, Boolean>()
    private val intBuffer = ConcurrentHashMap<Int, Int>()

    // Change tracking
    private val boolChanged = ConcurrentHashMap<Int, Boolean>()
    private val intChanged = ConcurrentHashMap<Int, Boolean>()

    // Timing control
    private val lastEmitTime = AtomicLong(0)
    private val pendingEmit = AtomicBoolean(false)

    // Configuration
    private val MIN_EMIT_INTERVAL = 1000L
    private val BATCH_DELAY = 250L

    // Output flow
    private val _dataFlow = MutableSharedFlow<PlcData>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val dataFlow: SharedFlow<PlcData> = _dataFlow.asSharedFlow()

    // Coroutine scope for buffer operations
    private val bufferScope = CoroutineScope(
        Dispatchers.Default + SupervisorJob() +
                CoroutineName("PlcDataBuffer")
    )

    private var emitJob: Job? = null

    // Priority tracking for critical values
    private val criticalBoolIndices = setOf(4, 10) // Power, E-Stop
    private val criticalIntIndices = setOf<Int>()

    // Track last emitted data to avoid redundant updates
    private var lastEmittedData: PlcData? = null
    private var updateCount = 0
    private var lastLogTime = 0L

    init {
        // Initialize with default values
        repeat(15) { boolBuffer[it] = false }
        repeat(28) { intBuffer[it] = 0 }
        Log.d("PlcDataBuffer", "Buffer initialized with default values")
    }

    /**
     * Update boolean value in buffer
     */
    fun updateBool(index: Int, value: Boolean) {
        val previousValue = boolBuffer[index]
        if (previousValue != value) {
            boolBuffer[index] = value
            boolChanged[index] = true

            // Critical values trigger immediate emit
            if (index in criticalBoolIndices) {
                scheduleImmediateEmit()
            } else {
                scheduleBatchEmit()
            }

            Log.v("PlcDataBuffer", "Bool[$index] changed: $previousValue → $value")
        }
    }

    /**
     * Update integer value in buffer
     */
    fun updateInt(index: Int, value: Int) {
        val previousValue = intBuffer[index]
        if (previousValue != value) {
            intBuffer[index] = value
            intChanged[index] = true

            // Critical values trigger immediate emit
            if (index in criticalIntIndices) {
                scheduleImmediateEmit()
            } else {
                scheduleBatchEmit()
            }

            Log.v("PlcDataBuffer", "Int[$index] changed: $previousValue → $value")
        }
    }

    /**
     * Schedule immediate emit for critical values
     */
    private fun scheduleImmediateEmit() {
        emitJob?.cancel()
        emitJob = bufferScope.launch {
            emitData()
        }
    }

    /**
     * Schedule batch emit with delay
     */
    private fun scheduleBatchEmit() {
        if (!pendingEmit.getAndSet(true)) {
            emitJob?.cancel()
            emitJob = bufferScope.launch {
                delay(BATCH_DELAY)

                val now = System.currentTimeMillis()
                val timeSinceLastEmit = now - lastEmitTime.get()

                if (timeSinceLastEmit < MIN_EMIT_INTERVAL) {
                    delay(MIN_EMIT_INTERVAL - timeSinceLastEmit)
                }

                emitData()
            }
        }
    }

    /**
     * Emit buffered data with redundancy check
     */
    private suspend fun emitData() {
        if (!hasChanges()) {
            pendingEmit.set(false)
            return
        }

        // Create snapshot of current data
        val boolList = (0 until 15).map { boolBuffer[it] ?: false }
        val intList = (0 until 28).map { intBuffer[it] ?: 0 }

        val newData = PlcData(
            bools = boolList,
            ints = intList
        )

        // Only emit if data is actually different
        if (newData != lastEmittedData) {
            lastEmittedData = newData

            // Clear change flags
            boolChanged.clear()
            intChanged.clear()

            // Update timing
            lastEmitTime.set(System.currentTimeMillis())
            pendingEmit.set(false)

            // Record performance metric
            performanceMonitor.recordPlcUpdate()

            // Track update rate for debugging
            updateCount++
            val now = System.currentTimeMillis()
            if (now - lastLogTime >= 5000) {
                val rate = updateCount * 1000.0 / (now - lastLogTime)
                Log.d("PlcDataBuffer", "Update rate: ${String.format("%.1f", rate)}/s")
                updateCount = 0
                lastLogTime = now
            }

            // Emit data
            _dataFlow.emit(newData)
        } else {
            pendingEmit.set(false)
            boolChanged.clear()
            intChanged.clear()
        }
    }

    /**
     * Check if there are pending changes
     */
    private fun hasChanges(): Boolean {
        return boolChanged.isNotEmpty() || intChanged.isNotEmpty()
    }

    /**
     * Get current data snapshot without triggering emit
     */
    fun getCurrentData(): PlcData {
        val boolList = (0 until 15).map { boolBuffer[it] ?: false }
        val intList = (0 until 28).map { intBuffer[it] ?: 0 }
        return PlcData(bools = boolList, ints = intList)
    }

    /**
     * Force emit current data
     */
    fun forceEmit() {
        bufferScope.launch {
            lastEmittedData = null
            emitData()
        }
    }

    /**
     * Clear buffer and reset to defaults
     */
    fun clear() {
        emitJob?.cancel()

        boolBuffer.clear()
        intBuffer.clear()
        boolChanged.clear()
        intChanged.clear()

        repeat(15) { boolBuffer[it] = false }
        repeat(28) { intBuffer[it] = 0 }

        pendingEmit.set(false)
        lastEmitTime.set(0)
        lastEmittedData = null
        updateCount = 0
        lastLogTime = 0

        Log.d("PlcDataBuffer", "Buffer cleared")
    }

    /**
     * FIXED: Clean up resources WITHOUT blocking
     */
    suspend fun dispose() {
        Log.d("PlcDataBuffer", "Disposing buffer...")

        // Cancel emit job first
        emitJob?.cancel()

        // Cancel scope children and wait in coroutine context
        bufferScope.coroutineContext.cancelChildren()

        // Wait for all jobs to complete (non-blocking)
        withTimeoutOrNull(1000L) {
            bufferScope.coroutineContext[Job]?.children?.forEach { job ->
                job.join()
            }
        }

        // Clear data after coroutines complete
        boolBuffer.clear()
        intBuffer.clear()
        boolChanged.clear()
        intChanged.clear()

        Log.d("PlcDataBuffer", "Buffer disposed")
    }

    /**
     * Alternative synchronous cleanup for when suspend is not available
     */
    fun cleanup() {
        // Cancel all jobs immediately
        emitJob?.cancel()
        bufferScope.cancel()

        // Clear data
        boolBuffer.clear()
        intBuffer.clear()
        boolChanged.clear()
        intChanged.clear()

        Log.d("PlcDataBuffer", "Buffer cleaned up")
    }

    /**
     * Get buffer statistics for debugging
     */
    fun getStats(): BufferStats {
        return BufferStats(
            boolCount = boolBuffer.size,
            intCount = intBuffer.size,
            pendingChanges = boolChanged.size + intChanged.size,
            lastEmitTime = lastEmitTime.get(),
            hasPendingEmit = pendingEmit.get(),
            updateRate = if (lastLogTime > 0) {
                updateCount * 1000.0 / (System.currentTimeMillis() - lastLogTime)
            } else 0.0
        )
    }

    data class BufferStats(
        val boolCount: Int,
        val intCount: Int,
        val pendingChanges: Int,
        val lastEmitTime: Long,
        val hasPendingEmit: Boolean,
        val updateRate: Double
    )
}