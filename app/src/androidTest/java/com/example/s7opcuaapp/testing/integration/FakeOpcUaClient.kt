package com.example.s7opcuaapp.testing.integration

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant
import java.util.concurrent.CompletableFuture

class FakeOpcUaClient {

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState

    private val nodeValues = mutableMapOf<String, Any>()
    private val subscriptions = mutableMapOf<String, (DataValue) -> Unit>()

    var simulateConnectionFailure = false
    var simulateWriteFailure = false
    var connectionDelay = 100L
    var writeDelay = 50L

    suspend fun connect(): Boolean {
        delay(connectionDelay)
        return if (!simulateConnectionFailure) {
            _connectionState.value = true
            true
        } else {
            false
        }
    }

    suspend fun disconnect() {
        _connectionState.value = false
        subscriptions.clear()
    }

    suspend fun writeValue(nodeId: String, value: Any): StatusCode {
        delay(writeDelay)
        return if (!simulateWriteFailure) {
            nodeValues[nodeId] = value
            // Notify subscribers
            subscriptions[nodeId]?.let { callback ->
                val dataValue = DataValue(Variant(value))
                callback(dataValue)
            }
            StatusCode.GOOD
        } else {
            StatusCode.BAD
        }
    }

    fun subscribe(nodeId: String, callback: (DataValue) -> Unit) {
        subscriptions[nodeId] = callback
        // Send initial value
        nodeValues[nodeId]?.let { value ->
            callback(DataValue(Variant(value)))
        }
    }

    fun simulateValueChange(nodeId: String, value: Any) {
        nodeValues[nodeId] = value
        subscriptions[nodeId]?.let { callback ->
            callback(DataValue(Variant(value)))
        }
    }

    fun reset() {
        nodeValues.clear()
        subscriptions.clear()
        _connectionState.value = false
        simulateConnectionFailure = false
        simulateWriteFailure = false
    }
}