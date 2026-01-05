package com.example.campergas.data.local.vehicle

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.campergas.domain.model.VehicleType

/**
 * Entity representing the vehicle configuration for stability calculations.
 *
 * Stores the physical dimensions of the vehicle (caravan or motorhome) required
 * for calculating wheel elevations and overall vehicle stability
 * based on inclination sensor data.
 *
 * @property id Unique identifier for the configuration (default: "default_config")
 * @property type Vehicle type (CARAVAN or AUTOCARAVANA)
 * @property distanceBetweenRearWheels Distance between rear wheels in centimeters
 * @property distanceToFrontSupport Distance from rear wheels to front support point in cm (caravan only)
 * @property distanceBetweenFrontWheels Distance between front wheels in cm (motorhome only)
 */
@Entity(tableName = "vehicle_config")
data class VehicleConfigEntity(
    @PrimaryKey
    val id: String = "default_config", // We only need one configuration
    val type: VehicleType = VehicleType.CARAVAN,
    val distanceBetweenRearWheels: Float, // Distance between rear wheels in cm
    val distanceToFrontSupport: Float, // Distance between rear wheels and front support point in cm (caravan only)
    val distanceBetweenFrontWheels: Float? = null // Distance between front wheels in cm (motorhome only)
)
