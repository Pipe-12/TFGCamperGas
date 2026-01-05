package com.example.campergas.ui.screens.caravanconfig

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.campergas.domain.model.VehicleType
import com.example.campergas.domain.usecase.GetVehicleConfigUseCase
import com.example.campergas.domain.usecase.SaveVehicleConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for vehicle configuration management.
 *
 * Manages:
 * - Vehicle type selection (Caravan/Motorhome)
 * - Physical dimension inputs and validation
 * - Configuration persistence
 * - Loading and saving vehicle setup
 *
 * Provides reactive state for the vehicle configuration screen.
 */
@HiltViewModel
class CaravanConfigViewModel @Inject constructor(
    private val getVehicleConfigUseCase: GetVehicleConfigUseCase,
    private val saveVehicleConfigUseCase: SaveVehicleConfigUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaravanConfigUiState())

    /** Flow of UI state for the vehicle configuration screen */
    val uiState: StateFlow<CaravanConfigUiState> = _uiState.asStateFlow()

    init {
        loadVehicleConfig()
    }

    private fun loadVehicleConfig() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                getVehicleConfigUseCase().collect { config ->
                    if (config != null) {
                        _uiState.value = _uiState.value.copy(
                            selectedVehicleType = config.type,
                            distanceBetweenWheels = config.distanceBetweenRearWheels,
                            distanceToFrontSupport = config.distanceToFrontSupport,
                            distanceBetweenFrontWheels = config.distanceBetweenFrontWheels ?: 0f,
                            isLoading = false,
                            error = null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun updateVehicleType(vehicleType: VehicleType) {
        _uiState.value = _uiState.value.copy(selectedVehicleType = vehicleType)
    }

    fun updateDistanceBetweenWheels(distance: Float) {
        _uiState.value = _uiState.value.copy(distanceBetweenWheels = distance)
    }

    fun updateDistanceToFrontSupport(distance: Float) {
        _uiState.value = _uiState.value.copy(distanceToFrontSupport = distance)
    }

    fun updateDistanceBetweenFrontWheels(distance: Float) {
        _uiState.value = _uiState.value.copy(distanceBetweenFrontWheels = distance)
    }

    fun saveConfiguration() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            try {
                saveVehicleConfigUseCase(
                    type = _uiState.value.selectedVehicleType,
                    distanceBetweenRearWheels = _uiState.value.distanceBetweenWheels,
                    distanceToFrontSupport = _uiState.value.distanceToFrontSupport,
                    distanceBetweenFrontWheels = if (_uiState.value.selectedVehicleType == VehicleType.AUTOCARAVANA)
                        _uiState.value.distanceBetweenFrontWheels else null
                )
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message
                )
            }
        }
    }
}

data class CaravanConfigUiState(
    val selectedVehicleType: VehicleType = VehicleType.CARAVAN,
    val distanceBetweenWheels: Float = 250f, // 2.5 metros en cm
    val distanceToFrontSupport: Float = 0f,
    val distanceBetweenFrontWheels: Float = 0f,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)
