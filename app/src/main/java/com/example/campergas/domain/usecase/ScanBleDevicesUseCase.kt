package com.example.campergas.domain.usecase

import android.Manifest
import androidx.annotation.RequiresPermission
import com.example.campergas.data.repository.BleRepository
import com.example.campergas.domain.model.BleDevice
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Use case for scanning available Bluetooth Low Energy (BLE) devices.
 *
 * This use case encapsulates the business logic for BLE device scanning,
 * including:
 * - Starting and stopping scans
 * - Filtering CamperGas-compatible devices
 * - Bluetooth state verification
 * - Real-time access to scan results
 *
 * Permission requirements:
 * - Android 12+: BLUETOOTH_SCAN
 * - Android 11 and earlier: BLUETOOTH_ADMIN + ACCESS_FINE_LOCATION
 *
 * Filter functionality:
 * - Enabled: Only shows devices with CamperGas services
 * - Disabled: Shows all BLE devices found
 *
 * Main use cases:
 * - Search for nearby CamperGas sensors to connect
 * - Discover BLE devices in the environment
 * - Filter incompatible devices to simplify the UI
 *
 * @property bleRepository BLE repository that manages scanning and connection
 * @author Felipe García Gómez
 */
class ScanBleDevicesUseCase @Inject constructor(
    private val bleRepository: BleRepository
) {
    /**
     * Starts scanning for BLE devices and returns the results state.
     *
     * Begins searching for nearby BLE devices and returns a StateFlow that
     * emits the list of devices found. The list updates automatically
     * as new devices are discovered or information is updated for
     * already found devices.
     *
     * Requires BLE scanning permissions according to Android version.
     *
     * @return StateFlow that emits the updated list of devices found
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    operator fun invoke(): StateFlow<List<BleDevice>> {
        bleRepository.startScan()
        return bleRepository.scanResults
    }

    /**
     * Stops scanning for BLE devices.
     *
     * Cancels the active scan to save battery. Devices already
     * found remain in the results list.
     *
     * Requires BLE scanning permissions according to Android version.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScan() {
        bleRepository.stopScan()
    }

    /**
     * Checks if Bluetooth is enabled on the device.
     *
     * @return true if Bluetooth is on, false if off or unavailable
     */
    fun isBluetoothEnabled(): Boolean {
        return bleRepository.isBluetoothEnabled()
    }

    /**
     * Checks if the compatibility filter is currently active.
     *
     * @return true if only compatible devices are shown, false if all are shown
     */
    fun isCompatibleFilterEnabled(): Boolean {
        return bleRepository.isCompatibleFilterEnabled()
    }

    /**
     * Toggles the compatibility filter state.
     *
     * If the filter is active, deactivates it. If inactive, activates it.
     * Provides a convenient way to switch between display modes.
     */
    fun toggleCompatibleDevicesFilter() {
        val currentState = bleRepository.isCompatibleFilterEnabled()
        bleRepository.setCompatibleDevicesFilter(!currentState)
    }
}
