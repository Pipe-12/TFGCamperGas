package com.example.campergas.data.repository

import com.example.campergas.data.local.db.GasCylinderDao
import com.example.campergas.domain.model.GasCylinder
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing gas cylinders in the database.
 *
 * This repository provides an abstraction layer for all CRUD operations
 * related to gas cylinders. It manages both data persistence and business
 * logic related to cylinder active status.
 *
 * Key functionality:
 * - Only one cylinder can be active at a time
 * - When activating a cylinder, others are automatically deactivated
 * - Support for multiple cylinders (different types, sizes, capacities)
 * - Reactive access via Flows for automatic UI updates
 *
 * Cylinders store:
 * - Descriptive name
 * - Tare (empty weight) for precise available gas calculations
 * - Maximum gas capacity
 * - Active/inactive status
 * - Creation date
 *
 * @property gasCylinderDao Room DAO for database access
 * @author Felipe García Gómez
 */
@Singleton
class GasCylinderRepository @Inject constructor(
    private val gasCylinderDao: GasCylinderDao
) {

    /**
     * Gets all registered gas cylinders.
     *
     * @return Flow that emits the list of all cylinders ordered by creation date
     */
    fun getAllCylinders(): Flow<List<GasCylinder>> = gasCylinderDao.getAllCylinders()

    /**
     * Gets the currently active cylinder.
     *
     * Only one cylinder can be active at a time. The active cylinder is
     * used for all current weight and gas measurements.
     *
     * @return Flow that emits the active cylinder or null if none is active
     */
    fun getActiveCylinder(): Flow<GasCylinder?> = gasCylinderDao.getActiveCylinder()

    /**
     * Gets the active cylinder synchronously.
     *
     * Executes a direct query without creating a reactive subscription.
     * This function must be called from a coroutine or suspend function.
     *
     * @return The active cylinder or null if none is active
     */
    suspend fun getActiveCylinderSync(): GasCylinder? = gasCylinderDao.getActiveCylinderSync()

    /**
     * Gets a specific cylinder by its ID.
     *
     * This function must be called from a coroutine or suspend function.
     *
     * @param id ID of the cylinder to find
     * @return The found cylinder or null if it doesn't exist
     */
    suspend fun getCylinderById(id: Long): GasCylinder? = gasCylinderDao.getCylinderById(id)

    /**
     * Inserts a new cylinder into the database.
     *
     * This function must be called from a coroutine or suspend function.
     *
     * @param cylinder Cylinder to insert
     * @return ID assigned to the inserted cylinder
     */
    suspend fun insertCylinder(cylinder: GasCylinder): Long {
        return gasCylinderDao.insertCylinder(cylinder)
    }


    /**
     * Sets a cylinder as active, deactivating all others.
     *
     * This operation is atomic: first deactivates all cylinders
     * then activates the specified one. Guarantees that only one cylinder
     * is active at a time.
     *
     * The active cylinder is critical because:
     * - All weight measurements are associated with this cylinder
     * - Its tare is used to calculate available gas
     * - It defines 100% capacity for percentage calculations
     *
     * This function must be called from a coroutine or suspend function.
     *
     * @param cylinderId ID of the cylinder to activate
     */
    suspend fun setActiveCylinder(cylinderId: Long) {
        gasCylinderDao.setActiveCylinder(cylinderId)
    }


}
