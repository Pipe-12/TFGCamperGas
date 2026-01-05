package com.example.campergas.domain.usecase

import com.example.campergas.data.repository.BleRepository
import com.example.campergas.domain.model.FuelMeasurement
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting real-time fuel data from the BLE sensor.
 *
 * This use case encapsulates the business logic for accessing fuel
 * measurements (weight, percentage, kilograms) calculated in real-time from
 * the BLE weight sensor data.
 *
 * Fuel data includes:
 * - Total weight measured by sensor (cylinder + gas)
 * - Kilograms of available gas (total weight - cylinder tare)
 * - Percentage of available gas relative to total capacity
 * - Associated active cylinder information
 * - Measurement timestamp
 *
 * This data is used for:
 * - Displaying current gas level on main screen
 * - Generating consumption charts and trends
 * - Alerting when gas level is low
 * - Updating home screen widgets
 * - Recording consumption history in database
 *
 * The Flow emits new measurements as the BLE sensor provides them,
 * typically every minute according to interval configuration.
 *
 * @property bleRepository BLE repository that provides access to sensor data
 * @author Felipe García Gómez
 */
class GetFuelDataUseCase @Inject constructor(
    private val bleRepository: BleRepository
) {
    /**
     * Gets real-time fuel data as a reactive Flow.
     *
     * Returns a Flow that emits the most recent fuel measurements
     * calculated from the weight sensor data. The Flow updates
     * automatically when new measurements arrive from the sensor.
     *
     * The value can be null if:
     * - No sensor is connected
     * - No active cylinder is configured
     * - The sensor hasn't sent weight measurements yet
     * - Connection to the sensor was lost
     *
     * @return Flow that emits FuelMeasurement objects with calculated data, or null if no data
     */
    operator fun invoke(): Flow<FuelMeasurement?> {
        return bleRepository.fuelData
    }
}
