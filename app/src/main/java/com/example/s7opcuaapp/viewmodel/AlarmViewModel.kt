// app/src/main/java/com/example/s7opcuaapp/viewmodel/AlarmViewModel.kt
package com.example.s7opcuaapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.s7opcuaapp.data.model.*
import com.example.s7opcuaapp.data.repository.AlarmRepository
import com.example.s7opcuaapp.util.AlarmManager
import com.example.s7opcuaapp.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlarmUiState(
    val activeAlarms: List<Alarm> = emptyList(),
    val statistics: AlarmStatistics = AlarmStatistics(),
    val soundMuted: Boolean = false,
    val showShelveDialog: Boolean = false,
    val selectedAlarm: Alarm? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val alarmManager: AlarmManager,
    private val sessionManager: SessionManager,
    private val alarmRepository: AlarmRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlarmUiState())
    val uiState: StateFlow<AlarmUiState> = _uiState.asStateFlow()

    val currentUser = sessionManager.currentUser

    init {
        observeAlarms()
        loadStatistics()
    }

    private fun observeAlarms() {
        viewModelScope.launch {
            combine(
                alarmRepository.getActiveAlarms(),
                alarmManager.alarmState
            ) { alarmList: List<Alarm>, managerState: AlarmManager.AlarmSystemState ->
                AlarmUiState(
                    activeAlarms = alarmList,
                    soundMuted = !managerState.soundEnabled,
                    statistics = managerState.statistics
                )
            }.collect { state ->
                _uiState.update { currentState ->
                    currentState.copy(
                        activeAlarms = state.activeAlarms,
                        soundMuted = state.soundMuted,
                        statistics = state.statistics
                    )
                }
            }
        }
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            alarmRepository.getAlarmStatistics().collect { stats ->
                _uiState.update { it.copy(statistics = stats) }
            }
        }
    }

    fun acknowledgeAlarm(alarmId: String) {
        viewModelScope.launch {
            currentUser.value?.let { user ->
                alarmManager.acknowledgeAlarm(alarmId, user.username)
            }
        }
    }

    fun acknowledgeAll() {
        viewModelScope.launch {
            currentUser.value?.let { user ->
                alarmManager.acknowledgeAllAlarms(user.username)
            }
        }
    }

    fun shelveAlarm(alarmId: String, minutes: Int) {
        viewModelScope.launch {
            alarmManager.shelveAlarm(alarmId, minutes)
            _uiState.update { it.copy(showShelveDialog = false, selectedAlarm = null) }
        }
    }

    fun showShelveDialog(alarm: Alarm) {
        _uiState.update { it.copy(showShelveDialog = true, selectedAlarm = alarm) }
    }

    fun hideShelveDialog() {
        _uiState.update { it.copy(showShelveDialog = false, selectedAlarm = null) }
    }

    fun toggleMute() {
        if (_uiState.value.soundMuted) {
            alarmManager.unmuteSound()
        } else {
            alarmManager.muteSound()
        }
    }
}