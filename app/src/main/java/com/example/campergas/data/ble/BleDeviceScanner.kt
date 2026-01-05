package com.example.campergas.data.ble

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import androidx.annotation.RequiresPermission
import com.example.campergas.domain.model.BleDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Bluetooth Low Energy (BLE) device scanner.
 *
 * This class manages scanning of available BLE devices in the environment,
 * allowing discovery of sensors compatible with CamperGas. Provides functionality
 * to start and stop scanning, as well as filter compatible devices.
 *
 * Main features:
 * - Continuous scanning of nearby BLE devices
 * - Real-time update of the discovered devices list
 * - Optional filtering of CamperGas-compatible devices
 * - Detection of RSSI (signal strength) information and available services
 * - Automatic management of duplicates by updating already found devices
 *
 * @property bleManager Bluetooth manager that provides access to the BLE adapter
 * @author Felipe García Gómez
 */
class BleDeviceScanner @Inject constructor(
    private val bleManager: BleManager
) {
    /**
     * List of BLE devices found during scanning.
     *
     * Automatically updated when new devices are found
     * or when information is updated for already found devices.
     * Devices are filtered according to the compatibility filter state.
     */
    private val _scanResults = MutableStateFlow<List<BleDevice>>(emptyList())
    val scanResults: StateFlow<List<BleDevice>> = _scanResults

    /**
     * Current scanning state.
     *
     * Indicates if the scanner is currently searching for BLE devices (true)
     * or if scanning is stopped (false).
     */
    private val _isScanning = MutableStateFlow(false)

    /**
     * Filter to show only CamperGas-compatible devices.
     *
     * When active (true), only devices advertising the specific
     * CamperGas service UUIDs are included in the results.
     */
    private var showOnlyCompatibleDevices = false

    /**
     * Bluetooth adapter obtained from the BLE manager.
     *
     * Provides access to the device's Bluetooth functionality.
     */
    private val bluetoothAdapter: BluetoothAdapter? get() = bleManager.bluetoothAdapter

    /**
     * BLE scanner obtained from the Bluetooth adapter.
     *
     * Used to start and stop BLE scanning operations.
     */
    private var scanner: BluetoothLeScanner? = null

    /**
     * Callback that processes BLE scan results.
     *
     * This callback is invoked every time a BLE device is found during
     * scanning. Extracts device information (name, address, RSSI, services)
     * and adds or updates it in the results list.
     */
    private val scanCallback = object : ScanCallback() {
        /**
         * Called when a BLE scan result is found.
         *
         * Processes the found device, extracts its information, and adds
         * it to the results list if it passes the active filters.
         *
         * @param callbackType Type of callback (match found, lost, or update)
         * @param result Scan result containing device information
         */
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val deviceName = device.name ?: "Unknown device"
            val deviceAddress = device.address
            val rssi = result.rssi

            // Get services from scan data
            val services = result.scanRecord?.serviceUuids?.map { it.toString() } ?: emptyList()

            val bleDevice = BleDevice(
                name = deviceName,
                address = deviceAddress,
                rssi = rssi,
                services = services,
                isConnectable = result.isConnectable
            )

            addDeviceToList(bleDevice)
        }

        /**
         * Called when BLE scanning fails.
         *
         * Stops the scanning state and logs the error code.
         *
         * @param errorCode Error code indicating why scanning failed
         */
        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
            // Could emit error here if you need to handle scan failures
        }
    }

    /**
     * Adds or updates a device in the results list.
     *
     * If the device already exists in the list (same MAC address), updates
     * its information. If new, adds it only if it passes the compatibility
     * filter (if active). After the operation, applies the filter to the entire list.
     *
     * @param device BLE device to add or update
     */
    private fun addDeviceToList(device: BleDevice) {
        val currentList = _scanResults.value.toMutableList()

        // Check if device is already in the list
        val existingIndex = currentList.indexOfFirst { it.address == device.address }

        if (existingIndex >= 0) {
        // Update existing device
            currentList[existingIndex] = device
        } else {
            // Add new device only if it passes filter or if filter is disabled
            if (!showOnlyCompatibleDevices || device.isCompatibleWithCamperGas) {
                currentList.add(device)
            }
        }

        // Apply filter to entire list if enabled
        val filteredList = if (showOnlyCompatibleDevices) {
            currentList.filter { it.isCompatibleWithCamperGas }
        } else {
            currentList
        }

        _scanResults.value = filteredList
    }

    /**
     * Sets the filter to show only CamperGas-compatible devices.
     *
     * When enabled, only devices advertising the specific CamperGas service
     * UUIDs are shown. When changing the filter, it is automatically reapplied
     * to all already found devices.
     *
     * @param enabled true to activate the filter, false to show all devices
     */
    fun setCompatibleDevicesFilter(enabled: Boolean) {
        showOnlyCompatibleDevices = enabled
        updateFilteredResults()
    }

    /**
     * Gets the current state of the compatibility filter.
     *
     * @return true if the filter is active, false if disabled
     */
    fun isCompatibleFilterEnabled(): Boolean = showOnlyCompatibleDevices

    /**
     * Updates results by applying the filter if enabled.
     *
     * Iterates through the found devices list and filters those that are
     * compatible with CamperGas if the filter is active. If disabled,
     * shows all devices.
     */
    private fun updateFilteredResults() {
        val allDevices = _scanResults.value
        val filteredDevices = if (showOnlyCompatibleDevices) {
            allDevices.filter { it.isCompatibleWithCamperGas }
        } else {
            allDevices
        }
        _scanResults.value = filteredDevices
    }

    /**
     * Starts scanning for BLE devices.
     *
     * Gets the BLE scanner from the Bluetooth adapter and starts searching
     * for nearby devices. Clears the previous results list and marks
     * scanning as active. If scanning is already in progress, does nothing.
     *
     * Requires BLUETOOTH_SCAN permission on Android 12+ or BLUETOOTH_ADMIN
     * on earlier versions.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScan() {
        if (_isScanning.value) return

        scanner = bluetoothAdapter?.bluetoothLeScanner

        if (scanner != null) {
            _scanResults.value = emptyList()
            _isScanning.value = true
            scanner?.startScan(scanCallback)
        }
    }

    /**
     * Stops scanning for BLE devices.
     *
     * Cancels the ongoing scan and marks the state as not scanning.
     * If there is no active scan, does nothing.
     *
     * Requires BLUETOOTH_SCAN permission on Android 12+ or BLUETOOTH_ADMIN
     * on earlier versions.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScan() {
        if (!_isScanning.value) return

        scanner?.stopScan(scanCallback)
        _isScanning.value = false
    }
}
