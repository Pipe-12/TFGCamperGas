package com.example.campergas.domain.usecase

import com.example.campergas.data.repository.FuelMeasurementRepository
import com.example.campergas.data.repository.GasCylinderRepository
import com.example.campergas.domain.model.FuelMeasurement
import com.example.campergas.domain.model.GasCylinder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.random.Random

/**
 * Use case to generate test fuel measurements on a separate test cylinder.
 *
 * Generates realistic test data with timestamps up to one month old to populate
 * the consumption screen with sample data. This is useful for testing and
 * demonstrating the application without actual sensor data.
 *
 * The test data is created on a new test cylinder (not the active one) to avoid
 * mixing test data with real consumption data. The test cylinder:
 * - Has a unique name with "🧪" prefix for easy identification
 * - Is NOT set as active
 * - Has typical cylinder parameters (12.5kg capacity, 14kg tare)
 *
 * @property fuelMeasurementRepository Repository for fuel measurements
 * @property gasCylinderRepository Repository for gas cylinders
 */
class GenerateTestDataUseCase @Inject constructor(
    private val fuelMeasurementRepository: FuelMeasurementRepository,
    private val gasCylinderRepository: GasCylinderRepository
) {
    companion object {
        /** Number of test measurements to generate */
        private const val TEST_MEASUREMENTS_COUNT = 100

        /** Maximum age of test data in milliseconds (30 days) */
        private const val MAX_AGE_MS = 30L * 24 * 60 * 60 * 1000

        /** Average consumption rate per day in kg */
        private const val AVG_CONSUMPTION_PER_DAY = 0.3f

        /** Prefix for test cylinder name to easily identify it */
        const val TEST_CYLINDER_NAME_PREFIX = "🧪 Prueba"

        /** Default capacity for test cylinder in kg */
        private const val TEST_CYLINDER_CAPACITY = 12.5f

        /** Default tare (empty weight) for test cylinder in kg */
        private const val TEST_CYLINDER_TARE = 14.0f
    }

    /**
     * Generates test fuel measurements on a new test cylinder.
     *
     * Creates a new test cylinder (not active) and populates it with measurements:
     * - Timestamps distributed over the last 30 days
     * - Realistic decreasing fuel levels (simulating consumption)
     * - Some random variation to simulate real sensor data
     * - All marked as historical data
     *
     * @return Result with number of measurements generated or error
     */
    suspend operator fun invoke(): Result<Int> {
        return try {
            val currentTime = System.currentTimeMillis()

            // Create a unique test cylinder name with date/time
            val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
            val testCylinderName = "$TEST_CYLINDER_NAME_PREFIX - ${dateFormat.format(Date(currentTime))}"

            // Create a new test cylinder (NOT active)
            val testCylinder = GasCylinder(
                name = testCylinderName,
                tare = TEST_CYLINDER_TARE,
                capacity = TEST_CYLINDER_CAPACITY,
                isActive = false, // Not active to avoid mixing with real data
                createdAt = currentTime
            )

            // Insert the test cylinder and get its ID
            val testCylinderId = gasCylinderRepository.insertCylinder(testCylinder)

            val measurements = mutableListOf<FuelMeasurement>()

            // Starting fuel percentage (80-95%)
            var currentFuelPercentage = Random.nextFloat() * 15f + 80f

            // Calculate consumption rate once (constant for all measurements)
            val consumptionRate = AVG_CONSUMPTION_PER_DAY / TEST_MEASUREMENTS_COUNT * 30

            // Generate measurements from oldest to newest
            for (i in 0 until TEST_MEASUREMENTS_COUNT) {
                // Calculate timestamp (distributed over last 30 days)
                val ageMs = MAX_AGE_MS - (i * MAX_AGE_MS / TEST_MEASUREMENTS_COUNT)
                val timestamp = currentTime - ageMs

                // Simulate gradual consumption with some randomness
                currentFuelPercentage -= consumptionRate + Random.nextFloat() * 0.5f - 0.25f

                // Keep within valid range (5% to 100%)
                currentFuelPercentage = currentFuelPercentage.coerceIn(5f, 100f)

                // Calculate fuel kilograms from percentage
                val fuelKilograms = TEST_CYLINDER_CAPACITY * currentFuelPercentage / 100f

                // Calculate total weight
                val totalWeight = TEST_CYLINDER_TARE + fuelKilograms

                val measurement = FuelMeasurement(
                    cylinderId = testCylinderId,
                    cylinderName = testCylinderName,
                    timestamp = timestamp,
                    fuelKilograms = fuelKilograms,
                    fuelPercentage = currentFuelPercentage,
                    totalWeight = totalWeight,
                    isCalibrated = true,
                    isHistorical = true // Mark as historical test data
                )

                measurements.add(measurement)
            }

            // Save all measurements
            fuelMeasurementRepository.insertMeasurements(measurements)

            Result.success(measurements.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
