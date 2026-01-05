package com.example.campergas.domain.model

/**
 * Domain model representing a fuel gas measurement.
 *
 * This data class encapsulates all information related to a gas level measurement
 * for a specific cylinder. Includes both raw sensor data (total weight) and
 * calculated values (kilograms of gas, percentage).
 *
 * Data origin:
 * - Real-time: Measurements taken at the moment from the BLE sensor
 * - Historical: Measurements synchronized from the sensor's offline storage
 *
 * The class provides utility methods for:
 * - Formatting values for UI display
 * - Validating measurement data integrity
 * - Distinguishing between real-time and historical data
 *
 * @property id Unique identifier of the measurement in the database
 * @property cylinderId ID of the gas cylinder this measurement belongs to
 * @property cylinderName Descriptive name of the cylinder for UI reference
 * @property timestamp Unix moment (milliseconds) when the measurement was taken
 * @property fuelKilograms Kilograms of available gas (total weight minus cylinder tare)
 * @property fuelPercentage Percentage of available gas relative to total capacity (0-100%)
 * @property totalWeight Total weight measured by the sensor including cylinder and gas (in kg)
 * @property isCalibrated Indicates if the measurement is calibrated with the correct cylinder tare
 * @property isHistorical Indicates if it's a historical/offline synced data or real-time measurement
 * @author Felipe García Gómez
 */
data class FuelMeasurement(
    val id: Long = 0,
    val cylinderId: Long,
    val cylinderName: String,
    val timestamp: Long,
    val fuelKilograms: Float,
    val fuelPercentage: Float,
    val totalWeight: Float,
    val isCalibrated: Boolean = true,
    val isHistorical: Boolean = false
) {
    /**
     * Formats fuel kilograms for display in the user interface.
     *
     * Returns the value with 2 decimal places followed by the unit "kg".
     * Example: "12.50 kg"
     *
     * @return Formatted string with kilograms and unit
     */
    fun getFormattedFuelKilograms(): String = "%.2f kg".format(fuelKilograms)

    /**
     * Formats fuel percentage for display in the user interface.
     *
     * Returns the value with 1 decimal place followed by the "%" symbol.
     * Example: "75.5%"
     *
     * @return Formatted string with percentage and symbol
     */
    fun getFormattedPercentage(): String = "%.1f%%".format(fuelPercentage)

    /**
     * Formats total measured weight for display in the user interface.
     *
     * Returns the value with 2 decimal places followed by the unit "kg".
     * Includes both cylinder weight and gas weight.
     * Example: "25.75 kg"
     *
     * @return Formatted string with total weight and unit
     */
    fun getFormattedTotalWeight(): String = "%.2f kg".format(totalWeight)

    /**
     * Gets the formatted date and time of the measurement.
     *
     * Converts the Unix timestamp to human-readable time format "HH:mm:ss"
     * using the device's locale configuration.
     * Example: "14:35:22"
     *
     * @return String with formatted time
     */
    fun getFormattedTimestamp(): String {
        val date = java.util.Date(timestamp)
        val formatter = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        return formatter.format(date)
    }

    /**
     * Verifies if the measurement contains valid values.
     *
     * A measurement is valid if:
     * - Fuel kilograms is a valid number (not NaN or infinite) and non-negative
     * - Percentage is a valid number, non-negative, and not exceeding 100%
     *
     * This validation is important for detecting sensor errors or incorrect calculations.
     *
     * @return true if all values are valid, false if any value is invalid
     */
    fun isValid(): Boolean =
        !fuelKilograms.isNaN() && !fuelKilograms.isInfinite() && fuelKilograms >= 0 &&
                !fuelPercentage.isNaN() && !fuelPercentage.isInfinite() && fuelPercentage >= 0 && fuelPercentage <= 100
}
