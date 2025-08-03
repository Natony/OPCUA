package com.example.s7opcuaapp.testing.unit

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestRule

/**
 * Base class for unit tests with common setup
 * Provides:
 * - Coroutine test support
 * - MockK initialization
 * - LiveData testing support
 */
@ExperimentalCoroutinesApi
abstract class BaseUnitTest {

    @get:Rule
    val instantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    protected val testScope = TestScope(UnconfinedTestDispatcher())
    protected val testDispatcher = coroutineTestRule.testDispatcher

    @Before
    open fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        onSetUp()
    }

    @After
    open fun tearDown() {
        onTearDown()
        clearAllMocks()
        unmockkAll()
    }

    /**
     * Override this to add custom setup logic
     */
    protected open fun onSetUp() {}

    /**
     * Override this to add custom teardown logic
     */
    protected open fun onTearDown() {}

    /**
     * Run a test with coroutine support
     */
    protected fun runBlockingTest(block: suspend TestScope.() -> Unit) = runTest {
        block()
    }
}