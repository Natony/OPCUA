package com.example.s7opcuaapp.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Base repository providing common functionality for all repositories
 */
abstract class BaseRepository {

    protected val TAG = this::class.simpleName ?: "BaseRepository"

    /**
     * Execute a suspend function safely with error handling
     */
    protected suspend fun <T> safeExecute(
        errorMessage: String = "Operation failed",
        block: suspend () -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (e: Exception) {
            Log.e(TAG, "$errorMessage: ${e.message}", e)
            Result.failure(Exception("$errorMessage: ${e.message}"))
        }
    }

    /**
     * Execute a suspend function that returns Result
     */
    protected suspend fun <T> safeCall(
        block: suspend () -> Result<T>
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            block()
        } catch (e: Exception) {
            Log.e(TAG, "Safe call failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Log debug message
     */
    protected fun logDebug(message: String) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, message)
        }
    }

    /**
     * Log info message
     */
    protected fun logInfo(message: String) {
        Log.i(TAG, message)
    }

    /**
     * Log warning message
     */
    protected fun logWarning(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w(TAG, message, throwable)
        } else {
            Log.w(TAG, message)
        }
    }

    /**
     * Log error message
     */
    protected fun logError(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(TAG, message, throwable)
        } else {
            Log.e(TAG, message)
        }
    }
}