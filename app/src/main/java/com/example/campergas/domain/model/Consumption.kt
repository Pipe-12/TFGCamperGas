package com.example.campergas.domain.model

/**
 * Domain model representing a fuel consumption record.
 *
 * This data class is a simplified view of FuelMeasurement, focused
 * specifically on gas consumption history for display in
 * charts and historical consumption lists.
 *
 * Contains information about:
 * - The associated gas cylinder (ID and name)
 * - The measurement date/time
 * - Fuel values (kilograms, percentage, total weight)
 * - Calibration status
 * - Data origin (real-time vs historical/offline)
 *
 * @property id Unique identifier of the consumption record
 * @property cylinderId ID of the gas cylinder this measurement belongs to
 * @property cylinderName Descriptive name of the gas cylinder
 * @property date Unix timestamp (milliseconds) of when the measurement was taken
 * @property fuelKilograms Amount of available gas in kilograms
 * @property fuelPercentage Percentage of available gas relative to total capacity (0-100%)
 * @property totalWeight Total measured weight including cylinder and gas (in kg)
 * @property isCalibrated Indicates if the measurement is calibrated with the cylinder tare
 * @property isHistorical Indicates if it's a historical/offline synced data from the sensor or real-time data
 * @author Felipe García Gómez
 */
data class Consumption(
    val id: Long = 0,
    val cylinderId: Long,
    val cylinderName: String,
    val date: Long,
    val fuelKilograms: Float,
    val fuelPercentage: Float,
    val totalWeight: Float,
    val isCalibrated: Boolean = true,
    val isHistorical: Boolean = false
) {
    companion object {
        /**
         * Converts a complete fuel measurement to a consumption record.
         *
         * This factory method transforms a FuelMeasurement object (containing
         * detailed measurement information) into a simpler Consumption object
         * oriented for history display.
         *
         * @param fuelMeasurement Complete fuel measurement to convert
         * @return Consumption object with relevant data for history
         */
        fun fromFuelMeasurement(fuelMeasurement: FuelMeasurement): Consumption {
            return Consumption(
                id = fuelMeasurement.id,
                cylinderId = fuelMeasurement.cylinderId,
                cylinderName = fuelMeasurement.cylinderName,
                date = fuelMeasurement.timestamp,
                fuelKilograms = fuelMeasurement.fuelKilograms,
                fuelPercentage = fuelMeasurement.fuelPercentage,
                totalWeight = fuelMeasurement.totalWeight,
                isCalibrated = fuelMeasurement.isCalibrated,
                isHistorical = fuelMeasurement.isHistorical
            )
        }
    }
}
