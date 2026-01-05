package com.example.campergas.domain.usecase

import com.example.campergas.data.repository.BleRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Use case for checking BLE connection status with CamperGas sensors.
 *
 * This use case encapsulates the business logic for monitoring the
 * connection status with the BLE sensor. Provides both reactive (Flow)
 * and direct access to the connection state.
 *
 * Connection status is critical for:
 * - Enabling/disabling features that require connected sensor
 * - Displaying visual connection indicators in the UI
 * - Deciding whether to start data reading or show connection screen
 * - Handling automatic reconnections when connection is lost
 *
 * Possible states:
 * - true: BLE sensor connected and functional
 * - false: No sensor connected or connection lost
 *
 * @property bleRepository BLE repository that manages connection state
 * @author Felipe García Gómez
 */
class CheckBleConnectionUseCase @Inject constructor(
    private val bleRepository: BleRepository
) {
    /**
     * Gets the BLE connection status as a reactive StateFlow.
     *
     * Returns a StateFlow that emits the current connection status and updates
     * automatically when the state changes (connected/disconnected).
     * Allows the UI to react in real-time to connection changes.
     *
     * @return StateFlow that emits true if there's an active connection, false if not
     */
    operator fun invoke(): StateFlow<Boolean> {
        return bleRepository.connectionState
    }

    /**
     * Checks if there's an active BLE connection at this moment.
     *
     * Gets the current value of the connection state without creating a reactive
     * subscription. Useful for one-time checks.
     *
     * @return true if a BLE sensor is connected, false otherwise
     */
    fun isConnected(): Boolean {
        return bleRepository.connectionState.value
    }
}
