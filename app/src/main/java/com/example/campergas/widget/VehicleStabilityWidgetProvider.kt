package com.example.campergas.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.example.campergas.MainActivity
import com.example.campergas.R
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class VehicleStabilityWidgetProvider : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Get repositories using Hilt EntryPoint pattern for reliable dependency injection in widgets
     */
    private fun getEntryPoint(context: Context): WidgetEntryPoint {
        return EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Ensure BLE service is running for periodic requests
        ensureBleServiceRunning(context)

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // This method is called when first widget of this type is added
        Log.d("VehicleStabilityWidget", "First widget added - starting service BLE")
        ensureBleServiceRunning(context)
    }

    override fun onDisabled(context: Context) {
        // This method is called when last widget of this type is removed
        Log.d("VehicleStabilityWidget", "Last widget removed")
        // Verify if any active widget remains before stopping service
        checkAndStopServiceIfNoWidgets(context)
    }

    private fun ensureBleServiceRunning(context: Context) {
        try {
            val serviceStarted =
                com.example.campergas.service.BleForegroundService.startForWidgets(context)
            if (serviceStarted) {
                Log.d("VehicleStabilityWidget", "Service BLE started for widgets")
            } else {
                Log.w(
                    "VehicleStabilityWidget",
                    "No se pudo iniciar servicio BLE - continuando sin servicio de fondo"
                )
            }
        } catch (e: Exception) {
            Log.e("VehicleStabilityWidget", "Error on start servicio BLE", e)
            // Don't rethrow - this prevents infinite loops
        }
    }

    private fun checkAndStopServiceIfNoWidgets(context: Context) {
        scope.launch {
            try {
                // Verificar si hay widgets de gas activos
                val widgetManager = AppWidgetManager.getInstance(context)
                val gasComponentName = ComponentName(context, GasCylinderWidgetProvider::class.java)
                val gasWidgetIds = widgetManager.getAppWidgetIds(gasComponentName)

                // Verify if there are active stability widgets
                val stabilityComponentName =
                    ComponentName(context, VehicleStabilityWidgetProvider::class.java)
                val stabilityWidgetIds = widgetManager.getAppWidgetIds(stabilityComponentName)

                // If no active widgets, stop the service
                if (gasWidgetIds.isEmpty() && stabilityWidgetIds.isEmpty()) {
                    Log.d(
                        "VehicleStabilityWidget",
                        "No active widgets - stopping BLE service"
                    )
                    com.example.campergas.service.BleForegroundService.stopService(context)
                }
            } catch (e: Exception) {
                Log.e("VehicleStabilityWidget", "Error verifying active widgets", e)
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        scope.launch {
            try {
                // Get repositories using EntryPoint
                val entryPoint = getEntryPoint(context)
                val bleRepository = entryPoint.bleRepository()

                // Get current inclination data
                val inclinationData = bleRepository.inclinationData.first()
                val isConnected = bleRepository.connectionState.first()

                // Create the remote views
                val views = RemoteViews(context.packageName, R.layout.vehicle_stability_widget)

                // Configure inclination data
                if (inclinationData != null) {
                    views.setTextViewText(
                        R.id.widget_pitch_value,
                        context.getString(R.string.widget_pitch_format).format(inclinationData.pitch)
                    )
                    views.setTextViewText(
                        R.id.widget_roll_value,
                        context.getString(R.string.widget_roll_format).format(inclinationData.roll)
                    )

                    // Leveling state with simple indicator
                    val stabilityText = if (inclinationData.isLevel) context.getString(R.string.widget_stable) else context.getString(R.string.widget_inclined)
                    val stabilityIndicator = if (inclinationData.isLevel) context.getString(R.string.widget_stable_indicator) else context.getString(R.string.widget_unstable_indicator)
                    views.setTextViewText(R.id.widget_stability_status, stabilityText)
                    views.setTextViewText(R.id.widget_stability_indicator, stabilityIndicator)

                    // Timestamp
                    views.setTextViewText(
                        R.id.widget_last_update,
                        context.getString(R.string.widget_updated_format).format(inclinationData.getFormattedTimestamp())
                    )
                } else {
                    views.setTextViewText(R.id.widget_pitch_value, context.getString(R.string.widget_pitch_no_data))
                    views.setTextViewText(R.id.widget_roll_value, context.getString(R.string.widget_roll_no_data))
                    views.setTextViewText(R.id.widget_stability_status, context.getString(R.string.widget_no_data_status))
                    views.setTextViewText(R.id.widget_stability_indicator, context.getString(R.string.widget_unknown_indicator))
                    views.setTextViewText(R.id.widget_last_update, context.getString(R.string.widget_no_data_available))
                }

                // Configure connection state
                val connectionText = if (isConnected) context.getString(R.string.widget_connected) else context.getString(R.string.widget_disconnected)
                views.setTextViewText(R.id.widget_connection_status, connectionText)

                // Configurar intents
                setupIntents(context, views)

                // Updatesr widget
                appWidgetManager.updateAppWidget(appWidgetId, views)

            } catch (e: Exception) {
                Log.e("VehicleStabilityWidget", "Error updating widget", e)
                // Configurar vista de error
                val views = RemoteViews(context.packageName, R.layout.vehicle_stability_widget)
                views.setTextViewText(R.id.widget_pitch_value, context.getString(R.string.widget_pitch_no_data))
                views.setTextViewText(R.id.widget_roll_value, context.getString(R.string.widget_roll_no_data))
                views.setTextViewText(R.id.widget_stability_status, context.getString(R.string.widget_error_status_full))
                views.setTextViewText(R.id.widget_connection_status, context.getString(R.string.widget_error_status))
                views.setTextViewText(R.id.widget_last_update, context.getString(R.string.widget_connection_error))
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    private fun setupIntents(context: Context, views: RemoteViews) {
        // Intent to open application
        val openAppIntent = Intent(context, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, openAppPendingIntent)

        // Intent for solicitar data of inclination
        val requestDataIntent = Intent(context, VehicleStabilityWidgetProvider::class.java).apply {
            action = ACTION_REQUEST_INCLINATION_DATA
        }
        val requestDataPendingIntent = PendingIntent.getBroadcast(
            context, 2, requestDataIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_refresh_button, requestDataPendingIntent)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_REQUEST_INCLINATION_DATA -> {
                Log.d("VehicleStabilityWidget", "Manual inclination data request from widget")
                scope.launch {
                    try {
                        // Get repositories using EntryPoint
                        val entryPoint = getEntryPoint(context)
                        val bleRepository = entryPoint.bleRepository()

                        // Request inclination data manually
                        bleRepository.readInclinationDataOnDemand()

                        // Update all widgets
                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        val componentName =
                            ComponentName(context, VehicleStabilityWidgetProvider::class.java)
                        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                        onUpdate(context, appWidgetManager, appWidgetIds)

                    } catch (e: Exception) {
                        Log.e("VehicleStabilityWidget", "Error requesting inclination data", e)
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_REQUEST_INCLINATION_DATA =
            "com.example.campergas.widget.REQUEST_INCLINATION_DATA"

        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, VehicleStabilityWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, VehicleStabilityWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            context.sendBroadcast(intent)
        }
    }
}