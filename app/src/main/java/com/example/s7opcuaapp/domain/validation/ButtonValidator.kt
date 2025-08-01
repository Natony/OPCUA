package com.example.s7opcuaapp.domain.validation

/**
 * Validates button actions based on business rules
 */
interface ButtonValidator {

    /**
     * Validate button press action
     */
    suspend fun validateButtonPress(
        buttonIndex: Int,
        currentlyPressed: Set<Int>
    ): Result<Unit>

    /**
     * Validate button toggle action
     */
    suspend fun validateButtonToggle(buttonIndex: Int): Result<Unit>

    /**
     * Validate integer write action
     */
    suspend fun validateIntegerWrite(index: Int, value: Int): Result<Unit>
}