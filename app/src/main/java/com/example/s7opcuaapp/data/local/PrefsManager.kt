package com.example.s7opcuaapp.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.s7opcuaapp.data.model.DeviceEntity
import com.example.s7opcuaapp.data.model.UserCredentials
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject

class PrefsManager @Inject constructor(context: Context) {

    private val gson = Gson()

    // Regular prefs for non-sensitive data
    private val regularPrefs: SharedPreferences = context.getSharedPreferences(
        "app_prefs",
        Context.MODE_PRIVATE
    )

    // Encrypted prefs for sensitive data
    private val securePrefs: SharedPreferences = createEncryptedPrefs(context)

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        // Non-sensitive keys - use regular prefs
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_STATUS_LOCK_CONFIG = "status_lock_config"
        private const val KEY_STATUS_LOCK_OVERRIDE = "status_lock_override"
        private const val KEY_CURRENT_DEVICE_ID = "current_device_id"

        // Sensitive keys - use encrypted prefs
        private const val KEY_SESSION_ID = "session_id"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_DEVICES_JSON = "device_list"
        private const val KEY_SAVED_USERNAME = "saved_username" // Only username, not password
    }

    // Session Management (Sensitive - use secure prefs)
    fun saveSession(sessionId: String, userId: String, username: String, role: String) {
        securePrefs.edit()
            .putString(KEY_SESSION_ID, sessionId)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .putString(KEY_USER_ROLE, role)
            .apply()
    }

    fun clearSession() {
        securePrefs.edit()
            .remove(KEY_SESSION_ID)
            .remove(KEY_USER_ID)
            .remove(KEY_USERNAME)
            .remove(KEY_USER_ROLE)
            .apply()
    }

    fun getSessionId(): String? = securePrefs.getString(KEY_SESSION_ID, null)
    fun getUserId(): String? = securePrefs.getString(KEY_USER_ID, null)
    fun getUsername(): String? = securePrefs.getString(KEY_USERNAME, null)
    fun getUserRole(): String? = securePrefs.getString(KEY_USER_ROLE, null)

    // Remember Me (Non-sensitive)
    fun setRememberMe(remember: Boolean) {
        regularPrefs.edit().putBoolean(KEY_REMEMBER_ME, remember).apply()
    }

    fun getRememberMe(): Boolean = regularPrefs.getBoolean(KEY_REMEMBER_ME, false)

    // Save only username for remember me, NEVER password
    fun saveRememberedUsername(username: String?) {
        if (username != null) {
            securePrefs.edit().putString(KEY_SAVED_USERNAME, username).apply()
        } else {
            securePrefs.edit().remove(KEY_SAVED_USERNAME).apply()
        }
    }

    fun getRememberedUsername(): String? {
        return if (getRememberMe()) {
            securePrefs.getString(KEY_SAVED_USERNAME, null)
        } else {
            null
        }
    }

    // Device Management (Sensitive - contains IP addresses)
    fun saveDeviceList(list: List<DeviceEntity>) {
        // Encrypt device data before saving
        val devicesWithoutPasswords = list.map { device ->
            device.copy(
                opcPassword = "" // Never save OPC passwords
            )
        }
        securePrefs.edit()
            .putString(KEY_DEVICES_JSON, gson.toJson(devicesWithoutPasswords))
            .apply()
    }

    fun getAllDevices(): List<DeviceEntity> {
        val json = securePrefs.getString(KEY_DEVICES_JSON, null) ?: return emptyList()
        val type = object : TypeToken<List<DeviceEntity>>() {}.type
        return gson.fromJson(json, type)
    }

    fun setCurrentDevice(device: DeviceEntity) {
        regularPrefs.edit()
            .putString(KEY_CURRENT_DEVICE_ID, device.id)
            .apply()
    }

    fun getCurrentDevice(): DeviceEntity? {
        val all = getAllDevices()
        val currentId = regularPrefs.getString(KEY_CURRENT_DEVICE_ID, null) ?: return null
        return all.find { it.id == currentId }
    }

    fun clearCurrentDevice() {
        regularPrefs.edit()
            .remove(KEY_CURRENT_DEVICE_ID)
            .apply()
    }

    // Status Lock Config (Non-sensitive)
    fun setStatusLockOverride(enabled: Boolean) {
        regularPrefs.edit()
            .putBoolean(KEY_STATUS_LOCK_OVERRIDE, enabled)
            .apply()
    }

    fun getStatusLockOverride(): Boolean {
        return regularPrefs.getBoolean(KEY_STATUS_LOCK_OVERRIDE, false)
    }

    fun saveStatusLockConfig(config: String) {
        regularPrefs.edit()
            .putString(KEY_STATUS_LOCK_CONFIG, config)
            .apply()
    }

    fun getStatusLockConfig(): String? {
        return regularPrefs.getString(KEY_STATUS_LOCK_CONFIG, null)
    }

    // Clear all data
    fun clearAll() {
        regularPrefs.edit().clear().apply()
        securePrefs.edit().clear().apply()
    }
}