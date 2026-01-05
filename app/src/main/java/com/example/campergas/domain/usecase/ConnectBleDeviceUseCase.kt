package com.example.campergas.domain.usecase

import com.example.campergas.data.repository.BleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for connecting and disconnecting CamperGas BLE sensor devices.
 *
 * This use case encapsulates the business logic for establishing and terminating
 * BLE connections with CamperGas sensors. Manages both the physical connection
 * and storage of the last connected device address to allow automatic reconnection.
 *
 * Functionality:
 * - Connection to a BLE sensor by its MAC address
 * - Disconnection from the currently connected sensor
 * - Saving and retrieving the last connected device
 * - Support for automatic reconnection to the last used sensor
 *
 * Connection process:
 * 1. Receives device MAC address
 * 2. Establishes BLE connection with sensor
 * 3. Discovers available services and characteristics
 * 4. Saves address for future reconnections
 * 5. Starts automatic reading of historical (offline) data
 * 6. Begins periodic reading of real-time data
 *
 * @property bleRepository BLE repository that manages connections and communication
 * @author Felipe García Gómez
 */
class ConnectBleDeviceUseCase @Inject constructor(
    private val bleRepository: BleRepository
) {
    /**
     * Connects to a CamperGas BLE sensor device.
     *
     * Establishes a BLE connection with the sensor specified by its MAC address.
     * Once connected, the sensor automatically starts sending:
     * - Historical data stored offline
     * - Real-time weight and inclination measurements
     *
     * Saves the device address to allow automatic reconnection
     * the next time the application is opened.
     *
     * This function must be called from a coroutine or suspend function.
     *
     * @param deviceAddress MAC address of the BLE device (format: "XX:XX:XX:XX:XX:XX")
     */
    suspend operator fun invoke(deviceAddress: String) {
        // Connect to the unified sensor
        bleRepository.connectToSensor(deviceAddress)

        // Save address of last connected device
        bleRepository.saveLastConnectedDevice(deviceAddress)
    }

    /**
     * Disconnects from the currently connected BLE sensor.
     *
     * Closes the active BLE connection, stops periodic data readings
     * and releases associated resources. Does not delete the address of the last
     * connected device, allowing later reconnection.
     *
     * Safe to call this method even if no device is connected.
     */
    fun disconnect() {
        bleRepository.disconnectSensor()
    }

    /**
     * Gets the address of the last connected BLE device.
     *
     * Returns a Flow that emits the MAC address of the last sensor the
     * application connected to. Useful for implementing automatic reconnection or
     * showing the previously used device.
     *
     * @return Flow that emits the MAC address of the last connected device
     */
    fun getLastConnectedDevice(): Flow<String> {
        return bleRepository.lastConnectedDeviceAddress
    }
}
