package com.example.campergas.data.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.campergas.domain.model.GasCylinder

/**
 * Room entity representing a fuel measurement record in the database.
 *
 * Stores fuel level measurements from the BLE sensor, including both
 * real-time readings and historical data synchronized from the sensor's
 * offline storage.
 *
 * @property id Auto-generated unique identifier
 * @property cylinderId Foreign key reference to the gas cylinder
 * @property cylinderName Name of the cylinder for UI reference
 * @property timestamp Unix timestamp (milliseconds) when measurement was taken
 * @property fuelKilograms Available fuel kilograms (already calculated)
 * @property fuelPercentage Fuel percentage (0-100)
 * @property totalWeight Total measured weight (cylinder + fuel) in kg
 * @property isCalibrated Indicates if measurement is calibrated with cylinder tare
 * @property isHistorical Indicates if this is historical/offline data
 */
@Entity(
    tableName = "fuel_measurements",
    foreignKeys = [
        ForeignKey(
            entity = GasCylinder::class,
            parentColumns = ["id"],
            childColumns = ["cylinderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["cylinderId"]),
        Index(value = ["timestamp"])
    ]
)
data class FuelMeasurementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cylinderId: Long,
    val cylinderName: String,
    val timestamp: Long,
    val fuelKilograms: Float,
    val fuelPercentage: Float,
    val totalWeight: Float,
    val isCalibrated: Boolean = true,
    val isHistorical: Boolean = false
)
