package com.example.campergas.domain.usecase

import com.example.campergas.data.repository.GasCylinderRepository
import com.example.campergas.domain.model.GasCylinder
import javax.inject.Inject

/**
 * Use case for adding a new gas cylinder to the system.
 *
 * This use case encapsulates the business logic for creating and persisting
 * a new gas cylinder with validation of input parameters.
 *
 * Validations performed:
 * - Name cannot be empty or blank
 * - Tare (empty weight) cannot be negative
 * - Capacity must be greater than zero
 *
 * The cylinder can optionally be set as active upon creation, which will
 * automatically deactivate any previously active cylinder.
 *
 * @property repository Gas cylinder repository for database operations
 * @author Felipe García Gómez
 */
class AddGasCylinderUseCase @Inject constructor(
    private val repository: GasCylinderRepository
) {
    /**
     * Adds a new gas cylinder to the database.
     *
     * Creates a new cylinder with the provided parameters after validating them.
     * If setAsActive is true, the new cylinder becomes the active one and all
     * other cylinders are deactivated.
     *
     * @param name Descriptive name for the cylinder (e.g., "Butane 12.5kg")
     * @param tare Weight of the empty cylinder in kilograms
     * @param capacity Maximum gas capacity in kilograms
     * @param setAsActive Whether to set this cylinder as the active one (default: false)
     * @return Result with the ID of the created cylinder on success, or failure with exception
     */
    suspend operator fun invoke(
        name: String,
        tare: Float,
        capacity: Float,
        setAsActive: Boolean = false
    ): Result<Long> {
        return try {
            // Validate name is not empty
            if (name.isBlank()) {
                return Result.failure(IllegalArgumentException("Name cannot be empty"))
            }

            // Validate tare is non-negative
            if (tare < 0) {
                return Result.failure(IllegalArgumentException("Tare cannot be negative"))
            }

            // Validate capacity is positive
            if (capacity <= 0) {
                return Result.failure(IllegalArgumentException("Capacity must be greater than zero"))
            }

            val cylinder = GasCylinder(
                name = name.trim(),
                tare = tare,
                capacity = capacity,
                isActive = setAsActive
            )

            val id = repository.insertCylinder(cylinder)

            // If should be set as active, deactivate the others
            if (setAsActive) {
                repository.setActiveCylinder(id)
            }

            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
