package com.example.s7opcuaapp.util

import com.example.s7opcuaapp.BuildConfig
import com.example.s7opcuaapp.data.local.PrefsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Feature flags for gradual rollout and A/B testing
 * Supports both compile-time and runtime flags
 */
@Singleton
class FeatureFlags @Inject constructor(
    private val prefsManager: PrefsManager
) {

    /**
     * Feature flag definition
     */
    data class Feature(
        val key: String,
        val defaultValue: Boolean,
        val description: String,
        val isRemoteConfigurable: Boolean = true
    )

    // Define all features
    object Features {
        val USE_NEW_CONNECTION_MANAGER = Feature(
            key = "use_new_connection_manager",
            defaultValue = false,
            description = "Use new optimized connection manager"
        )

        val USE_OPTIMIZED_STATE_MANAGEMENT = Feature(
            key = "use_optimized_state_management",
            defaultValue = true,
            description = "Use optimized state management with buffer"
        )

        val ENABLE_PERFORMANCE_OVERLAY = Feature(
            key = "enable_performance_overlay",
            defaultValue = BuildConfig.DEBUG,
            description = "Show performance overlay in debug builds"
        )

        val USE_SMART_RETRY_POLICY = Feature(
            key = "use_smart_retry_policy",
            defaultValue = false,
            description = "Use exponential backoff for connection retries"
        )

        val ENABLE_OFFLINE_MODE = Feature(
            key = "enable_offline_mode",
            defaultValue = true,
            description = "Allow offline mode when connection fails"
        )

        val USE_SUBSCRIPTION_GROUPS = Feature(
            key = "use_subscription_groups",
            defaultValue = true,
            description = "Group OPC UA subscriptions by priority"
        )

        val ENABLE_MEMORY_LEAK_DETECTION = Feature(
            key = "enable_memory_leak_detection",
            defaultValue = BuildConfig.DEBUG,
            description = "Auto-detect memory leaks in debug builds"
        )

        val USE_ENHANCED_LOGGING = Feature(
            key = "use_enhanced_logging",
            defaultValue = BuildConfig.DEBUG,
            description = "Enable detailed logging for debugging"
        )

        val ENABLE_CRASH_REPORTING = Feature(
            key = "enable_crash_reporting",
            defaultValue = !BuildConfig.DEBUG,
            description = "Send crash reports in production"
        )

        val USE_SECURE_STORAGE = Feature(
            key = "use_secure_storage",
            defaultValue = false,
            description = "Encrypt sensitive data in storage"
        )
    }

    // Runtime overrides storage
    private val overrides = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private val _flagStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val flagStates: StateFlow<Map<String, Boolean>> = _flagStates.asStateFlow()

    init {
        // Load saved overrides
        loadOverrides()
    }

    /**
     * Check if a feature is enabled
     */
    fun isEnabled(feature: Feature): Boolean {
        // Check runtime override first
        overrides.value[feature.key]?.let { return it }

        // Check remote config (mock implementation)
        getRemoteValue(feature.key)?.let { return it }

        // Return default value
        return feature.defaultValue
    }

    /**
     * Set runtime override for testing
     */
    fun setOverride(feature: Feature, enabled: Boolean) {
        val newOverrides = overrides.value.toMutableMap()
        newOverrides[feature.key] = enabled
        overrides.value = newOverrides
        saveOverrides()
        updateFlagStates()
    }

    /**
     * Clear override to use default/remote value
     */
    fun clearOverride(feature: Feature) {
        val newOverrides = overrides.value.toMutableMap()
        newOverrides.remove(feature.key)
        overrides.value = newOverrides
        saveOverrides()
        updateFlagStates()
    }

    /**
     * Clear all overrides
     */
    fun clearAllOverrides() {
        overrides.value = emptyMap()
        saveOverrides()
        updateFlagStates()
    }

    /**
     * Get all feature states
     */
    fun getAllFeatures(): Map<Feature, Boolean> {
        return mapOf(
            Features.USE_NEW_CONNECTION_MANAGER to isEnabled(Features.USE_NEW_CONNECTION_MANAGER),
            Features.USE_OPTIMIZED_STATE_MANAGEMENT to isEnabled(Features.USE_OPTIMIZED_STATE_MANAGEMENT),
            Features.ENABLE_PERFORMANCE_OVERLAY to isEnabled(Features.ENABLE_PERFORMANCE_OVERLAY),
            Features.USE_SMART_RETRY_POLICY to isEnabled(Features.USE_SMART_RETRY_POLICY),
            Features.ENABLE_OFFLINE_MODE to isEnabled(Features.ENABLE_OFFLINE_MODE),
            Features.USE_SUBSCRIPTION_GROUPS to isEnabled(Features.USE_SUBSCRIPTION_GROUPS),
            Features.ENABLE_MEMORY_LEAK_DETECTION to isEnabled(Features.ENABLE_MEMORY_LEAK_DETECTION),
            Features.USE_ENHANCED_LOGGING to isEnabled(Features.USE_ENHANCED_LOGGING),
            Features.ENABLE_CRASH_REPORTING to isEnabled(Features.ENABLE_CRASH_REPORTING),
            Features.USE_SECURE_STORAGE to isEnabled(Features.USE_SECURE_STORAGE)
        )
    }

    /**
     * Mock remote config lookup
     * In production, this would fetch from Firebase Remote Config or similar
     */
    private fun getRemoteValue(key: String): Boolean? {
        // TODO: Implement remote config integration
        return null
    }

    /**
     * Load saved overrides from preferences
     */
    private fun loadOverrides() {
        // TODO: Load from PrefsManager
        updateFlagStates()
    }

    /**
     * Save overrides to preferences
     */
    private fun saveOverrides() {
        // TODO: Save to PrefsManager
    }

    /**
     * Update observable flag states
     */
    private fun updateFlagStates() {
        _flagStates.value = getAllFeatures().mapKeys { it.key.key }
            .mapValues { it.value }
    }

    /**
     * Extension functions for easy access
     */
    val useNewConnectionManager: Boolean
        get() = isEnabled(Features.USE_NEW_CONNECTION_MANAGER)

    val useOptimizedStateManagement: Boolean
        get() = isEnabled(Features.USE_OPTIMIZED_STATE_MANAGEMENT)

    val enablePerformanceOverlay: Boolean
        get() = isEnabled(Features.ENABLE_PERFORMANCE_OVERLAY)

    val useSmartRetryPolicy: Boolean
        get() = isEnabled(Features.USE_SMART_RETRY_POLICY)

    val enableOfflineMode: Boolean
        get() = isEnabled(Features.ENABLE_OFFLINE_MODE)

    val useSubscriptionGroups: Boolean
        get() = isEnabled(Features.USE_SUBSCRIPTION_GROUPS)

    val enableMemoryLeakDetection: Boolean
        get() = isEnabled(Features.ENABLE_MEMORY_LEAK_DETECTION)

    val useEnhancedLogging: Boolean
        get() = isEnabled(Features.USE_ENHANCED_LOGGING)

    val enableCrashReporting: Boolean
        get() = isEnabled(Features.ENABLE_CRASH_REPORTING)

    val useSecureStorage: Boolean
        get() = isEnabled(Features.USE_SECURE_STORAGE)
}

/**
 * Feature flag aware logging
 */
fun FeatureFlags.log(tag: String, message: String) {
    if (useEnhancedLogging) {
        android.util.Log.d("FF_$tag", message)
    }
}

/**
 * Execute block only if feature is enabled
 */
inline fun FeatureFlags.whenEnabled(
    feature: FeatureFlags.Feature,
    block: () -> Unit
) {
    if (isEnabled(feature)) {
        block()
    }
}

/**
 * Return different values based on feature flag
 */
inline fun <T> FeatureFlags.ifEnabled(
    feature: FeatureFlags.Feature,
    enabledValue: () -> T,
    disabledValue: () -> T
): T {
    return if (isEnabled(feature)) {
        enabledValue()
    } else {
        disabledValue()
    }
}