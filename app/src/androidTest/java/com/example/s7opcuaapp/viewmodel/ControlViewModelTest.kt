package com.example.s7opcuaapp.viewmodel

import com.example.s7opcuaapp.presentation.coordinator.ControlCoordinator
import com.example.s7opcuaapp.testing.unit.BaseUnitTest
import com.example.s7opcuaapp.testing.unit.TestDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class ControlViewModelTest : BaseUnitTest() {

    private val mockCoordinator = mockk<ControlCoordinator>(relaxed = true)

    private lateinit var viewModel: ControlViewModel

    override fun setUp() {
        super.setUp()
        viewModel = ControlViewModel(mockCoordinator)
    }

    @Test
    fun testStartConnectionCallsCoordinator() = runBlockingTest {
        // When
        viewModel.startConnection()

        // Then
        coVerify { mockCoordinator.startConnection() }
    }

    @Test
    fun testToggleBooleanDelegatesToCoordinator() = runBlockingTest {
        // Given
        val index = 5
        val value = true

        // When
        viewModel.onToggleBoolean(index, value)

        // Then
        coVerify { mockCoordinator.toggleBoolean(index, value) }
    }

    @Test
    fun testOpenNumberDialogUpdatesState() {
        // Given
        val title = "Test Dialog"
        val index = 10

        // When
        viewModel.openNumberDialog(title, index)

        // Then
        val state = viewModel.dialogState.value
        assertEquals(index, state.openDialogForIndex)
        assertEquals(title, state.dialogTitle)
    }

    @Test
    fun testDismissDialogClearsState() {
        // Given
        viewModel.openNumberDialog("Test", 10)

        // When
        viewModel.dismissDialog()

        // Then
        assertNull(viewModel.dialogState.value.openDialogForIndex)
    }
}