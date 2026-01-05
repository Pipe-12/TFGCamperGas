package com.example.campergas.data.repository

import com.example.campergas.data.local.vehicle.VehicleConfigEntity
import com.example.campergas.data.local.vehicle.VehicleDao
import com.example.campergas.domain.model.VehicleConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing recreational vehicle configuration.
 *
 * This repository provides access to the vehicle's geometric configuration
 * (caravan or motorhome), handling conversion between database entities
 * and domain models.
 *
 * Features:
 * - Only one vehicle configuration exists per application
 * - When saving, updates existing or creates new if it doesn't exist
 * - Stores critical dimensions for leveling and stability calculations
 *
 * The configuration includes:
 * - Vehicle type (caravan/motorhome)
 * - Distance between rear wheels
 * - Distance to front support point (caravans)
 * - Distance between front wheels (motorhomes)
 * - Gas system capacity
 *
 * This data is used for:
 * - Calculating weight distribution on axles
 * - Determining required wheel elevation for leveling
 * - Displaying vehicle geometry in the UI
 * - Generating leveling recommendations
 *
 * @property vehicleDao Room DAO for database access
 * @author Felipe García Gómez
 */
@Singleton
class VehicleRepository @Inject constructor(
    private val vehicleDao: VehicleDao
) {
    /**
     * Gets the vehicle configuration.
     *
     * @return Flow that emits the current configuration or null if not configured
     */
    fun getVehicleConfig(): Flow<VehicleConfig?> {
        return vehicleDao.getVehicleConfig().map { entity ->
            entity?.toDomainModel()
        }
    }

    /**
     * Saves or updates the vehicle configuration.
     *
     * If a configuration already exists, updates it. If not, creates a new one.
     * Only one vehicle configuration can exist in the system.
     *
     * This function must be called from a coroutine or suspend function.
     *
     * @param config Vehicle configuration to save
     */
    suspend fun saveVehicleConfig(config: VehicleConfig) {
        val entity = config.toEntity()
        val exists = vehicleDao.configExists()

        if (exists) {
            vehicleDao.updateVehicleConfig(entity)
        } else {
            vehicleDao.insertVehicleConfig(entity)
        }
    }

    /**
     * Converts a database entity to a domain model.
     *
     * @return VehicleConfig domain model object
     */
    private fun VehicleConfigEntity.toDomainModel(): VehicleConfig {
        return VehicleConfig(
            type = this.type,
            distanceBetweenRearWheels = this.distanceBetweenRearWheels,
            distanceToFrontSupport = this.distanceToFrontSupport,
            distanceBetweenFrontWheels = this.distanceBetweenFrontWheels
        )
    }

    /**
     * Converts a domain model to a database entity.
     *
     * @return VehicleConfigEntity object for Room persistence
     */
    private fun VehicleConfig.toEntity(): VehicleConfigEntity {
        return VehicleConfigEntity(
            type = this.type,
            distanceBetweenRearWheels = this.distanceBetweenRearWheels,
            distanceToFrontSupport = this.distanceToFrontSupport,
            distanceBetweenFrontWheels = this.distanceBetweenFrontWheels
        )
    }
}
