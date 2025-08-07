package com.example.s7opcuaapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.util.Log

/**
 * Base ViewModel with common functionality
 */
abstract class BaseViewModel<T : UiState> : ViewModel() {

    protected abstract val initialState: T

    private val _uiState: MutableStateFlow<T> by lazy {
        MutableStateFlow(initialState)
    }
    val uiState: StateFlow<T> = _uiState.asStateFlow()

    protected val currentState: T
        get() = _uiState.value

    /**
     * Update UI state safely
     */
    protected fun updateState(update: T.() -> T) {
        _uiState.update(update)
    }

    /**
     * Set loading state - to be overridden by subclasses if needed
     */
    protected open fun setLoading(isLoading: Boolean) {
        // Default implementation - subclasses can override
    }

    /**
     * Set error message - to be overridden by subclasses if needed
     */
    protected open fun setError(message: String?) {
        // Default implementation - subclasses can override
    }

    /**
     * Clear error message
     */
    protected fun clearError() {
        setError(null)
    }

    /**
     * Execute async operation with error handling, returns Job
     */
    protected fun execute(
        onError: ((Throwable) -> Unit)? = null,
        block: suspend () -> Unit
    ): Job {
        return viewModelScope.launch(
            CoroutineExceptionHandler { _, throwable ->
                Log.e(this@BaseViewModel::class.simpleName, "Error in execute", throwable)
                onError?.invoke(throwable) ?: setError(throwable.message)
            }
        ) {
            block()
        }
    }

    /**
     * Execute async operation with loading state, returns Job
     */
    protected fun executeWithLoading(
        onError: ((Throwable) -> Unit)? = null,
        block: suspend () -> Unit
    ): Job {
        return execute(
            onError = { error ->
                setLoading(false)
                onError?.invoke(error)
            }
        ) {
            setLoading(true)
            try {
                block()
            } finally {
                setLoading(false)
            }
        }
    }
}

/**
 * Marker interface for UI states
 */
interface UiState