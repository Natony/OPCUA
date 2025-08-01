package com.example.s7opcuaapp.domain.state

import com.example.s7opcuaapp.data.model.PlcData
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages PLC state and provides current status information
 */
interface PlcStateManager {

    /**
     * Current PLC data
     */
    val plcData: StateFlow<PlcData>

    /**
     * Check if in offline mode
     */
    fun isOfflineMode(): Boolean

    /**
     * Get current PLC status value
     */
    fun getCurrentStatus(): Int

    /**
     * Update PLC data
     */
    suspend fun updatePlcData(data: PlcData)
}