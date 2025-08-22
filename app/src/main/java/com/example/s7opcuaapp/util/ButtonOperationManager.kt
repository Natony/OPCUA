package com.example.s7opcuaapp.util

import android.util.Log
import com.example.s7opcuaapp.data.repository.S7Repository
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages button operations with thread-safe execution
 * Extracted from ControlViewModel to reduce complexity
 */
@Singleton
class ButtonOperationManager @Inject constructor() {

    companion object {
        private const val TAG = "ButtonOperationManager"
        private const val MIN_ACTION_INTERVAL_MS = 100L
        private const val OPERATION_TIMEOUT_MS = 5000L
    }

    // Operation types
    sealed class Operation {
        data class Press(val index: Int) : Operation()
        data class Release(val index: Int) : Operation()
        data class Toggle(val index: Int, val value: Boolean) : Operation()
        data class WriteInt(val index: Int, val value: Int) : Operation()
    }

    // Button state
    data class ButtonState(
        val index: Int,
        val isPressed: Boolean = false,
        val isBusy: Boolean = false,
        val lastActionTime: Long = 0L
    )

    // Operation result
    sealed class Result {
        object Success : Result()
        data class Error(val message: String) : Result()
        object Timeout : Result()
        object Locked : Result()
    }

    // State management
    private val buttonStates = ConcurrentHashMap<Int, ButtonState>()
    private val _busyButtons = MutableStateFlow<Set<Int>>(emptySet())
    val busyButtons: StateFlow<Set<Int>> = _busyButtons.asStateFlow()

    // Operation queue
    private val operationChannel = Channel<Operation>(Channel.UNLIMITED)
    private val operationScope = CoroutineScope(
        Dispatchers.Default + SupervisorJob() + CoroutineName("ButtonOperations")
    )

    // Thread safety
    private val operationMutex = Mutex()
    private val globalLock = Mutex()

    // Current operation tracking
    @Volatile
    private var currentOperation: Operation? = null

    init {
        // Start operation processor
        operationScope.launch {
            operationChannel.receiveAsFlow().collect { operation ->
                processOperation(operation)
            }
        }
    }

    /**
     * Queue a button operation
     */
    suspend fun queueOperation(operation: Operation) {
        operationChannel.send(operation)
    }

