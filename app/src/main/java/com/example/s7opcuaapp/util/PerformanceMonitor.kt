package com.example.s7opcuaapp.util

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optimized Performance monitoring utility with better ANR prevention and lifecycle management
 */
@Singleton
class PerformanceMonitor @Inject constructor() {

    private val metrics = ConcurrentHashMap<MetricType, MetricData>()
    private val _performanceReport = MutableStateFlow(PerformanceReport())
    val performanceReport: StateFlow<PerformanceReport> = _performanceReport

    // Lightweight coroutine scope for background operations
    private val monitorScope = CoroutineScope(
        Dispatchers.Default + SupervisorJob() +
                CoroutineName("PerformanceMonitor") +
                CoroutineExceptionHandler { _, throwable ->
                    Log.e(TAG, "Performance monitor error", throwable)
                }
    )

    // State management
    private val isMonitorActive = AtomicBoolean(true)
    private val isReportingEnabled = AtomicBoolean(true)

    // Jobs management
    private var reportGenerationJob: Job? = null
    private var cleanupJob: Job? = null

    // Rate limiting for report generation
    private val lastReportTime = AtomicLong(0)
    private val minReportInterval = 500L // Minimum 500ms between reports

    // Lightweight ring buffer for high frequency metrics
    private val ringBufferSize = 100
    private val plcUpdateBuffer = CircularBuffer(ringBufferSize)
    private val uiUpdateBuffer = CircularBuffer(ringBufferSize)

    enum class MetricType {
        PLC_UPDATE_RATE,
        UI_RECOMPOSITION_RATE,
        WRITE_COMMAND_RATE,
        NETWORK_LATENCY,
        SUBSCRIPTION_COUNT,
        MEMORY_USAGE,
        CONNECTION_COUNT,
        ERROR_COUNT
    }

    /**
     * Lightweight circular buffer for high-frequency data
     */
    private class CircularBuffer(private val size: Int) {
        private val buffer = LongArray(size)
        private val head = AtomicInteger(0)
        private val count = AtomicInteger(0)

        fun add(value: Long) {
            val index = head.getAndIncrement() % size
            buffer[index] = value
            if (count.get() < size) count.incrementAndGet()
        }

        fun getRate(): Double {
            val currentCount = count.get()
            if (currentCount < 2) return 0.0

            val now = System.currentTimeMillis()
            val oldestIndex = if (currentCount < size) 0 else head.get() % size
            val oldestTime = buffer[oldestIndex]

            return if (now > oldestTime) {
                (currentCount - 1) * 1000.0 / (now - oldestTime)
            } else 0.0
        }

        fun clear() {
            head.set(0)
            count.set(0)
        }
    }

    /**
     * Thread-safe metric data container
     */
    data class MetricData(
        val count: AtomicInteger = AtomicInteger(0),
        val totalTime: AtomicLong = AtomicLong(0),
        val minTime: AtomicLong = AtomicLong(Long.MAX_VALUE),
        val maxTime: AtomicLong = AtomicLong(0),
        val lastResetTime: AtomicLong = AtomicLong(System.currentTimeMillis())
    ) {
        fun record(value: Long = 1) {
            if (value <= 0) return

            count.incrementAndGet()
            totalTime.addAndGet(value)
            updateMinMax(value)
        }

        private fun updateMinMax(value: Long) {
            // Update min
            var currentMin: Long
            do {
                currentMin = minTime.get()
            } while (value < currentMin && !minTime.compareAndSet(currentMin, value))

            // Update max
            var currentMax: Long
            do {
                currentMax = maxTime.get()
            } while (value > currentMax && !maxTime.compareAndSet(currentMax, value))
        }

        fun getAverage(): Double {
            val c = count.get()
            return if (c > 0) totalTime.get().toDouble() / c else 0.0
        }

        fun getRate(): Double {
            val elapsed = System.currentTimeMillis() - lastResetTime.get()
            return if (elapsed > 0) count.get() * 1000.0 / elapsed else 0.0
        }

        fun reset() {
            count.set(0)
            totalTime.set(0)
            minTime.set(Long.MAX_VALUE)
            maxTime.set(0)
            lastResetTime.set(System.currentTimeMillis())
        }
    }

