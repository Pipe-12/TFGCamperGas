package com.example.campergas.domain.usecase

import com.example.campergas.data.repository.BleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case for configuring periodic reading intervals from the BLE sensor.
 *
 * This use case encapsulates the business logic for adjusting the frequency with
 * which data is requested from the BLE sensor. Allows configuring different intervals
 * for weight and inclination measurements independently.
 *
 * Configurable intervals:
 * - Weight: Defines every how many seconds the cylinder weight is read
 * - Inclination: Defines every how many seconds the vehicle inclination is read
 *
 * Interval considerations:
 * - Shorter intervals: Greater precision and more updated data, higher battery consumption
 * - Longer intervals: Lower battery consumption, less update frequency
 *
 * Recommended values:
 * - Weight: 60 seconds (1 minute) - Gas doesn't change quickly
 * - Inclination: 5-15 seconds - Allows real-time stability monitoring
 *
 * Configuration is persisted and applied immediately to the connected sensor.
 * If no sensor is connected, configuration is applied on the next connection.
 *
 * @property bleRepository BLE repository that manages sensor communication
 * @author Felipe García Gómez
 */
class ConfigureReadingIntervalsUseCase @Inject constructor(
    private val bleRepository: BleRepository
) {

    /**
     * Configures the sensor weight reading interval.
     *
     * Sets every how many seconds the system requests a new weight
     * measurement from the BLE sensor. Change is applied immediately if sensor
     * is connected, and saved for future connections.
     *
     * This function must be called from a coroutine or suspend function.
     *
     * @param intervalSeconds Interval in seconds between weight readings (e.g., 60 for 1 minute)
     */
    suspend fun setWeightReadInterval(intervalSeconds: Int) {
        val intervalMs = intervalSeconds * 1000L
        bleRepository.saveWeightReadInterval(intervalMs)
        bleRepository.configureReadingIntervals(
            weightIntervalMs = intervalMs,
            inclinationIntervalMs = bleRepository.getInclinationReadInterval()
        )
    }

    /**
     * Configures the sensor inclination reading interval.
     *
     * Sets every how many seconds the system requests a new inclination
     * measurement from the BLE sensor. Change is applied immediately if sensor
     * is connected, and saved for future connections.
     *
     * This function must be called from a coroutine or suspend function.
     *
     * @param intervalSeconds Interval in seconds between inclination readings (e.g., 15 for 15 seconds)
     */
    suspend fun setInclinationReadInterval(intervalSeconds: Int) {
        val intervalMs = intervalSeconds * 1000L
        bleRepository.saveInclinationReadInterval(intervalMs)
        bleRepository.configureReadingIntervals(
            weightIntervalMs = bleRepository.getWeightReadInterval(),
            inclinationIntervalMs = intervalMs
        )
    }

    /**
     * Gets the current weight reading interval in seconds.
     *
     * Returns a Flow that emits the configured interval in seconds and
     * updates when configuration changes.
     *
     * @return Flow that emits the weight interval in seconds
     */
    fun getWeightReadIntervalSeconds(): Flow<Int> {
        return bleRepository.weightReadInterval.map { it.toInt() / 1000 }
    }

    /**
     * Gets the current inclination reading interval in seconds.
     *
     * Returns a Flow that emits the configured interval in seconds and
     * updates when configuration changes.
     *
     * @return Flow that emits the inclination interval in seconds
     */
    fun getInclinationReadIntervalSeconds(): Flow<Int> {
        return bleRepository.inclinationReadInterval.map { it.toInt() / 1000 }
    }

}
