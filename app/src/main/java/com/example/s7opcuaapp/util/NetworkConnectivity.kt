package com.example.s7opcuaapp.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkConnectivity @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Cache for network status to avoid repeated calls
    @Volatile
    private var lastNetworkCheck = 0L
    @Volatile
    private var cachedNetworkStatus = false
    private val cacheValidityMs = 2000L // 2 seconds cache

    companion object {
        private const val TAG = "NetworkConnectivity"
        private const val DEFAULT_TIMEOUT_MS = 3000
        private const val PING_TIMEOUT_MS = 1500
    }

    /**
     * Quick network availability check with caching
     */
    fun isNetworkAvailable(): Boolean {
        val now = System.currentTimeMillis()

        // Use cached result if recent
        if (now - lastNetworkCheck < cacheValidityMs) {
            return cachedNetworkStatus
        }

        return try {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)

            val isAvailable = capabilities?.let { caps ->
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
                        (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
            } ?: false

            // Update cache
            lastNetworkCheck = now
            cachedNetworkStatus = isAvailable

            Log.d(TAG, "Network available: $isAvailable")
            isAvailable
        } catch (e: Exception) {
            Log.e(TAG, "Error checking network availability", e)
            // In case of error, return cached value if recent, otherwise false
            if (now - lastNetworkCheck < cacheValidityMs * 2) {
                cachedNetworkStatus
            } else {
                false
            }
        }
    }

    /**
     * Enhanced network availability observer with better error handling
     */
    fun observeNetworkAvailability(): Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Network available: $network")
                cachedNetworkStatus = true
                lastNetworkCheck = System.currentTimeMillis()
                trySend(true)
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "Network lost: $network")
                cachedNetworkStatus = false
                lastNetworkCheck = System.currentTimeMillis()
                trySend(false)
            }

            override fun onUnavailable() {
                Log.d(TAG, "Network unavailable")
                cachedNetworkStatus = false
                lastNetworkCheck = System.currentTimeMillis()
                trySend(false)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

                Log.d(TAG, "Network capabilities changed: hasInternet=$hasInternet")
                cachedNetworkStatus = hasInternet
                lastNetworkCheck = System.currentTimeMillis()
                trySend(hasInternet)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        try {
            connectivityManager.registerNetworkCallback(request, callback)

            // Send initial state
            trySend(isNetworkAvailable())

        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
            // Send current state and close
            trySend(isNetworkAvailable())
            close(e)
        }

        awaitClose {
            try {
                connectivityManager.unregisterNetworkCallback(callback)
                Log.d(TAG, "Network callback unregistered")
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering network callback", e)
            }
        }
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)

    /**
     * Test connectivity to a specific host and port
     * Useful for testing PLC connectivity specifically
     */
    suspend fun testConnectivity(
        host: String,
        port: Int,
        timeoutMs: Int = DEFAULT_TIMEOUT_MS
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "No network available for connectivity test")
            return@withContext false
        }

        return@withContext try {
            withTimeout(timeoutMs.toLong()) {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), timeoutMs)
                    Log.d(TAG, "Successfully connected to $host:$port")
                    true
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Timeout connecting to $host:$port")
            false
        } catch (e: Exception) {
            Log.w(TAG, "Failed to connect to $host:$port - ${e.message}")
            false
        }
    }

    /**
     * Ping-like test using socket connection
     * Faster than full connectivity test
     */
    suspend fun pingHost(
        host: String,
        port: Int = 80,
        timeoutMs: Int = PING_TIMEOUT_MS
    ): Long = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext -1L
        }

        val startTime = System.currentTimeMillis()
        return@withContext try {
            withTimeout(timeoutMs.toLong()) {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), timeoutMs)
                    val latency = System.currentTimeMillis() - startTime
                    Log.d(TAG, "Ping to $host:$port = ${latency}ms")
                    latency
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Ping timeout to $host:$port")
            -1L
        } catch (e: Exception) {
            Log.w(TAG, "Ping failed to $host:$port - ${e.message}")
            -1L
        }
    }

    /**
     * Get current network type (WiFi, Cellular, Ethernet, etc.)
     */
    fun getNetworkType(): NetworkType {
        return try {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)

            capabilities?.let { caps ->
                when {
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkType.VPN
                    else -> NetworkType.UNKNOWN
                }
            } ?: NetworkType.NONE
        } catch (e: Exception) {
            Log.e(TAG, "Error getting network type", e)
            NetworkType.UNKNOWN
        }
    }

    /**
     * Get network strength/quality indicator (0-100)
     * Higher is better, -1 if unavailable
     */
    fun getNetworkQuality(): Int {
        return try {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)

            capabilities?.let { caps ->
                when {
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        // For WiFi, we could check signal strength but it requires location permission
                        // For now, return a basic quality check
                        if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) 85 else 60
                    }
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                        // Ethernet typically has best quality
                        95
                    }
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        // Basic cellular quality check
                        if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) 70 else 40
                    }
                    else -> 50
                }
            } ?: -1
        } catch (e: Exception) {
            Log.e(TAG, "Error getting network quality", e)
            -1
        }
    }

    /**
     * Check if network is metered (has data usage limits)
     */
    fun isNetworkMetered(): Boolean {
        return try {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if network is metered", e)
            false
        }
    }

    /**
     * Invalidate network status cache
     * Useful when you want to force a fresh network check
     */
    fun invalidateCache() {
        lastNetworkCheck = 0L
        cachedNetworkStatus = false
        Log.d(TAG, "Network status cache invalidated")
    }

    enum class NetworkType {
        NONE,
        WIFI,
        CELLULAR,
        ETHERNET,
        VPN,
        UNKNOWN
    }
}