package com.example.campergas.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for fuel measurement operations.
 *
 * Provides methods to insert, update, delete, and query fuel measurements
 * from the local database. Supports real-time and historical data tracking,
 * time range queries, and cylinder-specific measurements.
 */
@Dao
interface FuelMeasurementDao {

    /**
     * Retrieves all fuel measurements ordered by descending timestamp.
     *
     * @return Flow emitting a list of all measurements, most recent first
     */
    @Query("SELECT * FROM fuel_measurements ORDER BY timestamp DESC")
    fun getAllMeasurements(): Flow<List<FuelMeasurementEntity>>

    /**
     * Retrieves measurements for a specific gas cylinder.
     *
     * @param cylinderId ID of the gas cylinder
     * @return Flow emitting measurements for the specified cylinder, most recent first
     */
    @Query("SELECT * FROM fuel_measurements WHERE cylinderId = :cylinderId ORDER BY timestamp DESC")
    fun getMeasurementsByCylinder(cylinderId: Long): Flow<List<FuelMeasurementEntity>>

    /**
     * Retrieves the most recent real-time (non-historical) measurement.
     *
     * @return Flow emitting the latest real-time measurement, or null if none exists
     */
    @Query("SELECT * FROM fuel_measurements WHERE isHistorical = 0 ORDER BY timestamp DESC LIMIT 1")
    fun getLatestRealTimeMeasurement(): Flow<FuelMeasurementEntity?>

    /**
     * Retrieves all historical measurements for a specific cylinder.
     *
     * @param cylinderId ID of the gas cylinder
     * @return Flow emitting historical measurements for the cylinder, most recent first
     */
    @Query("SELECT * FROM fuel_measurements WHERE isHistorical = 1 AND cylinderId = :cylinderId ORDER BY timestamp DESC")
    fun getHistoricalMeasurements(cylinderId: Long): Flow<List<FuelMeasurementEntity>>

    /**
     * Retrieves measurements within a specific time range.
     *
     * @param startTime Start of the time range (milliseconds since epoch)
     * @param endTime End of the time range (milliseconds since epoch)
     * @return Flow emitting measurements within the time range, most recent first
     */
    @Query("SELECT * FROM fuel_measurements WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getMeasurementsByTimeRange(
        startTime: Long,
        endTime: Long
    ): Flow<List<FuelMeasurementEntity>>

    /**
     * Retrieves measurements for a specific cylinder within a time range.
     *
     * @param cylinderId ID of the gas cylinder
     * @param startTime Start of the time range (milliseconds since epoch)
     * @param endTime End of the time range (milliseconds since epoch)
     * @return Flow emitting matching measurements, most recent first
     */
    @Query(
        """
        SELECT * FROM fuel_measurements 
        WHERE cylinderId = :cylinderId AND timestamp BETWEEN :startTime AND :endTime 
        ORDER BY timestamp DESC
    """
    )
    fun getMeasurementsByCylinderAndTimeRange(
        cylinderId: Long,
        startTime: Long,
        endTime: Long
    ): Flow<List<FuelMeasurementEntity>>

    /**
     * Inserts a new measurement, replacing if conflict occurs.
     *
     * @param measurement The measurement to insert
     * @return Row ID of the inserted measurement
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurement(measurement: FuelMeasurementEntity): Long

    /**
     * Inserts multiple measurements, replacing on conflicts.
     *
     * @param measurements List of measurements to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurements(measurements: List<FuelMeasurementEntity>)

    /**
     * Updates an existing measurement.
     *
     * @param measurement The measurement to update
     */
    @Update
    suspend fun updateMeasurement(measurement: FuelMeasurementEntity)

    /**
     * Deletes a specific measurement.
     *
     * @param measurement The measurement to delete
     */
    @Delete
    suspend fun deleteMeasurement(measurement: FuelMeasurementEntity)

    /**
     * Deletes a measurement by its ID.
     *
     * @param id ID of the measurement to delete
     */
    @Query("DELETE FROM fuel_measurements WHERE id = :id")
    suspend fun deleteMeasurementById(id: Long)

    /**
     * Deletes all measurements for a specific cylinder.
     *
     * @param cylinderId ID of the gas cylinder
     */
    @Query("DELETE FROM fuel_measurements WHERE cylinderId = :cylinderId")
    suspend fun deleteMeasurementsByCylinder(cylinderId: Long)

    /**
     * Deletes measurements older than a specified timestamp.
     *
     * Useful for maintaining database size and removing stale data.
     *
     * @param beforeTimestamp Timestamp threshold (milliseconds since epoch)
     */
    @Query("DELETE FROM fuel_measurements WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOldMeasurements(beforeTimestamp: Long)

    /**
     * Deletes all measurements from the database.
     */
    @Query("DELETE FROM fuel_measurements")
    suspend fun deleteAllMeasurements()

    /**
     * Retrieves the most recent measurements up to a specified limit.
     *
     * @param limit Maximum number of measurements to retrieve
     * @return Flow emitting the most recent measurements
     */
    @Query("SELECT * FROM fuel_measurements ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMeasurements(limit: Int): Flow<List<FuelMeasurementEntity>>

    /**
     * Retrieves the last two measurements for a specific cylinder.
     *
     * Used for calculating consumption rate and trends.
     *
     * @param cylinderId ID of the gas cylinder
     * @return List of the two most recent measurements for the cylinder
     */
    @Query(
        """
        SELECT * FROM fuel_measurements 
        WHERE cylinderId = :cylinderId 
        ORDER BY timestamp DESC 
        LIMIT 2
    """
    )
    suspend fun getLastTwoMeasurements(cylinderId: Long): List<FuelMeasurementEntity>

    /**
     * Retrieves the last N measurements for a specific cylinder.
     *
     * @param cylinderId ID of the gas cylinder
     * @param limit Number of measurements to retrieve
     * @return List of the N most recent measurements for the cylinder
     */
    @Query(
        """
        SELECT * FROM fuel_measurements 
        WHERE cylinderId = :cylinderId 
        ORDER BY timestamp DESC 
        LIMIT :limit
    """
    )
    suspend fun getLastNMeasurements(cylinderId: Long, limit: Int): List<FuelMeasurementEntity>
}
