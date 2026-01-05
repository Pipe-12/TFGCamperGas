package com.example.campergas.domain.usecase

import com.example.campergas.data.repository.VehicleRepository
import com.example.campergas.domain.model.VehicleConfig
import com.example.campergas.domain.model.VehicleType
import javax.inject.Inject

/**
 * Use case for saving or updating the recreational vehicle configuration.
 *
 * This use case encapsulates the business logic for persisting the
 * geometric configuration of the vehicle (caravan or motorhome) in the database.
 *
 * Behavior:
 * - If no previous configuration exists, creates a new one
 * - If configuration already exists, updates it (only one configuration per app)
 *
 * Provides two ways to save:
 * 1. Via individual parameters (type, distances)
 * 2. Via a complete VehicleConfig object
 *
 * The saved configuration is used for:
 * - Leveling and stability calculations
 * - Vehicle geometry visualizations
 * - Wheel elevation recommendations
 * - Weight distribution on axles
 *
 * Recommended validations (not implemented here, delegate to UI):
 * - Distances must be positive
 * - Distances must be in realistic ranges (e.g., 100-300 cm)
 * - For motorhomes, distanceBetweenFrontWheels should not be null
 *
 * @property vehicleRepository Vehicle configuration repository
 * @author Felipe García Gómez
 */
class SaveVehicleConfigUseCase @Inject constructor(
    private val vehicleRepository: VehicleRepository
) {
    /**
     * Saves or updates vehicle configuration via individual parameters.
     *
     * Creates a VehicleConfig object with the provided parameters and persists
     * it in the database. If configuration already exists, replaces it.
     *
     * This function must be called from a coroutine or suspend function.
     *
     * @param type Vehicle type (CARAVAN or AUTOCARAVANA)
     * @param distanceBetweenRearWheels Distance between rear wheels in centimeters
     * @param distanceToFrontSupport Distance from rear axle to front support in cm (caravans)
     * @param distanceBetweenFrontWheels Distance between front wheels in cm (motorhomes, optional)
     */
    suspend operator fun invoke(
        type: VehicleType,
        distanceBetweenRearWheels: Float,
        distanceToFrontSupport: Float,
        distanceBetweenFrontWheels: Float? = null
    ) {
        val config = VehicleConfig(
            type = type,
            distanceBetweenRearWheels = distanceBetweenRearWheels,
            distanceToFrontSupport = distanceToFrontSupport,
            distanceBetweenFrontWheels = distanceBetweenFrontWheels
        )

        vehicleRepository.saveVehicleConfig(config)
    }

}
