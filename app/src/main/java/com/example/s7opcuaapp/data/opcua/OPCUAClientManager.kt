package com.example.s7opcuaapp.data.opcua

import android.util.Log
import kotlinx.coroutines.*
import org.eclipse.milo.opcua.sdk.client.OpcUaClient
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription
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

object OPCUAClientManager {
    var client: OpcUaClient? = null
        private set

    private var subscription: UaSubscription? = null

    // Connection state tracking
    private val isConnectionLost = AtomicBoolean(false)
    private var connectionLostCallback: (() -> Unit)? = null

    // Track last successful data receive time for each node
    private val lastDataReceived = ConcurrentHashMap<String, Long>()
    private var connectionMonitorJob: Job? = null
    private val monitorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Timeout constants
    private const val DISCOVERY_TIMEOUT = 5000L
    private const val CONNECTION_TIMEOUT = 10000L
    private const val OPERATION_TIMEOUT = 5000L
    private const val DATA_TIMEOUT = 10000L // 10s without data = connection lost

    /**
     * Set callback for connection lost events
     */
    fun setConnectionLostCallback(callback: (() -> Unit)) {
        connectionLostCallback = callback
    }

    /**
     * Handle connection lost
     */
    private fun handleConnectionLost() {
        if (!isConnectionLost.getAndSet(true)) {
            Log.e("OPCUAClient", "🔌 CONNECTION LOST")
            connectionLostCallback?.invoke()
        }
    }

