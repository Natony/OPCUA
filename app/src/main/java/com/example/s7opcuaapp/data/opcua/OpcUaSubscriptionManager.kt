package com.example.s7opcuaapp.data.opcua

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages OPC UA subscriptions with lifecycle awareness
 * Prevents memory leaks by properly cleaning up subscriptions
 */
@Singleton
class OpcUaSubscriptionManager @Inject constructor() : LifecycleEventObserver {

    companion object {
        private const val TAG = "OpcUaSubscriptionManager"
    }

    data class SubscriptionInfo(
        val subscription: UaSubscription,
        val monitoredItems: MutableList<UaMonitoredItem> = mutableListOf(),
        val lifecycleOwner: LifecycleOwner? = null,
        val publishingInterval: Double = 250.0 // Store publishing interval
    )

    // Thread-safe storage
    private val subscriptions = ConcurrentHashMap<String, SubscriptionInfo>()
    private val subscriptionMutex = Mutex()

    // Cleanup job
    private var cleanupJob: Job? = null
    private val cleanupScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        startPeriodicCleanup()
    }

    /**
     * Create or get subscription with lifecycle
     */
    suspend fun createSubscription(
        subscriptionId: String,
        publishingInterval: Double = 250.0,
        lifecycleOwner: LifecycleOwner? = null
    ): UaSubscription? = subscriptionMutex.withLock {

        // Check existing
        subscriptions[subscriptionId]?.let { info ->
            Log.d(TAG, "Reusing existing subscription: $subscriptionId")
            return info.subscription
        }

        // Create new
        return try {
            val client = OPCUAClientManager.client ?: run {
                Log.e(TAG, "No OPC UA client available")
                return null
            }

            val subscription = client.subscriptionManager
                .createSubscription(publishingInterval)
                .get()

            val info = SubscriptionInfo(
                subscription = subscription,
                lifecycleOwner = lifecycleOwner,
                publishingInterval = publishingInterval
            )

            // Register lifecycle observer
            lifecycleOwner?.lifecycle?.addObserver(this)

            subscriptions[subscriptionId] = info

            Log.d(TAG, "✅ Created subscription: $subscriptionId")
            subscription

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create subscription", e)
            null
        }
    }

    /**
     * Add monitored item to subscription
     */
    suspend fun addMonitoredItem(
        subscriptionId: String,
        item: UaMonitoredItem
    ): Boolean = subscriptionMutex.withLock {

        subscriptions[subscriptionId]?.let { info ->
            info.monitoredItems.add(item)
            Log.d(TAG, "Added monitored item to $subscriptionId")
            return true
        }

        Log.w(TAG, "Subscription not found: $subscriptionId")
        return false
    }

    /**
     * Remove subscription
     */
    suspend fun removeSubscription(subscriptionId: String) = subscriptionMutex.withLock {
        subscriptions.remove(subscriptionId)?.let { info ->
            cleanupSubscription(info)
            Log.d(TAG, "Removed subscription: $subscriptionId")
        }
    }

    /**
     * Clean up subscription
     */
    private suspend fun cleanupSubscription(info: SubscriptionInfo) {
        try {
            withContext(Dispatchers.IO) {
                // Delete monitored items
                if (info.monitoredItems.isNotEmpty()) {
                    try {
                        // Use the monitored items list directly
                        val itemsToDelete = info.monitoredItems.toList()
                        if (itemsToDelete.isNotEmpty()) {
                            info.subscription.deleteMonitoredItems(itemsToDelete).get()
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error deleting monitored items", e)
                    }
                }

                // Delete subscription from client
                try {
                    val client = OPCUAClientManager.client
                    client?.subscriptionManager?.deleteSubscription(info.subscription.subscriptionId)?.get()
                } catch (e: Exception) {
                    Log.w(TAG, "Error deleting subscription", e)
                }
            }

            // Remove lifecycle observer
            info.lifecycleOwner?.lifecycle?.removeObserver(this@OpcUaSubscriptionManager)

        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up subscription", e)
        }
    }

    /**
     * Lifecycle event handler
     */
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_DESTROY -> {
                // Clean up subscriptions associated with this lifecycle
                cleanupScope.launch {
                    cleanupLifecycleSubscriptions(source)
                }
            }
            else -> {}
        }
    }

    /**
     * Clean up subscriptions for lifecycle owner
     */
    private suspend fun cleanupLifecycleSubscriptions(owner: LifecycleOwner) {
        subscriptionMutex.withLock {
            val toRemove = subscriptions.entries
                .filter { it.value.lifecycleOwner == owner }
                .map { it.key }

            toRemove.forEach { subscriptionId ->
                subscriptions.remove(subscriptionId)?.let { info ->
                    cleanupSubscription(info)
                    Log.d(TAG, "Cleaned up lifecycle subscription: $subscriptionId")
                }
            }
        }
    }

    /**
     * Start periodic cleanup job
     */
    private fun startPeriodicCleanup() {
        cleanupJob = cleanupScope.launch {
            while (isActive) {
                delay(60000) // Every minute
                performCleanup()
            }
        }
    }

    /**
     * Perform cleanup of invalid subscriptions
     */
    private suspend fun performCleanup() {
        subscriptionMutex.withLock {
            val toRemove = mutableListOf<String>()

            subscriptions.forEach { (id, info) ->
                try {
                    // Check if subscription is still valid by checking if we can access its ID
                    // This is a simple check - if it throws, subscription is invalid
                    val subscriptionId = info.subscription.subscriptionId

                    // Also check if monitored items are still valid
                    val hasValidItems = info.monitoredItems.any { item ->
                        try {
                            item.clientHandle // Try to access a property
                            true
                        } catch (e: Exception) {
                            false
                        }
                    }

                    // If no monitored items or all are invalid, consider for removal
                    if (info.monitoredItems.isEmpty() || !hasValidItems) {
                        Log.w(TAG, "Subscription $id has no valid monitored items")
                        // Don't remove immediately - subscription might still be valid
                    }

                } catch (e: Exception) {
                    Log.w(TAG, "Invalid subscription detected: $id")
                    toRemove.add(id)
                }
            }

            // Remove invalid subscriptions
            toRemove.forEach { id ->
                subscriptions.remove(id)?.let { info ->
                    cleanupSubscription(info)
                }
            }

            if (toRemove.isNotEmpty()) {
                Log.d(TAG, "Cleaned up ${toRemove.size} invalid subscriptions")
            }
        }
    }

    /**
     * Clean up all resources
     */
    suspend fun cleanup() {
        Log.d(TAG, "Cleaning up all subscriptions")

        cleanupJob?.cancel()

        subscriptionMutex.withLock {
            subscriptions.values.forEach { info ->
                cleanupSubscription(info)
            }
            subscriptions.clear()
        }

        cleanupScope.cancel()
    }

    /**
     * Get statistics
     */
    fun getStatistics(): SubscriptionStatistics {
        return SubscriptionStatistics(
            totalSubscriptions = subscriptions.size,
            subscriptionDetails = subscriptions.map { (id, info) ->
                SubscriptionDetail(
                    id = id,
                    monitoredItemCount = info.monitoredItems.size,
                    hasLifecycle = info.lifecycleOwner != null,
                    publishingInterval = info.publishingInterval
                )
            }
        )
    }

    data class SubscriptionStatistics(
        val totalSubscriptions: Int,
        val subscriptionDetails: List<SubscriptionDetail>
    )

    data class SubscriptionDetail(
        val id: String,
        val monitoredItemCount: Int,
        val hasLifecycle: Boolean,
        val publishingInterval: Double
    )
}