package com.example.s7opcuaapp.domain.validation

import com.example.s7opcuaapp.domain.state.PlcStateManager
import com.example.s7opcuaapp.util.ButtonLockConfig
import com.example.s7opcuaapp.util.StatusLockConfig
import javax.inject.Inject

/**
 * Implementation of button validation with business rules
 */
class ButtonValidatorImpl @Inject constructor(
    private val stateManager: PlcStateManager,
    private val buttonLockConfig: ButtonLockConfig,
    private val statusLockConfig: StatusLockConfig
) : ButtonValidator {

    override suspend fun validateButtonPress(
        buttonIndex: Int,
        currentlyPressed: Set<Int>
    ): Result<Unit> {
        // Check if offline mode
        if (stateManager.isOfflineMode()) {
            return Result.failure(ValidationException("Cannot control in offline mode"))
        }

        // Check if button is locked by status
        val currentStatus = stateManager.getCurrentStatus()
        val lockedByStatus = statusLockConfig.getLockedButtonsForStatus(currentStatus)
        if (buttonIndex in lockedByStatus) {
            return Result.failure(ValidationException("Button locked by system status"))
        }

        // Check button group conflicts
        val lockedByGroup = buttonLockConfig.getLockedButtons(
            activeButtons = currentlyPressed,
            busyButtons = emptySet()
        )
        if (buttonIndex in lockedByGroup) {
            return Result.failure(ValidationException("Button locked by group conflict"))
        }

        // Check manual movement buttons (0-3)
        if (buttonIndex in 0..3) {
            val otherManualPressed = currentlyPressed.intersect(0..3)
            if (otherManualPressed.isNotEmpty()) {
                return Result.failure(
                    ValidationException("Another manual movement button is already pressed")
                )
            }
        }

        return Result.success(Unit)
    }

    override suspend fun validateButtonToggle(buttonIndex: Int): Result<Unit> {
        // Check if offline mode
        if (stateManager.isOfflineMode()) {
            return Result.failure(ValidationException("Cannot control in offline mode"))
        }

        // Check if button is locked by status
        val currentStatus = stateManager.getCurrentStatus()
        val lockedByStatus = statusLockConfig.getLockedButtonsForStatus(currentStatus)
        if (buttonIndex in lockedByStatus) {
            return Result.failure(ValidationException("Button locked by system status"))
        }

        return Result.success(Unit)
    }

    override suspend fun validateIntegerWrite(index: Int, value: Int): Result<Unit> {
        // Check if offline mode
        if (stateManager.isOfflineMode()) {
            return Result.failure(ValidationException("Cannot control in offline mode"))
        }

        // Validate value range based on index
        val validRange = when (index) {
            in 3..4 -> 0..999     // Pallet count
            in 5..13 -> -9999..9999  // Coordinates
            14 -> 0..10          // Function code
            else -> Int.MIN_VALUE..Int.MAX_VALUE
        }

        if (value !in validRange) {
            return Result.failure(
                ValidationException("Value $value out of range $validRange for index $index")
            )
        }

        return Result.success(Unit)
    }
}

/**
 * Exception for validation errors
 */
class ValidationException(message: String) : Exception(message)