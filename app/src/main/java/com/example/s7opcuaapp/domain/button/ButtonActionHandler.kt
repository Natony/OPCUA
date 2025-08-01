package com.example.s7opcuaapp.domain.button

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Handles button press/release actions with proper synchronization
 */
interface ButtonActionHandler {

    /**
     * Currently pressed buttons
     */
    val pressedButtons: StateFlow<Set<Int>>

    /**
     * Buttons that are busy (processing)
     */
    val busyButtons: StateFlow<Set<Int>>

    /**
     * Press a button
     */
    suspend fun pressButton(index: Int): Result<Unit>

    /**
     * Release a button
     */
    suspend fun releaseButton(index: Int): Result<Unit>

    /**
     * Toggle a boolean button
     */
    suspend fun toggleBoolean(index: Int, value: Boolean): Result<Unit>

    /**
     * Write an integer value
     */
    suspend fun writeInteger(index: Int, value: Int): Result<Unit>

    /**
     * Release all pressed buttons
     */
    suspend fun releaseAllButtons()
}