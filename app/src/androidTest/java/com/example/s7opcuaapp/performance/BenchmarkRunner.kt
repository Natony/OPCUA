package com.example.s7opcuaapp.testing.performance

import android.os.Debug
import android.util.Log
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

class BenchmarkRunner {

    data class BenchmarkResult(
        val name: String,
        val executionTime: Long,
        val memoryUsed: Long,
        val iterations: Int
    )

    fun benchmark(
        name: String,
        iterations: Int = 100,
        warmup: Int = 10,
        block: () -> Unit
    ): BenchmarkResult {
        // Warmup
        repeat(warmup) { block() }

        // Force GC before measurement
        System.gc()
        Thread.sleep(100)

        val startMemory = Debug.getNativeHeapAllocatedSize()

        val time = measureTimeMillis {
            repeat(iterations) {
                block()
            }
        }

        val endMemory = Debug.getNativeHeapAllocatedSize()
        val memoryUsed = endMemory - startMemory

        return BenchmarkResult(name, time, memoryUsed, iterations).also {
            Log.d("Benchmark", "$name: ${time}ms for $iterations iterations, memory: ${memoryUsed / 1024}KB")
        }
    }

    fun benchmarkSuspend(
        name: String,
        iterations: Int = 100,
        warmup: Int = 10,
        block: suspend () -> Unit
    ): BenchmarkResult = runBlocking {
        benchmark(name, iterations, warmup) {
            runBlocking { block() }
        }
    }
}