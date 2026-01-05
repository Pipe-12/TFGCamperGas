package com.example.campergas.data.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized Bluetooth Low Energy (BLE) manager.
 *
 * This singleton class provides unified access to the device's Bluetooth adapter
 * and manages verification of permissions required for BLE operations. It abstracts
 * the differences between Android versions (especially permission changes in Android 12+).
 *
 * Main functionality:
 * - Access to the system's Bluetooth adapter
 * - Verification of Bluetooth state (enabled/disabled)
 * - BLE permission checking according to Android version
 * - Handling BLUETOOTH_SCAN and BLUETOOTH_CONNECT permissions (Android 12+)
 * - Handling legacy permissions (Android 11 and earlier)
 *
 * @property context Application context for accessing system services
 * @author Felipe García Gómez
 */
@Singleton
class BleManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * System Bluetooth manager.
     *
     * Lazy initialized the first time it's accessed, obtaining
     * the BLUETOOTH_SERVICE from the Android system.
     */
    private val bluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    /**
     * Device Bluetooth adapter.
     *
     * Provides access to Bluetooth functionality, including
     * the BLE scanner and connection management. Can be null if the
     * device doesn't have Bluetooth hardware.
     */
    val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager.adapter

    /**
     * Verifies if Bluetooth is enabled on the device.
     *
     * @return true if Bluetooth is turned on, false if it's off or unavailable
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    /**
     * Verifies if we have the necessary permissions to connect BLE devices.
     *
     * On Android 12+ (API 31+), requires BLUETOOTH_CONNECT permission.
     * On earlier versions, requires legacy BLUETOOTH permission.
     *
     * @return true if we have the necessary permission, false otherwise
     */
    fun hasBluetoothConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }


}
