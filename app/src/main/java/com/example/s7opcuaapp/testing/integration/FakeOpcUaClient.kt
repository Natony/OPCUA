package com.example.s7opcuaapp.testing.integration

import com.example.s7opcuaapp.data.model.PlcData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Fake OPC UA client for testing
 * Simulates PLC behavior without real connection
 */
class FakeOpcUaClient {

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val nodeValues = ConcurrentHashMap<String, Any>()
    private val subscribers = ConcurrentHashMap<String, (DataValue) -> Unit>()

    // Configuration
    var shouldFailConnection = false
    var connectionDelay = 100L
    var writeDelay = 50L
    var shouldFailWrites = false

    // Metrics
    var connectAttempts = 0
    var writeCount = 0
    var subscriptionCount = 0

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR
    }

    init {
        // Initialize default values
        repeat(15) { i ->
            nodeValues["ns=4;i=${i + 3}"] = false
        }
        repeat(28) { i ->
            nodeValues["ns=4;i=${i + 18}"] = 0
        }
    }

    suspend fun connect(
        ipAddress: String,
        port: Int,
        username: String? = null,
        password: String? = null
    ): Boolean {
        connectAttempts++
        _connectionState.value = ConnectionState.CONNECTING

        delay(connectionDelay)

        return if (shouldFailConnection) {
            _connectionState.value = ConnectionState.ERROR
            false
        } else {
            _connectionState.value = ConnectionState.CONNECTED
            // Start simulation
            startSimulation()
            true
        }
    }

    fun disconnect() {
        _connectionState.value = ConnectionState.DISCONNECTED
        subscribers.clear()
    }

    suspend fun writeNode(nodeId: String, value: Any): StatusCode {
        writeCount++

        if (_connectionState.value != ConnectionState.CONNECTED) {
            return StatusCode.BAD
        }

        delay(writeDelay)

        return if (shouldFailWrites) {
            StatusCode.BAD
        } else {
            nodeValues[nodeId] = value
            // Notify subscribers
            notifySubscribers(nodeId, value)
            StatusCode.GOOD
        }
    }

    fun subscribe(nodeId: String, callback: (DataValue) -> Unit) {
        subscriptionCount++
        subscribers[nodeId] = callback

        // Send initial value
        nodeValues[nodeId]?.let { value ->
            callback(DataValue(Variant(value)))
        }
    }

    fun unsubscribe(nodeId: String) {
        subscribers.remove(nodeId)
    }

    fun isConnected(): Boolean {
        return _connectionState.value == ConnectionState.CONNECTED
    }

    /**
     * Update values for testing
     */
    fun updateValue(nodeId: String, value: Any) {
        nodeValues[nodeId] = value
        notifySubscribers(nodeId, value)
    }

    /**
     * Get current PlcData
     */
    fun getCurrentPlcData(): PlcData {
        val bools = (3..17).map { i ->
            nodeValues["ns=4;i=$i"] as? Boolean ?: false
        }
        val ints = (18..45).map { i ->
            nodeValues["ns=4;i=$i"] as? Int ?: 0
        }
        return PlcData(bools = bools, ints = ints)
    }

    /**
     * Simulate realistic PLC behavior
     */
    fun simulateStatusChange(status: Int) {
        updateValue("ns=4;i=18", status)
    }

    fun simulateBatteryLevel(level: Int) {
        updateValue("ns=4;i=19", level)
    }

    fun simulateEmergencyStop() {
        updateValue("ns=4;i=13", true)
        updateValue("ns=4;i=18", 11) // Emergency status
    }

    private fun notifySubscribers(nodeId: String, value: Any) {
        subscribers[nodeId]?.invoke(DataValue(Variant(value)))
    }

    private suspend fun startSimulation() {
        // Simulate periodic value changes
        kotlinx.coroutines.GlobalScope.launch {
            while (_connectionState.value == ConnectionState.CONNECTED) {
                delay(1000)
                // Simulate random changes
                if (Random.nextBoolean()) {
                    val randomNode = "ns=4;i=${Random.nextInt(3, 18)}"
                    updateValue(randomNode, Random.nextBoolean())
                }
            }
        }
    }

    /**
     * Reset all values to defaults
     */
    fun reset() {
        nodeValues.clear()
        repeat(15) { i ->
            nodeValues["ns=4;i=${i + 3}"] = false
        }
        repeat(28) { i ->
            nodeValues["ns=4;i=${i + 18}"] = 0
        }

        shouldFailConnection = false
        shouldFailWrites = false
        connectAttempts = 0
        writeCount = 0
        subscriptionCount = 0
    }
}