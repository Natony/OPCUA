package com.example.s7opcuaapp.util

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Performance monitoring utility to track app performance metrics
 */
@Singleton
class PerformanceMonitor @Inject constructor() {

    private val metrics = ConcurrentHashMap<MetricType, MetricData>()
    private val _performanceReport = MutableStateFlow(PerformanceReport())
    val performanceReport: StateFlow<PerformanceReport> = _performanceReport

    enum class MetricType {
        PLC_UPDATE_RATE,
        UI_RECOMPOSITION_RATE,
        WRITE_COMMAND_RATE,
        NETWORK_LATENCY,
        SUBSCRIPTION_COUNT,
        MEMORY_USAGE
    }

    data class MetricData(
        val count: AtomicInteger = AtomicInteger(0),
        val totalTime: AtomicLong = AtomicLong(0),
        val minTime: AtomicLong = AtomicLong(Long.MAX_VALUE),
        val maxTime: AtomicLong = AtomicLong(0),
        val lastResetTime: AtomicLong = AtomicLong(System.currentTimeMillis())
    ) {
        fun record(value: Long = 1) {
            count.incrementAndGet()
            totalTime.addAndGet(value)

            // Update min/max
            var currentMin: Long
            do {
                currentMin = minTime.get()
            } while (value < currentMin && !minTime.compareAndSet(currentMin, value))

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
    }

    data class PerformanceReport(
        val plcUpdateRate: Double = 0.0,
        val uiRecompositionRate: Double = 0.0,
        val writeCommandRate: Double = 0.0,
        val avgNetworkLatency: Double = 0.0,
        val activeSubscriptions: Int = 0,
        val memoryUsageMB: Double = 0.0,
        val timestamp: Long = System.currentTimeMillis()
    )

    init {
        // Initialize all metric types
        MetricType.values().forEach { type ->
            metrics[type] = MetricData()
        }

        CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                delay(300000) // Every 5 minutes
                cleanupOldMetrics()
            }
        }
    }

    private fun cleanupOldMetrics() {
        val cutoffTime = System.currentTimeMillis() - 3600000 // 1 hour

        metrics.forEach { (type, data) ->
            if (data.lastResetTime.get() < cutoffTime) {
                // Reset old metrics
                data.count.set(0)
                data.totalTime.set(0)
                data.minTime.set(Long.MAX_VALUE)
                data.maxTime.set(0)
                data.lastResetTime.set(System.currentTimeMillis())

                Log.d("PerformanceMonitor", "Reset old metrics for $type")
            }
        }
    }

    fun recordPlcUpdate() {
        metrics[MetricType.PLC_UPDATE_RATE]?.record()
        logIfExcessive(MetricType.PLC_UPDATE_RATE, 10.0, "PLC updates too frequent")
    }

    fun recordUiRecomposition() {
        metrics[MetricType.UI_RECOMPOSITION_RATE]?.record()
        logIfExcessive(MetricType.UI_RECOMPOSITION_RATE, 5.0, "UI recomposing too often")
    }

    fun recordWriteCommand() {
        metrics[MetricType.WRITE_COMMAND_RATE]?.record()
    }

    fun recordNetworkLatency(latencyMs: Long) {
        metrics[MetricType.NETWORK_LATENCY]?.record(latencyMs)
        if (latencyMs > 1000) {
            Log.w("PerformanceMonitor", "High network latency: ${latencyMs}ms")
        }
    }

    fun updateSubscriptionCount(count: Int) {
        metrics[MetricType.SUBSCRIPTION_COUNT]?.record(count.toLong())
    }

    fun recordMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024 // MB
        metrics[MetricType.MEMORY_USAGE]?.record(usedMemory)
    }

    private fun logIfExcessive(type: MetricType, threshold: Double, message: String) {
        val rate = metrics[type]?.getRate() ?: 0.0
        if (rate > threshold) {
            Log.w("PerformanceMonitor", "$message: ${rate.format(2)}/s (threshold: $threshold/s)")
        }
    }

    fun generateReport(): PerformanceReport {
        recordMemoryUsage()

        val report = PerformanceReport(
            plcUpdateRate = metrics[MetricType.PLC_UPDATE_RATE]?.getRate() ?: 0.0,
            uiRecompositionRate = metrics[MetricType.UI_RECOMPOSITION_RATE]?.getRate() ?: 0.0,
            writeCommandRate = metrics[MetricType.WRITE_COMMAND_RATE]?.getRate() ?: 0.0,
            avgNetworkLatency = metrics[MetricType.NETWORK_LATENCY]?.getAverage() ?: 0.0,
            activeSubscriptions = metrics[MetricType.SUBSCRIPTION_COUNT]?.count?.get() ?: 0,
            memoryUsageMB = metrics[MetricType.MEMORY_USAGE]?.getAverage() ?: 0.0
        )

        _performanceReport.value = report
        logReport(report)

        return report
    }

    fun reset() {
        metrics.forEach { (_, data) ->
            data.count.set(0)
            data.totalTime.set(0)
            data.minTime.set(Long.MAX_VALUE)
            data.maxTime.set(0)
            data.lastResetTime.set(System.currentTimeMillis())
        }
    }

    private fun logReport(report: PerformanceReport) {
        Log.d("PerformanceMonitor", """
            === Performance Report ===
            PLC Update Rate: ${report.plcUpdateRate.format(2)}/s
            UI Recomposition Rate: ${report.uiRecompositionRate.format(2)}/s
            Write Command Rate: ${report.writeCommandRate.format(2)}/s
            Avg Network Latency: ${report.avgNetworkLatency.format(2)}ms
            Active Subscriptions: ${report.activeSubscriptions}
            Memory Usage: ${report.memoryUsageMB.format(2)}MB
            =========================
        """.trimIndent())
    }

    fun <T> measureTime(metricType: MetricType, block: () -> T): T {
        val startTime = System.currentTimeMillis()
        return try {
            block()
        } finally {
            val elapsed = System.currentTimeMillis() - startTime
            metrics[metricType]?.record(elapsed)
        }
    }
}

private fun Double.format(decimals: Int): String = "%.${decimals}f".format(this)