package com.example.s7opcuaapp.domain.state

import com.example.s7opcuaapp.data.model.PlcData
import com.example.s7opcuaapp.domain.connection.ConnectionManager
import com.example.s7opcuaapp.domain.connection.ConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PLC state management
 */
@Singleton
class PlcStateManagerImpl @Inject constructor(
    private val connectionManager: ConnectionManager
) : PlcStateManager {

    private val _plcData = MutableStateFlow(PlcData.empty())
    override val plcData: StateFlow<PlcData> = _plcData.asStateFlow()

    private val updateMutex = Mutex()

    override fun isOfflineMode(): Boolean {
        return connectionManager.connectionState.value is ConnectionState.Offline
    }

    override fun getCurrentStatus(): Int {
        return _plcData.value.ints.getOrNull(0) ?: 0
    }

    override suspend fun updatePlcData(data: PlcData) {
        updateMutex.withLock {
            _plcData.value = data
        }
    }
}