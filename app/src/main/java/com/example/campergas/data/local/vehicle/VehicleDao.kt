package com.example.campergas.data.local.vehicle

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for vehicle configuration operations.
 *
 * Manages persistence of vehicle physical dimensions and type configuration,
 * which are used for stability calculations and UI visualization.
 */
@Dao
interface VehicleDao {
    /**
     * Inserts or replaces the vehicle configuration.
     *
     * @param config The vehicle configuration to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicleConfig(config: VehicleConfigEntity)

    /**
     * Updates an existing vehicle configuration.
     *
     * @param config The vehicle configuration to update
     * @return Number of rows updated
     */
    @Update
    suspend fun updateVehicleConfig(config: VehicleConfigEntity): Int

    /**
     * Retrieves the vehicle configuration as a Flow.
     *
     * @param id Configuration ID (default: "default_config")
     * @return Flow emitting the vehicle configuration, or null if not found
     */
    @Query("SELECT * FROM vehicle_config WHERE id = :id")
    fun getVehicleConfig(id: String = "default_config"): Flow<VehicleConfigEntity?>

    /**
     * Checks if a vehicle configuration exists.
     *
     * @param id Configuration ID (default: "default_config")
     * @return True if the configuration exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM vehicle_config WHERE id = :id)")
    suspend fun configExists(id: String = "default_config"): Boolean
}
