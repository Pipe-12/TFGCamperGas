package com.example.campergas.domain.usecase

import com.example.campergas.data.repository.FuelMeasurementRepository
import com.example.campergas.data.repository.GasCylinderRepository
import com.example.campergas.domain.model.FuelMeasurement
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case for saving fuel measurements to the database.
 *
 * This use case encapsulates complex business logic for persisting gas level
 * measurements, distinguishing between:
 * - Real-time measurements: Current sensor data (saved every 2 minutes)
 * - Historical/offline measurements: Data synchronized from sensor storage
 *
 * Main functionality:
 * - Save frequency control (avoids database spam with 2-minute limit)
 * - Automatic calculation of available fuel (total weight - tare)
 * - Data validation before saving
 * - Automatic detection and removal of erroneous measurements (outliers)
 * - Support for batch saving of historical data
 *
 * Outlier detection:
 * Identifies and removes anomalous measurements based on patterns:
 * - A measurement that deviates >30% from the previous value
 * - Then the value returns to normal
 * - Considered a sensor error and removed
 *
 * @property fuelMeasurementRepository Repository for fuel measurements
 * @property gasCylinderRepository Repository for cylinders to get tare and capacity
 * @author Felipe García Gómez
 */
class SaveFuelMeasurementUseCase @Inject constructor(
    private val fuelMeasurementRepository: FuelMeasurementRepository,
    private val gasCylinderRepository: GasCylinderRepository
) {

    companion object {
        /** Minimum time between real-time measurement saves (2 minutes) */
        private const val MIN_TIME_BETWEEN_SAVES_MS = 2 * 60 * 1000L

        /** Percentage change considered anomalous (30%) */
        private const val OUTLIER_THRESHOLD_PERCENTAGE = 30.0f

        /** Minimum number of measurements needed for outlier detection */
        private const val MIN_MEASUREMENTS_FOR_OUTLIER_DETECTION = 3
    }

    /**
     * Timestamp of the last real-time measurement saved.
     * Used to control save frequency.
     */
    @Volatile
    private var lastSaveTimestamp: Long = 0L

    /**
     * Saves a REAL-TIME fuel measurement.
     *
     * This function is invoked when data arrives from the BLE sensor in real-time
     * (WEIGHT_CHARACTERISTIC_UUID characteristic). Measurements are marked as
     * isHistorical = false.
     *
     * Frequency control:
     * Measurements are only saved every 2 minutes to avoid saturating the database.
     * If an attempt is made to save before that, a result indicating the remaining time is returned.
     *
     * Process:
     * 1. Verifies minimum time has passed since last save
     * 2. Gets active cylinder and its configuration (tare, capacity)
     * 3. Calculates available fuel and percentage
     * 4. Validates calculated data
     * 5. Saves measurement to database
     * 6. Executes outlier detection to maintain data quality
     *
     * @param totalWeight Total weight measured by sensor (cylinder + gas) in kilograms
     * @param timestamp Timestamp of when measurement was taken (default: now)
     * @return Result with SaveMeasurementResult indicating if saved or why not
     */
    suspend fun saveRealTimeMeasurement(
        totalWeight: Float,
        timestamp: Long = System.currentTimeMillis()
    ): Result<SaveMeasurementResult> {
        return try {
            // Verify if it has passed at least 2 minutes since the last saved measurement
            val currentTime = System.currentTimeMillis()
            val timeSinceLastSave = currentTime - lastSaveTimestamp

            if (lastSaveTimestamp > 0 && timeSinceLastSave < MIN_TIME_BETWEEN_SAVES_MS) {
                val remainingTimeMs = MIN_TIME_BETWEEN_SAVES_MS - timeSinceLastSave
                val remainingTimeMinutes = (remainingTimeMs / 1000 / 60).toInt()
                val remainingTimeSeconds = ((remainingTimeMs / 1000) % 60).toInt()

                return Result.success(
                    SaveMeasurementResult(
                        measurementId = -1L,
                        processed = false,
                        reason = "Measurement skipped: remaining ${remainingTimeMinutes}m ${remainingTimeSeconds}s"
                    )
                )
            }

            // Get la cylinder activa
            val activeCylinder = gasCylinderRepository.getActiveCylinder().first()
                ?: return Result.failure(Exception("No active cylinder configured"))

            // Calculatesr el combustible disponible
            val fuelKilograms = maxOf(0f, totalWeight - activeCylinder.tare)
            val fuelPercentage = if (activeCylinder.capacity > 0) {
                (fuelKilograms / activeCylinder.capacity * 100).coerceIn(0f, 100f)
            } else {
                0f
            }

            // Create the measurement
            val measurement = FuelMeasurement(
                cylinderId = activeCylinder.id,
                cylinderName = activeCylinder.name,
                timestamp = timestamp,
                fuelKilograms = fuelKilograms,
                fuelPercentage = fuelPercentage,
                totalWeight = totalWeight,
                isCalibrated = true,
                isHistorical = false
            )

            // Validate the measurement
            if (!measurement.isValid()) {
                return Result.failure(Exception("Measurement data is not valid"))
            }

            // Save the measurement
            val id = fuelMeasurementRepository.insertMeasurement(measurement)

            // Detect and remove erroneous measurements (outliers)
            detectAndRemoveOutliers(activeCylinder.id)

            // Update the timestamp of the last saved measurement
            lastSaveTimestamp = currentTime

            Result.success(
                SaveMeasurementResult(
                    measurementId = id,
                    processed = true,
                    reason = "Measurement saved successfully"
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Saves multiple HISTORICAL fuel measurements.
     *
     * This function is invoked when historical data stored in the BLE sensor
     * while disconnected is synchronized (OFFLINE_CHARACTERISTIC_UUID characteristic).
     * Measurements are marked as isHistorical = true.
     *
     * Unlike real-time measurements, these:
     * - Have NO save frequency limit
     * - Are saved in complete batches
     * - Represent past data, not current measurements
     *
     * Process:
     * 1. Gets cylinder information (tare, capacity)
     * 2. For each (weight, timestamp) pair:
     *    - Calculates available fuel and percentage
     *    - Creates FuelMeasurement object marked as historical
     *    - Validates data
     * 3. Filters only valid measurements
     * 4. Saves all measurements in batch
     *
     * @param cylinderId ID of the cylinder the measurements belong to
     * @param weightMeasurements List of pairs (total weight in kg, Unix timestamp)
     * @return Result with number of measurements saved or error
     */
    suspend fun saveHistoricalMeasurements(
        cylinderId: Long,
        weightMeasurements: List<Pair<Float, Long>>
    ): Result<Int> {
        return try {
            // Get cylinder information
            val cylinder = gasCylinderRepository.getCylinderById(cylinderId)
                ?: return Result.failure(Exception("Cylinder not found"))

            // Create the measurements
            val measurements = weightMeasurements.map { (totalWeight, timestamp) ->
                val fuelKilograms = maxOf(0f, totalWeight - cylinder.tare)
                val fuelPercentage = if (cylinder.capacity > 0) {
                    (fuelKilograms / cylinder.capacity * 100).coerceIn(0f, 100f)
                } else {
                    0f
                }

                FuelMeasurement(
                    cylinderId = cylinder.id,
                    cylinderName = cylinder.name,
                    timestamp = timestamp,
                    fuelKilograms = fuelKilograms,
                    fuelPercentage = fuelPercentage,
                    totalWeight = totalWeight,
                    isCalibrated = true,
                    isHistorical = true
                )
            }.filter { it.isValid() } // Filter only valid measurements

            // Save all measurements
            fuelMeasurementRepository.insertMeasurements(measurements)

            Result.success(measurements.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Detects and removes erroneous measurements (outliers) based on weight patterns.
     *
     * An outlier is a measurement that deviates significantly from the expected pattern
     * and probably represents a sensor error. This function analyzes the last 3
     * measurements looking for the pattern: previous -> outlier -> current
     *
     * Detection pattern:
     * - Previous measurement: normal weight (e.g., 25 kg)
     * - Outlier: substantially different weight >30% (e.g., 15 kg - error)
     * - Current measurement: weight returns to normal (e.g., 24.8 kg)
     *
     * If this pattern is detected, the middle measurement is considered erroneous
     * and is removed from the database to maintain data quality.
     *
     * This function runs automatically after each real-time measurement save.
     *
     * @param cylinderId ID of the cylinder to analyze
     */
    private suspend fun detectAndRemoveOutliers(cylinderId: Long) {
        try {
            // Get last 3 measurements for pattern analysis
            val recentMeasurements = fuelMeasurementRepository.getLastNMeasurements(
                cylinderId,
                MIN_MEASUREMENTS_FOR_OUTLIER_DETECTION
            )

            // We need at least 3 measurements to detect the pattern: previous -> outlier -> current
            if (recentMeasurements.size < MIN_MEASUREMENTS_FOR_OUTLIER_DETECTION) {
                return
            }

            // Measurements come sorted by timestamp DESC, so:
            // current = recentMeasurements[0] (most recent)
            // outlier = recentMeasurements[1] (middle)
            // previous = recentMeasurements[2] (previous)
            val current = recentMeasurements[0]
            val outlier = recentMeasurements[1]
            val previous = recentMeasurements[2]

            // Verify if middle measurement is an outlier
            if (isOutlierMeasurement(previous, outlier, current)) {
                // Remove outlier measurement
                fuelMeasurementRepository.deleteMeasurementById(outlier.id)
            }
        } catch (_: Exception) {
            // If any error occurs in outlier detection, do not affect main flow
            // Silently log the error
        }
    }

    /**
     * Determines if a measurement is an outlier by analyzing the pattern of three measurements.
     *
     * Analyzes three consecutive measurements (previous -> candidate -> current) and determines
     * if the middle measurement is anomalous.
     *
     * Criteria to consider an outlier:
     * 1. Significant deviation (>30%) compared to the previous value
     * 2. The current value returns close to the previous value (difference <30%)
     * 3. The outlier's deviation is greater than the normal variation between previous and current
     *
     * Example of detected outlier:
     * - Previous: 25.0 kg
     * - Outlier: 15.0 kg (40% change - anomalous)
     * - Current: 24.8 kg (returns to normal)
     *
     * Example of normal change (NOT an outlier):
     * - Previous: 25.0 kg
     * - Middle: 24.0 kg (gradual consumption)
     * - Current: 23.0 kg (consistent trend)
     *
     * @param previous Previous measurement (oldest)
     * @param outlier Outlier candidate measurement (middle)
     * @param current Current measurement (most recent)
     * @return true if the middle measurement is an outlier that should be removed
     */
    private fun isOutlierMeasurement(
        previous: FuelMeasurement,
        outlier: FuelMeasurement,
        current: FuelMeasurement
    ): Boolean {
        // Use total weight for analysis as it is most direct value from sensor
        val prevWeight = previous.totalWeight
        val outlierWeight = outlier.totalWeight
        val currentWeight = current.totalWeight

        // Avoid division by zero
        if (prevWeight <= 0f || outlierWeight <= 0f || currentWeight <= 0f) {
            return false
        }

        // Calculate percentage changes
        val outlierVsPrevious = kotlin.math.abs(outlierWeight - prevWeight) / prevWeight * 100f
        val currentVsPrevious = kotlin.math.abs(currentWeight - prevWeight) / prevWeight * 100f
        val currentVsOutlier = kotlin.math.abs(currentWeight - outlierWeight) / outlierWeight * 100f

        // Conditions to consider outlier:
        // 1. The outlier deviates substantially from previous (>30%)
        val isSignificantDeviation = outlierVsPrevious > OUTLIER_THRESHOLD_PERCENTAGE

        // 2. Current value is closer to previous than to outlier
        val returnsToNormal = currentVsPrevious < outlierVsPrevious

        // 3. Change from outlier to current is also significant (confirms outlier was anomalous)
        val significantCorrectionFromOutlier = currentVsOutlier > OUTLIER_THRESHOLD_PERCENTAGE

        return isSignificantDeviation && returnsToNormal && significantCorrectionFromOutlier
    }

    /**
     * Result of saving a measurement.
     *
     * Contains information about whether the measurement was processed and saved successfully,
     * or why it wasn't saved (e.g., minimum time not elapsed, invalid data).
     *
     * @property measurementId ID assigned to the saved measurement, or -1 if not saved
     * @property processed true if measurement was saved, false if skipped
     * @property reason Explanation of why the measurement was saved or not saved
     */
    data class SaveMeasurementResult(
        val measurementId: Long,
        val processed: Boolean,
        val reason: String = ""
    )
}
