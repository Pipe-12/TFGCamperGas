package com.example.campergas.domain.usecase

import com.example.campergas.data.repository.BleRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Use case for reading CamperGas sensor data on demand.
 *
 * This use case encapsulates the business logic for requesting immediate
 * readings of BLE sensor data, outside the automatic periodic readings
 * cycle. Allows the user or system to get updated measurements
 * at any time.
 *
 * Provided functionality:
 * - On-demand cylinder weight reading
 * - On-demand vehicle inclination reading
 * - Combined reading of all sensor data
 * - Connection state query
 *
 * On-demand readings are useful for:
 * - Updating data when the user requests it ("refresh" button)
 * - Getting measurements before critical operations
 * - Verifying values without waiting for the next automatic cycle
 * - Responding to application events that require current data
 *
 * Results are delivered asynchronously through the corresponding
 * StateFlows in the BLE repository.
 *
 * @property bleRepository BLE repository that manages sensor communication
 * @author Felipe García Gómez
 */
class ReadSensorDataUseCase @Inject constructor(
    private val bleRepository: BleRepository
) {

    /**
     * Requests on-demand weight data reading.
     *
     * Executes an immediate BLE reading of the weight characteristic.
     * The result is delivered through the fuel data StateFlow
     * in the BLE repository.
     *
     * This function is private because readAllSensorData() is preferred
     * to get a complete view of the sensor state.
     */
    private fun readWeightData() {
        bleRepository.readWeightDataOnDemand()
    }

    /**
     * Requests on-demand inclination data reading.
     *
     * Executes an immediate BLE reading of the inclination characteristic.
     * The result is delivered through the inclination data StateFlow
     * in the BLE repository.
     *
     * This function is private because readAllSensorData() is preferred
     * to get a complete view of the sensor state.
     */
    private fun readInclinationData() {
        bleRepository.readInclinationDataOnDemand()
    }

    /**
     * Requests on-demand reading of all sensor data (weight and inclination).
     *
     * Executes immediate BLE readings of both characteristics:
     * - Total cylinder weight
     * - Vehicle inclination (pitch and roll)
     *
     * Results are delivered asynchronously through their respective
     * StateFlows. This is the recommended way to get a complete
     * update of the sensor state.
     *
     * Safe to call this method even if no sensor is connected (does nothing).
     */
    fun readAllSensorData() {
        readWeightData()
        readInclinationData()
    }

    /**
     * Gets the BLE connection state as StateFlow.
     *
     * Allows observing the connection state in real-time, useful for
     * enabling/disabling data reading functionality based on
     * whether a sensor is connected.
     *
     * @return StateFlow that emits true if there's an active connection, false otherwise
     */
    fun getConnectionState(): StateFlow<Boolean> {
        return bleRepository.connectionState
    }
}
