package com.example.campergas.domain.model

/**
 * Singleton object containing BLE service and characteristic UUIDs for CamperGas.
 *
 * This object centralizes all UUID constants needed for communication
 * with CamperGas sensors via Bluetooth Low Energy (BLE). CamperGas sensors
 * use a single BLE service with three different characteristics:
 *
 * Sensor architecture:
 * - 1 Main BLE service (SENSOR_SERVICE_UUID)
 *   - Real-time weight characteristic (WEIGHT_CHARACTERISTIC_UUID)
 *   - Offline historical data characteristic (OFFLINE_CHARACTERISTIC_UUID)
 *   - Inclination characteristic (INCLINATION_CHARACTERISTIC_UUID)
 *
 * These constants are used for:
 * - Identifying compatible devices during BLE scanning
 * - Subscribing to correct characteristics after connecting
 * - Reading weight, inclination, and historical data from the sensor
 *
 * @author Felipe García Gómez
 */
object CamperGasUuids {
    /**
     * UUID of the main BLE service for the CamperGas sensor.
     *
     * This is the unique service containing all measurement characteristics
     * (weight, inclination, and historical data). A BLE device must advertise this
     * UUID to be considered a compatible CamperGas sensor.
     */
    const val SENSOR_SERVICE_UUID = "91bad492-b950-4226-aa2b-4ede9fa42f59"

    /**
     * UUID of the BLE characteristic for real-time weight measurements.
     *
     * This characteristic provides the current total weight of the gas cylinder
     * as measured by the sensor. Data is obtained via READ operation.
     * Data format: JSON {"w": weight_in_kg}
     */
    const val WEIGHT_CHARACTERISTIC_UUID = "cba1d466-344c-4be3-ab3f-189f80dd7518"

    /**
     * UUID of the BLE characteristic for offline historical data.
     *
     * This characteristic provides historical measurements stored in the sensor
     * while disconnected. Data is obtained via successive reads
     * until the sensor has no more data to send.
     * Data format: JSON array [{"w": weight_in_kg, "t": elapsed_milliseconds}, ...]
     */
    const val OFFLINE_CHARACTERISTIC_UUID = "87654321-4321-4321-4321-cba987654321"

    /**
     * UUID of the BLE characteristic for inclination measurements.
     *
     * This characteristic provides vehicle inclination data on two axes
     * (pitch and roll) obtained from the accelerometer/gyroscope integrated in the sensor.
     * Data format: JSON {"p": pitch_in_degrees, "r": roll_in_degrees}
     */
    const val INCLINATION_CHARACTERISTIC_UUID = "fedcba09-8765-4321-fedc-ba0987654321"

    /**
     * UUID of the BLE characteristic for tare (zeroing) operation.
     *
     * This characteristic allows writing any value to trigger the tare operation
     * on the sensor, which sets the current weight as zero reference.
     * Type: WRITE
     * Payload: Any value (e.g., "1")
     */
    const val TARE_CHARACTERISTIC_UUID = "12345678-1234-1234-1234-123456789abc"

    /**
     * UUID of the BLE characteristic for calibration operation.
     *
     * This characteristic accepts a JSON payload with the known weight value
     * to calibrate the sensor's scale factor.
     * Type: WRITE
     * Format: JSON {"cal": float_value}
     * Example: {"cal": 2.50} for 2.50 kg
     */
    const val CALIBRATION_CHARACTERISTIC_UUID = "a1b2c3d4-e5f6-4789-ab01-abcdef123456"
    /**
     * Verifies if a BLE device is compatible with CamperGas.
     *
     * A device is compatible if it advertises the main CamperGas service UUID
     * among its available services. This verification is performed during BLE
     * scanning to filter out incompatible devices.
     *
     * @param serviceUuids List of service UUIDs advertised by the BLE device
     * @return true if the device is compatible with CamperGas, false otherwise
     */
    fun isCompatibleDevice(serviceUuids: List<String>): Boolean {
        return serviceUuids.any { uuid ->
            uuid.equals(SENSOR_SERVICE_UUID, ignoreCase = true)
        }
    }

}
