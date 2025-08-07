package com.example.s7opcuaapp.domain.usecase

import com.example.s7opcuaapp.data.model.PlcData
import com.example.s7opcuaapp.data.repository.S7Repository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for syncing PLC data
 */
@Singleton
class PlcDataSyncUseCase @Inject constructor(
    private val repository: S7Repository
) {
    companion object {
        private const val TAG = "PlcDataSyncUseCase"
    }

    /**
     * Observe PLC data changes
     */
    fun observePlcData(): Flow<PlcDataState> {
        return repository.observePlcData()
            .distinctUntilChanged()
            .map { data ->
                PlcDataState.Success(data)
            }
            .catch { error ->
                Log.e(TAG, "Error observing PLC data", error)
//                emit(PlcDataState.Error(error.message ?: "Unknown error"))
            }
            .flowOn(Dispatchers.Default)
    }

    /**
     * PLC data state
     */
    sealed class PlcDataState {
        data class Success(val data: PlcData) : PlcDataState()
        data class Error(val message: String) : PlcDataState()
        object Loading : PlcDataState()
    }
}

/**
 * Use case for connection management
 */
@Singleton
class ConnectionStateUseCase @Inject constructor(
    private val connectionManager: com.example.s7opcuaapp.data.opcua.OpcUaConnectionManager,
    private val prefsManager: com.example.s7opcuaapp.data.local.PrefsManager
) {

    /**
     * Start connection to current device
     */
    suspend fun connect(): Result<Unit> {
        val device = prefsManager.getCurrentDevice()
            ?: return Result.failure(Exception("No device configured"))

        return connectionManager.connect(device)
    }

    /**
     * Disconnect from PLC
     */
    suspend fun disconnect() {
        connectionManager.disconnect()
    }

    /**
     * Check if connected
     */
    fun isConnected(): Boolean {
        return connectionManager.isConnected.value
    }

    /**
     * Observe connection state
     */
    fun observeConnectionState() = connectionManager.isConnected
}