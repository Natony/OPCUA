package com.example.s7opcuaapp.data.opcua

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.eclipse.milo.opcua.sdk.client.OpcUaClient
import org.eclipse.milo.opcua.sdk.client.SessionActivityListener
import org.eclipse.milo.opcua.sdk.client.api.UaSession
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscriptionManager.SubscriptionListener
import org.eclipse.milo.opcua.stack.client.DiscoveryClient
import org.eclipse.milo.opcua.stack.core.Identifiers
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger
import org.eclipse.milo.opcua.stack.core.AttributeId
import org.eclipse.milo.opcua.stack.core.types.enumerated.UserTokenType
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription
import org.eclipse.milo.opcua.stack.core.types.structured.UserTokenPolicy
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy
import org.eclipse.milo.opcua.stack.core.UaException
import org.eclipse.milo.opcua.stack.core.StatusCodes
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ConcurrentHashMap

/**
 * FIXED: Thread-safe singleton OPC UA client manager
 */
object OPCUAClientManager {

    private const val TAG = "OPCUAClient"

    // Thread-safe client access
    @Volatile
    private var _client: OpcUaClient? = null
    private val clientMutex = Mutex()

    val client: OpcUaClient?
        get() = _client

    // Thread-safe subscription management
    @Volatile
    private var subscription: UaSubscription? = null
    private val subscriptionMutex = Mutex()

    // Connection state tracking
    private val isConnectionLost = AtomicBoolean(false)

    // Track last successful data receive time for each node
    private val lastDataReceived = ConcurrentHashMap<String, Long>()

    // Connection monitoring - properly managed
    @Volatile
    private var connectionMonitorJob: Job? = null
    private val monitorScope = CoroutineScope(
        Dispatchers.IO + SupervisorJob() + CoroutineName("OPCUAMonitor")
    )

    // Timeout constants
    private const val DISCOVERY_TIMEOUT = 10000L
    private const val CONNECTION_TIMEOUT = 20000L
    private const val OPERATION_TIMEOUT = 10000L
    private const val DATA_TIMEOUT = 15000L
    private const val HEALTH_CHECK_INTERVAL = 5000L
    private const val HEALTH_CHECK_TIMEOUT = 3000L
    private const val SESSION_TIMEOUT = 10000L

    // Listener references
    @Volatile
    private var sessionListener: SessionActivityListener? = null
    @Volatile
    private var subscriptionListener: SubscriptionListener? = null

    // Callbacks
    @Volatile
    internal var connectionLostCallback: (() -> Unit)? = null
    @Volatile
    private var connectionRestoredCallback: (() -> Unit)? = null

    // Health check mutex to prevent concurrent checks
    private val healthCheckMutex = Mutex()

    @Volatile
    private var lastSessionActiveTime = 0L

    // Flag to control monitoring loop
    private val isMonitoringActive = AtomicBoolean(false)

    fun setConnectionCallbacks(
        onLost: (() -> Unit)? = null,
        onRestored: (() -> Unit)? = null
    ) {
        connectionLostCallback = onLost
        connectionRestoredCallback = onRestored
    }

