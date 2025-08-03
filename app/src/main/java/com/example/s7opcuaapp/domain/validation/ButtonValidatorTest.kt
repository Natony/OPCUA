package com.example.s7opcuaapp.domain.validation

import com.example.s7opcuaapp.data.model.PlcData
import com.example.s7opcuaapp.domain.state.PlcStateManager
import com.example.s7opcuaapp.testing.unit.BaseUnitTest
import com.example.s7opcuaapp.util.ButtonLockConfig
import com.example.s7opcuaapp.util.StatusLockConfig
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@ExperimentalCoroutinesApi
class ButtonValidatorTest : BaseUnitTest() {

    private lateinit var validator: ButtonValidator
    private lateinit var stateManager: PlcStateManager
    private lateinit var buttonLockConfig: ButtonLockConfig
    private lateinit var statusLockConfig: StatusLockConfig

    private val plcDataFlow = MutableStateFlow(PlcData.empty())

    override fun onSetUp() {
        stateManager = mockk {
            every { plcData } returns plcDataFlow
            every { isOfflineMode() } returns false
            every { getCurrentStatus() } returns 0
        }

        statusLockConfig = mockk {
            every { getLockedButtonsForStatus(any()) } returns emptySet()
        }

        buttonLockConfig = mockk {
            every { getLockedButtons(any(), any()) } returns emptySet()
        }

        validator = ButtonValidatorImpl(stateManager, buttonLockConfig, statusLockConfig)
    }

    @Test
    fun `validateButtonPress should fail in offline mode`() = runTest {
        // Given
        every { stateManager.isOfflineMode() } returns true

        // When
        val result = validator.validateButtonPress(0, emptySet())

        // Then
        assertTrue(result.isFailure)
        assertEquals("Cannot control in offline mode", result.exceptionOrNull()?.message)
    }

    @Test
    fun `validateButtonPress should fail when button locked by status`() = runTest {
        // Given
        val buttonIndex = 5
        every { statusLockConfig.getLockedButtonsForStatus(0) } returns setOf(buttonIndex)

        // When
        val result = validator.validateButtonPress(buttonIndex, emptySet())

        // Then
        assertTrue(result.isFailure)
        assertEquals("Button locked by system status", result.exceptionOrNull()?.message)
    }

    @Test
    fun `validateButtonPress should fail when button locked by group`() = runTest {
        // Given
        val buttonIndex = 2
        val activeButtons = setOf(1)
        every { buttonLockConfig.getLockedButtons(activeButtons, emptySet()) } returns setOf(buttonIndex)

        // When
        val result = validator.validateButtonPress(buttonIndex, activeButtons)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Button locked by group conflict", result.exceptionOrNull()?.message)
    }

    @Test
    fun `validateButtonPress should fail when another manual button is pressed`() = runTest {
        // Given
        val buttonIndex = 0 // Manual forward
        val activeButtons = setOf(1) // Manual reverse already pressed

        // When
        val result = validator.validateButtonPress(buttonIndex, activeButtons)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Another manual movement button is already pressed", result.exceptionOrNull()?.message)
    }

    @Test
    fun `validateButtonPress should succeed when valid`() = runTest {
        // Given
        val buttonIndex = 4 // Power button
        val activeButtons = emptySet<Int>()

        // When
        val result = validator.validateButtonPress(buttonIndex, activeButtons)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `validateIntegerWrite should fail in offline mode`() = runTest {
        // Given
        every { stateManager.isOfflineMode() } returns true

        // When
        val result = validator.validateIntegerWrite(3, 100)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Cannot control in offline mode", result.exceptionOrNull()?.message)
    }

    @Test
    fun `validateIntegerWrite should fail when value out of range`() = runTest {
        // Given
        val index = 3 // Pallet count (0-999)
        val value = 1500 // Out of range

        // When
        val result = validator.validateIntegerWrite(index, value)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("out of range") == true)
    }

    @Test
    fun `validateIntegerWrite should succeed when value in range`() = runTest {
        // Given
        val index = 5 // Coordinate (-9999 to 9999)
        val value = -500

        // When
        val result = validator.validateIntegerWrite(index, value)

        // Then
        assertTrue(result.isSuccess)
    }
}