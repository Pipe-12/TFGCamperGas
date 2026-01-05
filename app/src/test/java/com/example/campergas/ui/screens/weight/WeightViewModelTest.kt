package com.example.campergas.ui.screens.weight

import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.campergas.domain.model.FuelMeasurement
import com.example.campergas.domain.model.GasCylinder
import com.example.campergas.domain.model.VehicleConfig
import com.example.campergas.domain.model.VehicleType
import com.example.campergas.domain.usecase.CheckBleConnectionUseCase
import com.example.campergas.domain.usecase.GetActiveCylinderUseCase
import com.example.campergas.domain.usecase.GetFuelDataUseCase
import com.example.campergas.domain.usecase.GetVehicleConfigUseCase
import com.example.campergas.domain.usecase.RequestWeightDataUseCase
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class WeightViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: WeightViewModel
    private val getFuelDataUseCase: GetFuelDataUseCase = mockk()
    private val getVehicleConfigUseCase: GetVehicleConfigUseCase = mockk()
    private val getActiveCylinderUseCase: GetActiveCylinderUseCase = mockk()
    private val requestWeightDataUseCase: RequestWeightDataUseCase = mockk()
    private val checkBleConnectionUseCase: CheckBleConnectionUseCase = mockk()

    private val testDispatcher = UnconfinedTestDispatcher()

    // Flows for simulated data
    private val fuelDataFlow = MutableStateFlow<FuelMeasurement?>(null)
    private val vehicleConfigFlow = MutableStateFlow<VehicleConfig?>(null)
    private val activeCylinderFlow = MutableStateFlow<GasCylinder?>(null)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Mock Android Log
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0

        // Setup mock responses
        every { getFuelDataUseCase() } returns fuelDataFlow
        every { getVehicleConfigUseCase() } returns vehicleConfigFlow
        every { getActiveCylinderUseCase() } returns activeCylinderFlow
        every { requestWeightDataUseCase() } returns Unit
        every { checkBleConnectionUseCase.isConnected() } returns true

        viewModel = WeightViewModel(
            getFuelDataUseCase,
            getVehicleConfigUseCase,
            getActiveCylinderUseCase,
            requestWeightDataUseCase,
            checkBleConnectionUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
        clearAllMocks()
    }

    @Test
    fun `initial state is null for all values`() = runTest {
        assertNull(viewModel.fuelState.value)
        assertNull(viewModel.vehicleState.value)
        assertNull(viewModel.activeCylinder.value)
        assertFalse(viewModel.isRequestingData.value)
    }

    @Test
    fun `updates fuel state when data is received`() = runTest {
        val testFuel = FuelMeasurement(
            cylinderId = 1L,
            cylinderName = "Test Cylinder",
            timestamp = 12345L,
            fuelKilograms = 5.0f,
            fuelPercentage = 50.0f,
            totalWeight = 10.0f,
            isCalibrated = true
        )
        fuelDataFlow.value = testFuel
        advanceUntilIdle()
        assertEquals(testFuel, viewModel.fuelState.value)
    }

    @Test
    fun `updates vehicle state when data is received`() = runTest {
        val testVehicle = VehicleConfig(
            type = VehicleType.CARAVAN,
            distanceBetweenRearWheels = 180f,
            distanceToFrontSupport = 350f
        )
        vehicleConfigFlow.value = testVehicle
        advanceUntilIdle()
        assertEquals(testVehicle, viewModel.vehicleState.value)
    }

    @Test
    fun `updates active cylinder when data is received`() = runTest {
        val testCylinder = GasCylinder(
            id = 1L,
            name = "Test Cylinder",
            tare = 5.0f,
            capacity = 10.0f,
            isActive = true
        )
        activeCylinderFlow.value = testCylinder
        advanceUntilIdle()
        assertEquals(testCylinder, viewModel.activeCylinder.value)
    }

    @Test
    fun `requestWeightDataManually calls the use case and updates isRequestingData state`() =
        runTest {

            viewModel.requestWeightDataManually()
            verify { requestWeightDataUseCase() }
            assertTrue(viewModel.isRequestingData.value)
            advanceTimeBy(1600)
            assertFalse(viewModel.isRequestingData.value)
        }

    @Test
    fun `requestWeightDataManually blocks repeated rapid calls`() = runTest {

        viewModel.requestWeightDataManually()


        viewModel.requestWeightDataManually()


        verify(exactly = 1) { requestWeightDataUseCase() }
    }

    @Test
    fun `isConnected delegates to CheckBleConnectionUseCase`() {

        every { checkBleConnectionUseCase.isConnected() } returns true
        val result = viewModel.isConnected()
        assertTrue(result)
        verify { checkBleConnectionUseCase.isConnected() }
    }

    @Test
    fun `canMakeRequest returns false when request is in progress`() = runTest {

        assertTrue(viewModel.canMakeRequest())
        viewModel.requestWeightDataManually()
        assertFalse(viewModel.canMakeRequest())
        advanceTimeBy(1600)
        assertFalse(viewModel.isRequestingData.value)


    }


}
