package com.example.campergas.domain.usecase

import com.example.campergas.data.repository.BleRepository
import javax.inject.Inject

/**
 * Use case for requesting an immediate inclination reading from the BLE sensor.
 *
 * This use case encapsulates the business logic for forcing an inclination
 * reading (pitch and roll) from the BLE sensor outside the automatic periodic
 * readings cycle.
 *
 * Main utility:
 * - Get updated measurement when user needs to check leveling
 * - Update stability data before showing widgets or charts
 * - Verify inclination without waiting for the next automatic cycle
 * - Allow user to manually "refresh" leveling data
 *
 * How it works:
 * The BLE sensor reads the inclination characteristic on demand (READ operation).
 * The response is processed asynchronously and updates the inclination data
 * Flow automatically when it arrives.
 *
 * Difference from automatic readings:
 * - Automatic readings: Occur every X seconds according to configuration
 * - On-demand reading: Occurs immediately when invoking this use case
 *
 * Requirements:
 * - A BLE sensor must be connected
 * - The sensor must have a functional accelerometer/gyroscope
 *
 * @property bleRepository BLE repository that manages sensor communication
 * @author Felipe García Gómez
 */
class RequestInclinationDataUseCase @Inject constructor(
    private val bleRepository: BleRepository
) {
    /**
     * Immediately requests an inclination reading from the BLE sensor.
     *
     * Executes a BLE reading of the inclination characteristic without waiting
     * for the next automatic reading cycle. The result is delivered
     * asynchronously through the inclination data Flow.
     *
     * The invocation is safe even if no sensor is connected (does nothing
     * in that case). Does not block execution.
     */
    operator fun invoke() {
        bleRepository.readInclinationDataOnDemand()
    }
}
