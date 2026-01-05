package com.example.campergas.domain.usecase

import com.example.campergas.data.repository.BleRepository
import javax.inject.Inject

/**
 * Use case for requesting an immediate weight reading from the BLE sensor.
 *
 * This use case encapsulates the business logic for forcing a weight
 * reading from the BLE sensor outside the automatic periodic readings cycle.
 *
 * Main utility:
 * - Get updated measurement when user explicitly requests it
 * - Update data before a critical operation
 * - Verify measurements without waiting for the next automatic cycle
 * - Allow user to manually "refresh" data
 *
 * How it works:
 * The BLE sensor reads the weight characteristic on demand (READ operation).
 * The response is processed asynchronously and updates the fuel data
 * Flow automatically when it arrives.
 *
 * Difference from automatic readings:
 * - Automatic readings: Occur every X minutes according to configuration
 * - On-demand reading: Occurs immediately when invoking this use case
 *
 * Requirements:
 * - A BLE sensor must be connected
 * - The sensor must support weight characteristic reading
 *
 * @property bleRepository BLE repository that manages sensor communication
 * @author Felipe García Gómez
 */
class RequestWeightDataUseCase @Inject constructor(
    private val bleRepository: BleRepository
) {
    /**
     * Immediately requests a weight reading from the BLE sensor.
     *
     * Executes a BLE reading of the weight characteristic without waiting for
     * the next automatic reading cycle. The result is delivered
     * asynchronously through the fuel data Flow.
     *
     * The invocation is safe even if no sensor is connected (does nothing
     * in that case). Does not block execution.
     */
    operator fun invoke() {
        bleRepository.readWeightDataOnDemand()
    }
}