    /**
     * Start monitoring connection health
     */
    private fun startConnectionMonitor() {
        connectionMonitorJob?.cancel()
        connectionMonitorJob = monitorScope.launch {
            while (isActive) {
                delay(3000) // Check every 3 seconds

                try {
                    // Kiểm tra client và session
                    val clientExists = client != null
                    val hasSession = try {
                        client?.session != null
                    } catch (e: Exception) {
                        false
                    }

                    if (clientExists && !hasSession) {
                        Log.w("OPCUAClient", "Client exists but no session!")
                        handleConnectionLost()
                        continue
                    }

                    // Check data timeout
                    val now = System.currentTimeMillis()
                    val hasRecentData = lastDataReceived.values.any {
                        (now - it) < DATA_TIMEOUT
                    }

                    if (!hasRecentData && lastDataReceived.isNotEmpty()) {
                        Log.w("OPCUAClient", "⚠️ No data received for ${DATA_TIMEOUT}ms")

                        // Try health check
                        if (!performHealthCheck()) {
                            handleConnectionLost()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("OPCUAClient", "Error in connection monitor", e)
                }
            }
        }
    }

    /**
     * Perform health check by reading server state
     */
    private suspend fun performHealthCheck(): Boolean = withContext(Dispatchers.IO) {
        try {
            client?.let { cli ->
                // Kiểm tra bằng cách đọc server state
                val future = cli.readValue(
                    0.0,
                    TimestampsToReturn.Neither,
                    Identifiers.Server_ServerStatus_State
                )

                try {
                    withTimeoutOrNull(2000L) {
                        val dataValue = future.get(2000, TimeUnit.MILLISECONDS)
                        // Nếu đọc được và status OK thì connection còn tốt
                        dataValue?.statusCode?.isGood == true
                    } ?: false
                } catch (e: Exception) {
                    Log.e("OPCUAClient", "Health check read failed", e)
                    false
                }
            } ?: false
        } catch (e: Exception) {
            Log.e("OPCUAClient", "Health check error", e)
            false
        }
    }

    /**
     * Connect với better error handling
     */
    suspend fun connect(
        ipAddress: String,
        port: Int,
        username: String? = null,
        password: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            isConnectionLost.set(false)
            lastDataReceived.clear()

            val endpointUrl = "opc.tcp://$ipAddress:$port"
            Log.d("OPCUAClient", "🔌 Connecting to $endpointUrl...")

            // Discovery với timeout
            val endpoints: List<EndpointDescription> = try {
                withTimeout(DISCOVERY_TIMEOUT) {
                    val future = DiscoveryClient.getEndpoints(endpointUrl)
                    future.get(DISCOVERY_TIMEOUT, TimeUnit.MILLISECONDS)
                }
            } catch (e: Exception) {
                Log.e("OPCUAClient", "❌ Discovery error", e)
                throw e
            }

            val noSecEndpoint = endpoints.firstOrNull {
                it.securityPolicyUri == SecurityPolicy.None.uri
            } ?: throw Exception("No SecurityPolicy.None endpoint found")

            val tokenPoliciesList = noSecEndpoint.userIdentityTokens?.toList() ?: emptyList()

            val identityProvider = when {
                !username.isNullOrBlank() &&
                        tokenPoliciesList.any { it.tokenType == UserTokenType.UserName } -> {
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

            client = OpcUaClient.create(config)

            // Connect với timeout
            try {
                withTimeout(CONNECTION_TIMEOUT) {
                    val connectFuture = client!!.connect()
                    connectFuture.get(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                }
            } catch (e: Exception) {
                Log.e("OPCUAClient", "❌ Connect error", e)
                client = null
                throw e
            }

            // Start connection monitor
            startConnectionMonitor()

            Log.d("OPCUAClient", "✅ Connected successfully")
            true

        } catch (e: Exception) {
            Log.e("OPCUAClient", "❌ Connection failed", e)
            handleConnectionLost()
            false
        }
    }

    /**
     * Create subscription với data tracking
     */
    suspend fun createSubscription(
        nodeIdString: String,
        samplingInterval: UInteger = UInteger.valueOf(250),
        onValueChange: (DataValue) -> Unit
    ) = withContext(Dispatchers.IO) {
        val cli = client ?: return@withContext

        try {
            withTimeout(OPERATION_TIMEOUT) {
                if (subscription == null) {
                    val subFuture = cli.subscriptionManager.createSubscription(250.0)
                    subscription = subFuture.get(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS)
                    Log.d("OPCUAClient", "📡 Subscription created")
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
                                Log.d("OPCUAClient", "✅ Connection restored - data received")
                            }

                            onValueChange(dataValue)
                        } catch (e: Exception) {
                            Log.w("OPCUAClient", "Error in value consumer", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("OPCUAClient", "❌ Subscription error", e)

            // Check if it's a connection error
            if (e.message?.contains("Bad_", ignoreCase = true) == true ||
                e.message?.contains("timeout", ignoreCase = true) == true) {
                handleConnectionLost()
            }
            throw e
        }
    }

    /**
     * Write node với connection error detection
     */
    suspend fun writeNode(nodeIdString: String, rawValue: Any): StatusCode? = withContext(Dispatchers.IO) {
        val cli = client ?: return@withContext null

        try {
            withTimeout(OPERATION_TIMEOUT) {
                val nodeId = NodeId.parse(nodeIdString)
                val variant = when (rawValue) {
                    is Boolean -> Variant(rawValue)
                    is Int -> Variant(rawValue)
                    is Short -> Variant(rawValue)
                    else -> throw IllegalArgumentException("Unsupported type: ${rawValue.javaClass.simpleName}")
                }

                val dataValue = DataValue(variant, null, null)

                val statusFuture = cli.writeValue(nodeId, dataValue)
                val status = statusFuture.get(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS)

                if (status.isGood) {
                    // Reset connection lost on successful write
                    isConnectionLost.set(false)
                    Log.d("OPCUAClient", "✅ Write successful: $nodeIdString = $rawValue")
                } else {
                    Log.e("OPCUAClient", "❌ Write failed: $nodeIdString, Status: $status")

                    // Check for connection-related errors
                    if (status.value == StatusCodes.Bad_NotConnected ||
                        status.value == StatusCodes.Bad_ConnectionClosed ||
                        status.value == StatusCodes.Bad_SessionClosed) {
                        handleConnectionLost()
                    }
                }
                status
            }
        } catch (e: Exception) {
            Log.e("OPCUAClient", "❌ Write error", e)

            // Check if it's a connection error
            if (e.message?.contains("Bad_", ignoreCase = true) == true ||
                e.message?.contains("timeout", ignoreCase = true) == true ||
                e.message?.contains("connection", ignoreCase = true) == true) {
                handleConnectionLost()
            }
            null
        }
    }

    /**
     * Read access level của node
     */
    suspend fun readAccessLevel(nodeIdString: String): UByte? = withContext(Dispatchers.IO) {
        val cli = client ?: return@withContext null

        try {
            withTimeout(OPERATION_TIMEOUT) {
                val nodeId = NodeId.parse(nodeIdString)
                val readValueId = ReadValueId(nodeId, AttributeId.AccessLevel.uid(), null, null)
                val responseFuture = cli.read(0.0, TimestampsToReturn.Neither, listOf(readValueId))
                val response = responseFuture.get(OPERATION_TIMEOUT, TimeUnit.MILLISECONDS)
                val dataValue = response.results?.firstOrNull()
                dataValue?.value?.value as? UByte
            }
        } catch (e: Exception) {
            Log.e("OPCUAClient", "Failed to read AccessLevel for $nodeIdString", e)
            null
        }
    }

    /**
     * Check if connected
     */
    fun isConnected(): Boolean {
        if (isConnectionLost.get()) {
            return false
        }

        return try {
            client?.let { cli ->
                // Kiểm tra xem client có session không
                val hasSession = try {
                    cli.session != null
                } catch (e: Exception) {
                    false
                }

                if (!hasSession) {
                    Log.w("OPCUAClient", "No active session")
                }

                hasSession
            } ?: false
        } catch (e: Exception) {
            Log.e("OPCUAClient", "Error checking connection", e)
            false
        }
    }

    /**
     * Disconnect với cleanup
     */
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            Log.d("OPCUAClient", "🔌 Disconnecting...")

            // Stop monitor
            connectionMonitorJob?.cancel()

            // Clean subscription
            subscription?.let { sub ->
                try {
                    withTimeout(2000L) {
                        val deleteFuture = sub.deleteMonitoredItems(sub.monitoredItems)
                        deleteFuture.get(2000, TimeUnit.MILLISECONDS)
                    }
                } catch (e: Exception) {
                    Log.w("OPCUAClient", "Error deleting monitored items", e)
                }
            }
            subscription = null

            // Disconnect client
            client?.let { cli ->
                try {
                    withTimeout(2000L) {
                        val disconnectFuture = cli.disconnect()
                        disconnectFuture.get(2000, TimeUnit.MILLISECONDS)
                    }
                } catch (e: Exception) {
                    Log.w("OPCUAClient", "Error during disconnect", e)
                }
            }

            client = null
            isConnectionLost.set(false)
            connectionLostCallback = null
            lastDataReceived.clear()

            Log.d("OPCUAClient", "✅ Disconnected")
        } catch (e: Exception) {
            Log.e("OPCUAClient", "❌ Error during disconnect", e)
        }
    }

    /**
     * Force cleanup on app exit
     */
    fun cleanup() {
        connectionMonitorJob?.cancel()
        monitorScope.cancel()
    }
}