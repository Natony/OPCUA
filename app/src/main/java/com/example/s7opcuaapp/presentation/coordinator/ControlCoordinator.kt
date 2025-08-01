package com.example.s7opcuaapp.presentation.coordinator

import com.example.s7opcuaapp.domain.connection.ConnectionState
import com.example.s7opcuaapp.ui.screen.control.ControlUiState
import kotlinx.coroutines.flow.StateFlow

/**
 * Coordinates all control screen operations
 */
interface ControlCoordinator {

    /**
     * UI state
     */
    val uiState: StateFlow<ControlUiState>

    /**
     * Connection state
     */
    val connectionState: StateFlow<ConnectionState>

    /**
     * Start connection to PLC
     */
    suspend fun startConnection()

    /**
     * Stop connection
     */
    suspend fun stopConnection()

    /**
     * Reset connection
     */
    suspend fun resetConnection()

    /**
     * Continue in offline mode
     */
    fun continueOffline()

    /**
     * Toggle boolean value
     */
    suspend fun toggleBoolean(index: Int, value: Boolean)

    /**
     * Press button
     */
    suspend fun pressButton(index: Int)

    /**
     * Release button
     */
    suspend fun releaseButton(index: Int)

    /**
     * Write integer value
     */
    suspend fun writeInteger(index: Int, value: Int)

    /**
     * Execute function with parameters
     */
    suspend fun executeFunction(functionCode: Int, parameters: Map<Int, Int>)
}