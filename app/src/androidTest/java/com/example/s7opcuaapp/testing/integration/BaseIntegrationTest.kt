package com.example.s7opcuaapp.testing.integration

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.s7opcuaapp.testing.unit.CoroutineTestRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
abstract class BaseIntegrationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val coroutineTestRule = CoroutineTestRule()

    protected val context: Context
        get() = ApplicationProvider.getApplicationContext()

    protected val application: Application
        get() = ApplicationProvider.getApplicationContext()

    @Before
    open fun setUp() {
        hiltRule.inject()
    }

    @After
    open fun tearDown() {
        // Cleanup
    }

    protected fun runIntegrationTest(block: suspend () -> Unit) = runTest {
        block()
    }
}