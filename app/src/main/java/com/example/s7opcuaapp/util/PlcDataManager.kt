package com.example.s7opcuaapp.util

import android.util.Log
import com.example.s7opcuaapp.data.model.PlcData
import com.example.s7opcuaapp.data.repository.S7Repository
import com.example.s7opcuaapp.ui.screen.control.ControlUiState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

/**
 * Manages PLC data flow and UI state updates
 * Extracted from ControlViewModel to reduce complexity
 */
@Singleton
class PlcDataManager @Inject constructor(
    private val buttonLockConfig: ButtonLockConfig,
    private val statusLockConfig: StatusLockConfig,
    private val performanceMonitor: PerformanceMonitor
) {

    companion object {
        private const val TAG = "PlcDataManager"
        private const val UI_UPDATE_THROTTLE_MS = 300L
    }

    // UI State
    private val _uiState = MutableStateFlow(ControlUiState())
    val uiState: StateFlow<ControlUiState> = _uiState.asStateFlow()

    // Data observation
    private var dataObservationJob: Job? = null
    private val dataScope = CoroutineScope(
        Dispatchers.Default + SupervisorJob() + CoroutineName("PlcDataManager")
    )

    // Update tracking
    private var lastUiUpdateTime = 0L
    private var updateCount = 0
    private var lastLogTime = 0L

    /**
     * Start observing PLC data
     */
    fun startDataObservation(repository: S7Repository) {
        dataObservationJob?.cancel()

        Log.d(TAG, "Starting data observation")

        dataObservationJob = dataScope.launch {
            var consecutiveErrors = 0
            val maxErrors = 3

            repository.observePlcData()
                .flowOn(Dispatchers.Default)
                .distinctUntilChanged()
                .sample(UI_UPDATE_THROTTLE_MS.milliseconds)
                .catch { error ->
                    consecutiveErrors++
                    Log.e(TAG, "Data observation error ($consecutiveErrors/$maxErrors)", error)

                    if (consecutiveErrors >= maxErrors) {
                        updateError("Connection lost: ${error.message}")
                        throw error
                    }
                }
                .collect { data ->
                    consecutiveErrors = 0
                    updateWithPlcData(data)
                }
        }
    }

    /**
     * Stop data observation
     */
    fun stopDataObservation() {
        Log.d(TAG, "Stopping data observation")
        dataObservationJob?.cancel()
        dataObservationJob = null
    }

    /**
     * Update UI with PLC data
     */
    private fun updateWithPlcData(data: PlcData) {
        // Throttle UI updates
        val now = System.currentTimeMillis()
        if (now - lastUiUpdateTime < UI_UPDATE_THROTTLE_MS) {
            return
        }
        lastUiUpdateTime = now

        // Track update rate
        updateCount++
        if (now - lastLogTime >= 5000) {
            val rate = updateCount * 1000.0 / (now - lastLogTime)
            Log.d(TAG, "UI update rate: ${String.format("%.1f", rate)}/s")
            updateCount = 0
            lastLogTime = now

            // Record performance
            performanceMonitor.recordUiRecomposition()
        }

        // Calculate locked buttons based on status
        val currentStatus = data.ints.getOrNull(0) ?: 0
        val statusLockedButtons = statusLockConfig.getLockedButtonsForStatus(currentStatus)

        // Update UI state
        _uiState.update { currentState ->
            currentState.copy(
                plcData = data,
                lockedButtons = currentState.lockedButtons + statusLockedButtons,
                errorMessage = null
            )
        }
    }

    /**
     * Update loading percent
     */
    fun updateLoadingPercent(percent: Int) {
        _uiState.update { it.copy(loadingPercent = percent) }
    }

    /**
     * Update busy buttons
     */
    fun updateBusyButtons(busyButtons: Set<Int>) {
        _uiState.update { it.copy(busyButtons = busyButtons) }
    }

    /**
     * Update locked buttons
     */
    fun updateLockedButtons(
        activeButtons: Set<Int>,
        busyButtons: Set<Int>,
        currentStatus: Int
    ) {
        // Get locks from button config
        val buttonLocks = buttonLockConfig.getLockedButtons(activeButtons, busyButtons)

        // Get locks from status config
        val statusLocks = statusLockConfig.getLockedButtonsForStatus(currentStatus)

        // Combine all locks
        val allLocks = buttonLocks + statusLocks

        _uiState.update { it.copy(lockedButtons = allLocks) }
    }

    /**
     * Update error message
     */
    fun updateError(error: String?) {
        _uiState.update { it.copy(errorMessage = error) }
    }

    /**
     * Update writing state
     */
    fun updateWritingState(isWriting: Boolean) {
        _uiState.update { it.copy(isWriting = isWriting) }
    }

    /**
     * Update dialog state
     */
    fun showNumberDialog(title: String, index: Int) {
        _uiState.update {
            it.copy(
                openDialogForIndex = index,
                dialogTitle = title
            )
        }
    }

    /**
     * Dismiss dialog
     */
    fun dismissDialog() {
        _uiState.update {
            it.copy(
                openDialogForIndex = null,
                dialogTitle = ""
            )
        }
    }

    /**
     * Update selected function
     */
    fun updateSelectedFunction(code: Int) {
        _uiState.update { it.copy(selectedFunction = code) }
    }

    /**
     * Update input value
     */
    fun updateInputValue(index: Int, value: String) {
        _uiState.update { currentState ->
            currentState.copy(
                intInputs = currentState.intInputs.toMutableMap().apply {
                    put(index, value)
                }
            )
        }
    }

    /**
     * Get current input value
     */
    fun getInputValue(index: Int): String {
        val state = _uiState.value
        return state.intInputs[index]
            ?: state.plcData.ints.getOrNull(index)?.toString()
            ?: "0"
    }

    /**
     * Set offline mode
     */
    fun setOfflineMode(enabled: Boolean) {
        if (enabled) {
            // Lock all controls in offline mode
            val allButtons = (0..14).toSet() + (203..204).toSet() + setOf(999)

            _uiState.update {
                it.copy(
                    lockedButtons = allButtons,
                    busyButtons = emptySet(),
                    isWriting = false,
                    errorMessage = null,
                    loadingPercent = 100 // Show UI
                )
            }
        } else {
            // Clear offline locks
            _uiState.update {
                it.copy(
                    lockedButtons = emptySet(),
                    loadingPercent = 0
                )
            }
        }
    }

    /**
     * Set controls blocked by alarm
     */
    fun setControlsBlockedByAlarm(blocked: Boolean) {
        _uiState.update { it.copy(controlsBlockedByAlarm = blocked) }
    }

    /**
     * Reset UI state
     */
    fun reset() {
        Log.d(TAG, "Resetting UI state")

        _uiState.value = ControlUiState()
        updateCount = 0
        lastLogTime = 0L
        lastUiUpdateTime = 0L
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Get active buttons from PLC data
     */
    fun getActiveButtons(data: PlcData): Set<Int> {
        val active = mutableSetOf<Int>()

        // Check bool buttons
        data.bools.forEachIndexed { index, value ->
            if (value) active.add(index)
        }

        // Check int buttons (with offset)
        listOf(3, 4).forEach { index ->
            if ((data.ints.getOrNull(index) ?: 0) != 0) {
                active.add(index + 200)
            }
        }

        return active
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up")

        dataObservationJob?.cancel()
        dataScope.cancel()
        reset()
    }
}