    data class PerformanceReport(
        val plcUpdateRate: Double = 0.0,
        val uiRecompositionRate: Double = 0.0,
        val writeCommandRate: Double = 0.0,
        val avgNetworkLatency: Double = 0.0,
        val activeSubscriptions: Int = 0,
        val memoryUsageMB: Double = 0.0,
        val connectionCount: Int = 0,
        val errorCount: Int = 0,
        val timestamp: Long = System.currentTimeMillis()
    )

    companion object {
        private const val TAG = "PerformanceMonitor"
        private const val CLEANUP_INTERVAL_MS = 300000L // 5 minutes
        private const val MAX_METRIC_AGE_MS = 3600000L // 1 hour
        private const val MEMORY_CHECK_INTERVAL_MS = 10000L // 10 seconds
    }

    init {
        // Initialize all metric types
        MetricType.values().forEach { type ->
            metrics[type] = MetricData()
        }

        startBackgroundTasks()
    }

    private fun startBackgroundTasks() {
        // Periodic cleanup job
        cleanupJob = monitorScope.launch {
            while (isMonitorActive.get() && isActive) {
                try {
                    delay(CLEANUP_INTERVAL_MS)
                    if (isMonitorActive.get()) {
                        performCleanup()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Cleanup task error", e)
                }
            }
        }

        // Periodic report generation (only if enabled)
        reportGenerationJob = monitorScope.launch {
            while (isMonitorActive.get() && isActive) {
                try {
                    delay(1000L) // Generate report every second
                    if (isReportingEnabled.get() && isMonitorActive.get()) {
                        generateReportAsync()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Report generation error", e)
                }
            }
        }
    }

    // ==================== RECORDING METHODS ====================

    fun recordPlcUpdate() {
        if (!isMonitorActive.get()) return

        val now = System.currentTimeMillis()
        plcUpdateBuffer.add(now)
        metrics[MetricType.PLC_UPDATE_RATE]?.record()

        // Log excessive rate
        if (plcUpdateBuffer.getRate() > 20.0) {
            Log.w(TAG, "High PLC update rate: ${plcUpdateBuffer.getRate().format(1)}/s")
        }
    }

    fun recordUiRecomposition() {
        if (!isMonitorActive.get()) return

        val now = System.currentTimeMillis()
        uiUpdateBuffer.add(now)
        metrics[MetricType.UI_RECOMPOSITION_RATE]?.record()

        // Log excessive recomposition
        if (uiUpdateBuffer.getRate() > 10.0) {
            Log.w(TAG, "High UI recomposition rate: ${uiUpdateBuffer.getRate().format(1)}/s")
        }
    }

    fun recordWriteCommand() {
        if (!isMonitorActive.get()) return
        metrics[MetricType.WRITE_COMMAND_RATE]?.record()
    }

    fun recordNetworkLatency(latencyMs: Long) {
        if (!isMonitorActive.get() || latencyMs <= 0) return

        metrics[MetricType.NETWORK_LATENCY]?.record(latencyMs)

        if (latencyMs > 2000) {
            Log.w(TAG, "High network latency: ${latencyMs}ms")
        }
    }

    fun updateSubscriptionCount(count: Int) {
        if (!isMonitorActive.get()) return
        metrics[MetricType.SUBSCRIPTION_COUNT]?.record(count.toLong())
    }

    fun recordConnectionEvent() {
        if (!isMonitorActive.get()) return
        metrics[MetricType.CONNECTION_COUNT]?.record()
    }

    fun recordError() {
        if (!isMonitorActive.get()) return
        metrics[MetricType.ERROR_COUNT]?.record()
    }

    // ==================== MEMORY MONITORING ====================

    private fun recordMemoryUsage() {
        if (!isMonitorActive.get()) return

        try {
            val runtime = Runtime.getRuntime()
            val maxMemory = runtime.maxMemory()
            val totalMemory = runtime.totalMemory()
            val freeMemory = runtime.freeMemory()
            val usedMemory = totalMemory - freeMemory
            val usedMemoryMB = usedMemory / (1024 * 1024)

            metrics[MetricType.MEMORY_USAGE]?.record(usedMemoryMB)

            // Log memory pressure
            val memoryPressure = (usedMemory.toDouble() / maxMemory) * 100
            if (memoryPressure > 85.0) {
                Log.w(TAG, "High memory usage: ${memoryPressure.format(1)}% (${usedMemoryMB}MB)")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error recording memory usage", e)
        }
    }

    // ==================== REPORT GENERATION ====================

    private suspend fun generateReportAsync() = withContext(Dispatchers.Default) {
        val now = System.currentTimeMillis()

        // Rate limiting
        if (now - lastReportTime.get() < minReportInterval) {
            return@withContext
        }

        lastReportTime.set(now)

        // Record current memory
        recordMemoryUsage()

        val report = PerformanceReport(
            plcUpdateRate = plcUpdateBuffer.getRate(),
            uiRecompositionRate = uiUpdateBuffer.getRate(),
            writeCommandRate = metrics[MetricType.WRITE_COMMAND_RATE]?.getRate() ?: 0.0,
            avgNetworkLatency = metrics[MetricType.NETWORK_LATENCY]?.getAverage() ?: 0.0,
            activeSubscriptions = metrics[MetricType.SUBSCRIPTION_COUNT]?.count?.get() ?: 0,
            memoryUsageMB = metrics[MetricType.MEMORY_USAGE]?.getAverage() ?: 0.0,
            connectionCount = metrics[MetricType.CONNECTION_COUNT]?.count?.get() ?: 0,
            errorCount = metrics[MetricType.ERROR_COUNT]?.count?.get() ?: 0,
            timestamp = now
        )

        // Update state flow on Main thread to avoid ANR
        withContext(Dispatchers.Main.immediate) {
            _performanceReport.value = report
        }
    }

    fun generateReport(): PerformanceReport {
        return if (isMonitorActive.get()) {
            val current = _performanceReport.value

            // Only update if report is stale (avoid blocking)
            val now = System.currentTimeMillis()
            if (now - current.timestamp > minReportInterval) {
                // Trigger async update
                monitorScope.launch {
                    generateReportAsync()
                }
            }

            current
        } else {
            PerformanceReport()
        }
    }

    // ==================== CONTROL METHODS ====================

    fun pause() {
        Log.d(TAG, "Pausing performance monitoring")
        isMonitorActive.set(false)
    }

    fun resume() {
        Log.d(TAG, "Resuming performance monitoring")
        isMonitorActive.set(true)
    }

    fun enableReporting() {
        isReportingEnabled.set(true)
    }

    fun disableReporting() {
        isReportingEnabled.set(false)
    }

    fun reset() {
        if (!isMonitorActive.get()) return

        Log.d(TAG, "Resetting all metrics")

        monitorScope.launch {
            metrics.values.forEach { it.reset() }
            plcUpdateBuffer.clear()
            uiUpdateBuffer.clear()

            withContext(Dispatchers.Main.immediate) {
                _performanceReport.value = PerformanceReport()
            }
        }
    }

    // ==================== UTILITY METHODS ====================

    fun <T> measureTime(metricType: MetricType, block: () -> T): T {
        if (!isMonitorActive.get()) return block()

        val startTime = System.currentTimeMillis()
        return try {
            block()
        } finally {
            val elapsed = System.currentTimeMillis() - startTime
            metrics[metricType]?.record(elapsed)
        }
    }

    suspend fun <T> measureTimeAsync(metricType: MetricType, block: suspend () -> T): T {
        if (!isMonitorActive.get()) return block()

        val startTime = System.currentTimeMillis()
        return try {
            block()
        } finally {
            val elapsed = System.currentTimeMillis() - startTime
            metrics[metricType]?.record(elapsed)
        }
    }

    private fun performCleanup() {
        if (!isMonitorActive.get()) return

        val cutoffTime = System.currentTimeMillis() - MAX_METRIC_AGE_MS

        metrics.values.forEach { metric ->
            if (metric.lastResetTime.get() < cutoffTime) {
                metric.reset()
            }
        }

        Log.d(TAG, "Performed metric cleanup")
    }

    // ==================== LIFECYCLE ====================

    fun cleanup() {
        Log.d(TAG, "Cleaning up PerformanceMonitor")

        isMonitorActive.set(false)
        isReportingEnabled.set(false)

        // Cancel all jobs
        reportGenerationJob?.cancel()
        cleanupJob?.cancel()

        // Cancel scope
        monitorScope.cancel()

        // Clear data
        metrics.clear()
        plcUpdateBuffer.clear()
        uiUpdateBuffer.clear()

        // Reset flow
        _performanceReport.value = PerformanceReport()

        Log.d(TAG, "PerformanceMonitor cleanup completed")
    }

    fun isActive(): Boolean = isMonitorActive.get() && monitorScope.isActive
}

// Extension function for formatting
private fun Double.format(decimals: Int): String = "%.${decimals}f".format(this)