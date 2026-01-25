package com.example.campergas.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

import androidx.navigation.NavController
import com.example.campergas.R
import com.example.campergas.domain.model.AppLanguage
import com.example.campergas.domain.model.ThemeMode

/**
 * Settings screen for application configuration.
 *
 * Provides UI for managing:
 * - Theme mode (light, dark, system)
 * - Notification preferences
 * - BLE sensor reading intervals
 * - Low fuel threshold
 *
 * @param navController Navigation controller for screen navigation
 * @param viewModel ViewModel managing settings state and persistence
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()
    val uiState by viewModel.uiState.collectAsState()

    // States for BLE intervals
    val weightInterval by viewModel.weightInterval.collectAsState()
    val inclinationInterval by viewModel.inclinationInterval.collectAsState()
    val operationStatus by viewModel.operationStatus.collectAsState()

    // Local states for text fields
    var weightIntervalText by remember { mutableStateOf("") }
    var inclinationIntervalText by remember { mutableStateOf("") }
    var gasThresholdText by remember { mutableStateOf("") }

    // Update text fields when values change
    LaunchedEffect(weightInterval) {
        weightIntervalText = weightInterval.toString()
    }

    LaunchedEffect(inclinationInterval) {
        inclinationIntervalText = inclinationInterval.toString()
    }

    // Update gas threshold text field
    LaunchedEffect(uiState.gasLevelThreshold) {
        gasThresholdText = uiState.gasLevelThreshold.toString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Back button and title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.nav_back)
                )
            }
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Show operation status if there is one
        operationStatus?.let { status ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_status),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Application language configuration
        LanguageSelectionCard(
            currentLanguage = uiState.language,
            onLanguageSelected = { language ->
                viewModel.setAppLanguage(language)
            }
        )

        // Application theme configuration
        ThemeSelectionCard(
            currentThemeMode = uiState.themeMode,
            onThemeModeSelected = { themeMode ->
                viewModel.setThemeMode(themeMode)
            }
        )

        // Notification configuration
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_notifications),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_enable_notifications),
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = uiState.notificationsEnabled,
                        onCheckedChange = { viewModel.toggleNotifications() }
                    )
                }

                if (uiState.notificationsEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.settings_gas_threshold),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = gasThresholdText,
                        onValueChange = { newValue ->
                            gasThresholdText = newValue
                        },
                        label = { Text(stringResource(R.string.kilograms_minimum_gas)) },
                        suffix = { Text(stringResource(R.string.settings_kg_suffix)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val threshold = gasThresholdText.toFloatOrNull()
                            if (threshold != null && threshold in 0.5f..15.0f) {
                                viewModel.setGasLevelThreshold(threshold)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.save_threshold))
                    }

                    Text(
                        text = stringResource(
                            R.string.settings_gas_threshold_description,
                            "%.2f".format(uiState.gasLevelThreshold)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        // Interval configuration of weight
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_weight_reading),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = stringResource(R.string.settings_weight_interval_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = weightIntervalText,
                    onValueChange = { newValue ->
                        weightIntervalText = newValue
                    },
                    label = { Text(stringResource(R.string.settings_weight_interval_label)) },
                    suffix = { Text(stringResource(R.string.unit_minutes)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val interval = weightIntervalText.toIntOrNull()
                        if (interval != null && interval in 1..60) {
                            viewModel.setWeightInterval(interval)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = weightIntervalText.toIntOrNull()?.let { it in 1..60 } == true
                ) {
                    Text(stringResource(R.string.settings_apply_weight_interval))
                }

                Text(
                    text = stringResource(R.string.settings_weight_range),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
        }

        // Interval configuration of inclination
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_inclination_reading),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = stringResource(R.string.settings_inclination_interval_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = inclinationIntervalText,
                    onValueChange = { newValue ->
                        inclinationIntervalText = newValue
                    },
                    label = { Text(stringResource(R.string.settings_inclination_interval_label)) },
                    suffix = { Text(stringResource(R.string.unit_seconds)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val interval = inclinationIntervalText.toIntOrNull()
                        if (interval != null && interval in 1..300) {
                            viewModel.setInclinationInterval(interval)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = inclinationIntervalText.toIntOrNull()?.let { it in 1..300 } == true
                ) {
                    Text(stringResource(R.string.settings_apply_inclination_interval))
                }

                Text(
                    text = stringResource(R.string.settings_inclination_range),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
        }

        // Sensor Calibration section
        SensorCalibrationCard(
            isConnected = viewModel.isConnected.collectAsState().value,
            onTare = { onSuccess, onError ->
                viewModel.performTare(onSuccess, onError)
            },
            onCalibrate = { weight, onSuccess, onError ->
                viewModel.performCalibration(weight, onSuccess, onError)
            }
        )

        // Test Data Management
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_test_data),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = stringResource(R.string.settings_test_data_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Button(
                    onClick = { viewModel.generateTestData() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_generate_test_data))
                }

                Text(
                    text = stringResource(R.string.settings_generate_test_data_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Button(
                    onClick = { viewModel.deleteNonActiveData() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_delete_non_active_data))
                }

                Text(
                    text = stringResource(R.string.settings_delete_non_active_data_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Additional information
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_information),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = stringResource(R.string.settings_info_short_intervals),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = stringResource(R.string.settings_info_long_intervals),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = stringResource(R.string.settings_info_defaults),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = stringResource(R.string.settings_info_auto_save),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * Theme selection card for the application.
 *
 * Provides a user interface to select between different available theme modes:
 * light, dark, and system. Uses a dropdown menu for selection.
 *
 * @param currentThemeMode Currently selected theme mode
 * @param onThemeModeSelected Callback invoked when user selects a new theme
 */
