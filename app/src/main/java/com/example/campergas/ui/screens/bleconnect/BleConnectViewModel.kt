package com.example.campergas.ui.screens.bleconnect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campergas.domain.model.BleDevice
import com.example.campergas.domain.usecase.CheckBleConnectionUseCase
import com.example.campergas.domain.usecase.ConnectBleDeviceUseCase
import com.example.campergas.domain.usecase.ScanBleDevicesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for BLE device connection management.
 *
 * Handles:
 * - BLE device scanning with permission checks
 * - Device connection/disconnection
 * - Connection state monitoring
 * - Error handling for BLE operations
 *
 * Provides reactive state flows for UI updates and coordinates with
 * BLE use cases to perform actual Bluetooth operations.
 */
@HiltViewModel
class BleConnectViewModel @Inject constructor(
    private val scanBleDevicesUseCase: ScanBleDevicesUseCase,
    private val connectBleDeviceUseCase: ConnectBleDeviceUseCase,
    checkBleConnectionUseCase: CheckBleConnectionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BleConnectUiState())

    /** Flow of UI state for the BLE connection screen */
    val uiState: StateFlow<BleConnectUiState> = _uiState.asStateFlow()

    /** Flow of current BLE connection state */
    val connectionState = checkBleConnectionUseCase()

    init {
        // Observe changes in connection state
        viewModelScope.launch {
            connectionState.collect { isConnected ->
                android.util.Log.d(
                    "BleConnectViewModel",
                    "🔄 Connection state changed to: $isConnected"
                )
                _uiState.value = _uiState.value.copy(
                    isConnected = isConnected,
                    isConnecting = if (isConnected) null else _uiState.value.isConnecting
                )
                android.util.Log.d(
                    "BleConnectViewModel",
                    "🔄 UI State actualizado - isConnected: ${_uiState.value.isConnected}"
                )
            }
        }
    }

    fun startScan() {
        // Verify we have permissions before scanning
        if (!scanBleDevicesUseCase.isBluetoothEnabled()) {
            _uiState.value = _uiState.value.copy(
                error = "Bluetooth is not enabled"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isScanning = true,
                error = null
            )
            try {
                scanBleDevicesUseCase().collect { devices ->
                    _uiState.value = _uiState.value.copy(
                        availableDevices = devices,
                        error = null
                    )
                }
            } catch (_: SecurityException) {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    error = "Permisos de Bluetooth requeridos for escanear devices"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    error = e.message ?: "Error al escanear devices"
                )
            }
        }
    }

    fun stopScan() {
        try {
            scanBleDevicesUseCase.stopScan()
            _uiState.value = _uiState.value.copy(isScanning = false)
        } catch (_: SecurityException) {
            _uiState.value = _uiState.value.copy(
                isScanning = false,
                error = "Permisos de Bluetooth requeridos for detener el escaneo"
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isScanning = false,
                error = e.message ?: "Error al detener el escaneo"
            )
        }
    }

    fun connectToDevice(device: BleDevice) {
        if (!scanBleDevicesUseCase.isBluetoothEnabled()) {
            _uiState.value = _uiState.value.copy(
                error = "Bluetooth is not enabled"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isConnecting = device.address,
                error = null
            )
            try {
                // Use the use case to connect and save device
                connectBleDeviceUseCase(device.address)

                _uiState.value = _uiState.value.copy(
                    connectedDevice = device,
                    error = null
                )
            } catch (_: SecurityException) {
                _uiState.value = _uiState.value.copy(
                    isConnecting = null,
                    error = "Permisos de Bluetooth requeridos for conectar"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isConnecting = null,
                    error = e.message ?: "Error connecting with the device"
                )
            }
        }
    }

    fun disconnectDevice() {
        viewModelScope.launch {
            try {
                android.util.Log.d("BleConnectViewModel", "🔌 Starting disconnection from ViewModel")

                // Stop scan if active
                if (_uiState.value.isScanning) {
                    android.util.Log.d(
                        "BleConnectViewModel",
                        "🔌 Deteniendo escaneo antes de desconectar"
                    )
                    stopScan()
                }

                android.util.Log.d(
                    "BleConnectViewModel",
                    "🔌 Calling connectBleDeviceUseCase.disconnect()"
                )
                // Disconnect from device - state will update automatically
                // through observable connectionState from use case
                connectBleDeviceUseCase.disconnect()

                android.util.Log.d("BleConnectViewModel", "🔌 Cleaning local ViewModel state")
                // Only clear local UI data, not connection state
                _uiState.value = _uiState.value.copy(
                    connectedDevice = null,
                    isConnecting = null,
                    error = null,
                    availableDevices = emptyList() // Limpiar lista for forzar nuevo escaneo
                )

                android.util.Log.d(
                    "BleConnectViewModel",
                    "🔌 Disconnection completed from ViewModel"
                )

            } catch (e: Exception) {
                android.util.Log.e("BleConnectViewModel", "🔌 Error al desconectar: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Error al desconectar device"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun isBluetoothEnabled(): Boolean {
        return scanBleDevicesUseCase.isBluetoothEnabled()
    }

    // Filter management
    fun toggleCompatibleDevicesFilter() {
        scanBleDevicesUseCase.toggleCompatibleDevicesFilter()
        _uiState.value = _uiState.value.copy(
            showOnlyCompatibleDevices = scanBleDevicesUseCase.isCompatibleFilterEnabled()
        )
    }
}

data class BleConnectUiState(
    val availableDevices: List<BleDevice> = emptyList(),
    val connectedDevice: BleDevice? = null,
    val isConnected: Boolean = false,
    val isScanning: Boolean = false,
    val isConnecting: String? = null, // MAC address of the device that is connecting
    val error: String? = null,
    val showOnlyCompatibleDevices: Boolean = false
)
