package com.example.campergas.ui.components.gas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campergas.domain.model.GasCylinder
import com.example.campergas.domain.usecase.AddGasCylinderUseCase
import com.example.campergas.domain.usecase.GetActiveCylinderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for gas cylinder operations.
 *
 * @property isLoading Whether an operation is in progress
 * @property errorMessage Error message to display, null if no error
 * @property successMessage Success message to display, null if no success
 */
data class GasCylinderUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * ViewModel for managing gas cylinder operations in UI components.
 *
 * Handles adding new cylinders, tracking the active cylinder, and managing
 * UI state for cylinder-related operations. Provides reactive state flows
 * for UI updates.
 */
@HiltViewModel
class GasCylinderViewModel @Inject constructor(
    private val application: android.app.Application,
    private val addGasCylinderUseCase: AddGasCylinderUseCase,
    getActiveCylinderUseCase: GetActiveCylinderUseCase
) : ViewModel() {

    private val _activeCylinder = MutableStateFlow<GasCylinder?>(null)

    /** Flow of the currently active gas cylinder, null if none active */
    val activeCylinder: StateFlow<GasCylinder?> = _activeCylinder

    private val _uiState = MutableStateFlow(GasCylinderUiState())

    /** Flow of UI state for cylinder operations */
    val uiState: StateFlow<GasCylinderUiState> = _uiState

    init {
        // Observe the active cylinder
        viewModelScope.launch {
            getActiveCylinderUseCase().collectLatest { cylinder ->
                _activeCylinder.value = cylinder
                // Update message if no active cylinder
                if (cylinder == null) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = application.getString(com.example.campergas.R.string.cylinder_no_active_warning)
                    )
                } else {
                    _uiState.value = _uiState.value.copy(errorMessage = null)
                }
            }
        }
    }

    /**
     * Adds a new gas cylinder to the system.
     *
     * Validates input and creates a new cylinder with the provided details.
     * Updates UI state to reflect success or failure.
     *
     * @param name Name/label for the cylinder
     * @param tare Empty weight of the cylinder in kilograms
     * @param capacity Maximum fuel capacity in kilograms
     * @param setAsActive Whether to set this cylinder as the active one
     */
    fun addCylinder(name: String, tare: Float, capacity: Float, setAsActive: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                val result = addGasCylinderUseCase(name, tare, capacity, setAsActive)

                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = application.getString(com.example.campergas.R.string.cylinder_added_success)
                    )
                    // Clear message after a few seconds
                    kotlinx.coroutines.delay(3000)
                    _uiState.value = _uiState.value.copy(successMessage = null)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message
                            ?: application.getString(com.example.campergas.R.string.cylinder_error_adding)
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = application.getString(com.example.campergas.R.string.cylinder_error_format, e.message ?: "")
                )
            }
        }
    }

}
