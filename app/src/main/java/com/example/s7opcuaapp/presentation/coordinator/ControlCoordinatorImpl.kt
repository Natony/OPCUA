package com.example.s7opcuaapp.presentation.coordinator

import android.util.Log
import com.example.s7opcuaapp.core.dispatchers.DispatcherProvider
import com.example.s7opcuaapp.data.local.PrefsManager
import com.example.s7opcuaapp.domain.button.ButtonActionHandler
import com.example.s7opcuaapp.domain.connection.ConnectionManager
import com.example.s7opcuaapp.domain.connection.ConnectionState
import com.example.s7opcuaapp.domain.state.PlcStateManager
import com.example.s7opcuaapp.domain.sync.DataSyncManager
import com.example.s7opcuaapp.ui.screen.control.ControlUiState
import com.example.s7opcuaapp.util.ButtonLockConfig
import com.example.s7opcuaapp.util.StatusLockConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation that coordinates all control operations
 */
@Singleton
class ControlCoordinatorImpl @Inject constructor(
    private val connectionManager: ConnectionManager,
    private val buttonHandler: ButtonActionHandler,
    private val dataSyncManager: DataSyncManager,
    private val stateManager: PlcStateManager,
    private val buttonLockConfig: ButtonLockConfig,
    private val statusLockConfig: StatusLockConfig,
    private val prefsManager: PrefsManager,
    private val dispatchers: DispatcherProvider
) : ControlCoordinator {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.main)

    override val connectionState: StateFlow<ConnectionState> = connectionManager.connectionState

    override val uiState: StateFlow<ControlUiState> = combine(
        stateManager.plcData,
        connectionManager.loadingProgress,
        buttonHandler.pressedButtons,
        buttonHandler.busyButtons,
        connectionState
    ) { plcData, loadingProgress, pressedButtons, busyButtons, connState ->

        // Calculate locked buttons
        val statusLocks = if (connState is ConnectionState.Offline) {
            // Lock all buttons in offline mode
            (0..14).toSet() + (203..204).toSet() + setOf(999)
        } else {
            statusLockConfig.getLockedButtonsForStatus(plcData.ints.getOrNull(0) ?: 0)
        }

        val dynamicLocks = buttonLockConfig.getLockedButtons(
            activeButtons = pressedButtons,
            busyButtons = busyButtons
        )

        ControlUiState(
            plcData = plcData,
            isWriting = busyButtons.isNotEmpty(),
            loadingPercent = loadingProgress,
            lockedButtons = statusLocks + dynamicLocks,
            busyButtons = busyButtons,
            errorMessage = when (connState) {
                is ConnectionState.Failed -> connState.error
                is ConnectionState.Offline -> "Offline mode - controls disabled"
                else -> null
            }
        )
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ControlUiState()
    )

    companion object {
        private const val TAG = "ControlCoordinator"
    }

    init {
        // Monitor connection state and manage data sync
        scope.launch {
            connectionState.collect { state ->
                Log.d(TAG, "Connection state changed: $state")
                when (state) {
                    is ConnectionState.Connected -> {
                        Log.d(TAG, "Connected - starting data sync")
                        dataSyncManager.startDataSync()
                    }
                    else -> {
                        Log.d(TAG, "Not connected - stopping data sync")
                        dataSyncManager.stopDataSync()
                    }
                }
            }
        }

        scope.launch {
            stateManager.plcData.collect { data ->
                Log.d(TAG, "PLC data updated: bools=${data.bools.size}, ints=${data.ints.size}")
            }
        }
    }

    override suspend fun startConnection() {
        val device = prefsManager.getCurrentDevice()
        if (device == null) {
            Log.e(TAG, "No device configured")
            return
        }

        connectionManager.connect(device)
    }

    override suspend fun stopConnection() {
        buttonHandler.releaseAllButtons()
        dataSyncManager.stopDataSync()
        connectionManager.disconnect()
    }

    override suspend fun resetConnection() {
        connectionManager.resetConnection()
    }

    override fun continueOffline() {
        connectionManager.setOfflineMode(true)
    }

    override suspend fun toggleBoolean(index: Int, value: Boolean) {
        try {
            buttonHandler.toggleBoolean(index, value)
                .onFailure { error ->
                    Log.e(TAG, "Failed to toggle boolean $index", error)

                    // Nếu lỗi là mất kết nối, trigger reconnect
                    if (error.message?.contains("Not connected") == true) {
                        connectionManager.resetConnection()
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in toggleBoolean", e)
            // Prevent crash
        }
    }

    override suspend fun pressButton(index: Int) {
        buttonHandler.pressButton(index)
            .onFailure { error ->
                Log.e(TAG, "Failed to press button $index", error)
            }
    }

    override suspend fun releaseButton(index: Int) {
        buttonHandler.releaseButton(index)
            .onFailure { error ->
                Log.e(TAG, "Failed to release button $index", error)
            }
    }

    override suspend fun writeInteger(index: Int, value: Int) {
        buttonHandler.writeInteger(index, value)
            .onFailure { error ->
                Log.e(TAG, "Failed to write integer $index", error)
            }
    }

    override suspend fun executeFunction(functionCode: Int, parameters: Map<Int, Int>) {
        // Write function code
        buttonHandler.writeInteger(14, functionCode)
            .onFailure { error ->
                Log.e(TAG, "Failed to write function code", error)
                return
            }

        // Write parameters
        parameters.forEach { (index, value) ->
            buttonHandler.writeInteger(index, value)
                .onFailure { error ->
                    Log.e(TAG, "Failed to write parameter $index", error)
                }
        }
    }
}