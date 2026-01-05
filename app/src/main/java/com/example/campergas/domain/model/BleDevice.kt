package com.example.campergas.domain.model

/**
 * Domain model representing a Bluetooth Low Energy (BLE) device.
 *
 * This data class encapsulates all relevant information about a BLE device
 * discovered during scanning, including its identification, connection status,
 * available services, and signal quality.
 *
 * Provides computed properties for:
 * - Evaluating signal quality (excellent, good, fair, weak)
 * - Verifying compatibility with CamperGas sensors
 * - Identifying device type based on its name and services
 *
 * @property name Visible name of the BLE device
 * @property address Unique MAC address of the device
 * @property rssi Received Signal Strength Indicator (in dBm, negative values)
 * @property isConnected Indicates if the device is currently connected
 * @property services List of BLE service UUIDs advertised by the device
 * @property isConnectable Indicates if the device accepts connections
 * @property lastSeen Timestamp of the last time the device was detected
 * @author Felipe García Gómez
 */
data class BleDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val isConnected: Boolean = false,
    val services: List<String> = emptyList(),
    val isConnectable: Boolean = true,
    val lastSeen: Long = System.currentTimeMillis()
) {
    /**
     * Human-readable signal quality interpretation.
     *
     * Classifies RSSI signal strength into four categories:
     * - "Excellent": RSSI >= -50 dBm (very strong signal, device very close)
     * - "Good": RSSI >= -70 dBm (strong signal, device nearby)
     * - "Fair": RSSI >= -85 dBm (acceptable signal, device at medium distance)
     * - "Weak": RSSI < -85 dBm (weak signal, device far away or obstructed)
     */
    val signalStrength: String
        get() = when {
            rssi >= -50 -> "Excellent"
            rssi >= -70 -> "Good"
            rssi >= -85 -> "Fair"
            else -> "Weak"
        }

    /**
     * Verifies if this device is compatible with CamperGas.
     *
     * A device is compatible if it advertises any of the service UUIDs
     * defined as compatible with the CamperGas system.
     *
     * @return true if the device is compatible with CamperGas, false otherwise
     */
    val isCompatibleWithCamperGas: Boolean
        get() = CamperGasUuids.isCompatibleDevice(services)

    /**
     * Gets the device type based on its name and compatibility.
     *
     * Classifies the device by analyzing its name and services into the following categories:
     * - "CamperGas Sensor": Device with name containing "CamperGas"
     * - "Weight Sensor": Device specialized in weight measurement
     * - "Inclination Sensor": Device specialized in inclination measurement
     * - "Compatible Device": Compatible BLE device without specific name
     * - "BLE Device": Generic non-compatible BLE device
     */
    val deviceType: String
        get() = when {
            name.contains("CamperGas", ignoreCase = true) -> "CamperGas Sensor"
            name.contains("Weight", ignoreCase = true) -> "Weight Sensor"
            name.contains("Inclination", ignoreCase = true) -> "Inclination Sensor"
            isCompatibleWithCamperGas -> "Compatible Device"
            else -> "BLE Device"
        }
}
