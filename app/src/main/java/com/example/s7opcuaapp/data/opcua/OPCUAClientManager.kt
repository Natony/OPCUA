package com.example.s7opcuaapp.data.opcua

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.eclipse.milo.opcua.sdk.client.OpcUaClient
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider
import org.eclipse.milo.opcua.stack.client.DiscoveryClient
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger
import org.eclipse.milo.opcua.stack.core.AttributeId
import org.eclipse.milo.opcua.stack.core.types.enumerated.UserTokenType
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription
import org.eclipse.milo.opcua.stack.core.types.structured.UserTokenPolicy
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy
import java.util.concurrent.CompletableFuture
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription
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
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object OPCUAClientManager {
    var client: OpcUaClient? = null
        private set

    // Giữ một subscription duy nhất để tránh Bad_TooManySubscriptions
    private var subscription: UaSubscription? = null

    /**
     * Kết nối đến OPC UA Server với Anonymous hoặc Username (nếu server hỗ trợ).
     */
    suspend fun connect(
        ipAddress: String,
        port: Int,
        username: String? = null,
        password: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val endpointUrl = "opc.tcp://$ipAddress:$port"
            Log.d("OPCUAClient", "🔌 Connecting to $endpointUrl...")

            // 1) Lấy danh sách endpoint từ server
            val endpoints = withContext(Dispatchers.IO) {
                try {
                    withTimeout(5000L) { // 5 second timeout
                        suspendCancellableCoroutine<List<EndpointDescription>> { continuation ->
                            val future = DiscoveryClient.getEndpoints(endpointUrl)

                            continuation.invokeOnCancellation {
                                future.cancel(true)
                            }

                            future.whenComplete { result, throwable ->
                                if (throwable != null) {
                                    continuation.resumeWithException(throwable)
                                } else {
                                    continuation.resume(result)
                                }
                            }
                        }
                    }
                } catch (e: TimeoutCancellationException) {
                    Log.e("OPCUAClient", "Discovery timeout")
                    throw e
                }
            }

            // 2) Chọn endpoint có SecurityPolicy=None
            val noSecEndpoint: EndpointDescription? = endpoints.firstOrNull {
                it.securityPolicyUri == SecurityPolicy.None.uri
            }
            if (noSecEndpoint == null) {
                Log.e("OPCUAClient", "❌ Không tìm thấy endpoint Security=None tại $endpointUrl")
                return@withContext false
            }

            // 3) Lấy danh sách UserTokenPolicy
            val tokenPoliciesList: List<UserTokenPolicy> =
                noSecEndpoint.userIdentityTokens?.toList() ?: emptyList()
            Log.d("OPCUAClient", "Available UserTokenPolicies: ${tokenPoliciesList.map { it.tokenType }}")

            // 4) Chọn IdentityProvider
            val identityProvider = when {
                !username.isNullOrBlank() &&
                        tokenPoliciesList.any { it.tokenType == UserTokenType.UserName } -> {
                    Log.d("OPCUAClient", "✔️ Using Username authentication")
                    UsernameProvider(username, password ?: "")
                }
                else -> {
                    Log.d("OPCUAClient", "✔️ Using Anonymous authentication")
                    AnonymousProvider()
                }
            }

            // 5) Build config và connect
            val config = OpcUaClientConfig.builder()
                .setApplicationName(LocalizedText.english("AndroidUAClient"))
                .setApplicationUri("urn:android:opcua:client")
                .setEndpoint(noSecEndpoint)
                .setIdentityProvider(identityProvider)
                .setRequestTimeout(UInteger.valueOf(10_000))
                .build()

            client = OpcUaClient.create(config)

            withTimeout(5000L) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    val future = client!!.connect()

                    continuation.invokeOnCancellation {
                        future.cancel(true)
                    }

                    future.whenComplete { _, throwable ->
                        if (throwable != null) {
                            continuation.resumeWithException(throwable)
                        } else {
                            continuation.resume(Unit)
                        }
                    }
                }
            }

            Log.d("OPCUAClient", "✅ Connected successfully to $endpointUrl")
            true

        } catch (e: Exception) {
            Log.e("OPCUAClient", "❌ Connection failed", e)
            client = null
            false
        }
    }

    /**
     * Tạo monitored item cho một NodeId (dùng chung một subscription).
     */
    suspend fun createSubscription(
        nodeIdString: String,
        samplingInterval: UInteger = UInteger.valueOf(250),
        onValueChange: (DataValue) -> Unit
    ) = withContext(Dispatchers.IO) {
        val cli = client ?: return@withContext
        try {
            if (subscription == null) {
                val createSubFuture = cli.subscriptionManager.createSubscription(250.0)
                subscription = suspendCancellableCoroutine { continuation ->
                    continuation.invokeOnCancellation {
                        createSubFuture.cancel(true)
                    }

                    createSubFuture.whenComplete { result, throwable ->
                        if (throwable != null) {
                            continuation.resumeWithException(throwable)
                        } else {
                            continuation.resume(result)
                        }
                    }
                }
                Log.d("OPCUAClient", "📡 Subscription created (250ms)")
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

            val createItemsFuture = sub.createMonitoredItems(
                TimestampsToReturn.Both,
                listOf(request)
            )

            val items = suspendCancellableCoroutine<List<org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem>> { continuation ->
                continuation.invokeOnCancellation {
                    createItemsFuture.cancel(true)
                }

                createItemsFuture.whenComplete { result, throwable ->
                    if (throwable != null) {
                        continuation.resumeWithException(throwable)
                    } else {
                        continuation.resume(result)
                    }
                }
            }

            items.forEach { item ->
                item.setValueConsumer { _, dataValue ->
                    try {
                        onValueChange(dataValue)
                    } catch (e: Exception) {
                        Log.w("OPCUAClient", "Error in value consumer for $nodeIdString", e)
                    }
                }
                Log.d("OPCUAClient", "✅ MonitoredItem created for $nodeIdString")
            }
        } catch (e: Exception) {
            Log.e("OPCUAClient", "❌ Failed to create subscription for $nodeIdString", e)
        }
    }

    /**
     * Viết một node Bool hoặc Int. Đơn giản hóa và bỏ qua check AccessLevel.
     */
    suspend fun writeNode(nodeIdString: String, rawValue: Any): StatusCode? = withContext(Dispatchers.IO) {
        val cli = client ?: return@withContext null
        try {
            Log.d("OPCUAClient", "📝 Writing $rawValue to $nodeIdString")
            val nodeId = NodeId.parse(nodeIdString)
            val variant = when (rawValue) {
                is Boolean  -> Variant(rawValue)
                is Int      -> Variant(rawValue)
                is Short    -> Variant(rawValue)
                else        -> {
                    Log.e("OPCUAClient", "❌ Unsupported type: ${rawValue.javaClass.simpleName}")
                    return@withContext null
                }
            }
            val dataValue = DataValue(variant, null, null)

            val writeFuture = cli.writeValue(nodeId, dataValue)
            val status = suspendCancellableCoroutine<StatusCode> { continuation ->
                continuation.invokeOnCancellation {
                    writeFuture.cancel(true)
                }

                writeFuture.whenComplete { result, throwable ->
                    if (throwable != null) {
                        continuation.resumeWithException(throwable)
                    } else {
                        continuation.resume(result)
                    }
                }
            }

            if (status.isGood) {
                Log.d("OPCUAClient", "✅ Write successful: $nodeIdString = $rawValue")
            } else {
                Log.e("OPCUAClient", "❌ Write failed: $nodeIdString, Status: $status")
            }
            status
        } catch (e: Exception) {
            Log.e("OPCUAClient", "❌ Write exception for $nodeIdString", e)
            null
        }
    }

    suspend fun readAccessLevel(nodeIdString: String): UByte? = withContext(Dispatchers.IO) {
        val cli = client ?: return@withContext null
        try {
            val nodeId = NodeId.parse(nodeIdString)
            val readValueId = ReadValueId(nodeId, AttributeId.AccessLevel.uid(), null, null)

            val readFuture = cli.read(0.0, TimestampsToReturn.Neither, listOf(readValueId))
            val response = suspendCancellableCoroutine<org.eclipse.milo.opcua.stack.core.types.structured.ReadResponse> { continuation ->
                continuation.invokeOnCancellation {
                    readFuture.cancel(true)
                }

                readFuture.whenComplete { result, throwable ->
                    if (throwable != null) {
                        continuation.resumeWithException(throwable)
                    } else {
                        continuation.resume(result)
                    }
                }
            }

            val dataValue = response.results?.firstOrNull()
            dataValue?.value?.value as? UByte
        } catch (e: Exception) {
            Log.e("OPCUAClient", "Failed to read AccessLevel for $nodeIdString", e)
            null
        }
    }

    /**
     * Ngắt hết subscription và đóng connection.
     */
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            client?.let { cli ->
                subscription?.let { sub ->
                    try {
                        val deleteItemsFuture = sub.deleteMonitoredItems(sub.monitoredItems)
                        suspendCancellableCoroutine<List<StatusCode>> { continuation ->
                            continuation.invokeOnCancellation {
                                deleteItemsFuture.cancel(true)
                            }

                            deleteItemsFuture.whenComplete { result, throwable ->
                                if (throwable != null) {
                                    Log.w("OPCUAClient", "Error deleting monitored items", throwable)
                                    // Don't fail disconnection for this
                                    continuation.resume(emptyList())
                                } else {
                                    continuation.resume(result)
                                }
                            }
                        }
                        Log.d("OPCUAClient", "🗑️ Deleted all monitored items")
                    } catch (e: Exception) {
                        Log.w("OPCUAClient", "Error deleting monitored items", e)
                    }
                }
                subscription = null

                val disconnectFuture = cli.disconnect()
                suspendCancellableCoroutine<Unit> { continuation ->
                    continuation.invokeOnCancellation {
                        disconnectFuture.cancel(true)
                    }

                    disconnectFuture.whenComplete { _, throwable ->
                        if (throwable != null) {
                            Log.w("OPCUAClient", "Error during disconnect", throwable)
                            // Don't fail for disconnect errors
                            continuation.resume(Unit)
                        } else {
                            continuation.resume(Unit)
                        }
                    }
                }
                Log.d("OPCUAClient", "🔌 Disconnected successfully")
            }
        } catch (e: Exception) {
            Log.e("OPCUAClient", "❌ Error during disconnect", e)
        } finally {
            client = null
            subscription = null
        }
    }

    fun isConnected(): Boolean {
        return client?.let {
            try {
                it.session != null
            } catch (e: Exception) {
                false
            }
        } ?: false
    }
}