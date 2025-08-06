package com.example.s7opcuaapp.domain.usecase

import android.util.Log
import com.example.s7opcuaapp.data.model.PlcData
import com.example.s7opcuaapp.data.opcua.OpcUaConnectionManager
import com.example.s7opcuaapp.data.repository.S7Repository
import com.example.s7opcuaapp.util.ButtonLockConfig
import com.example.s7opcuaapp.util.StatusLockConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for handling button press/release operations
 */
@Singleton
class ButtonControlUseCase @Inject constructor(
    private val repository: S7Repository,
    private val buttonLockConfig: ButtonLockConfig,
    private val statusLockConfig: StatusLockConfig
) {
    companion object {
        private const val TAG = "ButtonControlUseCase"
        private const val MIN_ACTION_INTERVAL = 100L
    }

    private val operationMutex = Mutex()
    private val lastActionTimes = mutableMapOf<Int, Long>()

    /**
     * Check if button can be pressed
     */
    fun canPressButton(
        buttonIndex: Int,
        currentStatus: Int,
        lockedButtons: Set<Int>,
        busyButtons: Set<Int>
    ): Boolean {
        // Check if locked
        if (buttonIndex in lockedButtons) {
            Log.d(TAG, "Button $buttonIndex is locked")
            return false
        }

        // Check if busy
        if (buttonIndex in busyButtons) {
            Log.d(TAG, "Button $buttonIndex is busy")
            return false
        }

        // Check minimum interval
        val now = System.currentTimeMillis()
        val lastAction = lastActionTimes[buttonIndex] ?: 0
        if (now - lastAction < MIN_ACTION_INTERVAL) {
            Log.d(TAG, "Button $buttonIndex action too fast")
            return false
        }

        // Check status lock
        if (statusLockConfig.isButtonLockedInStatus(buttonIndex, currentStatus)) {
            Log.d(TAG, "Button $buttonIndex locked by status $currentStatus")
            return false
        }

        return true
    }

    /**
     * Press button
     */
    suspend fun pressButton(buttonIndex: Int): Result<Unit> = operationMutex.withLock {
        try {
            Log.d(TAG, "Pressing button $buttonIndex")
            lastActionTimes[buttonIndex] = System.currentTimeMillis()
            repository.writeBoolean(buttonIndex, true)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to press button $buttonIndex", e)
            Result.failure(e)
        }
    }

    /**
     * Release button
     */
    suspend fun releaseButton(buttonIndex: Int): Result<Unit> = operationMutex.withLock {
        try {
            Log.d(TAG, "Releasing button $buttonIndex")
            lastActionTimes[buttonIndex] = System.currentTimeMillis()
            repository.writeBoolean(buttonIndex, false)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release button $buttonIndex", e)
            Result.failure(e)
        }
    }

    /**
     * Toggle boolean value
     */
    suspend fun toggleBoolean(index: Int, newValue: Boolean): Result<Unit> {
        return try {
            Log.d(TAG, "Toggling boolean $index to $newValue")
            repository.writeBoolean(index, newValue)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle boolean $index", e)
            Result.failure(e)
        }
    }
}

/**
 * Use case for handling integer write operations
 */
@Singleton
class IntegerWriteUseCase @Inject constructor(
    private val repository: S7Repository
) {
    companion object {
        private const val TAG = "IntegerWriteUseCase"
    }

    /**
     * Write integer value to PLC
     */
    suspend fun writeInteger(index: Int, value: Int): Result<Unit> {
        return try {
            Log.d(TAG, "Writing integer $index = $value")
            repository.writeInt(index, value)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write integer $index", e)
            Result.failure(e)
        }
    }

    /**
     * Write multiple integers (for send all)
     */
    suspend fun writeMultipleIntegers(values: Map<Int, Int>): Result<Unit> {
        return try {
            Log.d(TAG, "Writing ${values.size} integers")
            values.forEach { (index, value) ->
                repository.writeInt(index, value)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write multiple integers", e)
            Result.failure(e)
        }
    }
}

/**
 * Use case for calculating locked buttons based on current state
 */
@Singleton
class ButtonLockCalculationUseCase @Inject constructor(
    private val buttonLockConfig: ButtonLockConfig,
    private val statusLockConfig: StatusLockConfig
) {

    /**
     * Calculate which buttons should be locked
     */
    fun calculateLockedButtons(
        plcData: PlcData,
        currentStatus: Int,
        activeButtons: Set<Int>,
        busyButtons: Set<Int>,
        currentProcessingButton: Int?
    ): Set<Int> {
        // If processing, lock all except current
        if (currentProcessingButton != null) {
            val allButtons = (0..14).toSet() + (203..230).toSet()
            return allButtons - currentProcessingButton
        }

        // Get status-based locks
        val statusLocks = statusLockConfig.getLockedButtonsForStatus(currentStatus)

        // Get group-based locks
        val groupLocks = buttonLockConfig.getLockedButtons(activeButtons, busyButtons)

        // Combine all locks
        return statusLocks + groupLocks
    }

    /**
     * Get active buttons from PLC data
     */
    fun getActiveButtons(plcData: PlcData): Set<Int> {
        val active = mutableSetOf<Int>()

        // Check bool buttons
        plcData.bools.forEachIndexed { index, value ->
            if (value) active.add(index)
        }

        // Check int buttons (with offset)
        listOf(3, 4).forEach { index ->
            if ((plcData.ints.getOrNull(index) ?: 0) != 0) {
                active.add(index + 200)
            }
        }

        return active
    }
}

/**
 * Use case for connection management
 */
@Singleton
class ConnectionManagementUseCase @Inject constructor(
    private val connectionManager: OpcUaConnectionManager,
    private val repository: S7Repository
) {
    companion object {
        private const val TAG = "ConnectionManagementUseCase"
    }

    /**
     * Start connection to PLC
     */
    suspend fun connect(device: com.example.s7opcuaapp.data.model.DeviceEntity): Result<Unit> {
        return try {
            Log.d(TAG, "Connecting to device: ${device.name}")

            // Update repository device
            repository.updateDevice(device)

            // Connect
            connectionManager.connect(device)

        } catch (e: Exception) {
            Log.e(TAG, "Connection failed", e)
            Result.failure(e)
        }
    }

    /**
     * Disconnect from PLC
     */
    suspend fun disconnect(): Result<Unit> {
        return try {
            Log.d(TAG, "Disconnecting from PLC")
            connectionManager.disconnect()
            repository.stop()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Disconnect failed", e)
            Result.failure(e)
        }
    }

    /**
     * Check connection health
     */
    fun isConnected(): Boolean {
        return connectionManager.isConnected.value
    }
}