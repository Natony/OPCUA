package com.example.s7opcuaapp.domain.sync

import com.example.s7opcuaapp.data.model.PlcData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages synchronization of PLC data between repository and UI
 */
interface DataSyncManager {

    /**
     * Current PLC data
     */
    val plcData: StateFlow<PlcData>

    /**
     * Start observing PLC data changes
     */
    suspend fun startDataSync()

    /**
     * Stop observing PLC data
     */
    fun stopDataSync()

    /**
     * Check if currently syncing
     */
    fun isSyncing(): Boolean
}