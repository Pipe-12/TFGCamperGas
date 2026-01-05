package com.example.campergas.utils

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.campergas.R

/**
 * Manager for handling complex Bluetooth and location permissions on Android.
 *
 * Handles permission requests across different Android versions, including:
 * - Bluetooth permissions (BLUETOOTH_CONNECT, BLUETOOTH_SCAN for Android 12+)
 * - Location permissions (required for BLE scanning on older Android versions)
 * - Bluetooth and location enable/disable state management
 *
 * Simplifies the complex permission flow by managing activity launchers and
 * providing callbacks for success and failure states.
 *
 * @property activity ComponentActivity that will host permission requests
 * @property onPermissionsGranted Callback invoked when all required permissions are granted
 * @property onPermissionsDenied Callback invoked with list of denied permission descriptions
 */
class BluetoothPermissionManager(
    private val activity: ComponentActivity,
    private val onPermissionsGranted: () -> Unit,
    private val onPermissionsDenied: (List<String>) -> Unit
) {

    private lateinit var enableBluetoothLauncher: ActivityResultLauncher<Intent>
    private lateinit var enableLocationLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionsLauncher: ActivityResultLauncher<Array<String>>

    init {
        setupLaunchers()
    }

    private fun setupLaunchers() {
        // Launcher to enable Bluetooth
        enableBluetoothLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Bluetooth enabled, now verify permissions
                checkAndRequestPermissions()
            } else {
                onPermissionsDenied(listOf(activity.getString(R.string.error_bluetooth_not_enabled)))
            }
        }

        // Launcher to activate location
        enableLocationLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            // Verify if location is now active
            if (isLocationEnabled()) {
                checkAndRequestPermissions()
            } else {
                onPermissionsDenied(listOf(activity.getString(R.string.error_location_not_enabled)))
            }
        }

        // Launcher to request permissions
        @TargetApi(Build.VERSION_CODES.Q)
        requestPermissionsLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val deniedPermissions = permissions.filter { !it.value }.keys.toList()

            if (deniedPermissions.isEmpty()) {
                // All requested permissions were granted
                // Verify if we need to request ACCESS_BACKGROUND_LOCATION separately
                val backgroundLocationPermission = Manifest.permission.ACCESS_BACKGROUND_LOCATION
                val needsBackgroundLocation = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                        getRequiredPermissions().contains(backgroundLocationPermission)

                if (needsBackgroundLocation &&
                    ContextCompat.checkSelfPermission(
                        activity,
                        backgroundLocationPermission
                    ) != PackageManager.PERMISSION_GRANTED &&
                    !permissions.containsKey(backgroundLocationPermission)
                ) {
                    // Request ACCESS_BACKGROUND_LOCATION separately
                    requestPermissionsLauncher.launch(arrayOf(backgroundLocationPermission))
                } else {
                    // All necessary permissions are granted
                    onPermissionsGranted()
                }
            } else {
                onPermissionsDenied(deniedPermissions)
            }
        }
    }

    fun checkAndRequestAllPermissions() {
        when {
            !isBluetoothSupported() -> {
                onPermissionsDenied(listOf(activity.getString(R.string.error_bluetooth_not_supported)))
            }

            !isBluetoothEnabled() -> {
                requestEnableBluetooth()
            }

            !isLocationEnabled() -> {
                requestEnableLocation()
            }

            else -> {
                checkAndRequestPermissions()
            }
        }
    }

    private fun isBluetoothSupported(): Boolean {
        return activity.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    }

    private fun isBluetoothEnabled(): Boolean {
        val bluetoothManager =
            activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return bluetoothManager.adapter?.isEnabled == true
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun requestEnableBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBluetoothLauncher.launch(enableBtIntent)
    }

    private fun requestEnableLocation() {
        val enableLocationIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        enableLocationLauncher.launch(enableLocationIntent)
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private fun checkAndRequestPermissions() {
        val requiredPermissions = getRequiredPermissions()

        // Separate normal permissions from ACCESS_BACKGROUND_LOCATION
        val backgroundLocationPermission = Manifest.permission.ACCESS_BACKGROUND_LOCATION
        val normalPermissions = requiredPermissions.filter { it != backgroundLocationPermission }
        val needsBackgroundLocation = requiredPermissions.contains(backgroundLocationPermission)

        // Verify normal permissions first
        val missingNormalPermissions = normalPermissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }

        when {
            missingNormalPermissions.isNotEmpty() -> {
                // Request normal permissions first
                requestPermissionsLauncher.launch(missingNormalPermissions.toTypedArray())
            }

            needsBackgroundLocation &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    ContextCompat.checkSelfPermission(
                        activity,
                        backgroundLocationPermission
                    ) != PackageManager.PERMISSION_GRANTED -> {
                // If normal permissions are ok, request background location if necessary
                // For Android 11+, this permission must be requested separately
                requestPermissionsLauncher.launch(arrayOf(backgroundLocationPermission))
            }

            else -> {
                // All permissions are granted
                onPermissionsGranted()
            }
        }
    }

    private fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf<String>()

        // Permissions for Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.addAll(
                listOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE
                )
            )
        } else {
            // Permissions for versions prior to Android 12
            permissions.addAll(
                listOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                )
            )
        }

        // Location permission always necessary for BLE (from API 23)
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)

        // ACCESS_COARSE_LOCATION can also be useful
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        // Background location permission for Android 10+ if BLE is used in background
        // IMPORTANT: This permission must be requested separately on Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        return permissions
    }

    fun hasAllPermissions(): Boolean {
        return isBluetoothSupported() &&
                isBluetoothEnabled() &&
                isLocationEnabled() &&
                getRequiredPermissions().all {
                    ContextCompat.checkSelfPermission(
                        activity,
                        it
                    ) == PackageManager.PERMISSION_GRANTED
                }
    }

    companion object
}
