package com.example.s7opcuaapp.util

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

@Singleton
class ConnectionManager @Inject constructor() {

    sealed class State {
        object Idle : State()
        data class Connecting(val attempt: Int) : State()
        object Connected : State()
        data class Failed(val reason: String, val canRetry: Boolean) : State()
        object Disconnected : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val isMonitoring = AtomicBoolean(false)
    private var monitorJob: Job? = null

    // Exponential backoff
    private var retryDelay = 1000L
    private val maxRetryDelay = 30000L

    // Trong ConnectionManager.kt
    fun startMonitoring(
        scope: CoroutineScope,
        checkConnection: suspend () -> Boolean,
        onConnectionLost: suspend () -> Unit
    ): Job {  // Thêm return type Job
        if (isMonitoring.getAndSet(true)) {
            return monitorJob ?: scope.launch { }  // Return existing or empty job
        }

        monitorJob?.cancel()
        monitorJob = scope.launch {
            var consecutiveFailures = 0

            while (isActive && isMonitoring.get()) {
                delay(5000) // Check every 5 seconds

                try {
                    val isConnected = checkConnection()

                    if (isConnected) {
                        consecutiveFailures = 0
                        retryDelay = 1000L // Reset delay

                        if (_state.value !is State.Connected) {
                            _state.value = State.Connected
                        }
                    } else {
                        consecutiveFailures++

                        if (consecutiveFailures >= 3) {
                            Log.w("ConnectionManager", "Connection lost after $consecutiveFailures failures")
                            _state.value = State.Failed("Connection lost", true)
                            onConnectionLost()
                            break
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ConnectionManager", "Monitor error", e)
                    consecutiveFailures++
                }
            }
        }

        return monitorJob!!  // Return the job
    }

    suspend fun reconnectWithBackoff(
        connect: suspend () -> Boolean
    ): Boolean {
        repeat(3) { attempt ->
            _state.value = State.Connecting(attempt + 1)

            if (connect()) {
                _state.value = State.Connected
                retryDelay = 1000L
                return true
            }

            // Exponential backoff
            delay(retryDelay)
            retryDelay = (retryDelay * 2).coerceAtMost(maxRetryDelay)
        }

        _state.value = State.Failed("Max retries exceeded", false)
        return false
    }

    fun stopMonitoring() {
        isMonitoring.set(false)
        monitorJob?.cancel()
        _state.value = State.Disconnected
    }
}