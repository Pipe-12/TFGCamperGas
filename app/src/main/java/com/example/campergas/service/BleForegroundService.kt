package com.example.campergas.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.campergas.R
import com.example.campergas.data.local.preferences.PreferencesDataStore
import com.example.campergas.data.repository.BleRepository
import com.example.campergas.utils.ForegroundServiceUtils
import com.example.campergas.widget.GasCylinderWidgetProvider
import com.example.campergas.widget.VehicleStabilityWidgetProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BleForegroundService : Service() {

    @Inject
    lateinit var bleRepository: BleRepository

    @Inject
    lateinit var preferencesDataStore: PreferencesDataStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val notificationId = 123
    private val channelId = "ble_service_channel"
    private val alertChannelId = "gas_alert_channel"
    private val alertNotificationId = 124

    // State to prevent notification spam
    private var lastAlertThreshold: Float? = null
    private var hasAlertBeenSent = false

    // Control for periodic BLE requests
    private var isPeriodicRequestsActive = false
    private var periodicRequestsJob: kotlinx.coroutines.Job? = null
    private var lastWeightRequestTime = 0L
    private var lastInclinationRequestTime = 0L

    // Configurable intervals (loaded from preferences)
    private var weightRequestInterval = 5000L // 5 seconds by default
    private var inclinationRequestInterval = 5000L // 5 seconds by default

    companion object {
        private const val TAG = "BleForegroundService"
        const val ACTION_START_FOR_WIDGETS = "START_FOR_WIDGETS"

        fun startForWidgets(context: Context): Boolean {
            return ForegroundServiceUtils.startServiceSafelyIfNotRunning(
                context,
                BleForegroundService::class.java
            ) { intent ->
                intent.action = ACTION_START_FOR_WIDGETS
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, BleForegroundService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        createAlertNotificationChannel()
        Log.d(TAG, "BleForegroundService creado")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Try to start as foreground service if possible using the same safe logic as starting the service
        try {
            if (ForegroundServiceUtils.canStartForegroundService(this)) {
                val notification = createNotification(getString(R.string.ble_service_running))
                startForeground(notificationId, notification)
                Log.d(TAG, "Service started as foreground service")
            } else {
                Log.d(TAG, "Service started as regular service (foreground not allowed)")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not start as foreground service, running as regular service", e)
            // Continue as regular service - widgets will still be updated when app is in foreground
        }

        when (intent?.action) {
            ACTION_START_FOR_WIDGETS -> {
                Log.d(TAG, "Service started for widgets")
                // Try to connect to last known device
                connectToLastKnownDevice()
            }

            else -> {
                Log.d(TAG, "Service started - connecting to last known device")
                connectToLastKnownDevice()
            }
        }

        // Return START_STICKY so service restarts automatically if system kills it
        return START_STICKY
    }

    private fun connectToLastKnownDevice() {
        serviceScope.launch {
            try {
                val lastDeviceAddress = preferencesDataStore.lastConnectedDeviceAddress.first()
                if (lastDeviceAddress.isNotEmpty()) {
                    Log.d(TAG, "Connecting to last known device: $lastDeviceAddress")
                    connectToDevice(lastDeviceAddress)
                } else {
                    Log.d(
                        TAG,
                        "No previous known device, starting periodic requests without connection"
                    )
                    // Still load configuration and be ready for when it connects
                    loadConfigurationAndStartPeriodicRequests()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to last known device: ${e.message}")
                loadConfigurationAndStartPeriodicRequests()
            }
        }
    }

    private fun connectToDevice(deviceAddress: String) {
        serviceScope.launch {
            try {
                // Connect to BLE device
                bleRepository.connectToSensor(deviceAddress)

                // Load interval configuration from preferences
                loadConfigurationAndStartPeriodicRequests()

                // Monitor fuel measurements and update notification
                launch {
                    bleRepository.fuelMeasurementData.collect { fuelMeasurement ->
                        if (fuelMeasurement != null) {
                            // Check gas level threshold for alerts
                            checkGasLevelThreshold(fuelMeasurement.fuelKilograms)

                            // Update gas cylinder widget
                            GasCylinderWidgetProvider.updateAllWidgets(this@BleForegroundService)
                        }
                    }
                }

                // Monitor inclination data to update widget
                launch {
                    bleRepository.inclinationData.collect { inclinationData ->
                        if (inclinationData != null) {
                            // Update vehicle stability widget
                            VehicleStabilityWidgetProvider.updateAllWidgets(this@BleForegroundService)
                        }
                    }
                }

                // Monitor connection state to handle reconnections
                launch {
                    bleRepository.connectionState.collect { isConnected ->
                        if (isConnected) {
                            Log.d(TAG, "Connected - starting periodic requests")
                            if (!isPeriodicRequestsActive) {
                                startPeriodicBleRequests()
                            }
                        } else {
                            Log.d(TAG, "Disconnected - stopping periodic requests")
                            stopPeriodicBleRequests()
                        }

                        // Update widgets when connection state changes
                        GasCylinderWidgetProvider.updateAllWidgets(this@BleForegroundService)
                        VehicleStabilityWidgetProvider.updateAllWidgets(this@BleForegroundService)
                    }
                }

                // Save connected device address
                bleRepository.saveLastConnectedDevice(deviceAddress)

            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to device: ${e.message}")
                // Do not stop service to allow automatic retries
                updateNotification("Connection error - retrying...")
            }
        }
    }

    /**
     * Loads interval configuration from preferences and starts periodic requests
     */
    private fun loadConfigurationAndStartPeriodicRequests() {
        serviceScope.launch {
            try {
                // Load intervals from preferences
                weightRequestInterval = preferencesDataStore.weightReadInterval.first()
                inclinationRequestInterval = preferencesDataStore.inclinationReadInterval.first()

                Log.d(
                    TAG,
                    "Configuration loaded - Weight: ${weightRequestInterval}ms, Inclination: ${inclinationRequestInterval}ms"
                )

                // Start periodic requests if there is connection
                if (bleRepository.connectionState.first()) {
                    startPeriodicBleRequests()
                }

                // Listen for configuration changes to update intervals
                launch {
                    preferencesDataStore.weightReadInterval.collect { newInterval ->
                        if (newInterval != weightRequestInterval) {
                            Log.d(TAG, "Weight interval updated: $newInterval ms")
                            weightRequestInterval = newInterval
                            restartPeriodicRequestsIfActive()
                        }
                    }
                }

                launch {
                    preferencesDataStore.inclinationReadInterval.collect { newInterval ->
                        if (newInterval != inclinationRequestInterval) {
                            Log.d(TAG, "Inclination interval updated: $newInterval ms")
                            inclinationRequestInterval = newInterval
                            restartPeriodicRequestsIfActive()
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading configuration: ${e.message}")
                // Use default values if it fails
                weightRequestInterval = 5000L
                inclinationRequestInterval = 5000L
            }
        }
    }

    /**
     * Starts periodic BLE data requests according to configured intervals
     */
    private fun startPeriodicBleRequests() {
        if (isPeriodicRequestsActive) {
            Log.d(TAG, "Periodic requests are already active")
            return
        }

        isPeriodicRequestsActive = true
        periodicRequestsJob = serviceScope.launch {
            Log.d(TAG, "🔄 Starting periodic requests of data BLE...")
            Log.d(
                TAG,
                "📊 Weight interval: ${weightRequestInterval}ms, Inclination interval: ${inclinationRequestInterval}ms"
            )

            while (isPeriodicRequestsActive && isActive) {
                try {
                    val currentTime = System.currentTimeMillis()

                    // Verify if there is BLE connection before making requests
                    val isConnected = bleRepository.connectionState.first()
                    if (!isConnected) {
                        Log.d(TAG, "No connection BLE, pausing requests...")
                        delay(2000) // Pause if no connection
                        continue
                    }

                    // Request weight data if more than configured interval has passed
                    if (currentTime - lastWeightRequestTime > weightRequestInterval) {
                        Log.d(TAG, "Requesting data of weight...")
                        bleRepository.readWeightDataOnDemand()
                        lastWeightRequestTime = currentTime
                    }

                    // Wait a bit before requesting inclination to avoid BLE conflicts
                    delay(500)

                    // Request inclination data if more than configured interval has passed
                    if (currentTime - lastInclinationRequestTime > inclinationRequestInterval) {
                        Log.d(TAG, "Requesting data of inclination...")
                        bleRepository.readInclinationDataOnDemand()
                        lastInclinationRequestTime = currentTime
                    }

                    // Pause between verification cycles (1000ms)
                    delay(1000)

                } catch (e: Exception) {
                    Log.e(TAG, "Error in periodic BLE request: ${e.message}")
                    delay(2000) // Longer pause in case of error
                }
            }

            Log.d(TAG, "Periodic BLE data requests finished")
        }
    }

    /**
     * Stops periodic BLE data requests
     */
    private fun stopPeriodicBleRequests() {
        isPeriodicRequestsActive = false
        periodicRequestsJob?.cancel()
        periodicRequestsJob = null
        Log.d(TAG, "Periodic BLE data requests stopped")
    }

    /**
     * Restarts periodic requests if active (useful when intervals change)
     */
    private fun restartPeriodicRequestsIfActive() {
        if (isPeriodicRequestsActive) {
            Log.d(TAG, "Restarting periodic requests with new intervals...")
            stopPeriodicBleRequests()

            // Short pause before restarting
            serviceScope.launch {
                delay(500)
                if (bleRepository.connectionState.first()) {
                    startPeriodicBleRequests()
                }
            }
        }
    }

    private fun createNotificationChannel() {
        val name = getString(R.string.ble_service_channel_name)
        val descriptionText = getString(R.string.ble_service_channel_description)
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(channelId, name, importance).apply {
            description = descriptionText
        }

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Creates notification channel for gas level alerts.
     *
     * Configures a high-priority notification channel with sound, vibration,
     * and LED lights to ensure critical alerts are visible to users.
     * Required for Android 8.0 (API 26) and above.
     */
    private fun createAlertNotificationChannel() {
        val name = getString(R.string.gas_alert_channel_name)
        val descriptionText = getString(R.string.gas_alert_channel_description)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(alertChannelId, name, importance).apply {
            description = descriptionText
            // Enable sound for alert notifications
            setSound(
                android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .build()
            )
            // Enable vibration
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 250, 500)
            // Enable LED lights if available
            enableLights(true)
            lightColor = android.graphics.Color.RED
            // Show on lock screen
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(message: String): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("CamperGas")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(message: String) {
        val notification = createNotification(message)
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    private fun checkGasLevelThreshold(currentKilograms: Float) {
        serviceScope.launch {
            try {
                val notificationsEnabled = preferencesDataStore.areNotificationsEnabled.first()
                if (!notificationsEnabled) return@launch

                val threshold = preferencesDataStore.gasLevelThreshold.first()

                // Only send alert if gas is below threshold
                if (currentKilograms <= threshold) {
                    // Avoid spam: only send if not sent for this threshold or if threshold changed
                    if (!hasAlertBeenSent || lastAlertThreshold != threshold) {
                        sendGasAlert(currentKilograms, threshold)
                        hasAlertBeenSent = true
                        lastAlertThreshold = threshold
                    }
                } else {
                    // Reset alert state when gas is above threshold
                    if (hasAlertBeenSent) {
                        hasAlertBeenSent = false
                    }
                }
            } catch (_: Exception) {
                // Handle errors silently to not affect the main service
            }
        }
    }

    /**
     * Sends a critical gas level alert notification to the user.
     *
     * Creates and displays a high-priority notification when gas level falls
     * below the configured threshold. Includes vibration, sound, and LED indicators
     * for maximum visibility. Checks for POST_NOTIFICATIONS permission on Android 13+.
     *
     * @param currentKilograms Current gas level in kilograms
     * @param threshold Configured threshold level in kilograms
     */
    private fun sendGasAlert(currentKilograms: Float, threshold: Float) {
        try {
            // Check POST_NOTIFICATIONS permission for Android 13+ (API 33+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Cannot send notification: POST_NOTIFICATIONS permission not granted")
                    return
                }
            }

            val title = getString(R.string.gas_alert_title)
            val message = getString(
                R.string.gas_alert_message,
                "%.2f".format(currentKilograms),
                "%.2f".format(threshold)
            )

            val alertNotification = NotificationCompat.Builder(this, alertChannelId)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                // Category for system to prioritize appropriately
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                // Auto-dismiss when user taps notification
                .setAutoCancel(true)
                // Enable vibration pattern
                .setVibrate(longArrayOf(0, 500, 250, 500))
                // Use default notification sound
                .setDefaults(NotificationCompat.DEFAULT_SOUND)
                // Show on lock screen
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                // Make notification stand out
                .setColorized(true)
                .setColor(android.graphics.Color.RED)
                .build()

            val notificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(alertNotificationId, alertNotification)

            Log.d(TAG, "Gas alert notification sent: $currentKilograms kg (threshold: $threshold kg)")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending gas alert notification", e)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "Destruyendo BleForegroundService...")

        // Stop periodic requests
        stopPeriodicBleRequests()

        serviceScope.launch {
            bleRepository.disconnectSensor()
        }
        serviceScope.cancel()
        super.onDestroy()

        Log.d(TAG, "BleForegroundService destruido")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
