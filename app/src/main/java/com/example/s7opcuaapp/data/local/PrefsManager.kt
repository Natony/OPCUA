package com.example.s7opcuaapp.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.s7opcuaapp.data.model.DeviceEntity
import com.example.s7opcuaapp.data.model.UserCredentials
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrefsManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("s7opcua_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_CREDENTIALS = "key_credentials"
        private const val KEY_DEVICE_LIST = "key_device_list"
        private const val KEY_CURRENT_DEVICE = "key_current_device"
    }

    // Credentials (login)
    fun saveCredentials(credentials: UserCredentials) {
        prefs.edit().putString(KEY_CREDENTIALS, gson.toJson(credentials)).apply()
    }

    fun getCredentials(): UserCredentials? {
        val json = prefs.getString(KEY_CREDENTIALS, null) ?: return null
        return gson.fromJson(json, UserCredentials::class.java)
    }

    fun clearCredentials() {
        prefs.edit().remove(KEY_CREDENTIALS).apply()
    }

    // Device list
    fun saveDeviceList(list: List<DeviceEntity>) {
        val json = gson.toJson(list)
        prefs.edit().putString(KEY_DEVICE_LIST, json).apply()
    }

    fun getAllDevices(): List<DeviceEntity> {
        val json = prefs.getString(KEY_DEVICE_LIST, null) ?: return emptyList()
        val type = object : TypeToken<List<DeviceEntity>>() {}.type
        return gson.fromJson(json, type)
    }

    // Current device
    fun setCurrentDevice(device: DeviceEntity) {
        prefs.edit().putString(KEY_CURRENT_DEVICE, gson.toJson(device)).apply()
    }

    fun getCurrentDevice(): DeviceEntity? {
        val json = prefs.getString(KEY_CURRENT_DEVICE, null) ?: return null
        return gson.fromJson(json, DeviceEntity::class.java)
    }

    fun clearCurrentDevice() {
        prefs.edit().remove(KEY_CURRENT_DEVICE).apply()
    }
}
