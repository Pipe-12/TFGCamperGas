package com.example.campergas.domain.usecase

import com.example.campergas.data.repository.GasCylinderRepository
import com.example.campergas.domain.model.GasCylinder
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting the currently active gas cylinder.
 *
 * This use case encapsulates the business logic for retrieving the gas
 * cylinder that is marked as active in the system. The active cylinder is the
 * one used for current weight and gas consumption measurements.
 *
 * Provides two access variants:
 * - Reactive (Flow): For observing real-time changes to the active cylinder
 * - Synchronous (suspend): For getting the current value at a specific moment
 *
 * Main use cases:
 * - Display current cylinder information on the main screen
 * - Calculate available gas using the active cylinder's tare
 * - Associate weight measurements to the correct cylinder
 * - Validate that an active cylinder exists before performing operations
 *
 * @property repository Gas cylinder repository that accesses the database
 * @author Felipe García Gómez
 */
class GetActiveCylinderUseCase @Inject constructor(
    private val repository: GasCylinderRepository
) {
    /**
     * Gets the active cylinder as a reactive Flow.
     *
     * Returns a Flow that emits the current active cylinder and updates
     * automatically when it changes in the database. Allows the UI
     * to react to real-time changes.
     *
     * @return Flow that emits the active cylinder or null if none is active
     */
    operator fun invoke(): Flow<GasCylinder?> {
        return repository.getActiveCylinder()
    }

    /**
     * Gets the active cylinder synchronously.
     *
     * Executes a direct database query to get the current active cylinder.
     * Useful when the value is needed at a specific moment without observing
     * continuous changes.
     *
     * This function must be called from a coroutine or suspend function.
     *
     * @return The current active cylinder or null if none is active
     */
    suspend fun getActiveCylinderSync(): GasCylinder? {
        return repository.getActiveCylinderSync()
    }
}
