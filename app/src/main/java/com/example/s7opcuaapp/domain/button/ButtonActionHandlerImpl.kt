package com.example.s7opcuaapp.domain.button

import android.util.Log
import com.example.s7opcuaapp.core.dispatchers.DispatcherProvider
import com.example.s7opcuaapp.data.repository.S7Repository
import com.example.s7opcuaapp.domain.validation.ButtonValidator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thread-safe implementation of button action handling
 */
@Singleton
class ButtonActionHandlerImpl @Inject constructor(
    private val repository: S7Repository,
    private val validator: ButtonValidator,
    private val dispatchers: DispatcherProvider
) : ButtonActionHandler {

    private val _pressedButtons = MutableStateFlow<Set<Int>>(emptySet())
    override val pressedButtons: StateFlow<Set<Int>> = _pressedButtons.asStateFlow()

    private val _busyButtons = MutableStateFlow<Set<Int>>(emptySet())
    override val busyButtons: StateFlow<Set<Int>> = _busyButtons.asStateFlow()

    private val buttonMutex = Mutex()
    private val buttonJobs = ConcurrentHashMap<Int, Job>()

    // Create scope with SupervisorJob
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)

    companion object {
        private const val TAG = "ButtonActionHandler"
        private const val MIN_ACTION_INTERVAL = 100L
    }

    override suspend fun pressButton(index: Int): Result<Unit> = buttonMutex.withLock {
        if (_pressedButtons.value.contains(index)) {
            Log.w(TAG, "Button $index already pressed")
            return Result.success(Unit)
        }

        return try {
            // Validate action
            validator.validateButtonPress(index, _pressedButtons.value)
                .onFailure { return Result.failure(it) }

            // Mark as busy
            _busyButtons.value = _busyButtons.value + index

            // Perform press
            val job = scope.launch {
                repository.writeBoolean(index, true)
            }

            buttonJobs[index] = job
            job.join()

            // Update pressed state
            _pressedButtons.value = _pressedButtons.value + index

            Log.d(TAG, "Button $index pressed successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to press button $index", e)
            Result.failure(e)
        } finally {
            _busyButtons.value = _busyButtons.value - index
        }
    }

    override suspend fun releaseButton(index: Int): Result<Unit> = buttonMutex.withLock {
        if (!_pressedButtons.value.contains(index)) {
            Log.w(TAG, "Button $index not pressed")
            return Result.success(Unit)
        }

        return try {
            // Cancel any ongoing job
            buttonJobs[index]?.cancel()
            buttonJobs.remove(index)

            // Mark as busy
            _busyButtons.value = _busyButtons.value + index

            // Perform release
            withContext(dispatchers.io) {
                repository.writeBoolean(index, false)
            }

            // Update pressed state
            _pressedButtons.value = _pressedButtons.value - index

            Log.d(TAG, "Button $index released successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to release button $index", e)
            Result.failure(e)
        } finally {
            _busyButtons.value = _busyButtons.value - index
        }
    }

    override suspend fun toggleBoolean(index: Int, value: Boolean): Result<Unit> {
        return try {
            // Validate action
            validator.validateButtonToggle(index)
                .onFailure { return Result.failure(it) }

            // Mark as busy
            _busyButtons.value = _busyButtons.value + index

            // Perform toggle
            withContext(dispatchers.io) {
                repository.writeBoolean(index, value)
            }

            Log.d(TAG, "Boolean $index toggled to $value")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle boolean $index", e)
            Result.failure(e)
        } finally {
            _busyButtons.value = _busyButtons.value - index
        }
    }

    override suspend fun writeInteger(index: Int, value: Int): Result<Unit> {
        return try {
            // Validate action
            validator.validateIntegerWrite(index, value)
                .onFailure { return Result.failure(it) }

            // Mark as busy (use special offset for int buttons)
            val buttonIndex = index + 200
            _busyButtons.value = _busyButtons.value + buttonIndex

            // Perform write
            withContext(dispatchers.io) {
                repository.writeInt(index, value)
            }

            Log.d(TAG, "Integer $index written with value $value")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to write integer $index", e)
            Result.failure(e)
        } finally {
            val buttonIndex = index + 200
            _busyButtons.value = _busyButtons.value - buttonIndex
        }
    }

    override suspend fun releaseAllButtons() {
        Log.d(TAG, "Releasing all buttons")

        val pressed = _pressedButtons.value.toList()

        coroutineScope {
            pressed.map { index ->
                async {
                    releaseButton(index)
                }
            }.awaitAll()
        }

        // Clear all jobs
        buttonJobs.values.forEach { it.cancel() }
        buttonJobs.clear()

        _pressedButtons.value = emptySet()
        _busyButtons.value = emptySet()
    }
}