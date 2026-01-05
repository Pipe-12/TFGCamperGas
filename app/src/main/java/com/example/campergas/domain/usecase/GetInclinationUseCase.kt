package com.example.campergas.domain.usecase

import com.example.campergas.data.repository.BleRepository
import com.example.campergas.domain.model.Inclination
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting inclination data from the BLE sensor.
 *
 * This use case encapsulates the business logic for accessing inclination
 * measurements (pitch and roll) in real-time from the accelerometer/gyroscope
 * sensor integrated in the CamperGas BLE device.
 *
 * Inclination data is used for:
 * - Real-time vehicle stability monitoring
 * - Detection of irregular surfaces or steep slopes
 * - Calculating leveling needed to stabilize the vehicle
 * - Generating safety alerts when vehicle is not level
 * - Displaying stability visualizations in screens and widgets
 *
 * The Flow emits new measurements as the BLE sensor provides them,
 * typically every few seconds according to interval configuration.
 *
 * @property bleRepository BLE repository that provides access to sensor data
 * @author Felipe García Gómez
 */
class GetInclinationUseCase @Inject constructor(
    private val bleRepository: BleRepository
) {
    /**
     * Gets inclination data as a reactive Flow.
     *
     * Returns a Flow that emits the most recent inclination measurements
     * from the BLE sensor. The Flow updates automatically when new
     * measurements arrive from the sensor.
     *
     * The value can be null if:
     * - No sensor is connected
     * - The sensor hasn't sent inclination measurements yet
     * - Connection to the sensor was lost
     *
     * @return Flow that emits Inclination objects with pitch and roll, or null if no data
     */
    operator fun invoke(): Flow<Inclination?> {
        return bleRepository.inclinationData
    }
}
