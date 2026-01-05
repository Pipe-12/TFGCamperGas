package com.example.campergas.domain.usecase

import com.example.campergas.data.repository.GasCylinderRepository
import javax.inject.Inject

/**
 * Use case for setting a gas cylinder as active in the system.
 *
 * This use case encapsulates the business logic for changing which gas cylinder
 * is currently active. Only one cylinder can be active at a time, and changing
 * the active cylinder automatically deactivates the previous one.
 *
 * The active cylinder is crucial because:
 * - All weight measurements are associated with this cylinder
 * - The active cylinder's tare is used to calculate available gas
 * - Widgets and main screens display information from this cylinder
 *
 * Validations performed:
 * - Verifies the cylinder exists before activating it
 * - Captures and reports database errors
 *
 * Common use cases:
 * - Switch from butane to propane cylinder
 * - Activate a spare cylinder when the main one runs out
 * - Select the correct cylinder after adding several
 *
 * @property repository Gas cylinder repository that accesses the database
 * @author Felipe García Gómez
 */
class SetActiveCylinderUseCase @Inject constructor(
    private val repository: GasCylinderRepository
) {
    /**
     * Sets a specific cylinder as active in the system.
     *
     * Looks up the cylinder by its ID and, if it exists, marks it as active.
     * Automatically deactivates any other cylinder that was active.
     *
     * This function must be called from a coroutine or suspend function.
     *
     * @param cylinderId ID of the cylinder to activate
     * @return Result.success(Unit) if the cylinder was activated successfully
     * @return Result.failure with IllegalArgumentException if the cylinder doesn't exist
     * @return Result.failure with the corresponding exception if there's a database error
     */
    suspend operator fun invoke(cylinderId: Long): Result<Unit> {
        return try {
            val cylinder = repository.getCylinderById(cylinderId)
            if (cylinder != null) {
                repository.setActiveCylinder(cylinderId)
                Result.success(Unit)
            } else {
                Result.failure(IllegalArgumentException("Cylinder not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
