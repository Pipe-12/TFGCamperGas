package com.example.campergas.domain.usecase

import com.example.campergas.data.local.db.FuelMeasurementDao
import com.example.campergas.data.local.db.GasCylinderDao
import com.example.campergas.domain.model.GasCylinder
import javax.inject.Inject

/**
 * Use case to delete all non-active gas cylinders and their associated measurements.
 *
 * This is useful for cleaning up test data or removing old cylinders that are no longer
 * in use. Only the currently active cylinder and its measurements are preserved.
 *
 * @property gasCylinderDao DAO for gas cylinder operations
 * @property fuelMeasurementDao DAO for fuel measurement operations
 */
class DeleteNonActiveCylindersUseCase @Inject constructor(
    private val gasCylinderDao: GasCylinderDao,
    private val fuelMeasurementDao: FuelMeasurementDao
) {
    /**
     * Deletes all non-active cylinders and their measurements.
     *
     * Process:
     * 1. Verify there's an active cylinder to preserve
     * 2. Get all non-active cylinders
     * 3. Delete measurements for each non-active cylinder
     * 4. Delete all non-active cylinders
     *
     * @return Result with number of cylinders deleted or error
     */
    suspend operator fun invoke(): Result<Int> {
        return try {
            // Get the active cylinder to ensure we don't delete everything
            val activeCylinder = gasCylinderDao.getActiveCylinderSync()
                ?: return Result.failure(Exception("No active cylinder found"))

            // Get all cylinders to identify non-active ones
            val allCylinders = gasCylinderDao.getAllCylindersSync()

            // Filter non-active cylinders
            val nonActiveCylinders = allCylinders.filter { it.id != activeCylinder.id }

            // Delete measurements for each non-active cylinder
            // This is done before deleting cylinders to ensure clean up
            nonActiveCylinders.forEach { cylinder ->
                fuelMeasurementDao.deleteMeasurementsByCylinder(cylinder.id)
            }

            // Delete all non-active cylinders
            val deletedCount = gasCylinderDao.deleteNonActiveCylinders()

            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
