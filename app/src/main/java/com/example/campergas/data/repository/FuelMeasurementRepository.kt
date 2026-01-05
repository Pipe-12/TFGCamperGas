package com.example.campergas.data.repository

import com.example.campergas.data.local.db.FuelMeasurementDao
import com.example.campergas.data.local.db.FuelMeasurementEntity
import com.example.campergas.domain.model.FuelMeasurement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing fuel measurements in the database.
 *
 * This repository acts as an abstraction layer between the domain layer
 * and the persistence layer (Room Database). Provides CRUD operations
 * for fuel measurements and handles conversion between database entities
 * (FuelMeasurementEntity) and domain models (FuelMeasurement).
 *
 * Main responsibilities:
 * - Provide access to fuel measurements with different filters
 * - Insert new measurements (individual or batch)
 * - Delete old measurements or by specific criteria
 * - Convert between domain models and database entities
 * - Expose data as reactive Flows for automatic UI updates
 *
 * Supported measurement types:
 * - Real-time (isHistorical = false): Current measurements from the sensor
 * - Historical/Offline (isHistorical = true): Data synchronized from the sensor
 *
 * @property fuelMeasurementDao Room DAO for database access
 * @author Felipe García Gómez
 */
@Singleton
class FuelMeasurementRepository @Inject constructor(
    private val fuelMeasurementDao: FuelMeasurementDao
) {
    /**
     * Gets all fuel measurements ordered by descending timestamp.
     *
     * @return Flow that emits the complete list of measurements
     */
    fun getAllMeasurements(): Flow<List<FuelMeasurement>> {
        return fuelMeasurementDao.getAllMeasurements().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    /**
     * Gets the most recent real-time measurement.
     *
     * Only returns measurements marked as isHistorical = false.
     *
     * @return Flow that emits the most recent measurement or null if none exists
     */
    fun getLatestRealTimeMeasurement(): Flow<FuelMeasurement?> {
        return fuelMeasurementDao.getLatestRealTimeMeasurement().map { entity ->
            entity?.toDomainModel()
        }
    }

    /**
     * Gets measurements within a specific time range.
     *
     * @param startTime Unix timestamp of the period start
     * @param endTime Unix timestamp of the period end
     * @return Flow that emits the list of measurements in the range
     */
    fun getMeasurementsByTimeRange(startTime: Long, endTime: Long): Flow<List<FuelMeasurement>> {
        return fuelMeasurementDao.getMeasurementsByTimeRange(startTime, endTime).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    /**
     * Inserts a new fuel measurement into the database.
     *
     * This function must be called from a coroutine or suspend function.
     *
     * @param measurement Measurement to insert
     * @return ID assigned to the inserted measurement
     */
    suspend fun insertMeasurement(measurement: FuelMeasurement): Long {
        return fuelMeasurementDao.insertMeasurement(measurement.toEntity())
    }

    /**
     * Inserts multiple measurements into the database in batch.
     *
     * Optimized for inserting large amounts of historical data.
     * This function must be called from a coroutine or suspend function.
     *
     * @param measurements List of measurements to insert
     */
    suspend fun insertMeasurements(measurements: List<FuelMeasurement>) {
        fuelMeasurementDao.insertMeasurements(measurements.map { it.toEntity() })
    }


    /**
     * Gets the last N measurements for a cylinder.
     *
     * Useful for pattern analysis and outlier detection.
     * This function must be called from a coroutine or suspend function.
     *
     * @param cylinderId ID of the cylinder to query
     * @param limit Maximum number of measurements to get
     * @return List with the N most recent measurements ordered by descending timestamp
     */
    suspend fun getLastNMeasurements(cylinderId: Long, limit: Int): List<FuelMeasurement> {
        return fuelMeasurementDao.getLastNMeasurements(cylinderId, limit).map { it.toDomainModel() }
    }

    /**
     * Deletes a specific measurement by its ID.
     *
     * Useful for removing detected outliers.
     * This function must be called from a coroutine or suspend function.
     *
     * @param id ID of the measurement to delete
     */
    suspend fun deleteMeasurementById(id: Long) {
        fuelMeasurementDao.deleteMeasurementById(id)
    }

    /**
     * Converts a database entity to a domain model.
     *
     * @return FuelMeasurement domain model object
     */
    private fun FuelMeasurementEntity.toDomainModel(): FuelMeasurement {
        return FuelMeasurement(
            id = this.id,
            cylinderId = this.cylinderId,
            cylinderName = this.cylinderName,
            timestamp = this.timestamp,
            fuelKilograms = this.fuelKilograms,
            fuelPercentage = this.fuelPercentage,
            totalWeight = this.totalWeight,
            isCalibrated = this.isCalibrated,
            isHistorical = this.isHistorical
        )
    }

    /**
     * Converts a domain model to a database entity.
     *
     * @return FuelMeasurementEntity object for Room persistence
     */
    private fun FuelMeasurement.toEntity(): FuelMeasurementEntity {
        return FuelMeasurementEntity(
            id = this.id,
            cylinderId = this.cylinderId,
            cylinderName = this.cylinderName,
            timestamp = this.timestamp,
            fuelKilograms = this.fuelKilograms,
            fuelPercentage = this.fuelPercentage,
            totalWeight = this.totalWeight,
            isCalibrated = this.isCalibrated,
            isHistorical = this.isHistorical
        )
    }
}
