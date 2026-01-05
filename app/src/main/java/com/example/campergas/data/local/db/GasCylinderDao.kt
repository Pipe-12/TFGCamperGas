package com.example.campergas.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.campergas.domain.model.GasCylinder
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for gas cylinder operations.
 *
 * Provides methods for managing gas cylinders, including insertion, updates,
 * queries, and active cylinder state handling. Only one cylinder can be active
 * at a time.
 */
@Dao
interface GasCylinderDao {

    /**
     * Retrieves all gas cylinders ordered by creation date.
     *
     * @return Flow emitting list of all cylinders, most recent first
     */
    @Query("SELECT * FROM gas_cylinders ORDER BY createdAt DESC")
    fun getAllCylinders(): Flow<List<GasCylinder>>

    /**
     * Retrieves all gas cylinders synchronously.
     *
     * @return List of all cylinders, most recent first
     */
    @Query("SELECT * FROM gas_cylinders ORDER BY createdAt DESC")
    suspend fun getAllCylindersSync(): List<GasCylinder>

    /**
     * Retrieves the currently active gas cylinder.
     *
     * Only one cylinder can be active at a time.
     *
     * @return Flow emitting the active cylinder, or null if none is active
     */
    @Query("SELECT * FROM gas_cylinders WHERE isActive = 1 LIMIT 1")
    fun getActiveCylinder(): Flow<GasCylinder?>

    /**
     * Retrieves the currently active gas cylinder synchronously.
     *
     * @return The active cylinder, or null if none is active
     */
    @Query("SELECT * FROM gas_cylinders WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveCylinderSync(): GasCylinder?

    /**
     * Retrieves a specific cylinder by its ID.
     *
     * @param id ID of the cylinder to retrieve
     * @return The cylinder with the specified ID, or null if not found
     */
    @Query("SELECT * FROM gas_cylinders WHERE id = :id")
    suspend fun getCylinderById(id: Long): GasCylinder?

    /**
     * Inserts a new gas cylinder.
     *
     * @param cylinder The cylinder to insert
     * @return Row ID of the inserted cylinder
     */
    @Insert
    suspend fun insertCylinder(cylinder: GasCylinder): Long

    /**
     * Updates an existing gas cylinder.
     *
     * @param cylinder The cylinder to update
     */
    @Update
    suspend fun updateCylinder(cylinder: GasCylinder)

    /**
     * Deactivates all gas cylinders.
     *
     * Used internally to ensure only one cylinder is active at a time.
     */
    @Query("UPDATE gas_cylinders SET isActive = 0")
    suspend fun deactivateAllCylinders()

    /**
     * Sets a cylinder as active, deactivating all others.
     *
     * This is a transactional operation that ensures data consistency.
     *
     * @param cylinderId ID of the cylinder to set as active
     */
    @Transaction
    suspend fun setActiveCylinder(cylinderId: Long) {
        deactivateAllCylinders()
        updateCylinderActiveStatus(cylinderId, true)
    }

    /**
     * Updates the active status of a specific cylinder.
     *
     * @param id ID of the cylinder to update
     * @param isActive New active status
     */
    @Query("UPDATE gas_cylinders SET isActive = :isActive WHERE id = :id")
    suspend fun updateCylinderActiveStatus(id: Long, isActive: Boolean)

    /**
     * Deletes all gas cylinders that are not active.
     *
     * @return Number of cylinders deleted
     */
    @Query("DELETE FROM gas_cylinders WHERE isActive = 0")
    suspend fun deleteNonActiveCylinders(): Int
}
