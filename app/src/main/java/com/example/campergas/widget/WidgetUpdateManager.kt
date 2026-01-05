package com.example.campergas.widget

import android.content.Context
import com.example.campergas.data.repository.BleRepository
import com.example.campergas.data.repository.FuelMeasurementRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for automatic widget updates based on data changes.
 *
 * Observes BLE sensor data, fuel measurements, and connection state to
 * automatically update home screen widgets in real-time. Runs background
 * coroutines to listen for changes and trigger widget refreshes.
 *
 * @property context Application context
 * @property bleRepository Repository for BLE sensor data access
 * @property fuelMeasurementRepository Repository for fuel measurement access
 */
@Singleton
class WidgetUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bleRepository: BleRepository,
    private val fuelMeasurementRepository: FuelMeasurementRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        startListeningForUpdates()
    }

    /**
     * Starts background listeners for data changes.
     *
     * Monitors fuel measurements, BLE data, inclination, and connection state
     * to keep widgets updated in real-time.
     */
    private fun startListeningForUpdates() {
        // Listen for changes in fuel measurements
        scope.launch {
            fuelMeasurementRepository.getLatestRealTimeMeasurement().collect { fuelMeasurement ->
                // Update gas cylinder widget always, with or without data
                GasCylinderWidgetProvider.updateAllWidgets(context)
            }
        }

        // Listen for changes in real-time BLE fuel data
        scope.launch {
            bleRepository.fuelMeasurementData.collect { fuelMeasurement ->
                // Update widget when new BLE data arrives
                GasCylinderWidgetProvider.updateAllWidgets(context)
            }
        }

        // Listen for changes in inclination data
        scope.launch {
            bleRepository.inclinationData.collect { inclinationData ->
                if (inclinationData != null) {
                    // Update vehicle stability widget
                    VehicleStabilityWidgetProvider.updateAllWidgets(context)
                }
            }
        }

        // Listen for changes in connection state
        scope.launch {
            bleRepository.connectionState.collect { isConnected ->
                // Update both widgets when connection state changes
                GasCylinderWidgetProvider.updateAllWidgets(context)
                VehicleStabilityWidgetProvider.updateAllWidgets(context)
            }
        }
    }
}