package com.example.s7opcuaapp.domain.connection

import com.example.s7opcuaapp.data.model.DeviceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages PLC connection lifecycle
 */
interface ConnectionManager {

    /**
     * Current connection state
     */
    val connectionState: StateFlow<ConnectionState>

    /**
     * Loading progress (0-100)
     */
    val loadingProgress: StateFlow<Int>

    /**
     * Connect to PLC device
     */
    suspend fun connect(device: DeviceEntity): Result<Unit>

    /**
     * Disconnect from PLC
     */
    suspend fun disconnect()

    /**
     * Check if currently connected
     */
    fun isConnected(): Boolean

    /**
     * Reset connection with retry
     */
    suspend fun resetConnection()

    /**
     * Continue in offline mode
     */
    fun setOfflineMode(enabled: Boolean)
}

/**
 * Connection states
 */
sealed class ConnectionState {
    object Idle : ConnectionState()
    data class Connecting(val attempt: Int = 1) : ConnectionState()
    object Connected : ConnectionState()
    data class Failed(val error: String, val attempt: Int = 0) : ConnectionState()
    object Timeout : ConnectionState()
    object Offline : ConnectionState()
    data class MaxRetriesExceeded(val reason: String) : ConnectionState()
}