package com.example.campergas.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.campergas.domain.model.GasCylinder

/**
 * Main Room database for the CamperGas application.
 *
 * This database manages gas cylinder information and fuel measurements,
 * providing persistence for real-time sensor data and historical tracking.
 *
 * @property gasCylinderDao Data Access Object for gas cylinder operations
 * @property fuelMeasurementDao Data Access Object for fuel measurement operations
 */
@Database(
    entities = [
        GasCylinder::class,
        FuelMeasurementEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CamperGasDatabase : RoomDatabase() {

    /**
     * Provides access to gas cylinder database operations.
     *
     * @return DAO for managing gas cylinder entities
     */
    abstract fun gasCylinderDao(): GasCylinderDao

    /**
     * Provides access to fuel measurement database operations.
     *
     * @return DAO for managing fuel measurement entities
     */
    abstract fun fuelMeasurementDao(): FuelMeasurementDao

    companion object {
        /** Database file name */
        const val DATABASE_NAME = "campergas_database"
    }
}
