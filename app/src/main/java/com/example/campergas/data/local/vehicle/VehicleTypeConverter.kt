package com.example.campergas.data.local.vehicle

import androidx.room.TypeConverter
import com.example.campergas.domain.model.VehicleType

/**
 * Type converter for Room database to handle VehicleType enum.
 *
 * Converts between VehicleType enum values and String representation
 * for database storage.
 */
class VehicleTypeConverter {
    /**
     * Converts VehicleType enum to String for database storage.
     *
     * @param vehicleType The VehicleType enum value
     * @return String representation of the vehicle type
     */
    @TypeConverter
    fun fromVehicleType(vehicleType: VehicleType): String {
        return vehicleType.name
    }

    /**
     * Converts String from database to VehicleType enum.
     *
     * @param value String representation of the vehicle type
     * @return VehicleType enum value, defaults to CARAVAN if conversion fails
     */
    @TypeConverter
    fun toVehicleType(value: String): VehicleType {
        return try {
            VehicleType.valueOf(value)
        } catch (_: Exception) {
            VehicleType.CARAVAN // Default value
        }
    }
}
