package com.example.campergas.data.local.vehicle

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database for vehicle configuration storage.
 *
 * Maintains the vehicle's physical dimensions and type information used for
 * stability calculations. Includes migration from version 1 to 2 to support
 * updated vehicle types and additional measurements.
 */
@Database(
    entities = [VehicleConfigEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(VehicleTypeConverter::class)
abstract class VehicleDatabase : RoomDatabase() {
    /**
     * Provides access to vehicle configuration database operations.
     *
     * @return DAO for managing vehicle configuration
     */
    abstract fun vehicleDao(): VehicleDao

    companion object {
        /**
         * Migration from database version 1 to version 2.
         *
         * Adds the distanceBetweenFrontWheels column for motorhome support
         * and updates the MOTORHOME enum value to AUTOCARAVANA.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new column
                database.execSQL("ALTER TABLE vehicle_config ADD COLUMN distanceBetweenFrontWheels REAL")

                // Update enum MOTORHOME to AUTOCARAVANA if it exists
                database.execSQL("UPDATE vehicle_config SET type = 'AUTOCARAVANA' WHERE type = 'MOTORHOME'")
            }
        }
    }
}
