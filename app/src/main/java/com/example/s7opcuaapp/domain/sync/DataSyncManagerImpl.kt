package com.example.s7opcuaapp.domain.sync

import android.util.Log
import com.example.s7opcuaapp.core.dispatchers.DispatcherProvider
import com.example.s7opcuaapp.data.model.PlcData
import com.example.s7opcuaapp.data.repository.S7Repository
import com.example.s7opcuaapp.domain.state.PlcStateManager
import com.example.s7opcuaapp.util.ButtonLockConfig
import com.example.s7opcuaapp.util.StatusLockConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of data synchronization between PLC and UI
 */
@Singleton
class DataSyncManagerImpl @Inject constructor(
    private val repository: S7Repository,
    private val stateManager: PlcStateManager,
    private val buttonLockConfig: ButtonLockConfig,
    private val statusLockConfig: StatusLockConfig,
    private val dispatchers: DispatcherProvider
) : DataSyncManager {

    override val plcData: StateFlow<PlcData> = stateManager.plcData

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)
    private var syncJob: Job? = null

    companion object {
        private const val TAG = "DataSyncManager"
        private const val UI_UPDATE_THROTTLE = 300L // ms
    }

    override suspend fun startDataSync() {
        if (syncJob?.isActive == true) {
            Log.d(TAG, "Data sync already active")
            return
        }

        Log.d(TAG, "Starting data sync")

        syncJob = scope.launch {
            repository.observePlcData()
                .flowOn(dispatchers.default)
                .distinctUntilChanged()
                .sample(UI_UPDATE_THROTTLE)
                .catch { error ->
                    Log.e(TAG, "Data sync error", error)
                    // Let connection manager handle reconnection
                }
                .collect { data ->
                    updatePlcData(data)
                }
        }
    }

    override fun stopDataSync() {
        Log.d(TAG, "Stopping data sync")
        syncJob?.cancel()
        syncJob = null
    }

    override fun isSyncing(): Boolean {
        return syncJob?.isActive == true
    }

    private suspend fun updatePlcData(data: PlcData) {
        withContext(dispatchers.default) {
            // Update state manager
            stateManager.updatePlcData(data)

            // Log significant changes
            logDataChanges(data)
        }
    }

    private fun logDataChanges(data: PlcData) {
        // Log only significant changes to avoid spam
        val status = data.ints.getOrNull(0) ?: 0
        if (status != stateManager.getCurrentStatus()) {
            Log.d(TAG, "Status changed to: $status")
        }
    }
}
