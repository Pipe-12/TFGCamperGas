package com.example.campergas.ui.screens.inclination

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.campergas.R
import com.example.campergas.domain.model.VehicleType
import com.example.campergas.ui.components.VehicleInclinationView
import com.example.campergas.ui.components.WheelElevationsDisplay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen for monitoring vehicle inclination and stability.
 *
 * Features:
 * - Real-time pitch and roll angle display
 * - Vehicle visualization from side and rear views
 * - Wheel elevation calculations for leveling
 * - Request inclination data from BLE sensor
 *
 * @param navController Navigation controller for screen navigation
 * @param viewModel ViewModel managing inclination sensor data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InclinationScreen(
    navController: NavController,
    viewModel: InclinationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRequestingData by viewModel.isRequestingData.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.inclination_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.ble_back_button)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.inclination_waiting_data))

                        Spacer(modifier = Modifier.height(16.dp))

                        // Button to request data when loading
                        OutlinedButton(
                            onClick = { viewModel.requestInclinationDataManually() },
                            enabled = viewModel.isConnected() && viewModel.canMakeRequest()
                        ) {
                            if (isRequestingData) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isRequestingData) stringResource(R.string.inclination_requesting_button) else stringResource(R.string.inclination_request_button))
                        }

                        if (!viewModel.isConnected()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.inclination_connect_sensor_first),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else if (!viewModel.canMakeRequest()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.inclination_wait_between_requests),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Level status
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.isLevel)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (uiState.isLevel) stringResource(R.string.inclination_vehicle_leveled_status) else stringResource(R.string.inclination_vehicle_unleveled_status),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isLevel)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Vehicle inclination visualization

                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.inclination_visualization_icon_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            VehicleInclinationView(
                                vehicleType = uiState.vehicleType,
                                pitchAngle = uiState.inclinationPitch,
                                rollAngle = uiState.inclinationRoll,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = stringResource(
                                    R.string.inclination_vehicle_type,
                                    when (uiState.vehicleType) {
                                        VehicleType.CARAVAN -> stringResource(R.string.inclination_vehicle_type_caravan)
                                        VehicleType.AUTOCARAVANA -> stringResource(R.string.inclination_vehicle_type_motorhome)
                                    }
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Show wheel elevations only if there is configuration
                    if (uiState.distanceBetweenRearWheels > 0 && uiState.distanceToFrontSupport > 0) {
                        WheelElevationsDisplay(
                            vehicleType = uiState.vehicleType,
                            wheelElevations = uiState.wheelElevations,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        // Message to configure the vehicle
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.inclination_config_icon_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.inclination_config_description),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Additional information
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.inclination_sensor_info_title),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            if (uiState.timestamp > 0) {
                                Text(
                                    text = stringResource(R.string.inclination_last_update_format, formatTimestamp(uiState.timestamp)),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            // Show if each axis is leveled
                            Text(
                                text = stringResource(
                                    R.string.inclination_pitch_status_format,
                                    if (kotlin.math.abs(uiState.inclinationPitch) <= 2.0f) 
                                        stringResource(R.string.inclination_pitch_leveled_emoji) 
                                    else 
                                        stringResource(R.string.inclination_pitch_unleveled_emoji)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            Text(
                                text = stringResource(
                                    R.string.inclination_roll_status_format,
                                    if (kotlin.math.abs(uiState.inclinationRoll) <= 2.0f) 
                                        stringResource(R.string.inclination_pitch_leveled_emoji) 
                                    else 
                                        stringResource(R.string.inclination_pitch_unleveled_emoji)
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Button to manually request inclination data
                    Button(
                        onClick = { viewModel.requestInclinationDataManually() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = viewModel.isConnected() && viewModel.canMakeRequest()
                    ) {
                        if (isRequestingData) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isRequestingData) stringResource(R.string.inclination_requesting_button) else stringResource(R.string.inclination_update_inclination))
                    }

                    if (!viewModel.isConnected()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.inclination_sensor_not_connected_emoji),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (!viewModel.canMakeRequest()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.inclination_wait_between_requests),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Help information
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.inclination_info_icon_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.inclination_info_details),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
