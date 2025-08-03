package com.example.s7opcuaapp.testing.performance

import android.os.Debug
import android.util.Log
import java.lang.ref.WeakReference
import kotlin.concurrent.thread

class MemoryLeakDetector {

    private val trackedObjects = mutableListOf<WeakReference<Any>>()

    fun track(obj: Any) {
        trackedObjects.add(WeakReference(obj))
    }

    fun checkLeaks(): List<LeakReport> {
        // Force GC
        System.gc()
        Thread.sleep(500)
        System.gc()

        val leaks = mutableListOf<LeakReport>()

        trackedObjects.forEach { ref ->
            ref.get()?.let { obj ->
                leaks.add(LeakReport(
                    className = obj::class.java.simpleName,
                    hashCode = obj.hashCode(),
                    retainedSize = estimateSize(obj)
                ))
            }
        }

        return leaks
    }

    private fun estimateSize(obj: Any): Long {
        // Simple estimation - in real app use LeakCanary or similar
        return 1024L // Placeholder
    }

    fun clear() {
        trackedObjects.clear()
    }

    data class LeakReport(
        val className: String,
        val hashCode: Int,
        val retainedSize: Long
    )

    companion object {
        fun detectActivityLeaks(block: () -> Unit) {
            val before = Debug.getNativeHeapAllocatedSize()

            block()

            // Wait for cleanup
            thread {
                Thread.sleep(1000)
                System.gc()
                Thread.sleep(500)

                val after = Debug.getNativeHeapAllocatedSize()
                val leaked = after - before

                if (leaked > 1024 * 1024) { // 1MB threshold
                    Log.w("MemoryLeak", "Potential leak detected: ${leaked / 1024}KB")
                }
            }
        }
    }
}