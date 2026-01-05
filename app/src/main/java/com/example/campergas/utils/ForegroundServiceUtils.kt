package com.example.campergas.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log

/**
 * Utility class to handle foreground service startup restrictions on Android 12+
 */
object ForegroundServiceUtils {

    private const val TAG = "ForegroundServiceUtils"

    /**
     * Verifies if the application can start a foreground service from the current context
     *
     * Starting from Android 12 (API 31), there are strict limitations about when
     * applications in background can start foreground services. This method
     * verifies if the current context allows starting foreground services.
     */
    fun canStartForegroundService(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For Android 12+, verify if application is in foreground
            isAppInForeground(context)
        } else {
            // For Android 11 and earlier, foreground services can be started more freely
            true
        }
    }

    /**
     * Verifies if the application is currently in foreground
     */
    private fun isAppInForeground(context: Context): Boolean {
        return try {
            val activityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val appProcesses = activityManager.runningAppProcesses

            if (appProcesses.isNullOrEmpty()) {
                Log.d(TAG, "Not found application processes running")
                return false
            }

            val packageName = context.packageName
            val foregroundProcess = appProcesses.find {
                it.processName == packageName &&
                        it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
            }

            val isInForeground = foregroundProcess != null
            Log.d(TAG, "Application foreground state: $isInForeground")
            return isInForeground

        } catch (e: Exception) {
            Log.e(TAG, "Error verifying foreground state", e)
            // Default to false for safety when we cannot determine the state
            false
        }
    }

    /**
     * Verifies if a service specific is currently running
     */
    fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        return try {
            val activityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

            // For Android 7.0+ (API 24), use getRunningServices with lower limit for performance
            val maxServices =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) 100 else Integer.MAX_VALUE

            @Suppress("DEPRECATION") // Necessary for widget compatibility
            val runningServices = activityManager.getRunningServices(maxServices)

            val serviceName = serviceClass.name
            val isRunning = runningServices.any { service ->
                service.service.className == serviceName
            }

            Log.d(TAG, "Service .* running: $isRunning")
            return isRunning

        } catch (e: Exception) {
            Log.e(TAG, "Error verifying if service is running", e)
            // In case of error, we assume it is not running to allow retries
            false
        }
    }

    /**
     * Attempts to safely start a foreground service, with fallback to regular service
     * Returns true if any service started successfully
     */
    fun startServiceSafely(
        context: Context,
        serviceClass: Class<*>,
        configureIntent: (android.content.Intent) -> Unit = {}
    ): Boolean {
        return try {
            val intent = android.content.Intent(context, serviceClass).apply {
                configureIntent(this)
            }

            if (canStartForegroundService(context)) {
                // Attempt to start as foreground service
                Log.d(TAG, "Starting foreground service for ${serviceClass.simpleName}")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                true
            } else {
                // Fallback: start as regular service (widgets will continue receiving updates when app opens)
                Log.d(
                    TAG,
                    "Cannot start foreground service, starting regular service for ${serviceClass.simpleName}"
                )
                context.startService(intent)
                true
            }
        } catch (e: Exception) {
            // Check if this is a ForegroundServiceStartNotAllowedException (API 31+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                e::class.java.simpleName == "ForegroundServiceStartNotAllowedException"
            ) {
                Log.w(
                    TAG,
                    "Foreground service start not allowed, attempting to start regular service",
                    e
                )
                return try {
                    val intent = android.content.Intent(context, serviceClass).apply {
                        configureIntent(this)
                    }
                    context.startService(intent)
                    true
                } catch (regularServiceException: Exception) {
                    Log.e(
                        TAG,
                        "Error starting regular service as fallback",
                        regularServiceException
                    )
                    false
                }
            } else {
                Log.e(TAG, "Error on start servicio", e)
                false
            }
        }
    }

    /**
     * Attempts to start a service safely only if not already running
     * Avoids multiple start attempts that can cause infinite loops
     */
    fun startServiceSafelyIfNotRunning(
        context: Context,
        serviceClass: Class<*>,
        configureIntent: (android.content.Intent) -> Unit = {}
    ): Boolean {
        return if (isServiceRunning(context, serviceClass)) {
            Log.d(TAG, "Service .* is already running - no action required")
            true
        } else {
            Log.d(TAG, "Service .* is not running - starting...")
            startServiceSafely(context, serviceClass, configureIntent)
        }
    }
}