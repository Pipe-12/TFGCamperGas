package com.example.campergas

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.rememberNavController
import com.example.campergas.data.local.preferences.PreferencesDataStore
import com.example.campergas.domain.model.ThemeMode
import com.example.campergas.ui.components.PermissionDialog
import com.example.campergas.ui.navigation.NavGraph
import com.example.campergas.ui.theme.CamperGasTheme
import com.example.campergas.utils.BluetoothPermissionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Main activity for the CamperGas application.
 *
 * This activity is the entry point for the user interface and manages:
 * - Theme configuration (light, dark, or system)
 * - Bluetooth permissions required for BLE connection
 * - Navigation between screens using Jetpack Compose Navigation
 * - Edge-to-edge styling for system bars
 *
 * Uses Jetpack Compose for all UI and Hilt for dependency injection.
 *
 * @author Felipe García Gómez
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    /**
     * User preferences data store.
     *
     * Provides access to saved settings such as:
     * - Notification state
     * - Gas level thresholds
     * - Sensor reading intervals
     */
    @Inject
    lateinit var preferencesDataStore: PreferencesDataStore

    /**
     * Bluetooth permission manager.
     *
     * Handles request and verification of permissions required for:
     * - BLE device scanning
     * - BLE sensor connection
     * - Location access (required for BLE on Android)
     */
    private lateinit var bluetoothPermissionManager: BluetoothPermissionManager

    /**
     * Initializes the activity and configures the user interface.
     *
     * This method performs the following operations:
     * 1. Loads saved theme from user preferences
     * 2. Configures system bars in edge-to-edge mode according to theme
     * 3. Initializes Bluetooth permission manager
     * 4. Sets up content with Jetpack Compose applying the selected theme
     * 5. Establishes navigation system
     * 6. Shows permission dialog if necessary
     *
     * @param savedInstanceState Saved activity state if previously destroyed
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure the Bluetooth permission manager
        bluetoothPermissionManager = BluetoothPermissionManager(
            activity = this,
            onPermissionsGranted = {
                // Permissions granted, the application can use BLE
            },
            onPermissionsDenied = { deniedPermissions ->
                // Handle denied permissions
            }
        )

        setContent {
            // Load saved theme from preferences reactively
            // Use collectAsState so the theme updates automatically when it changes
            // Initial value is SYSTEM to avoid flickering on first frame
            val themeMode by preferencesDataStore.themeMode.collectAsState(initial = ThemeMode.SYSTEM)

            // Determine the current system theme
            val systemIsDark = androidx.compose.foundation.isSystemInDarkTheme()

            // Convert SYSTEM to actual system theme on first launch
            // LaunchedEffect(Unit) ensures this only runs once per app session
            // Note: This migration happens asynchronously. Meanwhile, the SYSTEM theme
            // is handled correctly in the UI using the systemIsDark value.
            LaunchedEffect(Unit) {
                // Get current theme from preferences
                val currentTheme = preferencesDataStore.themeMode.first()
                if (currentTheme == ThemeMode.SYSTEM) {
                    // If theme is SYSTEM, convert it to the current system theme
                    val newTheme = if (systemIsDark) ThemeMode.DARK else ThemeMode.LIGHT
                    preferencesDataStore.setThemeMode(newTheme)
                }
            }

            // Determine if dark theme should be used for configuring system bars
            val isDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemIsDark
            }

            // Configure system bars according to current theme
            LaunchedEffect(isDarkTheme) {
                configureSystemBars(isDarkTheme)
            }

            // Apply the application theme
            CamperGasTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // State to control whether to show the permission dialog
                    var showPermissionDialog by remember { mutableStateOf(false) }

                    // Verify permissions at startup
                    LaunchedEffect(Unit) {
                        if (!bluetoothPermissionManager.hasAllPermissions()) {
                            showPermissionDialog = true
                        }
                    }

                    // Show permission dialog if necessary
                    if (showPermissionDialog) {
                        PermissionDialog(
                            title = stringResource(R.string.permissions_needed_title),
                            message = stringResource(R.string.permissions_needed_message),
                            onAccept = {
                                showPermissionDialog = false
                                bluetoothPermissionManager.checkAndRequestAllPermissions()
                            },
                            onDismiss = {
                                showPermissionDialog = false
                            }
                        )
                    }

                    NavGraph(navController = navController)
                }
            }
        }
    }

    /**
     * Configures system bars styling according to current theme.
     *
     * This method applies appropriate styling to status bar and navigation bar
     * based on selected theme (light or dark). Uses transparent colors
     * to allow content to extend to screen edges (edge-to-edge).
     *
     * @param isDarkTheme true to apply dark theme, false for light theme
     */
    private fun configureSystemBars(isDarkTheme: Boolean) {
        enableEdgeToEdge(
            statusBarStyle = if (isDarkTheme) {
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                )
            },
            navigationBarStyle = if (isDarkTheme) {
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                )
            }
        )
    }

}