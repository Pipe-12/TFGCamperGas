package com.example.campergas.ui.screens.inclination

import androidx.lifecycle.viewModelScope
import com.example.campergas.domain.model.VehicleType
import com.example.campergas.domain.usecase.CheckBleConnectionUseCase
import com.example.campergas.domain.usecase.ConfigureReadingIntervalsUseCase
import com.example.campergas.domain.usecase.GetInclinationUseCase
import com.example.campergas.domain.usecase.GetVehicleConfigUseCase
import com.example.campergas.domain.usecase.RequestInclinationDataUseCase
import com.example.campergas.ui.base.BaseRequestViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.tan

@HiltViewModel
class InclinationViewModel @Inject constructor(
    private val getInclinationUseCase: GetInclinationUseCase,
    private val requestInclinationDataUseCase: RequestInclinationDataUseCase,
    checkBleConnectionUseCase: CheckBleConnectionUseCase,
    private val getVehicleConfigUseCase: GetVehicleConfigUseCase,
    private val configureReadingIntervalsUseCase: ConfigureReadingIntervalsUseCase
) : BaseRequestViewModel(checkBleConnectionUseCase) {

    private val _uiState = MutableStateFlow(InclinationUiState())
    val uiState: StateFlow<InclinationUiState> = _uiState.asStateFlow()

    companion object {
        private const val FAST_INCLINATION_INTERVAL_SECONDS = 1
    }

    // Scope for cleanup operations that need to survive viewModelScope cancellation
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // Load vehicle configuration
        loadVehicleConfig()

        // Set fast reading interval (1 second) when entering the inclination screen
        setFastInclinationReadingInterval()

        // Get data of inclination en real time
        viewModelScope.launch {
            getInclinationUseCase().collectLatest { inclination ->
                _uiState.value = if (inclination != null) {
                    val newState = _uiState.value.copy(
                        inclinationPitch = inclination.pitch,
                        inclinationRoll = inclination.roll,
                        isLevel = inclination.isLevel,
                        isLoading = false,
                        error = null,
                        timestamp = inclination.timestamp
                    )
                    // Calculate wheel elevations
                    newState.copy(wheelElevations = calculateWheelElevations(newState))
                } else {
                    _uiState.value.copy(
                        isLoading = true,
                        error = null
                    )
                }
            }
        }
    }

    /**
     * Sets the inclination reading interval to 1 second for faster updates on this screen.
     */
    private fun setFastInclinationReadingInterval() {
        configureReadingIntervalsUseCase.setTemporaryInclinationReadInterval(FAST_INCLINATION_INTERVAL_SECONDS)
    }

    /**
     * Restores the configured inclination reading interval when leaving the screen.
     */
    override fun onCleared() {
        super.onCleared()
        // Restore the configured interval when leaving the screen
        // Uses a separate scope since viewModelScope is cancelled at this point
        cleanupScope.launch {
            configureReadingIntervalsUseCase.restoreInclinationReadInterval()
        }
    }

    private fun loadVehicleConfig() {
        viewModelScope.launch {
            getVehicleConfigUseCase().collectLatest { config ->
                if (config != null) {
                    _uiState.value = _uiState.value.copy(
                        vehicleType = config.type,
                        distanceBetweenRearWheels = config.distanceBetweenRearWheels,
                        distanceToFrontSupport = config.distanceToFrontSupport,
                        distanceBetweenFrontWheels = config.distanceBetweenFrontWheels ?: 0f
                    ).let { newState ->
                        // Recalculate elevations with new configuration
                        newState.copy(wheelElevations = calculateWheelElevations(newState))
                    }
                }
            }
        }
    }

    /**
     * Calculates necessary elevation for each wheel based on inclination
     */
    private fun calculateWheelElevations(state: InclinationUiState): WheelElevations {
        if (state.distanceBetweenRearWheels == 0f || state.distanceToFrontSupport == 0f) {
            return WheelElevations()
        }

        // Convert degrees to radians
        val pitchRad = Math.toRadians(state.inclinationPitch.toDouble())
        val rollRad = Math.toRadians(state.inclinationRoll.toDouble())

        // Calculate elevations based on configured distances
        val rearWheelDistance = state.distanceBetweenRearWheels

        // For roll (side tilt)
        val rearLeftElevationRoll = rearWheelDistance * tan(rollRad)
        val rearRightElevationRoll = -rearWheelDistance * tan(rollRad)

        // For pitch (front/rear tilt)
        val frontElevationPitch = state.distanceToFrontSupport * tan(pitchRad)

        return when (state.vehicleType) {
            VehicleType.CARAVAN -> {
                // Caravan: rear wheels + front support wheel
                WheelElevations(
                    rearLeft = rearLeftElevationRoll.toFloat(),
                    rearRight = rearRightElevationRoll.toFloat(),
                    frontSupport = frontElevationPitch.toFloat()
                )
            }

            VehicleType.AUTOCARAVANA -> {
                // Autocaravana: 4 wheels
                // Use front wheel distance if available, otherwise use rear wheel distance as fallback
                val frontWheelDistance = if (state.distanceBetweenFrontWheels > 0f) {
                    state.distanceBetweenFrontWheels
                } else {
                    state.distanceBetweenRearWheels
                }
                val frontLeftElevationRoll = frontWheelDistance * tan(rollRad)
                val frontRightElevationRoll = -frontWheelDistance * tan(rollRad)

                WheelElevations(
                    rearLeft = rearLeftElevationRoll.toFloat(),
                    rearRight = rearRightElevationRoll.toFloat(),
                    frontLeft = (frontElevationPitch + frontLeftElevationRoll).toFloat(),
                    frontRight = (frontElevationPitch + frontRightElevationRoll).toFloat()
                )
            }
        }
    }

    /**
     * Solicita una lectura manual of data of inclination from sensor BLE
     * Includes protection against multiple consecutive requests
     */
    fun requestInclinationDataManually() {
        executeManualRequest(
            requestAction = { requestInclinationDataUseCase() },
            logTag = "InclinationViewModel",
            dataTypeDescription = "inclination"
        )
    }
}

data class InclinationUiState(
    val inclinationPitch: Float = 0f, // Pitch (front/back)
    val inclinationRoll: Float = 0f,  // Alabeo (lado a lado)
    val isLevel: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val timestamp: Long = 0L,
    val vehicleType: VehicleType = VehicleType.CARAVAN,
    val distanceBetweenRearWheels: Float = 250f, // 2.5 metros en cm
    val distanceToFrontSupport: Float = 0f,
    val distanceBetweenFrontWheels: Float = 0f,
    val wheelElevations: WheelElevations = WheelElevations()
)

data class WheelElevations(
    val rearLeft: Float = 0f,
    val rearRight: Float = 0f,
    val frontLeft: Float = 0f,
    val frontRight: Float = 0f,
    val frontSupport: Float = 0f // For the front support wheel of the caravana
)
