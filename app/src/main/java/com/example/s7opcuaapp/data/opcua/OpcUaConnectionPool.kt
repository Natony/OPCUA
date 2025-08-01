package com.example.s7opcuaapp.data.opcua

import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import org.eclipse.milo.opcua.sdk.client.OpcUaClient
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Connection pool for OPC UA clients
 * Manages multiple connections and prevents resource exhaustion
 */
@Singleton
class OpcUaConnectionPool @Inject constructor() {

    companion object {
        private const val TAG = "OpcUaConnectionPool"
        private const val DEFAULT_MAX_CONNECTIONS = 3
        private const val CONNECTION_TIMEOUT = 15000L
    }

    data class ConnectionConfig(
        val endpoint: String,
        val username: String? = null,
        val password: String? = null
    )

    data class PooledConnection(
        val client: OpcUaClient,
        val config: ConnectionConfig,
        val createdAt: Long = System.currentTimeMillis(),
        var lastUsed: Long = System.currentTimeMillis(),
        var useCount: Int = 0
    )

    // Thread-safe connection storage
    private val connections = ConcurrentHashMap<String, PooledConnection>()
    private val connectionMutex = Mutex()
    private val connectionSemaphore = Semaphore(DEFAULT_MAX_CONNECTIONS)

    // Statistics
    private var totalConnectionsCreated = 0
    private var totalConnectionsReused = 0
    private var totalConnectionsFailed = 0

    /**
     * Get or create connection for device
     */
    suspend fun getConnection(
        deviceId: String,
        config: ConnectionConfig
    ): OpcUaClient? = connectionMutex.withLock {

        // Check existing connection
        connections[deviceId]?.let { pooled ->
            if (isConnectionValid(pooled)) {
                pooled.lastUsed = System.currentTimeMillis()
                pooled.useCount++
                totalConnectionsReused++

                Log.d(TAG, "Reusing connection for $deviceId (use count: ${pooled.useCount})")
                return pooled.client
            } else {
                // Remove invalid connection
                Log.w(TAG, "Removing invalid connection for $deviceId")
                removeConnection(deviceId)
            }
        }

        // Create new connection
        return createNewConnection(deviceId, config)
    }

    /**
     * Create new connection with semaphore control
     */
    private suspend fun createNewConnection(
        deviceId: String,
        config: ConnectionConfig
    ): OpcUaClient? {

        return connectionSemaphore.withPermit {
            try {
                Log.d(TAG, "Creating new connection for $deviceId")

                // Check if we need to evict old connections
                if (connections.size >= DEFAULT_MAX_CONNECTIONS) {
                    evictOldestConnection()
                }

                // Create connection using existing manager
                val connected = OPCUAClientManager.connect(
                    ipAddress = config.endpoint.substringAfter("://").substringBefore(":"),
                    port = config.endpoint.substringAfterLast(":").toIntOrNull() ?: 4840,
                    username = config.username,
                    password = config.password
                )

                if (connected) {
                    OPCUAClientManager.client?.let { client ->
                        val pooled = PooledConnection(
                            client = client,
                            config = config
                        )
                        connections[deviceId] = pooled
                        totalConnectionsCreated++

                        Log.d(TAG, "✅ Created new connection for $deviceId")
                        return client
                    }
                }

                totalConnectionsFailed++
                Log.e(TAG, "❌ Failed to create connection for $deviceId")
                null

            } catch (e: Exception) {
                totalConnectionsFailed++
                Log.e(TAG, "Error creating connection for $deviceId", e)
                null
            }
        }
    }

    /**
     * Check if connection is still valid
     */
    private fun isConnectionValid(pooled: PooledConnection): Boolean {
        return try {
            // Check if client is connected
            pooled.client.session != null
        } catch (e: Exception) {
            Log.w(TAG, "Connection validation failed", e)
            false
        }
    }

    /**
     * Evict oldest connection to make room
     */
    private suspend fun evictOldestConnection() {
        val oldest = connections.entries
            .minByOrNull { it.value.lastUsed }

        oldest?.let { entry ->
            Log.w(TAG, "Evicting oldest connection: ${entry.key}")
            removeConnection(entry.key)
        }
    }

    /**
     * Remove connection from pool
     */
    suspend fun removeConnection(deviceId: String) {
        connections.remove(deviceId)?.let { pooled ->
            try {
                // Don't disconnect the global client if it's the current one
                if (OPCUAClientManager.client != pooled.client) {
                    pooled.client.disconnect().get()
                }
                Log.d(TAG, "Removed connection for $deviceId")
            } catch (e: Exception) {
                Log.w(TAG, "Error disconnecting client for $deviceId", e)
            }
        }
    }

    /**
     * Clear all connections
     */
    suspend fun clearAll() {
        Log.d(TAG, "Clearing all connections")

        connections.keys.toList().forEach { deviceId ->
            removeConnection(deviceId)
        }

        connections.clear()
    }

    /**
     * Get pool statistics
     */
    fun getStatistics(): PoolStatistics {
        return PoolStatistics(
            activeConnections = connections.size,
            totalCreated = totalConnectionsCreated,
            totalReused = totalConnectionsReused,
            totalFailed = totalConnectionsFailed,
            connectionDetails = connections.map { (id, pooled) ->
                ConnectionDetail(
                    deviceId = id,
                    endpoint = pooled.config.endpoint,
                    createdAt = pooled.createdAt,
                    lastUsed = pooled.lastUsed,
                    useCount = pooled.useCount,
                    isValid = isConnectionValid(pooled)
                )
            }
        )
    }

    data class PoolStatistics(
        val activeConnections: Int,
        val totalCreated: Int,
        val totalReused: Int,
        val totalFailed: Int,
        val connectionDetails: List<ConnectionDetail>
    )

    data class ConnectionDetail(
        val deviceId: String,
        val endpoint: String,
        val createdAt: Long,
        val lastUsed: Long,
        val useCount: Int,
        val isValid: Boolean
    )
}