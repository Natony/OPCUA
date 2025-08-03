package com.example.s7opcuaapp.testing.unit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.eclipse.milo.opcua.stack.core.util.annotations.Description
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit rule that configures coroutines for testing
 * - Replaces Main dispatcher with TestDispatcher
 * - Provides test control over coroutine execution
 */
@ExperimentalCoroutinesApi
class CoroutineTestRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        super.starting(description)
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        super.finished(description)
        Dispatchers.resetMain()
    }

    /**
     * Run pending coroutines manually
     */
    fun runCurrent() {
        if (testDispatcher is StandardTestDispatcher) {
            testDispatcher.scheduler.runCurrent()
        }
    }

    /**
     * Auto-advance time by the specified amount
     */
    fun advanceTimeBy(delayTimeMillis: Long) {
        testDispatcher.scheduler.advanceTimeBy(delayTimeMillis)
    }

    /**
     * Auto-advance time until all coroutines are idle
     */
    fun advanceUntilIdle() {
        testDispatcher.scheduler.advanceUntilIdle()
    }
}