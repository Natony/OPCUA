package com.example.s7opcuaapp.testing.unit

import com.example.s7opcuaapp.core.dispatchers.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class TestDispatcherProvider(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : DispatcherProvider {
    override val main: CoroutineDispatcher = testDispatcher
    override val io: CoroutineDispatcher = testDispatcher
    override val default: CoroutineDispatcher = testDispatcher
    override val unconfined: CoroutineDispatcher = testDispatcher
}

@OptIn(ExperimentalCoroutinesApi::class)
class StandardTestDispatcherProvider : DispatcherProvider {
    private val standardDispatcher = StandardTestDispatcher()
    override val main: CoroutineDispatcher = standardDispatcher
    override val io: CoroutineDispatcher = standardDispatcher
    override val default: CoroutineDispatcher = standardDispatcher
    override val unconfined: CoroutineDispatcher = standardDispatcher
}