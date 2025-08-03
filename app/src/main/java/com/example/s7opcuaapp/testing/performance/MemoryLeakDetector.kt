package com.example.s7opcuaapp.testing.performance

import android.os.Debug
import android.util.Log
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import kotlin.reflect.KClass

/**
 * Simple memory leak detector for testing
 * Tracks object lifecycle and detects potential leaks
 */
class MemoryLeakDetector {

    private val trackedObjects = mutableListOf<TrackedObject>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    data class TrackedObject(
        val name: String,
        val weakRef: WeakReference<Any>,
        val trackedAt: Long = System.currentTimeMillis(),
        val stackTrace: String
    )

    data class LeakReport(
        val suspectedLeaks: List<LeakInfo>,
        val clearedObjects: Int,
        val totalTracked: Int,
        val memoryInfo: MemoryInfo
    )

    data class LeakInfo(
        val name: String,
        val aliveForMs: Long,
        val stackTrace: String
    )

    data class MemoryInfo(
        val totalMemoryMB: Double,
        val freeMemoryMB: Double,
        val usedMemoryMB: Double,
        val maxMemoryMB: Double
    )

    /**
     * Track an object for potential memory leaks
     */
    fun track(obj: Any, name: String = obj::class.simpleName ?: "Unknown") {
        val stackTrace = Thread.currentThread().stackTrace
            .drop(3) // Skip getStackTrace and track methods
            .take(5)
            .joinToString("\n") { "  at ${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})" }

        trackedObjects.add(
            TrackedObject(
                name = name,
                weakRef = WeakReference(obj),
                stackTrace = stackTrace
            )
        )

        Log.d("MemoryLeakDetector", "Tracking object: $name")
    }

    /**
     * Track multiple objects
     */
    fun trackAll(vararg objects: Pair<Any, String>) {
        objects.forEach { (obj, name) ->
            track(obj, name)
        }
    }

    /**
     * Force garbage collection and check for leaks
     */
    suspend fun checkForLeaks(): LeakReport = withContext(Dispatchers.Default) {
        // Force garbage collection
        repeat(3) {
            System.gc()
            System.runFinalization()
            delay(100)
        }

        val now = System.currentTimeMillis()
        val suspectedLeaks = mutableListOf<LeakInfo>()
        var clearedCount = 0

        trackedObjects.forEach { tracked ->
            if (tracked.weakRef.get() != null) {
                val aliveTime = now - tracked.trackedAt
                if (aliveTime > 5000) { // Object alive for more than 5 seconds
                    suspectedLeaks.add(
                        LeakInfo(
                            name = tracked.name,
                            aliveForMs = aliveTime,
                            stackTrace = tracked.stackTrace
                        )
                    )
                }
            } else {
                clearedCount++
            }
        }

        // Clean up cleared references
        trackedObjects.removeAll { it.weakRef.get() == null }

        return@withContext LeakReport(
            suspectedLeaks = suspectedLeaks,
            clearedObjects = clearedCount,
            totalTracked = trackedObjects.size,
            memoryInfo = getMemoryInfo()
        )
    }

    /**
     * Get current memory information
     */
    fun getMemoryInfo(): MemoryInfo {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory() / 1024.0 / 1024.0
        val freeMemory = runtime.freeMemory() / 1024.0 / 1024.0
        val maxMemory = runtime.maxMemory() / 1024.0 / 1024.0

        return MemoryInfo(
            totalMemoryMB = totalMemory,
            freeMemoryMB = freeMemory,
            usedMemoryMB = totalMemory - freeMemory,
            maxMemoryMB = maxMemory
        )
    }

    /**
     * Log memory leak report
     */
    fun logReport(report: LeakReport) {
        Log.w("MemoryLeakDetector", """
            ╔══════════════════════════════════════════════════════╗
            ║ Memory Leak Detection Report
            ╠══════════════════════════════════════════════════════╣
            ║ Suspected Leaks: ${report.suspectedLeaks.size}
            ║ Cleared Objects: ${report.clearedObjects}
            ║ Still Tracked: ${report.totalTracked}
            ║ Memory Used: ${String.format("%.1f", report.memoryInfo.usedMemoryMB)} MB / ${String.format("%.1f", report.memoryInfo.maxMemoryMB)} MB
            ╠══════════════════════════════════════════════════════╣
        """.trimIndent())

        report.suspectedLeaks.forEach { leak ->
            Log.w("MemoryLeakDetector", """
            ║ LEAK: ${leak.name} (alive for ${leak.aliveForMs / 1000}s)
            ║ Stack trace:
            ${leak.stackTrace}
            ╠══════════════════════════════════════════════════════╣
            """.trimIndent())
        }

        Log.w("MemoryLeakDetector", """
            ╚══════════════════════════════════════════════════════╝
        """.trimIndent())
    }

    /**
     * Start automatic leak detection
     */
    fun startAutoDetection(intervalMs: Long = 30000) {
        scope.launch {
            while (isActive) {
                delay(intervalMs)
                val report = checkForLeaks()
                if (report.suspectedLeaks.isNotEmpty()) {
                    logReport(report)
                }
            }
        }
    }

    /**
     * Stop automatic detection and cleanup
     */
    fun stop() {
        scope.cancel()
        trackedObjects.clear()
    }

    /**
     * Clear all tracked objects
     */
    fun clear() {
        trackedObjects.clear()
    }
}

/**
 * DSL for memory leak detection
 */
inline fun <T> detectLeaks(
    detector: MemoryLeakDetector = MemoryLeakDetector(),
    block: MemoryLeakDetector.() -> T
): T {
    return detector.block()
}

/**
 * Extension to track any object
 */
fun Any.trackForLeaks(name: String = this::class.simpleName ?: "Unknown") {
    MemoryLeakDetector().track(this, name)
}