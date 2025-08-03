package com.example.s7opcuaapp.testing.integration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.s7opcuaapp.data.local.AppDatabase
import com.example.s7opcuaapp.testing.unit.CoroutineTestRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

/**
 * Base class for integration tests
 * Provides:
 * - In-memory database
 * - Hilt dependency injection
 * - Coroutine test support
 */
@ExperimentalCoroutinesApi
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
abstract class BaseIntegrationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val coroutineTestRule = CoroutineTestRule()

    protected lateinit var context: Context
    protected lateinit var database: AppDatabase

    @Before
    open fun setUp() {
        hiltRule.inject()

        context = ApplicationProvider.getApplicationContext()

        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        onSetUp()
    }

    @After
    open fun tearDown() {
        onTearDown()
        database.close()
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
     * Helper to run database operations
     */
    protected suspend fun <T> runDatabaseOperation(block: suspend AppDatabase.() -> T): T {
        return database.block()
    }
}