package com.example.campergas.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.campergas.R
import com.example.campergas.domain.model.AppLanguage
import com.example.campergas.domain.model.ThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val weightInterval by viewModel.weightInterval.collectAsState()
    val inclinationInterval by viewModel.inclinationInterval.collectAsState()
    val operationStatus by viewModel.operationStatus.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showGasThresholdDialog by remember { mutableStateOf(false) }
    var showWeightIntervalDialog by remember { mutableStateOf(false) }
    var showInclinationIntervalDialog by remember { mutableStateOf(false) }
    var showCalibrationDialog by remember { mutableStateOf(false) }

    var gasThresholdText by remember { mutableStateOf(uiState.gasLevelThreshold.toString()) }
    var weightIntervalText by remember { mutableStateOf(weightInterval.toString()) }
    var inclinationIntervalText by remember { mutableStateOf(inclinationInterval.toString()) }
    var calibrationWeightText by remember { mutableStateOf("") }
    val successTareMsg = stringResource(R.string.msg_success_tare)
    val successCalMsg = stringResource(R.string.msg_success_cal)
    val errorCalibrationMsg = stringResource(R.string.msg_error_calibration)

    LaunchedEffect(uiState.gasLevelThreshold) {
        gasThresholdText = uiState.gasLevelThreshold.toString()
    }

    LaunchedEffect(weightInterval) {
        weightIntervalText = weightInterval.toString()
    }

    LaunchedEffect(inclinationInterval) {
        inclinationIntervalText = inclinationInterval.toString()
    }

    LaunchedEffect(operationStatus) {
        operationStatus?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSectionHeader(title = stringResource(R.string.settings_section_appearance))
                SettingsSection {
                    SettingsItem(
                        title = stringResource(R.string.settings_language),
                        subtitle = languageLabel(uiState.language),
                        onClick = { showLanguageDialog = true }
                    )
                    HorizontalDivider()
                    SettingsItem(
                        title = stringResource(R.string.settings_theme),
                        subtitle = themeLabel(uiState.themeMode),
                        onClick = { showThemeDialog = true }
                    )
                }
            }

            item {
                SettingsSectionHeader(title = stringResource(R.string.settings_section_notifications))
                SettingsSection {
                    SettingsItemWithSwitch(
                        title = stringResource(R.string.settings_enable_notifications),
                        subtitle = stringResource(R.string.settings_notifications),
                        checked = uiState.notificationsEnabled,
                        onCheckedChange = { viewModel.toggleNotifications() }
                    )
                    if (uiState.notificationsEnabled) {
                        HorizontalDivider()
                        SettingsItem(
                            title = stringResource(R.string.settings_gas_threshold),
                            subtitle = stringResource(
                                R.string.settings_gas_threshold_description,
                                "%.2f".format(uiState.gasLevelThreshold)
                            ),
                            onClick = {
                                gasThresholdText = uiState.gasLevelThreshold.toString()
                                showGasThresholdDialog = true
                            }
                        )
                    }
                }
            }

            item {
                SettingsSectionHeader(title = stringResource(R.string.settings_section_sensors))
                SettingsSection {
                    SettingsItem(
                        title = stringResource(R.string.settings_weight_reading),
                        subtitle = stringResource(R.string.settings_weight_interval_value, weightInterval),
                        onClick = {
                            weightIntervalText = weightInterval.toString()
                            showWeightIntervalDialog = true
                        }
                    )
                    HorizontalDivider()
                    SettingsItem(
                        title = stringResource(R.string.settings_inclination_reading),
                        subtitle = stringResource(
                            R.string.settings_inclination_interval_value,
                            inclinationInterval
                        ),
                        onClick = {
                            inclinationIntervalText = inclinationInterval.toString()
                            showInclinationIntervalDialog = true
                        }
                    )
                }
            }

            item {
                SettingsSectionHeader(title = stringResource(R.string.settings_section_calibration))
                SettingsSection {
                    SettingsItem(
                        title = stringResource(R.string.settings_btn_tare),
                        subtitle = stringResource(R.string.settings_tare_desc),
                        onClick = {
                            viewModel.performTare(
                                onSuccess = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(successTareMsg)
                                    }
                                },
                                onError = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(errorCalibrationMsg)
                                    }
                                }
                            )
                        },
                        enabled = isConnected
                    )
                    HorizontalDivider()
                    SettingsItem(
                        title = stringResource(R.string.settings_btn_cal),
                        subtitle = stringResource(R.string.settings_cal_instr),
                        onClick = { showCalibrationDialog = true },
                        enabled = isConnected
                    )
                }
            }

            item {
                SettingsSectionHeader(title = stringResource(R.string.settings_section_data_management))
                SettingsSection {
                    SettingsItem(
                        title = stringResource(R.string.settings_generate_test_data),
                        subtitle = stringResource(R.string.settings_generate_test_data_description),
                        onClick = { viewModel.generateTestData() }
                    )
                    HorizontalDivider()
                    SettingsItem(
                        title = stringResource(R.string.settings_delete_non_active_data),
                        subtitle = stringResource(R.string.settings_delete_non_active_data_description),
                        onClick = { viewModel.deleteNonActiveData() }
                    )
                }
            }

            item {
                SettingsSectionHeader(title = stringResource(R.string.settings_section_information))
                SettingsSection {
                    val infoItems = listOf(
                        stringResource(R.string.settings_info_short_intervals),
                        stringResource(R.string.settings_info_long_intervals),
                        stringResource(R.string.settings_info_defaults),
                        stringResource(R.string.settings_info_auto_save)
                    )
                    infoItems.forEachIndexed { index, text ->
                        SettingsItem(title = text)
                        if (index < infoItems.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    if (showLanguageDialog) {
        SelectionDialog(
            title = stringResource(R.string.settings_language),
            options = AppLanguage.entries.map { it to languageLabel(it) },
            selected = uiState.language,
            onDismiss = { showLanguageDialog = false },
            onSelect = {
                viewModel.setAppLanguage(it)
                showLanguageDialog = false
            }
        )
    }

    if (showThemeDialog) {
        val options = listOf(ThemeMode.LIGHT, ThemeMode.DARK)
        SelectionDialog(
            title = stringResource(R.string.settings_theme),
            options = options.map { it to themeModeLabel(it) },
            selected = if (uiState.themeMode == ThemeMode.SYSTEM) {
                if (isSystemInDarkTheme()) ThemeMode.DARK else ThemeMode.LIGHT
            } else {
                uiState.themeMode
            },
            onDismiss = { showThemeDialog = false },
            onSelect = {
                viewModel.setThemeMode(it)
                showThemeDialog = false
            }
        )
    }

    if (showGasThresholdDialog) {
        NumberInputDialog(
            title = stringResource(R.string.settings_gas_threshold),
            value = gasThresholdText,
            suffix = stringResource(R.string.settings_kg_suffix),
            keyboardType = KeyboardType.Decimal,
            onValueChange = { gasThresholdText = it },
            onDismiss = { showGasThresholdDialog = false },
            onConfirm = {
                val threshold = gasThresholdText.toFloatOrNull()
                if (threshold != null && threshold in 0.5f..20.0f) {
                    viewModel.setGasLevelThreshold(threshold)
                    showGasThresholdDialog = false
                }
            },
            isConfirmEnabled = gasThresholdText.toFloatOrNull()?.let { it in 0.5f..20.0f } == true,
            hint = stringResource(R.string.settings_gas_threshold_description, "0.50")
        )
    }

    if (showWeightIntervalDialog) {
        NumberInputDialog(
            title = stringResource(R.string.settings_weight_reading),
            value = weightIntervalText,
            suffix = stringResource(R.string.unit_minutes),
            keyboardType = KeyboardType.Number,
            onValueChange = { weightIntervalText = it },
            onDismiss = { showWeightIntervalDialog = false },
            onConfirm = {
                val value = weightIntervalText.toIntOrNull()
                if (value != null && value in 1..60) {
                    viewModel.setWeightInterval(value)
                    showWeightIntervalDialog = false
                }
            },
            isConfirmEnabled = weightIntervalText.toIntOrNull()?.let { it in 1..60 } == true,
            hint = stringResource(R.string.settings_weight_range)
        )
    }

    if (showInclinationIntervalDialog) {
        NumberInputDialog(
            title = stringResource(R.string.settings_inclination_reading),
            value = inclinationIntervalText,
            suffix = stringResource(R.string.unit_seconds),
            keyboardType = KeyboardType.Number,
            onValueChange = { inclinationIntervalText = it },
            onDismiss = { showInclinationIntervalDialog = false },
            onConfirm = {
                val value = inclinationIntervalText.toIntOrNull()
                if (value != null && value in 1..300) {
                    viewModel.setInclinationInterval(value)
                    showInclinationIntervalDialog = false
                }
            },
            isConfirmEnabled = inclinationIntervalText.toIntOrNull()?.let { it in 1..300 } == true,
            hint = stringResource(R.string.settings_inclination_range)
        )
    }

    if (showCalibrationDialog) {
        NumberInputDialog(
            title = stringResource(R.string.settings_btn_cal),
            value = calibrationWeightText,
            suffix = stringResource(R.string.settings_kg_suffix),
            keyboardType = KeyboardType.Decimal,
            onValueChange = { calibrationWeightText = it },
            onDismiss = { showCalibrationDialog = false },
            onConfirm = {
                val weight = calibrationWeightText.toFloatOrNull()
                if (weight != null && weight > 0) {
                    viewModel.performCalibration(
                        weight,
                        onSuccess = {
                            scope.launch {
                                snackbarHostState.showSnackbar(successCalMsg)
                            }
                        },
                        onError = {
                            scope.launch {
                                snackbarHostState.showSnackbar(errorCalibrationMsg)
                            }
                        }
                    )
                    calibrationWeightText = ""
                    showCalibrationDialog = false
                }
            },
            isConfirmEnabled = isConnected &&
                    (calibrationWeightText.toFloatOrNull()?.let { it > 0 } == true),
            hint = stringResource(R.string.settings_cal_instr)
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun SettingsSection(content: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth()) {
        Column { content() }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(enabled = enabled, onClick = onClick)
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsItemWithSwitch(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun <T> SelectionDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column {
                options.forEach { option ->
                    TextButton(
                        onClick = { onSelect(option.first) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val prefix = if (option.first == selected) "✓ " else ""
                        Text(
                            text = prefix + option.second,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

@Composable
private fun NumberInputDialog(
    title: String,
    value: String,
    suffix: String,
    keyboardType: KeyboardType,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isConfirmEnabled: Boolean,
    hint: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                androidx.compose.material3.OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    singleLine = true,
                    suffix = { Text(suffix) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = isConfirmEnabled) {
                Text(stringResource(R.string.dialog_accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
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

@Composable
private fun themeModeLabel(themeMode: ThemeMode): String {
    return when (themeMode) {
        ThemeMode.LIGHT -> stringResource(R.string.theme_mode_light)
        ThemeMode.DARK -> stringResource(R.string.theme_mode_dark)
        ThemeMode.SYSTEM -> stringResource(R.string.theme_mode_system)
    }
}

@Composable
private fun themeLabel(themeMode: ThemeMode): String {
    return when (themeMode) {
        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
        ThemeMode.DARK -> stringResource(R.string.theme_dark)
        ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) {
            stringResource(R.string.theme_dark)
        } else {
            stringResource(R.string.theme_light)
        }
    }
}