@Composable
private fun LanguageSelectionCard(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = stringResource(R.string.settings_language_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Box {
                Button(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(languageLabel(currentLanguage))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    AppLanguage.entries.forEach { language ->
                        DropdownMenuItem(
                            text = { Text(languageLabel(language)) },
                            onClick = {
                                onLanguageSelected(language)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.settings_language_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ThemeSelectionCard(
    currentThemeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit
) {
    // State to control whether the dropdown menu is expanded
    var expanded by remember { mutableStateOf(false) }
    
    // Determine the current system theme
    val systemIsDark = androidx.compose.foundation.isSystemInDarkTheme()

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_theme),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = stringResource(R.string.settings_theme_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Button that shows the current theme and opens the dropdown menu
            Box {
                Button(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (currentThemeMode) {
                                ThemeMode.LIGHT -> stringResource(R.string.theme_mode_light)
                                ThemeMode.DARK -> stringResource(R.string.theme_mode_dark)
                                // If for some reason it's SYSTEM, show the current system theme
                                ThemeMode.SYSTEM -> if (systemIsDark) {
                                    stringResource(R.string.theme_mode_dark)
                                } else {
                                    stringResource(R.string.theme_mode_light)
                                }
                            }
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                }

                // Dropdown menu with theme options
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    // Light theme option
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.theme_mode_light)) },
                        onClick = {
                            onThemeModeSelected(ThemeMode.LIGHT)
                            expanded = false
                        }
                    )
                    // Dark theme option
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.theme_mode_dark)) },
                        onClick = {
                            onThemeModeSelected(ThemeMode.DARK)
                            expanded = false
                        }
                    )
                }
            }

            // Show the current theme status
            Text(
                text = when (currentThemeMode) {
                    ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                    ThemeMode.DARK -> stringResource(R.string.theme_dark)
                    // If for some reason it's SYSTEM, show the current system theme
                    ThemeMode.SYSTEM -> if (systemIsDark) {
                        stringResource(R.string.theme_dark)
                    } else {
                        stringResource(R.string.theme_light)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun languageLabel(language: AppLanguage): String {
    return when (language) {
        AppLanguage.SYSTEM -> stringResource(R.string.language_option_system)
        AppLanguage.SPANISH -> stringResource(R.string.language_option_spanish)
        AppLanguage.ENGLISH -> stringResource(R.string.language_option_english)
        AppLanguage.CATALAN -> stringResource(R.string.language_option_catalan)
    }
}

/**
 * Sensor calibration card for tare and calibration operations.
 *
 * Provides UI for:
 * - Tare (zeroing) the scale
 * - Calibrating with a known weight
 *
 * Requires an active BLE connection to the sensor.
 *
 * @param isConnected Whether the sensor is currently connected
 * @param onTare Callback for tare operation with success/error callbacks
 * @param onCalibrate Callback for calibration with weight and success/error callbacks
 */
@Composable
private fun SensorCalibrationCard(
    isConnected: Boolean,
    onTare: (onSuccess: (String) -> Unit, onError: (String) -> Unit) -> Unit,
    onCalibrate: (weight: Float, onSuccess: (String) -> Unit, onError: (String) -> Unit) -> Unit
) {
    var calibrationWeightText by remember { mutableStateOf("") }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    // Localized messages
    val successTareMsg = stringResource(R.string.msg_success_tare)
    val successCalMsg = stringResource(R.string.msg_success_cal)
    val errorNotConnectedMsg = stringResource(R.string.msg_error_not_connected)
    val errorCalibrationMsg = stringResource(R.string.msg_error_calibration)

    // Clear feedback message after delay
    LaunchedEffect(feedbackMessage) {
        if (feedbackMessage != null) {
            kotlinx.coroutines.delay(3000)
            feedbackMessage = null
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_cal_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Show feedback message if present
            feedbackMessage?.let { message ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isError) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    )
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isError) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Tare section
            Text(
                text = stringResource(R.string.settings_tare_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Button(
                onClick = {
                    if (!isConnected) {
                        feedbackMessage = errorNotConnectedMsg
                        isError = true
                    } else {
                        onTare(
                            { _ ->
                                feedbackMessage = successTareMsg
                                isError = false
                            },
                            { _ ->
                                feedbackMessage = errorCalibrationMsg
                                isError = true
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isConnected
            ) {
                Text(stringResource(R.string.settings_btn_tare))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calibration section
            Text(
                text = stringResource(R.string.settings_cal_instr),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = calibrationWeightText,
                onValueChange = { newValue ->
                    calibrationWeightText = newValue
                },
                label = { Text(stringResource(R.string.settings_lbl_weight)) },
                placeholder = { Text(stringResource(R.string.settings_cal_weight_placeholder)) },
                suffix = { Text(stringResource(R.string.settings_kg_suffix)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = isConnected,
                isError = calibrationWeightText.isNotEmpty() &&
                        calibrationWeightText.toFloatOrNull()?.let { it <= 0 } == true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (!isConnected) {
                        feedbackMessage = errorNotConnectedMsg
                        isError = true
                    } else {
                        val weight = calibrationWeightText.toFloatOrNull()
                        if (weight != null && weight > 0) {
                            onCalibrate(
                                weight,
                                { _ ->
                                    feedbackMessage = successCalMsg
                                    isError = false
                                    calibrationWeightText = ""
                                },
                                { _ ->
                                    feedbackMessage = errorCalibrationMsg
                                    isError = true
                                }
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isConnected &&
                        calibrationWeightText.toFloatOrNull()?.let { it > 0 } == true
            ) {
                Text(stringResource(R.string.settings_btn_cal))
            }

            // Connection status hint
            if (!isConnected) {
                Text(
                    text = stringResource(R.string.msg_error_not_connected),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}