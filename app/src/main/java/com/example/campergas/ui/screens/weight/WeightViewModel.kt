package com.example.campergas.ui.screens.weight

import androidx.lifecycle.viewModelScope
import com.example.campergas.domain.model.FuelMeasurement
import com.example.campergas.domain.model.GasCylinder
import com.example.campergas.domain.model.VehicleConfig
import com.example.campergas.domain.usecase.CheckBleConnectionUseCase
import com.example.campergas.domain.usecase.GetActiveCylinderUseCase
import com.example.campergas.domain.usecase.GetFuelDataUseCase
import com.example.campergas.domain.usecase.GetVehicleConfigUseCase
import com.example.campergas.domain.usecase.RequestWeightDataUseCase
import com.example.campergas.ui.base.BaseRequestViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeightViewModel @Inject constructor(
    private val getFuelDataUseCase: GetFuelDataUseCase,
    private val getVehicleConfigUseCase: GetVehicleConfigUseCase,
    private val getActiveCylinderUseCase: GetActiveCylinderUseCase,
    private val requestWeightDataUseCase: RequestWeightDataUseCase,
    checkBleConnectionUseCase: CheckBleConnectionUseCase
) : BaseRequestViewModel(checkBleConnectionUseCase) {

    private val _fuelState = MutableStateFlow<FuelMeasurement?>(null)
    val fuelState: StateFlow<FuelMeasurement?> = _fuelState

    private val _vehicleState = MutableStateFlow<VehicleConfig?>(null)
    val vehicleState: StateFlow<VehicleConfig?> = _vehicleState

    private val _activeCylinder = MutableStateFlow<GasCylinder?>(null)
    val activeCylinder: StateFlow<GasCylinder?> = _activeCylinder

    init {
        // Get vehicle configuration
        viewModelScope.launch {
            getVehicleConfigUseCase().collectLatest { vehicle ->
                _vehicleState.value = vehicle
            }
        }

        // Get active cylinder
        viewModelScope.launch {
            getActiveCylinderUseCase().collectLatest { cylinder ->
                _activeCylinder.value = cylinder
            }
        }

        // Get fuel data
        viewModelScope.launch {
            getFuelDataUseCase().collectLatest { fuel ->
                _fuelState.value = fuel
            }
        }
    }

    /**
     * Solicita una lectura manual of data of weight from sensor BLE
     * Includes protection against multiple consecutive requests
     */
    fun requestWeightDataManually() {
        executeManualRequest(
            requestAction = { requestWeightDataUseCase() },
            logTag = "WeightViewModel",
            dataTypeDescription = "peso"
        )
    }
}
