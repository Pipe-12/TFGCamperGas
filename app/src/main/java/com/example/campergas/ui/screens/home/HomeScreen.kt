package com.example.campergas.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.campergas.R
import com.example.campergas.domain.model.VehicleType
import com.example.campergas.ui.components.VehicleInclinationView
import com.example.campergas.ui.components.gas.AddGasCylinderDialog
import com.example.campergas.ui.components.gas.GasCylinderViewModel
import com.example.campergas.ui.navigation.Screen
import com.example.campergas.ui.screens.weight.GasCylinderVisualizer

/**
 * Main home screen of the CamperGas application.
 *
 * Displays:
 * - Current gas cylinder status and fuel level
 * - Real-time BLE connection status
 * - Quick access to add new cylinders
 * - Vehicle inclination overview (compact mode)
 * - Navigation to detailed screens
 *
 * @param navController Navigation controller for screen navigation
 * @param viewModel ViewModel managing home screen state
 * @param gasCylinderViewModel ViewModel for gas cylinder operations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
    gasCylinderViewModel: GasCylinderViewModel = hiltViewModel()
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val fuelData by viewModel.fuelData.collectAsState()
    val vehicleConfig by viewModel.vehicleConfig.collectAsState()
    val lastDayConsumption by viewModel.lastDayConsumption.collectAsState()
    val lastWeekConsumption by viewModel.lastWeekConsumption.collectAsState()
    val inclinationPitch by viewModel.inclinationPitch.collectAsState()
    val inclinationRoll by viewModel.inclinationRoll.collectAsState()

    var showAddCylinderDialog by remember { mutableStateOf(false) }

    // Make data request from sensor when opening screen
    LaunchedEffect(Unit) {
        viewModel.requestSensorDataOnScreenOpen()
    }

    // Dialog to add cylinder
    if (showAddCylinderDialog) {
        AddGasCylinderDialog(
            onDismiss = { showAddCylinderDialog = false },
            onConfirm = { name, tare, capacity, setAsActive ->
                gasCylinderViewModel.addCylinder(name, tare, capacity, setAsActive)
                showAddCylinderDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                "CamperGas",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Connection status in topbar
                        Text(
                            text = if (connectionState) stringResource(R.string.connection_status_connected) else stringResource(
                                R.string.connection_status_disconnected
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main content area - scrollable for smaller screens
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top section: Weight and History buttons side by side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Weight button (left)
                    NavigationButtonWithPreview(
                        title = stringResource(R.string.home_weight_monitoring_title),
                        description = stringResource(R.string.home_weight_monitoring_description),
                        onClick = { navController.navigate(Screen.Weight.route) },
                        modifier = Modifier.weight(1f),
                        isLargeButton = true,
                        verticalLayout = true
                    ) {
                        // Show the cylinder with the current percentage
                        fuelData?.let { fuel ->
                            GasCylinderVisualizer(
                                fuelPercentage = fuel.fuelPercentage,
                                modifier = Modifier.size(60.dp, 90.dp)
                            )
                        } ?: run {
                            // Empty cylinder if no data
                            GasCylinderVisualizer(
                                fuelPercentage = 0f,
                                modifier = Modifier.size(60.dp, 90.dp)
                            )
                        }
                    }

                    // History button (right)
                    NavigationButtonWithPreview(
                        title = stringResource(R.string.home_consumption_history_title),
                        description = stringResource(R.string.home_consumption_history_description),
                        onClick = { navController.navigate(Screen.Consumption.route) },
                        modifier = Modifier.weight(1f),
                        isLargeButton = true,
                        verticalLayout = true
                    ) {
                        // Mostrar resumen compacto de consumption
                        ConsumptionPreview(
                            lastDayConsumption = lastDayConsumption,
                            lastWeekConsumption = lastWeekConsumption
                        )
                    }
                }

                // Bottom section: Inclination button with larger space for drawing
                NavigationButtonWithPreview(
                    title = stringResource(R.string.home_inclination_title),
                    description = stringResource(R.string.home_inclination_description),
                    onClick = { navController.navigate(Screen.Inclination.route) },
                    isLargeButton = true
                ) {
                    // Show compact view of the vehicle with inclination for the home screen
                    vehicleConfig?.let { config ->
                        VehicleInclinationView(
                            vehicleType = config.type,
                            pitchAngle = inclinationPitch,
                            rollAngle = inclinationRoll,
                            modifier = Modifier.height(160.dp),
                            compact = true
                        )
                    } ?: run {
                        // Default view if there is no configuration
                        VehicleInclinationView(
                            vehicleType = VehicleType.CARAVAN,
                            pitchAngle = inclinationPitch,
                            rollAngle = inclinationRoll,
                            modifier = Modifier.height(160.dp),
                            compact = true
                        )
                    }
                }
            }

            // Add cylinder button section - positioned above configuration
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp)
            ) {
                Button(
                    onClick = { showAddCylinderDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(stringResource(R.string.home_add_cylinder))
                }
            }

            // Static bottom configuration buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.configuration),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // BLE connection button
                    ConfigurationButton(
                        title = stringResource(R.string.home_connect_ble_title),
                        text = stringResource(R.string.home_connect_icon),
                        onClick = { navController.navigate(Screen.BleConnect.route) },
                        modifier = Modifier.weight(1f)
                    )

                    // Configuration button
                    ConfigurationButton(
                        title = stringResource(R.string.home_configuration_title),
                        icon = Icons.Default.Settings,
                        onClick = { navController.navigate(Screen.Settings.route) },
                        modifier = Modifier.weight(1f)
                    )

                    // Vehicle settings button
                    val vehicleIcon = getVehicleIcon(vehicleConfig?.type)
                    ConfigurationButton(
                        title = stringResource(R.string.home_vehicle_settings_title),
                        text = vehicleIcon,
                        onClick = { navController.navigate(Screen.CaravanConfig.route) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationButtonWithPreview(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLargeButton: Boolean = false,
    verticalLayout: Boolean = false,
    content: @Composable () -> Unit
) {
    val buttonHeight = if (isLargeButton) 280.dp else 160.dp

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .height(buttonHeight),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        onClick = onClick
    ) {
        if (verticalLayout) {
            // Vertical layout: text on top, content below
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Text section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        lineHeight = MaterialTheme.typography.titleMedium.lineHeight,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                // Content section (cylinder, chart, etc.)
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    content()
                }
            }
        } else {
            // Horizontal layout: text on left, content on right (original layout)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        lineHeight = MaterialTheme.typography.titleMedium.lineHeight
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                    )
                }

                // Preview content (cylinder, chart, etc.)
                Box(
                    modifier = Modifier.width(if (isLargeButton) 140.dp else 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun ConsumptionPreview(
    lastDayConsumption: Float,
    lastWeekConsumption: Float
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.home_summary),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.home_consumption_24h, lastDayConsumption),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.home_consumption_7d, lastWeekConsumption),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun ConfigurationButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    text: String? = null
) {
    ElevatedCard(
        modifier = modifier.height(80.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else if (text != null) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun getVehicleIcon(vehicleType: VehicleType?): String {
    return when (vehicleType) {
        VehicleType.CARAVAN -> "🚐"
        VehicleType.AUTOCARAVANA -> "🚌"
        null -> "🚐"
    }
}