    /**
     * Process an operation
     */
    private suspend fun processOperation(operation: Operation) {
        // Try to acquire global lock for single operation at a time
        if (!globalLock.tryLock()) {
            Log.w(TAG, "Another operation in progress, queueing: $operation")
            return
        }

        try {
            currentOperation = operation

            // Check rate limiting
            if (!checkRateLimit(operation)) {
                Log.w(TAG, "Rate limit exceeded for operation: $operation")
                return
            }

            // Update busy state
            when (operation) {
                is Operation.Press -> updateBusyState(operation.index, true)
                is Operation.Toggle -> updateBusyState(operation.index, true)
                is Operation.WriteInt -> updateBusyState(operation.index + 200, true)
                else -> {}
            }

            // Process with timeout
            val result = withTimeoutOrNull(OPERATION_TIMEOUT_MS) {
                when (operation) {
                    is Operation.Press -> handlePress(operation.index)
                    is Operation.Release -> handleRelease(operation.index)
                    is Operation.Toggle -> handleToggle(operation.index, operation.value)
                    is Operation.WriteInt -> handleWriteInt(operation.index, operation.value)
                }
            } ?: Result.Timeout

            // Log result
            when (result) {
                is Result.Success -> Log.d(TAG, "✅ Operation successful: $operation")
                is Result.Error -> Log.e(TAG, "❌ Operation failed: $operation - ${result.message}")
                is Result.Timeout -> Log.w(TAG, "⏰ Operation timeout: $operation")
                is Result.Locked -> Log.d(TAG, "🔒 Operation locked: $operation")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Operation error", e)
        } finally {
            currentOperation = null

            // Clear busy state
            when (operation) {
                is Operation.Press -> {} // Keep pressed until release
                is Operation.Toggle -> updateBusyState(operation.index, false)
                is Operation.WriteInt -> updateBusyState(operation.index + 200, false)
                else -> {}
            }

            globalLock.unlock()
        }
    }

    /**
     * Check rate limiting
     */
    private fun checkRateLimit(operation: Operation): Boolean {
        val index = when (operation) {
            is Operation.Press -> operation.index
            is Operation.Release -> operation.index
            is Operation.Toggle -> operation.index
            is Operation.WriteInt -> operation.index + 200
        }

        val state = buttonStates[index]
        val now = System.currentTimeMillis()

        if (state != null && (now - state.lastActionTime) < MIN_ACTION_INTERVAL_MS) {
            return false
        }

        return true
    }

    /**
     * Update busy state
     */
    private fun updateBusyState(index: Int, isBusy: Boolean) {
        val currentState = buttonStates[index] ?: ButtonState(index)
        buttonStates[index] = currentState.copy(
            isBusy = isBusy,
            lastActionTime = System.currentTimeMillis()
        )

        _busyButtons.value = if (isBusy) {
            _busyButtons.value + index
        } else {
            _busyButtons.value - index
        }
    }

    /**
     * Handle press operation
     */
    private suspend fun handlePress(index: Int): Result {
        val state = buttonStates[index]

        if (state?.isPressed == true) {
            return Result.Error("Button already pressed")
        }

        // Release other buttons in same group
        val manualGroup = setOf(0, 1, 2, 3)
        if (index in manualGroup) {
            manualGroup.filter { it != index }.forEach { otherIndex ->
                buttonStates[otherIndex]?.let {
                    if (it.isPressed) {
                        handleRelease(otherIndex)
                    }
                }
            }
        }

        // Update state
        buttonStates[index] = ButtonState(
            index = index,
            isPressed = true,
            isBusy = true,
            lastActionTime = System.currentTimeMillis()
        )

        updateBusyState(index, true)

        return Result.Success
    }

    /**
     * Handle release operation
     */
    private suspend fun handleRelease(index: Int): Result {
        val state = buttonStates[index]

        if (state?.isPressed != true) {
            return Result.Error("Button not pressed")
        }

        // Update state
        buttonStates[index] = state.copy(
            isPressed = false,
            isBusy = false,
            lastActionTime = System.currentTimeMillis()
        )

        updateBusyState(index, false)

        return Result.Success
    }

    /**
     * Handle toggle operation
     */
    private suspend fun handleToggle(index: Int, value: Boolean): Result {
        buttonStates[index] = ButtonState(
            index = index,
            isPressed = value,
            isBusy = false,
            lastActionTime = System.currentTimeMillis()
        )

        return Result.Success
    }

    /**
     * Handle write int operation
     */
    private suspend fun handleWriteInt(index: Int, value: Int): Result {
        val buttonIndex = index + 200

        buttonStates[buttonIndex] = ButtonState(
            index = buttonIndex,
            isPressed = false,
            isBusy = false,
            lastActionTime = System.currentTimeMillis()
        )

        return Result.Success
    }

    /**
     * Execute operation with repository
     */
    suspend fun executeWithRepository(
        operation: Operation,
        repository: S7Repository
    ): Result = operationMutex.withLock {
        try {
            when (operation) {
                is Operation.Press -> {
                    repository.writeBoolean(operation.index, true)
                }
                is Operation.Release -> {
                    repository.writeBoolean(operation.index, false)
                }
                is Operation.Toggle -> {
                    repository.writeBoolean(operation.index, operation.value)
                }
                is Operation.WriteInt -> {
                    repository.writeInt(operation.index, operation.value)
                }
            }
            Result.Success
        } catch (e: Exception) {
            Log.e(TAG, "Repository operation failed", e)
            Result.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Release all pressed buttons
     */
    suspend fun releaseAllButtons() {
        Log.d(TAG, "Releasing all pressed buttons")

        val pressedButtons = buttonStates.values.filter { it.isPressed }

        coroutineScope {
            pressedButtons.map { state ->
                async {
                    handleRelease(state.index)
                }
            }.awaitAll()
        }

        buttonStates.clear()
        _busyButtons.value = emptySet()
    }

    /**
     * Check if button is locked
     */
    fun isButtonLocked(index: Int, lockedButtons: Set<Int>): Boolean {
        return index in lockedButtons
    }

    /**
     * Check if button is busy
     */
    fun isButtonBusy(index: Int): Boolean {
        return buttonStates[index]?.isBusy == true
    }

    /**
     * Check if button is pressed
     */
    fun isButtonPressed(index: Int): Boolean {
        return buttonStates[index]?.isPressed == true
    }

    /**
     * Get all pressed button indices
     */
    fun getPressedButtons(): Set<Int> {
        return buttonStates.values
            .filter { it.isPressed }
            .map { it.index }
            .toSet()
    }

    /**
     * Clear all states
     */
    fun clearAll() {
        buttonStates.clear()
        _busyButtons.value = emptySet()
        currentOperation = null
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up")

        operationScope.cancel()
        operationChannel.close()
        clearAll()
    }
}