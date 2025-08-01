package com.example.s7opcuaapp.core.dispatchers

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Interface for providing coroutine dispatchers.
 * Allows for easy testing by providing test dispatchers.
 */
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val unconfined: CoroutineDispatcher
}

/**
 * Standard implementation using actual dispatchers
 */
class StandardDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
}

/**
 * Test implementation for unit tests
 */
class TestDispatcherProvider(
    override val main: CoroutineDispatcher = Dispatchers.Unconfined,
    override val io: CoroutineDispatcher = Dispatchers.Unconfined,
    override val default: CoroutineDispatcher = Dispatchers.Unconfined,
    override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
) : DispatcherProvider