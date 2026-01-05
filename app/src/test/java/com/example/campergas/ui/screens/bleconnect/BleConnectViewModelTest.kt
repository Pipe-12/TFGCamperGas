package com.example.campergas.ui.screens.bleconnect

import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.campergas.domain.model.BleDevice
import com.example.campergas.domain.usecase.CheckBleConnectionUseCase
import com.example.campergas.domain.usecase.ConnectBleDeviceUseCase
import com.example.campergas.domain.usecase.ScanBleDevicesUseCase
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class BleConnectViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: BleConnectViewModel
    private val scanBleDevicesUseCase: ScanBleDevicesUseCase = mockk()
    private val connectBleDeviceUseCase: ConnectBleDeviceUseCase = mockk()
    private val checkBleConnectionUseCase: CheckBleConnectionUseCase = mockk()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Mock Android Log
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { Log.i(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0

        // Setup basic mock responses
        every { checkBleConnectionUseCase() } returns MutableStateFlow(false)
        every { scanBleDevicesUseCase.isBluetoothEnabled() } returns true
        every { scanBleDevicesUseCase.isCompatibleFilterEnabled() } returns false
        every { scanBleDevicesUseCase.stopScan() } returns Unit
        every { scanBleDevicesUseCase.toggleCompatibleDevicesFilter() } returns Unit

        viewModel = BleConnectViewModel(
            scanBleDevicesUseCase,
            connectBleDeviceUseCase,
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
    fun `initial ui state is correct`() {
        // Assert
        val initialState = viewModel.uiState.value
        assertEquals(emptyList<BleDevice>(), initialState.availableDevices)
        assertNull(initialState.connectedDevice)
        assertFalse(initialState.isConnected)
        assertFalse(initialState.isScanning)
        assertNull(initialState.isConnecting)
        assertNull(initialState.error)
        assertFalse(initialState.showOnlyCompatibleDevices)
    }

    @Test
    fun `startScan sets isScanning to true`() = runTest {
        // Arrange
        every { scanBleDevicesUseCase() } returns MutableStateFlow(emptyList())

        // Act
        viewModel.startScan()
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertTrue(state.isScanning)
        assertNull(state.error)
    }

    @Test
    fun `startScan with bluetooth disabled shows error`() = runTest {
        // Arrange
        every { scanBleDevicesUseCase.isBluetoothEnabled() } returns false

        // Act
        viewModel.startScan()
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertFalse(state.isScanning)
        assertEquals("Bluetooth is not enabled", state.error)
    }

    @Test
    fun `startScan collects devices from use case`() = runTest {
        // Arrange
        val testDevices = listOf(
            BleDevice("Test Device 1", "00:11:22:33:44:55", -60),
            BleDevice("Test Device 2", "AA:BB:CC:DD:EE:FF", -70)
        )
        every { scanBleDevicesUseCase() } returns MutableStateFlow(testDevices)

        // Act
        viewModel.startScan()
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertEquals(testDevices, state.availableDevices)
        assertTrue(state.isScanning)
        assertNull(state.error)
    }

    @Test
    fun `stopScan sets isScanning to false`() = runTest {
        // Arrange - start scanning first
        every { scanBleDevicesUseCase() } returns MutableStateFlow(emptyList())
        viewModel.startScan()
        advanceUntilIdle()

        // Act
        viewModel.stopScan()
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertFalse(state.isScanning)
        verify { scanBleDevicesUseCase.stopScan() }
    }

    @Test
    fun `connectToDevice sets connecting state`() = runTest {
        // Arrange
        val testDevice = BleDevice("Test Device", "00:11:22:33:44:55", -65)
        coEvery { connectBleDeviceUseCase(any()) } returns Unit

        // Act
        viewModel.connectToDevice(testDevice)
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertEquals(testDevice, state.connectedDevice)
        assertNull(state.error)

        coVerify {
            connectBleDeviceUseCase(testDevice.address)
        }
    }

    @Test
    fun `connectToDevice with bluetooth disabled shows error`() = runTest {
        // Arrange
        val testDevice = BleDevice("Test Device", "00:11:22:33:44:55", -65)
        every { scanBleDevicesUseCase.isBluetoothEnabled() } returns false

        // Act
        viewModel.connectToDevice(testDevice)
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertEquals("Bluetooth is not enabled", state.error)
        assertNull(state.connectedDevice)
    }

    @Test
    fun `disconnectDevice clears connected device`() = runTest {
        // Arrange - first connect a device
        val testDevice = BleDevice("Test Device", "00:11:22:33:44:55", -65)
        coEvery { connectBleDeviceUseCase(any()) } returns Unit
        coEvery { connectBleDeviceUseCase.disconnect() } returns Unit

        viewModel.connectToDevice(testDevice)
        advanceUntilIdle()

        // Act
        viewModel.disconnectDevice()
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertNull(state.connectedDevice)
        assertNull(state.isConnecting)
        assertNull(state.error)
        assertEquals(emptyList<BleDevice>(), state.availableDevices)

        coVerify { connectBleDeviceUseCase.disconnect() }
    }

    @Test
    fun `clearError clears error state`() = runTest {
        // Arrange - set an error first
        every { scanBleDevicesUseCase.isBluetoothEnabled() } returns false
        viewModel.startScan()
        advanceUntilIdle()

        // Act
        viewModel.clearError()

        // Assert
        val state = viewModel.uiState.value
        assertNull(state.error)
    }

    @Test
    fun `isBluetoothEnabled delegates to use case`() {
        // Arrange
        every { scanBleDevicesUseCase.isBluetoothEnabled() } returns true

        // Act
        val result = viewModel.isBluetoothEnabled()

        // Assert
        assertTrue(result)
        verify { scanBleDevicesUseCase.isBluetoothEnabled() }
    }

    @Test
    fun `toggleCompatibleDevicesFilter updates state`() = runTest {
        // Arrange
        every { scanBleDevicesUseCase.isCompatibleFilterEnabled() } returns true

        // Act
        viewModel.toggleCompatibleDevicesFilter()
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertTrue(state.showOnlyCompatibleDevices)

        verify {
            scanBleDevicesUseCase.toggleCompatibleDevicesFilter()
            scanBleDevicesUseCase.isCompatibleFilterEnabled()
        }
    }

    @Test
    fun `connection state flow updates ui state`() = runTest {
        // Arrange
        val connectionStateFlow = MutableStateFlow(false)
        every { checkBleConnectionUseCase() } returns connectionStateFlow

        // Create new viewModel to trigger init block with the flow
        val newViewModel = BleConnectViewModel(
            scanBleDevicesUseCase,
            connectBleDeviceUseCase,
            checkBleConnectionUseCase
        )

        // Act - simulate connection state change
        connectionStateFlow.value = true
        advanceUntilIdle()

        // Assert
        val state = newViewModel.uiState.value
        assertTrue(state.isConnected)
    }
}