    /**
     * Thread-safe connection method
     */
    suspend fun connect(
        ipAddress: String,
        port: Int,
        username: String? = null,
        password: String? = null
    ): Boolean = clientMutex.withLock {
        return@withLock try {
            // Reset state
            isConnectionLost.set(false)
            lastDataReceived.clear()

            val endpointUrl = "opc.tcp://$ipAddress:$port"
            Log.d(TAG, "🔌 Connecting to $endpointUrl...")

            // Discovery with timeout
            val endpoints = withTimeoutOrNull(DISCOVERY_TIMEOUT) {
                DiscoveryClient.getEndpoints(endpointUrl)
                    .get(DISCOVERY_TIMEOUT, TimeUnit.MILLISECONDS)
            } ?: throw Exception("Discovery timeout - PLC not responding")

            if (endpoints.isEmpty()) {
                throw Exception("No OPC UA endpoints found at $endpointUrl")
            }

            val noSecEndpoint = endpoints.firstOrNull {
                it.securityPolicyUri == SecurityPolicy.None.uri
            } ?: throw Exception("No SecurityPolicy.None endpoint found")

            val tokenPolicies = noSecEndpoint.userIdentityTokens?.toList() ?: emptyList()

            val identityProvider = when {
                !username.isNullOrBlank() &&
                        tokenPolicies.any { it.tokenType == UserTokenType.UserName } -> {
                    UsernameProvider(username, password ?: "")
                }
                else -> AnonymousProvider()
            }

            val config = OpcUaClientConfig.builder()
                .setApplicationName(LocalizedText.english("AndroidUAClient"))
                .setApplicationUri("urn:android:opcua:client")
                .setEndpoint(noSecEndpoint)
                .setIdentityProvider(identityProvider)
                .setRequestTimeout(UInteger.valueOf(5000))
                .setKeepAliveInterval(UInteger.valueOf(5000))
                .build()

            // Create and connect client
            _client = OpcUaClient.create(config)

            withTimeout(CONNECTION_TIMEOUT) {
                _client!!.connect().get(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
            }

            // Add listeners after successful connection
            setupListeners()

            // Start monitoring with proper control
            startConnectionMonitor()

            Log.d(TAG, "✅ Connected successfully")
            lastSessionActiveTime = System.currentTimeMillis()
            true

        } catch (e: CancellationException) {
            Log.d(TAG, "Connection cancelled")
            false
        } catch (e: Exception) {
            Log.e(TAG, "❌ Connection failed", e)
            handleConnectionLost("Connect failed: ${e.message}")
            false
        }
    }

    /**
     * Setup listeners safely
     */
    private fun setupListeners() {
        _client?.let { cli ->
            // Remove old listeners first
            sessionListener?.let { cli.removeSessionActivityListener(it) }
            subscriptionListener?.let { cli.subscriptionManager.removeSubscriptionListener(it) }

            // Add new listeners
            sessionListener = createSessionListener()
            cli.addSessionActivityListener(sessionListener!!)

            subscriptionListener = createSubscriptionListener()
            cli.subscriptionManager.addSubscriptionListener(subscriptionListener!!)

            Log.d(TAG, "✅ All listeners registered")
        }
    }

    /**
     * Create session listener
     */
    private fun createSessionListener(): SessionActivityListener {
        return object : SessionActivityListener {
            override fun onSessionActive(session: UaSession) {
                Log.d(TAG, "✅ Session ACTIVE")
                lastSessionActiveTime = System.currentTimeMillis()

                if (isConnectionLost.getAndSet(false)) {
                    Log.d(TAG, "🔄 Connection restored")
                    connectionRestoredCallback?.invoke()
                }
            }

            override fun onSessionInactive(session: UaSession) {
                Log.w(TAG, "⚠️ Session INACTIVE")
                handleConnectionLost("Session inactive")
            }
        }
    }

    /**
     * Create subscription listener
     */
    private fun createSubscriptionListener(): SubscriptionListener {
        return object : SubscriptionListener {
            override fun onSubscriptionTransferFailed(
                subscription: UaSubscription,
                statusCode: StatusCode
            ) {
                Log.e(TAG, "❌ Subscription transfer failed: $statusCode")
                handleConnectionLost("Subscription transfer failed")
            }

            override fun onStatusChanged(
                subscription: UaSubscription,
                status: StatusCode
            ) {
                Log.d(TAG, "Subscription status changed: $status")
                if (!status.isGood) {
                    Log.w(TAG, "⚠️ Subscription status not good: $status")
                }
            }

            override fun onSubscriptionWatchdogTimerElapsed(subscription: UaSubscription) {
                Log.w(TAG, "⚠️ Subscription watchdog timer elapsed")
                handleConnectionLost("Subscription watchdog timeout")
            }

            override fun onPublishFailure(exception: UaException) {
                Log.e(TAG, "❌ Publish failure", exception)

                if (exception.statusCode.value == StatusCodes.Bad_ConnectionClosed ||
                    exception.statusCode.value == StatusCodes.Bad_NotConnected ||
                    exception.statusCode.value == StatusCodes.Bad_SessionClosed) {
                    handleConnectionLost("Publish failure: ${exception.message}")
                }
            }

            override fun onNotificationDataLost(subscription: UaSubscription) {
                Log.w(TAG, "⚠️ Notification data lost")
            }
        }
    }

    /**
     * Start monitoring with proper lifecycle management
     */
    private fun startConnectionMonitor() {
        // Cancel any existing monitor
        connectionMonitorJob?.cancel()

        // Set monitoring active flag
        isMonitoringActive.set(true)

        connectionMonitorJob = monitorScope.launch {
            Log.d(TAG, "📡 Starting connection monitoring")

            while (isActive && isMonitoringActive.get()) {
                try {
                    delay(HEALTH_CHECK_INTERVAL)

                    // Skip if no client or already lost
                    if (_client == null || isConnectionLost.get()) {
                        continue
                    }

                    // Check session timeout
                    val now = System.currentTimeMillis()
                    if (lastSessionActiveTime > 0) {
                        val timeSinceActive = now - lastSessionActiveTime
                        if (timeSinceActive > SESSION_TIMEOUT * 2) {
                            Log.w(TAG, "⚠️ No session activity for ${timeSinceActive}ms")

                            val isHealthy = checkConnectionHealth()
                            if (!isHealthy) {
                                handleConnectionLost("No activity and health check failed")
                                break
                            }
                        }
                    }

                    // Check data flow
                    val hasRecentData = lastDataReceived.values.any {
                        (now - it) < DATA_TIMEOUT * 2
                    }

                    if (!hasRecentData && lastDataReceived.isNotEmpty()) {
                        Log.w(TAG, "⚠️ No data for long time")

                        val isHealthy = checkConnectionHealth()
                        if (!isHealthy) {
                            handleConnectionLost("No data and health check failed")
                            break
                        }
                    }

                } catch (e: CancellationException) {
                    // Normal cancellation
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Monitor error", e)
                }
            }

            Log.d(TAG, "📡 Connection monitoring stopped")
        }
    }

    /**
     * Thread-safe health check
     */
    suspend fun checkConnectionHealth(): Boolean = healthCheckMutex.withLock {
        try {
            _client?.let { cli ->
                val result = withTimeoutOrNull(HEALTH_CHECK_TIMEOUT) {
                    cli.readValue(
                        0.0,
                        TimestampsToReturn.Neither,
                        Identifiers.Server_ServerStatus_State
                    ).get(HEALTH_CHECK_TIMEOUT, TimeUnit.MILLISECONDS)
                    true
                } ?: false

                if (result) {
                    lastSessionActiveTime = System.currentTimeMillis()
                }

                return@withLock result
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Health check exception", e)
            false
        }
    }

    /**
     * Thread-safe subscription creation
     */
    suspend fun createSubscription(
        nodeIdString: String,
        samplingInterval: UInteger = UInteger.valueOf(250),
        onValueChange: (DataValue) -> Unit
    ) = subscriptionMutex.withLock {
        val cli = _client ?: return@withLock

        try {
            withTimeout(OPERATION_TIMEOUT) {
                if (subscription == null) {
                    val subFuture = cli.subscriptionManager.createSubscription(250.0)
                    subscription = subFuture.get(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS)
                    Log.d(TAG, "📡 Subscription created")
                }

                val sub = subscription!!

                val readValueId = ReadValueId(
                    NodeId.parse(nodeIdString),
                    AttributeId.Value.uid(),
                    null,
                    null
                )

                val clientHandle = UInteger.valueOf((System.nanoTime() % Int.MAX_VALUE).toInt())
                val monitoringParams = MonitoringParameters(
                    clientHandle,
                    samplingInterval.toDouble(),
                    null,
                    UInteger.valueOf(1),
                    true
                )

                val request = MonitoredItemCreateRequest(
                    readValueId,
                    MonitoringMode.Reporting,
                    monitoringParams
                )

                val itemsFuture = sub.createMonitoredItems(
                    TimestampsToReturn.Both,
                    listOf(request)
                )

                val items = itemsFuture.get(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS)

                items.forEach { item ->
                    item.setValueConsumer { _, dataValue ->
                        try {
                            // Track data received time
                            lastDataReceived[nodeIdString] = System.currentTimeMillis()

                            // Reset connection lost flag
                            if (isConnectionLost.getAndSet(false)) {
                                Log.d(TAG, "✅ Connection restored - data received")
                            }

                            onValueChange(dataValue)
                        } catch (e: Exception) {
                            Log.w(TAG, "Error in value consumer", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Subscription error", e)

            if (e.message?.contains("Bad_", ignoreCase = true) == true ||
                e.message?.contains("timeout", ignoreCase = true) == true) {
                handleConnectionLost("Subscription failed: ${e.message}")
            }
            throw e
        }
    }

    /**
     * Thread-safe write operation
     */
    suspend fun writeNode(nodeIdString: String, rawValue: Any): StatusCode? =
        withContext(Dispatchers.IO) {
            val cli = _client ?: return@withContext null

            try {
                withTimeout(OPERATION_TIMEOUT) {
                    val nodeId = NodeId.parse(nodeIdString)
                    val variant = when (rawValue) {
                        is Boolean -> Variant(rawValue)
                        is Int -> Variant(rawValue)
                        is Short -> Variant(rawValue)
                        else -> throw IllegalArgumentException(
                            "Unsupported type: ${rawValue.javaClass.simpleName}"
                        )
                    }

                    val dataValue = DataValue(variant, null, null)

                    val statusFuture = cli.writeValue(nodeId, dataValue)
                    val status = statusFuture.get(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS)

                    if (status.isGood) {
                        isConnectionLost.set(false)
                        lastSessionActiveTime = System.currentTimeMillis()
                        Log.d(TAG, "✅ Write successful: $nodeIdString = $rawValue")
                    } else {
                        Log.e(TAG, "❌ Write failed: $nodeIdString, Status: $status")

                        if (status.value == StatusCodes.Bad_NotConnected ||
                            status.value == StatusCodes.Bad_ConnectionClosed ||
                            status.value == StatusCodes.Bad_SessionClosed) {
                            handleConnectionLost("Write failed with connection error: $status")
                        }
                    }
                    status
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Write error", e)

                if (e.message?.contains("Bad_", ignoreCase = true) == true ||
                    e.message?.contains("timeout", ignoreCase = true) == true ||
                    e.message?.contains("connection", ignoreCase = true) == true) {
                    handleConnectionLost("Write failed: ${e.message}")
                }
                null
            }
        }

    /**
     * Thread-safe read access level
     */
    suspend fun readAccessLevel(nodeIdString: String): UByte? = withContext(Dispatchers.IO) {
        val cli = _client ?: return@withContext null

        try {
            withTimeout(OPERATION_TIMEOUT) {
                val nodeId = NodeId.parse(nodeIdString)
                val readValueId = ReadValueId(
                    nodeId,
                    AttributeId.AccessLevel.uid(),
                    null,
                    null
                )
                val responseFuture = cli.read(
                    0.0,
                    TimestampsToReturn.Neither,
                    listOf(readValueId)
                )
                val response = responseFuture.get(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS)
                val dataValue = response.results?.firstOrNull()
                dataValue?.value?.value as? UByte
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read AccessLevel for $nodeIdString", e)
            null
        }
    }

    /**
     * Handle connection lost
     */
    private fun handleConnectionLost(reason: String) {
        if (!isConnectionLost.getAndSet(true)) {
            Log.e(TAG, "🔌 CONNECTION LOST: $reason")
            connectionLostCallback?.invoke()
        }
    }

    /**
     * Check if connected
     */
    fun isConnected(): Boolean {
        if (isConnectionLost.get()) {
            return false
        }

        return _client != null
    }

    /**
     * Thread-safe disconnect
     */
    suspend fun disconnect() = clientMutex.withLock {
        try {
            Log.d(TAG, "🔌 Disconnecting...")

            // Stop monitoring first
            isMonitoringActive.set(false)
            connectionMonitorJob?.cancel()
            connectionMonitorJob = null

            // Clean subscription
            subscriptionMutex.withLock {
                subscription?.let { sub ->
                    try {
                        withTimeoutOrNull(2000L) {
                            val deleteFuture = sub.deleteMonitoredItems(sub.monitoredItems)
                            deleteFuture.get(2000, TimeUnit.MILLISECONDS)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error deleting monitored items", e)
                    }
                }
                subscription = null
            }

            // Remove listeners
            _client?.let { cli ->
                sessionListener?.let { cli.removeSessionActivityListener(it) }
                subscriptionListener?.let { cli.subscriptionManager.removeSubscriptionListener(it) }
            }

            // Disconnect client
            _client?.let { cli ->
                try {
                    withTimeoutOrNull(2000L) {
                        cli.disconnect().get(2000, TimeUnit.MILLISECONDS)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error during disconnect", e)
                }
            }

            // Clear state
            _client = null
            isConnectionLost.set(false)
            connectionLostCallback = null
            connectionRestoredCallback = null
            lastDataReceived.clear()
            sessionListener = null
            subscriptionListener = null

            Log.d(TAG, "✅ Disconnected")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error during disconnect", e)
        }
    }

    /**
     * Force cleanup on app exit
     */
    fun cleanup() {
        isMonitoringActive.set(false)
        connectionMonitorJob?.cancel()
        monitorScope.cancel()

        runBlocking {
            try {
                disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Error during cleanup", e)
            }
        }
    }
}