// app/src/main/java/com/example/s7opcuaapp/viewmodel/AlarmConfigViewModel.kt
package com.example.s7opcuaapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.s7opcuaapp.data.model.*
import com.example.s7opcuaapp.data.model.alarm.AlarmConfig
import com.example.s7opcuaapp.data.repository.AlarmRepository
import com.example.s7opcuaapp.util.AlarmDefaults
import com.example.s7opcuaapp.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlarmConfigUiState(
    val configs: List<AlarmConfig> = emptyList(),
    val showDialog: Boolean = false,
    val editingConfig: AlarmConfig? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class AlarmConfigViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlarmConfigUiState())
    val uiState: StateFlow<AlarmConfigUiState> = _uiState.asStateFlow()

    init {
        loadConfigs()
    }

    private fun loadConfigs() {
        viewModelScope.launch {
            alarmRepository.getAllConfigs().collect { configs ->
                _uiState.update { it.copy(configs = configs) }
            }
        }
    }

    fun showAddDialog() {
        _uiState.update {
            it.copy(
                showDialog = true,
                editingConfig = null
            )
        }
    }

    fun showEditDialog(config: AlarmConfig) {
        _uiState.update {
            it.copy(
                showDialog = true,
                editingConfig = config
            )
        }
    }

    fun hideDialog() {
        _uiState.update {
            it.copy(
                showDialog = false,
                editingConfig = null
            )
        }
    }

    fun addConfig(config: AlarmConfig) {
        viewModelScope.launch {
            try {
                val currentUser = sessionManager.getCurrentUser()
                val newConfig = config.copy(
                    createdBy = currentUser?.username ?: "system"
                )
                alarmRepository.insertConfig(newConfig)
                _uiState.update {
                    it.copy(
                        showDialog = false,
                        successMessage = "Alarm configuration added successfully"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to add configuration: ${e.message}")
                }
            }
        }
    }

    fun updateConfig(config: AlarmConfig) {
        viewModelScope.launch {
            try {
                val currentUser = sessionManager.getCurrentUser()
                val updatedConfig = config.copy(
                    modifiedBy = currentUser?.username ?: "system",
                    modifiedAt = System.currentTimeMillis()
                )
                alarmRepository.updateConfig(updatedConfig)
                _uiState.update {
                    it.copy(
                        showDialog = false,
                        successMessage = "Alarm configuration updated successfully"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to update configuration: ${e.message}")
                }
            }
        }
    }

    fun deleteConfig(config: AlarmConfig) {
        viewModelScope.launch {
            try {
                alarmRepository.deleteConfig(config)
                _uiState.update {
                    it.copy(successMessage = "Alarm configuration deleted successfully")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to delete configuration: ${e.message}")
                }
            }
        }
    }

    fun toggleEnabled(config: AlarmConfig) {
        viewModelScope.launch {
            try {
                val updatedConfig = config.copy(
                    enabled = !config.enabled,
                    modifiedBy = sessionManager.getCurrentUser()?.username ?: "system",
                    modifiedAt = System.currentTimeMillis()
                )
                alarmRepository.updateConfig(updatedConfig)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Failed to toggle configuration: ${e.message}")
                }
            }
        }
    }

    /**
     * Reset all configs to default
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                // Delete all existing configs
                val existingConfigs = alarmRepository.getAllConfigs().first()
                existingConfigs.forEach { config ->
                    alarmRepository.deleteConfig(config)
                }

                // Insert default configs
                AlarmDefaults.DEFAULT_ALARM_CONFIGS.forEach { config ->
                    alarmRepository.insertConfig(config)
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Reset to default configurations successfully"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to reset: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Import predefined config set
     */
    fun importConfigSet(configSet: List<AlarmConfig>) {
        viewModelScope.launch {
            try {
                configSet.forEach { config ->
                    alarmRepository.insertConfig(config)
                }

                _uiState.update {
                    it.copy(successMessage = "Imported ${configSet.size} configurations")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Import failed: ${e.message}")
                }
            }
        }
    }
}