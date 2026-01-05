package com.example.campergas.domain.usecase

import com.example.campergas.data.repository.GasCylinderRepository
import com.example.campergas.domain.model.GasCylinder
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting the list of all registered gas cylinders.
 *
 * This use case encapsulates the business logic for retrieving all gas
 * cylinders stored in the system, as well as accessing the current active cylinder.
 *
 * Provided functionality:
 * - Get complete list of cylinders (reactive with Flow)
 * - Get the current active cylinder (reactive and synchronous)
 *
 * Common use cases:
 * - Display cylinder list in configuration screen
 * - Allow user to select a cylinder to activate
 * - View all cylinders with their characteristics (tare, capacity)
 * - Manage multiple cylinders (add, edit, activate, delete)
 *
 * @property repository Gas cylinder repository that accesses the database
 * @author Felipe García Gómez
 */
class GetGasCylindersUseCase @Inject constructor(
    private val repository: GasCylinderRepository
) {
    /**
     * Gets all gas cylinders as a reactive Flow.
     *
     * Returns a Flow that emits the complete list of registered cylinders
     * and updates automatically when there are changes (add, delete, modify).
     *
     * @return Flow that emits the list of all cylinders (can be empty if no cylinders exist)
     */
    operator fun invoke(): Flow<List<GasCylinder>> {
        return repository.getAllCylinders()
    }

}
