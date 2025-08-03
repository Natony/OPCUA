package com.example.s7opcuaapp.testing.performance

import android.util.Log
import kotlinx.coroutines.runBlocking
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

/**
 * Simple benchmark runner for performance testing
 */
class BenchmarkRunner(
    private val name: String,
    private val warmupIterations: Int = 5,
    private val measurementIterations: Int = 10
) {

    data class BenchmarkResult(
        val name: String,
        val averageTimeMs: Double,
        val minTimeMs: Long,
        val maxTimeMs: Long,
        val standardDeviation: Double,
        val iterations: Int
    )

    /**
     * Run benchmark for suspend functions
     */
    suspend fun <T> runSuspend(block: suspend () -> T): BenchmarkResult {
        // Warmup
        repeat(warmupIterations) {
            block()
        }

        // Measurement
        val times = mutableListOf<Long>()
        repeat(measurementIterations) {
            val time = measureTimeMillis {
                block()
            }
            times.add(time)
        }

        return calculateResult(times)
    }

    /**
     * Run benchmark for regular functions
     */
    fun <T> run(block: () -> T): BenchmarkResult {
        return runBlocking {
            runSuspend { block() }
        }
    }

    /**
     * Run benchmark with custom time measurement (nanoseconds)
     */
    fun <T> runNano(block: () -> T): BenchmarkResult {
        // Warmup
        repeat(warmupIterations) {
            block()
        }

        // Measurement
        val times = mutableListOf<Long>()
        repeat(measurementIterations) {
            val time = measureNanoTime {
                block()
            }
            times.add(time / 1_000_000) // Convert to milliseconds
        }

        return calculateResult(times)
    }

    private fun calculateResult(times: List<Long>): BenchmarkResult {
        val average = times.average()
        val min = times.minOrNull() ?: 0
        val max = times.maxOrNull() ?: 0

        // Calculate standard deviation
        val variance = times.map { time ->
            val diff = time - average
            diff * diff
        }.average()
        val stdDev = kotlin.math.sqrt(variance)

        val result = BenchmarkResult(
            name = name,
            averageTimeMs = average,
            minTimeMs = min,
            maxTimeMs = max,
            standardDeviation = stdDev,
            iterations = measurementIterations
        )

        logResult(result)
        return result
    }

    private fun logResult(result: BenchmarkResult) {
        Log.d("Benchmark", """
            ╔══════════════════════════════════════════════════════╗
            ║ Benchmark: ${result.name}
            ║ Average: ${String.format("%.2f", result.averageTimeMs)} ms
            ║ Min: ${result.minTimeMs} ms
            ║ Max: ${result.maxTimeMs} ms
            ║ Std Dev: ${String.format("%.2f", result.standardDeviation)} ms
            ║ Iterations: ${result.iterations}
            ╚══════════════════════════════════════════════════════╝
        """.trimIndent())
    }
}

/**
 * DSL for creating benchmarks
 */
fun benchmark(
    name: String,
    warmup: Int = 5,
    iterations: Int = 10,
    block: BenchmarkRunner.() -> Unit
) {
    val runner = BenchmarkRunner(name, warmup, iterations)
    runner.block()
}

/**
 * Compare multiple benchmark results
 */
class BenchmarkComparator {
    private val results = mutableListOf<BenchmarkRunner.BenchmarkResult>()

    fun add(result: BenchmarkRunner.BenchmarkResult) {
        results.add(result)
    }

    fun compare() {
        if (results.isEmpty()) return

        val baseline = results.first()

        Log.d("BenchmarkCompare", """
            ╔══════════════════════════════════════════════════════╗
            ║ Benchmark Comparison (Baseline: ${baseline.name})
            ╠══════════════════════════════════════════════════════╣
        """.trimIndent())

        results.forEach { result ->
            val speedup = baseline.averageTimeMs / result.averageTimeMs
            val comparison = when {
                speedup > 1.1 -> "↑ ${String.format("%.1fx faster", speedup)}"
                speedup < 0.9 -> "↓ ${String.format("%.1fx slower", 1/speedup)}"
                else -> "≈ Similar"
            }

            Log.d("BenchmarkCompare", """
            ║ ${result.name}: ${String.format("%.2f", result.averageTimeMs)} ms $comparison
            """.trimIndent())
        }

        Log.d("BenchmarkCompare", """
            ╚══════════════════════════════════════════════════════╝
        """.trimIndent())
    }
}