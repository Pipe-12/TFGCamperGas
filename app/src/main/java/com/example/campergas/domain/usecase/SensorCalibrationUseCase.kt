package com.example.campergas.domain.usecase

import com.example.campergas.data.repository.BleRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * Use case for sensor calibration operations.
 *
 * This use case encapsulates the business logic for calibrating the BLE sensor,
 * including tare (zeroing) the scale and calibrating with a known weight.
 *
 * Calibration operations:
 * - Tare: Sets the current weight reading as zero reference
 * - Calibrate: Adjusts the scale factor using a known weight
 *
 * Both operations require an active BLE connection to the sensor.
 * The calibration values are persisted in the sensor's EEPROM.
 *
 * @property bleRepository BLE repository that manages sensor communication
 * @author Felipe García Gómez
 */
class SensorCalibrationUseCase @Inject constructor(
    private val bleRepository: BleRepository
) {

    /**
     * Gets the current BLE connection state.
     *
     * @return StateFlow indicating if sensor is connected
     */
    val connectionState: StateFlow<Boolean> = bleRepository.connectionState

    /**
     * Checks if the sensor is currently connected.
     *
     * @return true if connected, false otherwise
     */
    fun isConnected(): Boolean = bleRepository.isConnected()

    /**
     * Performs tare (zero) operation on the sensor.
     *
     * This operation sets the current weight reading as zero reference.
     * Ensure the scale is empty before calling this function.
     *
     * @return Result indicating success or failure with error message
     */
    suspend fun performTare(): Result<Unit> = suspendCancellableCoroutine { continuation ->
        if (!bleRepository.isConnected()) {
            continuation.resume(Result.failure(Exception("Sensor not connected")))
            return@suspendCancellableCoroutine
        }

        bleRepository.performTare { success ->
            if (success) {
                continuation.resume(Result.success(Unit))
            } else {
                continuation.resume(Result.failure(Exception("Tare operation failed")))
            }
        }
    }

    /**
     * Performs calibration with a known weight value.
     *
     * This operation adjusts the sensor's scale factor based on a reference weight.
     * Place the known weight on the scale before calling this function.
     *
     * @param knownWeight Weight in kilograms (must be positive)
     * @return Result indicating success or failure with error message
     */
    suspend fun performCalibration(knownWeight: Float): Result<Unit> = suspendCancellableCoroutine { continuation ->
        if (!bleRepository.isConnected()) {
            continuation.resume(Result.failure(Exception("Sensor not connected")))
            return@suspendCancellableCoroutine
        }

        if (knownWeight <= 0) {
            continuation.resume(Result.failure(Exception("Weight must be positive")))
            return@suspendCancellableCoroutine
        }

        bleRepository.performCalibration(knownWeight) { success ->
            if (success) {
                continuation.resume(Result.success(Unit))
            } else {
                continuation.resume(Result.failure(Exception("Calibration operation failed")))
            }
        }
    }
}