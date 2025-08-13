// app/src/main/java/com/example/s7opcuaapp/util/AlarmManager.kt
package com.example.s7opcuaapp.util

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.example.s7opcuaapp.data.local.PrefsManager
import com.example.s7opcuaapp.data.model.*
import com.example.s7opcuaapp.data.repository.AlarmRepository
import com.example.s7opcuaapp.data.repository.S7Repository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmManager @Inject constructor(
    private val context: Context,
    private val repository: S7Repository,
    private val alarmRepository: AlarmRepository,
    private val prefsManager: PrefsManager
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Track active alarms
    private val activeAlarms = ConcurrentHashMap<Int, Alarm>()

    // Sound management
    private var mediaPlayer: MediaPlayer? = null
    private var isSoundMuted = false
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    // Alarm processing
    private val alarmNodeIndex = 46 // Index của node chứa alarm code trong PLC
    private var lastAlarmCode = 0
    private var alarmProcessingJob: Job? = null

    // State flows
    private val _currentAlarmCode = MutableStateFlow(0)
    val currentAlarmCode: StateFlow<Int> = _currentAlarmCode

    private val _alarmState = MutableStateFlow(AlarmSystemState())
    val alarmState: StateFlow<AlarmSystemState> = _alarmState

    data class AlarmSystemState(
        val hasActiveAlarms: Boolean = false,
        val hasCriticalAlarms: Boolean = false,
        val hasUnacknowledged: Boolean = false,
        val soundEnabled: Boolean = true,
        val controlsBlocked: Boolean = false,
        val statistics: AlarmStatistics = AlarmStatistics()
    )

    init {
        startAlarmMonitoring()
    }

    private fun startAlarmMonitoring() {
        alarmProcessingJob = scope.launch {
            // Monitor PLC alarm value
            repository.observePlcData()
                .map { it.ints.getOrNull(alarmNodeIndex) ?: 0 }
                .distinctUntilChanged()
                .collect { alarmCode ->
                    _currentAlarmCode.value = alarmCode
                    processAlarmCode(alarmCode)
                }
        }

        // Monitor active alarms
        scope.launch {
            alarmRepository.getActiveAlarms().collect { alarms ->
                updateAlarmState(alarms)
            }
        }

        // Monitor statistics
        scope.launch {
            alarmRepository.getAlarmStatistics().collect { stats ->
                _alarmState.update { it.copy(statistics = stats) }
            }
        }
    }

    private suspend fun processAlarmCode(code: Int) {
        try {
            when {
                code == 0 -> {
                    // No alarm - clear all active alarms
                    clearAllActiveAlarms()
                }
                code != lastAlarmCode -> {
                    // New alarm code
                    handleNewAlarm(code)
                }
            }
            lastAlarmCode = code
        } catch (e: Exception) {
            Log.e("AlarmManager", "Error processing alarm code $code", e)
        }
    }

    private suspend fun handleNewAlarm(code: Int) {
        // Check if alarm already active
        val existingAlarm = alarmRepository.getActiveAlarmByCode(code)
        if (existingAlarm != null) {
            Log.d("AlarmManager", "Alarm $code already active")
            return
        }

        // Get alarm configuration
        val config = alarmRepository.getConfigByCode(code) ?: createDefaultConfig(code)

        if (!config.enabled) {
            Log.d("AlarmManager", "Alarm $code is disabled")
            return
        }

        // Create new alarm
        val device = prefsManager.getCurrentDevice()
        val alarm = Alarm(
            alarmCode = code,
            priority = config.priority,
            category = config.category,
            message = config.message,
            description = config.description,
            state = AlarmState.ACTIVE,
            deviceId = device?.id ?: "unknown"
        )

        // Save to database
        alarmRepository.insertAlarm(alarm)
        activeAlarms[code] = alarm

        // Execute alarm actions
        executeAlarmActions(alarm, config)

        Log.i("AlarmManager", "New alarm activated: $code - ${config.message}")
    }

    private suspend fun executeAlarmActions(alarm: Alarm, config: AlarmConfig) {
        when (alarm.priority) {
            AlarmPriority.LOW -> {
                // Just notification
                showNotification(alarm)
            }
            AlarmPriority.MEDIUM -> {
                // Notification + sound
                showNotification(alarm)
                if (config.soundEnabled && !isSoundMuted) {
                    playAlarmSound(config.soundFile)
                }
            }
            AlarmPriority.HIGH -> {
                // Notification + sound + vibration
                showNotification(alarm)
                if (config.soundEnabled && !isSoundMuted) {
                    playAlarmSound(config.soundFile)
                    vibrateDevice()
                }
            }
            AlarmPriority.CRITICAL, AlarmPriority.EMERGENCY -> {
                // All actions + block controls
                showNotification(alarm)
                if (config.soundEnabled && !isSoundMuted) {
                    playAlarmSound(config.soundFile, loop = true)
                    vibrateDevice(pattern = longArrayOf(0, 500, 200, 500))
                }
                blockControls(true)
            }
        }
    }

    private fun showNotification(alarm: Alarm) {
        // Use Android notification system
        // Implementation depends on NotificationHelper
    }

    private fun playAlarmSound(soundFile: String = "default", loop: Boolean = false) {
        try {
            stopSound() // Stop any existing sound

            mediaPlayer = MediaPlayer.create(
                context,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            )

            mediaPlayer?.apply {
                isLooping = loop
                setAudioStreamType(AudioManager.STREAM_ALARM)
                start()
            }
        } catch (e: Exception) {
            Log.e("AlarmManager", "Error playing alarm sound", e)
        }
    }

    private fun stopSound() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }

    private fun vibrateDevice(pattern: LongArray = longArrayOf(0, 250, 100, 250)) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    suspend fun acknowledgeAlarm(alarmId: String, userId: String) {
        alarmRepository.acknowledgeAlarm(alarmId, userId)

        // Stop sound if all critical alarms acknowledged
        val hasUnackCritical = activeAlarms.values.any {
            it.priority >= AlarmPriority.HIGH && it.state == AlarmState.ACTIVE
        }
        if (!hasUnackCritical) {
            stopSound()
        }
    }

    suspend fun acknowledgeAllAlarms(userId: String) {
        activeAlarms.values.forEach { alarm ->
            if (alarm.state == AlarmState.ACTIVE || alarm.state == AlarmState.CLEARED) {
                alarmRepository.acknowledgeAlarm(alarm.id, userId)
            }
        }
        stopSound()
    }

    suspend fun shelveAlarm(alarmId: String, minutes: Int) {
        alarmRepository.shelveAlarm(alarmId, minutes)
    }

    fun muteSound() {
        isSoundMuted = true
        stopSound()
    }

    fun unmuteSound() {
        isSoundMuted = false
    }

    private suspend fun clearAllActiveAlarms() {
        activeAlarms.keys.forEach { code ->
            alarmRepository.clearAlarmByCode(code)
        }
        activeAlarms.clear()
        stopSound()
        blockControls(false)
    }

    private fun blockControls(block: Boolean) {
        _alarmState.update { it.copy(controlsBlocked = block) }
    }

    private suspend fun updateAlarmState(alarms: List<Alarm>) {
        val hasActive = alarms.any { it.state == AlarmState.ACTIVE }
        val hasCritical = alarms.any {
            it.state == AlarmState.ACTIVE && it.priority >= AlarmPriority.CRITICAL
        }
        val hasUnack = alarms.any {
            it.acknowledgedAt == null && it.state != AlarmState.NORMAL
        }

        _alarmState.update {
            it.copy(
                hasActiveAlarms = hasActive,
                hasCriticalAlarms = hasCritical,
                hasUnacknowledged = hasUnack,
                controlsBlocked = hasCritical
            )
        }
    }

    private fun createDefaultConfig(code: Int): AlarmConfig {
        return AlarmConfig(
            alarmCode = code,
            priority = AlarmPriority.MEDIUM,
            category = AlarmCategory.PROCESS,
            message = "Alarm Code $code",
            description = "Undefined alarm",
            createdBy = "system"
        )
    }

    fun cleanup() {
        alarmProcessingJob?.cancel()
        scope.cancel()
        stopSound()
    }
}