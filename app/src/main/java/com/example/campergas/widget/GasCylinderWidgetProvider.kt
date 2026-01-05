package com.example.campergas.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
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
class GasCylinderWidgetProvider : AppWidgetProvider() {

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
        Log.d("GasCylinderWidget", "First widget added - starting service BLE")
        ensureBleServiceRunning(context)
    }

    override fun onDisabled(context: Context) {
        // This method is called when last widget of this type is removed
        Log.d("GasCylinderWidget", "Last widget removed")
        // Verify if any active widget remains before stopping service
        checkAndStopServiceIfNoWidgets(context)
    }

    private fun ensureBleServiceRunning(context: Context) {
        try {
            val serviceStarted =
                com.example.campergas.service.BleForegroundService.startForWidgets(context)
            if (serviceStarted) {
                Log.d("GasCylinderWidget", "Service BLE iniciado for widgets")
            } else {
                Log.w(
                    "GasCylinderWidget",
                    "No se pudo iniciar servicio BLE - continuando sin servicio de fondo"
                )
            }
        } catch (e: Exception) {
            Log.e("GasCylinderWidget", "Error on start servicio BLE", e)
            // Don't rethrow - this prevents infinite loops
        }
    }

    private fun checkAndStopServiceIfNoWidgets(context: Context) {
        scope.launch {
            try {
                // Check if there are active gas widgets
                val gasWidgetManager = AppWidgetManager.getInstance(context)
                val gasComponentName = ComponentName(context, GasCylinderWidgetProvider::class.java)
                val gasWidgetIds = gasWidgetManager.getAppWidgetIds(gasComponentName)

                // Check if there are active stability widgets
                val stabilityComponentName =
                    ComponentName(context, VehicleStabilityWidgetProvider::class.java)
                val stabilityWidgetIds = gasWidgetManager.getAppWidgetIds(stabilityComponentName)

                // If there are no active widgets, stop the service
                if (gasWidgetIds.isEmpty() && stabilityWidgetIds.isEmpty()) {
                    Log.d("GasCylinderWidget", "No active widgets - stopping BLE service")
                    com.example.campergas.service.BleForegroundService.stopService(context)
                }
            } catch (e: Exception) {
                Log.e("GasCylinderWidget", "Error verifying widgets activos", e)
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
                val gasCylinderRepository = entryPoint.gasCylinderRepository()
                val bleRepository = entryPoint.bleRepository()

                // Get only current data from BLE sensor
                val currentFuelMeasurement = bleRepository.fuelMeasurementData.first()
                val activeCylinder = gasCylinderRepository.getActiveCylinder().first()
                val isConnected = bleRepository.connectionState.first()

                // Create the remote views
                val views = RemoteViews(context.packageName, R.layout.gas_cylinder_widget)

                // Configure texts
                if (activeCylinder != null && currentFuelMeasurement != null) {
                    views.setTextViewText(R.id.widget_cylinder_name, activeCylinder.name)
                    views.setTextViewText(
                        R.id.widget_fuel_percentage,
                        currentFuelMeasurement.getFormattedPercentage()
                    )
                    views.setTextViewText(
                        R.id.widget_fuel_kg,
                        currentFuelMeasurement.getFormattedFuelKilograms()
                    )

                    // Crear imagen of the cylinder
                    val cylinderBitmap =
                        createCylinderBitmap(currentFuelMeasurement.fuelPercentage / 100f)
                    views.setImageViewBitmap(R.id.widget_cylinder_image, cylinderBitmap)
                } else {
                    views.setTextViewText(R.id.widget_cylinder_name, "Sin cylinder activa")
                    views.setTextViewText(R.id.widget_fuel_percentage, "--")
                    views.setTextViewText(R.id.widget_fuel_kg, "--")

                    // Empty cylinder image
                    val cylinderBitmap = createCylinderBitmap(0f)
                    views.setImageViewBitmap(R.id.widget_cylinder_image, cylinderBitmap)
                }

                // Configure connection state
                val connectionText = if (isConnected) "🟢 Conectado" else "🔴 Desconectado"
                views.setTextViewText(R.id.widget_connection_status, connectionText)

                // Configurar intents
                setupIntents(context, views)

                // Updatesr widget
                appWidgetManager.updateAppWidget(appWidgetId, views)

            } catch (e: Exception) {
                Log.e("GasCylinderWidget", "Error updating widget", e)
                // Configurar vista de error
                val views = RemoteViews(context.packageName, R.layout.gas_cylinder_widget)
                views.setTextViewText(R.id.widget_cylinder_name, "Error")
                views.setTextViewText(R.id.widget_fuel_percentage, "--")
                views.setTextViewText(R.id.widget_fuel_kg, "--")
                views.setTextViewText(R.id.widget_connection_status, "🔴 Error")
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
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // Only handle standard widget events
    }

    private fun createCylinderBitmap(fillPercentage: Float): Bitmap {
        val width = 200
        val height = 300
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint().apply {
            isAntiAlias = true
        }

        // Fondo transparente
        canvas.drawColor(Color.TRANSPARENT)

        // Dimensiones of the cylinder
        val cylinderWidth = width * 0.6f
        val cylinderHeight = height * 0.8f
        val startX = (width - cylinderWidth) / 2
        val startY = height * 0.1f

        // Contorno of the cylinder
        paint.color = Color.GRAY
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 8f
        val cylinderRect = RectF(startX, startY, startX + cylinderWidth, startY + cylinderHeight)
        canvas.drawRoundRect(cylinderRect, 20f, 20f, paint)

        // Interior of the cylinder (fondo)
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(cylinderRect, 18f, 18f, paint)

        // Fuel level
        if (fillPercentage > 0) {
            val fillHeight = cylinderHeight * fillPercentage
            val fillColor = when {
                fillPercentage > 0.5f -> Color.parseColor("#4CAF50") // Green
                fillPercentage > 0.2f -> Color.parseColor("#FF9800") // Orange
                else -> Color.parseColor("#F44336") // Red
            }

            paint.color = fillColor
            val fillRect = RectF(
                startX + 4,
                startY + cylinderHeight - fillHeight,
                startX + cylinderWidth - 4,
                startY + cylinderHeight - 4
            )
            canvas.drawRoundRect(fillRect, 15f, 15f, paint)
        }

        // Tapa superior
        paint.color = Color.GRAY
        paint.style = Paint.Style.FILL
        val capWidth = cylinderWidth * 0.3f
        val capHeight = 20f
        val capX = startX + (cylinderWidth - capWidth) / 2
        val capY = startY - capHeight + 4
        canvas.drawRoundRect(RectF(capX, capY, capX + capWidth, capY + capHeight), 10f, 10f, paint)

        return bitmap
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, GasCylinderWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, GasCylinderWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            context.sendBroadcast(intent)
        }
    }
}