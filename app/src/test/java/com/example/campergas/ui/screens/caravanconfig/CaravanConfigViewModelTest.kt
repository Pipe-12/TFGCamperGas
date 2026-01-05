package com.example.campergas.ui.screens.caravanconfig

import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.campergas.domain.model.VehicleConfig
import com.example.campergas.domain.model.VehicleType
import com.example.campergas.domain.usecase.GetVehicleConfigUseCase
import com.example.campergas.domain.usecase.SaveVehicleConfigUseCase
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class CaravanConfigViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: CaravanConfigViewModel
    private val getVehicleConfigUseCase: GetVehicleConfigUseCase = mockk()
    private val saveVehicleConfigUseCase: SaveVehicleConfigUseCase = mockk()

    private val testDispatcher = UnconfinedTestDispatcher()

    // Flow to simulate vehicle configuration
    private val vehicleConfigFlow = MutableStateFlow<VehicleConfig?>(null)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Mock Android Log
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0

        // Setup mock responses
        every { getVehicleConfigUseCase() } returns vehicleConfigFlow
        coEvery { saveVehicleConfigUseCase(any(), any(), any(), any()) } returns Unit

        viewModel = CaravanConfigViewModel(
            getVehicleConfigUseCase,
            saveVehicleConfigUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
        clearAllMocks()
    }

    @Test
    fun `initial state has default values`() {
        // Assert
        val state = viewModel.uiState.value
        assertEquals(VehicleType.CARAVAN, state.selectedVehicleType)
        assertEquals(250f, state.distanceBetweenWheels) // 2.5 metros en cm
        assertEquals(0f, state.distanceToFrontSupport)
        assertEquals(0f, state.distanceBetweenFrontWheels)
        assertFalse(state.isLoading)
        assertFalse(state.isSaving)
        assertNull(state.error)
    }

    @Test
    fun `loadVehicleConfig updates state with config values`() = runTest {
        // Arrange
        val testConfig = VehicleConfig(
            type = VehicleType.AUTOCARAVANA,
            distanceBetweenRearWheels = 180f,
            distanceToFrontSupport = 350f,
            distanceBetweenFrontWheels = 160f
        )

        // Act
        vehicleConfigFlow.value = testConfig
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertEquals(VehicleType.AUTOCARAVANA, state.selectedVehicleType)
        assertEquals(180f, state.distanceBetweenWheels)
        assertEquals(350f, state.distanceToFrontSupport)
        assertEquals(160f, state.distanceBetweenFrontWheels)
        assertFalse(state.isLoading)
    }

    @Test
    fun `updateVehicleType updates state with new vehicle type`() {
        // Act
        viewModel.updateVehicleType(VehicleType.AUTOCARAVANA)

        // Assert
        assertEquals(VehicleType.AUTOCARAVANA, viewModel.uiState.value.selectedVehicleType)
    }

    @Test
    fun `updateDistanceBetweenWheels updates state with new distance`() {
        // Act
        viewModel.updateDistanceBetweenWheels(200f)

        // Assert
        assertEquals(200f, viewModel.uiState.value.distanceBetweenWheels)
    }

    @Test
    fun `updateDistanceToFrontSupport updates state with new distance`() {
        // Act
        viewModel.updateDistanceToFrontSupport(300f)

        // Assert
        assertEquals(300f, viewModel.uiState.value.distanceToFrontSupport)
    }

    @Test
    fun `updateDistanceBetweenFrontWheels updates state with new distance`() {
        // Act
        viewModel.updateDistanceBetweenFrontWheels(150f)

        // Assert
        assertEquals(150f, viewModel.uiState.value.distanceBetweenFrontWheels)
    }

    @Test
    fun `saveConfiguration calls use case with correct parameters for Caravan`() = runTest {
        // Arrange
        viewModel.updateVehicleType(VehicleType.CARAVAN)
        viewModel.updateDistanceBetweenWheels(200f)
        viewModel.updateDistanceToFrontSupport(350f)

        // Act
        viewModel.saveConfiguration()
        advanceUntilIdle()

        // Assert
        coVerify {
            saveVehicleConfigUseCase(
                type = VehicleType.CARAVAN,
                distanceBetweenRearWheels = 200f,
                distanceToFrontSupport = 350f,
                distanceBetweenFrontWheels = null
            )
        }

        assertFalse(viewModel.uiState.value.isSaving)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `saveConfiguration calls use case with correct parameters for Autocaravana`() = runTest {
        // Arrange
        viewModel.updateVehicleType(VehicleType.AUTOCARAVANA)
        viewModel.updateDistanceBetweenWheels(200f)
        viewModel.updateDistanceToFrontSupport(350f)
        viewModel.updateDistanceBetweenFrontWheels(150f)

        // Act
        viewModel.saveConfiguration()
        advanceUntilIdle()

        // Assert
        coVerify {
            saveVehicleConfigUseCase(
                type = VehicleType.AUTOCARAVANA,
                distanceBetweenRearWheels = 200f,
                distanceToFrontSupport = 350f,
                distanceBetweenFrontWheels = 150f
            )
        }

        assertFalse(viewModel.uiState.value.isSaving)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `saveConfiguration handles exceptions`() = runTest {
        // Arrange
        val errorMessage = "Error guardando configuración"
        coEvery {
            saveVehicleConfigUseCase(any(), any(), any(), any())
        } throws Exception(errorMessage)

        // Act
        viewModel.saveConfiguration()
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `loadVehicleConfig handles exceptions`() = runTest {
        // Arrange
        val errorMessage = "Error cargando configuración"
        every { getVehicleConfigUseCase() } throws Exception(errorMessage)

        // Act - Create a new viewModel to trigger init/loadVehicleConfig
        val newViewModel = CaravanConfigViewModel(
            getVehicleConfigUseCase,
            saveVehicleConfigUseCase
        )
        advanceUntilIdle()

        // Assert
        assertEquals(errorMessage, newViewModel.uiState.value.error)
        assertFalse(newViewModel.uiState.value.isLoading)
    }
}
