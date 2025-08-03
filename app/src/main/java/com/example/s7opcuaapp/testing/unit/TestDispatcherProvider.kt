package com.example.s7opcuaapp.testing.unit

import com.example.s7opcuaapp.core.dispatchers.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/**
 * Test implementation of DispatcherProvider for unit tests
 * Allows tests to control coroutine execution
 */
@ExperimentalCoroutinesApi
class TestDispatcherProvider(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : DispatcherProvider {

    override val main: CoroutineDispatcher = testDispatcher
    override val io: CoroutineDispatcher = testDispatcher
    override val default: CoroutineDispatcher = testDispatcher
    override val unconfined: CoroutineDispatcher = testDispatcher
}

/**
 * Create TestDispatcherProvider with UnconfinedTestDispatcher
 * for immediate execution in tests
 */
@ExperimentalCoroutinesApi
fun createUnconfinedTestDispatcherProvider() = TestDispatcherProvider(
    UnconfinedTestDispatcher()
)

/**
 * Create TestDispatcherProvider with StandardTestDispatcher
 * for controlled execution in tests
 */
@ExperimentalCoroutinesApi
fun createStandardTestDispatcherProvider() = TestDispatcherProvider(
    StandardTestDispatcher()
)