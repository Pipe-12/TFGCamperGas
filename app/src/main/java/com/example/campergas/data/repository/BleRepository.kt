package com.example.campergas.data.repository

import android.Manifest
import androidx.annotation.RequiresPermission
import com.example.campergas.data.ble.BleDeviceScanner
import com.example.campergas.data.ble.BleManager
import com.example.campergas.data.ble.CamperGasBleService
import com.example.campergas.data.local.preferences.PreferencesDataStore
import com.example.campergas.domain.model.BleDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing Bluetooth Low Energy (BLE) operations.
 *
 * This repository provides a unified interface for all BLE-related functionality,
 * including device scanning, sensor connection, data reading, and preferences management.
 * It acts as an abstraction layer coordinating multiple BLE components.
 *
 * Components coordinated:
 * - BleManager: Bluetooth adapter and permission management
 * - BleDeviceScanner: Device discovery and filtering
 * - CamperGasBleService: Sensor connection and data reading
 * - PreferencesDataStore: Persistence of BLE-related preferences
 *
 * @property bleManager Bluetooth adapter manager
 * @property bleDeviceScanner BLE device scanner
 * @property camperGasBleService CamperGas sensor service
 * @property preferencesDataStore User preferences storage
 * @author Felipe García Gómez
 */
@Singleton
class BleRepository @Inject constructor(
    private val bleManager: BleManager,
    private val bleDeviceScanner: BleDeviceScanner,
    private val camperGasBleService: CamperGasBleService,
    private val preferencesDataStore: PreferencesDataStore
) {
    /** StateFlow of discovered BLE devices during scanning */
    val scanResults: StateFlow<List<BleDevice>> = bleDeviceScanner.scanResults

    /** StateFlow indicating if a sensor is currently connected */
    val connectionState: StateFlow<Boolean> = camperGasBleService.connectionState

    /** Real-time fuel measurement data from the connected sensor */
    val fuelMeasurementData = camperGasBleService.fuelMeasurementData

    /** Calculated fuel data (kilograms and percentage) */
    val fuelData = camperGasBleService.fuelData

    /** Real-time inclination data from the connected sensor */
    val inclinationData = camperGasBleService.inclinationData

    /** Flow of the last connected device MAC address */
    val lastConnectedDeviceAddress: Flow<String> = preferencesDataStore.lastConnectedDeviceAddress

    /** Flow of the weight sensor reading interval in milliseconds */
    val weightReadInterval: Flow<Long> = preferencesDataStore.weightReadInterval

    /** Flow of the inclination sensor reading interval in milliseconds */
    val inclinationReadInterval: Flow<Long> = preferencesDataStore.inclinationReadInterval

    /**
     * Checks if Bluetooth is enabled on the device.
     *
     * @return true if Bluetooth is enabled, false otherwise
     */
    fun isBluetoothEnabled(): Boolean = bleManager.isBluetoothEnabled()

    /**
     * Starts scanning for BLE devices.
     * Requires BLUETOOTH_SCAN permission on Android 12+.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScan() = bleDeviceScanner.startScan()

    /**
     * Stops the current BLE device scan.
     * Requires BLUETOOTH_SCAN permission on Android 12+.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScan() = bleDeviceScanner.stopScan()

    /**
     * Connects to a CamperGas sensor by its MAC address.
     *
     * @param deviceAddress MAC address of the device to connect (format: "XX:XX:XX:XX:XX:XX")
     */
    fun connectToSensor(deviceAddress: String) = camperGasBleService.connect(deviceAddress)

    /**
     * Disconnects from the currently connected sensor.
     */
    fun disconnectSensor() = camperGasBleService.disconnect()

    /**
     * Checks if a sensor is currently connected.
     *
     * @return true if connected, false otherwise
     */
    fun isConnected(): Boolean = camperGasBleService.isConnected()

    /**
     * Requests an on-demand weight data reading from the sensor.
     */
    fun readWeightDataOnDemand() = camperGasBleService.readWeightDataOnDemand()

    /**
     * Requests an on-demand inclination data reading from the sensor.
     */
    fun readInclinationDataOnDemand() = camperGasBleService.readInclinationDataOnDemand()

    /**
     * Configures the reading intervals for weight and inclination data.
     *
     * @param weightIntervalMs Interval in milliseconds for weight readings
     * @param inclinationIntervalMs Interval in milliseconds for inclination readings
     */
    fun configureReadingIntervals(weightIntervalMs: Long, inclinationIntervalMs: Long) {
        camperGasBleService.configureReadingIntervals(weightIntervalMs, inclinationIntervalMs)
    }

    /**
     * Gets the current weight reading interval.
     *
     * @return Interval in milliseconds
     */
    fun getWeightReadInterval(): Long = camperGasBleService.getWeightReadInterval()

    /**
     * Gets the current inclination reading interval.
     *
     * @return Interval in milliseconds
     */
    fun getInclinationReadInterval(): Long = camperGasBleService.getInclinationReadInterval()

    /**
     * Saves the address of the last connected device for automatic reconnection.
     *
     * @param address MAC address of the device
     */
    suspend fun saveLastConnectedDevice(address: String) =
        preferencesDataStore.saveLastConnectedDevice(address)

    /**
     * Saves the weight reading interval preference.
     *
     * @param intervalMs Interval in milliseconds
     */
    suspend fun saveWeightReadInterval(intervalMs: Long) =
        preferencesDataStore.setWeightReadInterval(intervalMs)

    /**
     * Saves the inclination reading interval preference.
     *
     * @param intervalMs Interval in milliseconds
     */
    suspend fun saveInclinationReadInterval(intervalMs: Long) =
        preferencesDataStore.setInclinationReadInterval(intervalMs)

    /**
     * Sets the filter to show only CamperGas-compatible devices.
     *
     * @param enabled true to show only compatible devices, false to show all
     */
    fun setCompatibleDevicesFilter(enabled: Boolean) =
        bleDeviceScanner.setCompatibleDevicesFilter(enabled)

    /**
     * Checks if the compatible devices filter is currently enabled.
     *
     * @return true if filter is enabled, false otherwise
     */
    fun isCompatibleFilterEnabled(): Boolean = bleDeviceScanner.isCompatibleFilterEnabled()
}
