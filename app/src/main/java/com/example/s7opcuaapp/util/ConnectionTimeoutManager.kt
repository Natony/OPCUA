package com.example.s7opcuaapp.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionTimeoutManager @Inject constructor() {

    companion object {
        const val CONNECTION_TIMEOUT = 30_000L // 30 seconds
        const val MAX_RETRY_ATTEMPTS = 3
    }

    private var timeoutJob: Job? = null
    private var retryCount = 0

    fun startTimeout(
        scope: CoroutineScope,
        onTimeout: () -> Unit
    ) {
        cancelTimeout()

        timeoutJob = scope.launch {
            delay(CONNECTION_TIMEOUT)
            if (isActive) {
                onTimeout()
            }
        }
    }

    fun cancelTimeout() {
        timeoutJob?.cancel()
        timeoutJob = null
    }

    fun shouldRetry(): Boolean {
        return retryCount < MAX_RETRY_ATTEMPTS
    }

    fun incrementRetry() {
        retryCount++
    }

    fun resetRetry() {
        retryCount = 0
    }
}