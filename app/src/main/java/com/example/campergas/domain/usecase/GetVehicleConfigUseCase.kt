package com.example.campergas.domain.usecase

import com.example.campergas.data.repository.VehicleRepository
import com.example.campergas.domain.model.VehicleConfig
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting the recreational vehicle configuration.
 *
 * This use case encapsulates the business logic for retrieving the
 * geometric configuration of the vehicle (caravan or motorhome) stored in the system.
 *
 * Vehicle configuration includes:
 * - Vehicle type (caravan or motorhome)
 * - Distance between rear wheels
 * - Distance to front support point (caravans)
 * - Distance between front wheels (motorhomes)
 * - Gas system total capacity
 *
 * This information is critical for:
 * - Correctly calculating weight distribution on axles
 * - Determining required wheel elevation for leveling
 * - Correctly displaying vehicle geometry in the UI
 * - Generating precise leveling recommendations
 *
 * @property vehicleRepository Vehicle configuration repository
 * @author Felipe García Gómez
 */
class GetVehicleConfigUseCase @Inject constructor(
    private val vehicleRepository: VehicleRepository
) {
    /**
     * Gets the vehicle configuration as a reactive Flow.
     *
     * Returns a Flow that emits the current vehicle configuration and
     * updates automatically when configuration is modified.
     *
     * Value can be null if user hasn't configured their vehicle yet.
     * In that case, the application should show the initial configuration
     * screen for the user to enter dimensions.
     *
     * @return Flow that emits vehicle configuration or null if not configured
     */
    operator fun invoke(): Flow<VehicleConfig?> {
        return vehicleRepository.getVehicleConfig()
    }
}
