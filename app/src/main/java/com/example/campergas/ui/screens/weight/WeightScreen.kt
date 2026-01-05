package com.example.campergas.ui.screens.weight

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.campergas.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GasCylinderVisualizer(
    fuelPercentage: Float,
    modifier: Modifier = Modifier
) {
    val fillColor = when {
        fuelPercentage > 50 -> MaterialTheme.colorScheme.primary
        fuelPercentage > 20 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    // Use theme-appropriate colors for better consistency
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val cylinderBackground = if (isDarkTheme) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    }

    Box(
        modifier = modifier.size(width = 140.dp, height = 220.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            drawGasCylinder(
                fillPercentage = fuelPercentage / 100f,
                fillColor = fillColor,
                backgroundColor = cylinderBackground
            )
        }

        // Percentage text with theme-aware coloring
        Text(
            text = stringResource(R.string.weight_percentage_format, fuelPercentage.toInt()),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

private fun DrawScope.drawGasCylinder(
    fillPercentage: Float,
    fillColor: Color,
    backgroundColor: Color
) {
    val cylinderWidth = size.width * 0.7f
    val cylinderHeight = size.height * 0.7f
    val topCapHeight = size.height * 0.06f
    val bottomCapHeight = size.height * 0.06f
    val valveWidth = cylinderWidth * 0.25f
    val valveHeight = size.height * 0.08f

    val startX = (size.width - cylinderWidth) / 2f
    val startY = size.height * 0.12f

    // Dibujar la base inferior
    drawRoundRect(
        color = Color.Gray,
        topLeft = Offset(startX, startY + topCapHeight + cylinderHeight),
        size = Size(cylinderWidth, bottomCapHeight),
        cornerRadius = CornerRadius(cylinderWidth * 0.05f)
    )

    // Draw main cylinder outline (thicker)
    val strokeWidth = 8f
    drawRoundRect(
        color = Color.Gray,
        topLeft = Offset(startX - strokeWidth / 2, startY + topCapHeight - strokeWidth / 2),
        size = Size(cylinderWidth + strokeWidth, cylinderHeight + strokeWidth),
        cornerRadius = CornerRadius(cylinderWidth * 0.08f)
    )

    // Dibujar el interior with the color de fondo according to el tema
    drawRoundRect(
        color = backgroundColor,
        topLeft = Offset(startX, startY + topCapHeight),
        size = Size(cylinderWidth, cylinderHeight),
        cornerRadius = CornerRadius(cylinderWidth * 0.06f)
    )

    // Dibujar el nivel de gas (lleno) from abajo hacia arriba
    if (fillPercentage > 0) {
        val fillHeight = cylinderHeight * fillPercentage
        val fillY = startY + topCapHeight + (cylinderHeight - fillHeight)

        drawRoundRect(
            color = fillColor,
            topLeft = Offset(startX, fillY),
            size = Size(cylinderWidth, fillHeight),
            cornerRadius = CornerRadius(cylinderWidth * 0.06f)
        )
    }

    // Dibujar la tapa superior
    drawRoundRect(
        color = Color.Gray,
        topLeft = Offset(startX, startY),
        size = Size(cylinderWidth, topCapHeight),
        cornerRadius = CornerRadius(topCapHeight * 0.3f)
    )

    // Draw upper valve (more realistic)
    val valveX = (size.width - valveWidth) / 2f
    drawRoundRect(
        color = Color.Gray,
        topLeft = Offset(valveX, startY - valveHeight * 0.7f),
        size = Size(valveWidth, valveHeight),
        cornerRadius = CornerRadius(valveWidth * 0.15f)
    )

    // Draw valve tip
    val nozzleWidth = valveWidth * 0.4f
    val nozzleHeight = valveHeight * 0.3f
    val nozzleX = (size.width - nozzleWidth) / 2f
    drawRoundRect(
        color = Color.Gray,
        topLeft = Offset(nozzleX, startY - valveHeight * 0.8f - nozzleHeight),
        size = Size(nozzleWidth, nozzleHeight),
        cornerRadius = CornerRadius(nozzleWidth * 0.2f)
    )
}

/**
 * Screen for real-time gas cylinder weight monitoring.
 *
 * Features:
 * - Visual gas cylinder representation with fill level
 * - Request weight data from BLE sensor
 * - Real-time fuel percentage and weight display
 * - Active cylinder status and information
 *
 * @param navController Navigation controller for screen navigation
 * @param viewModel ViewModel managing weight sensor data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(
    navController: NavController,
    viewModel: WeightViewModel = hiltViewModel()
) {
    val fuelState by viewModel.fuelState.collectAsState()
    val activeCylinder by viewModel.activeCylinder.collectAsState()
    val isRequestingData by viewModel.isRequestingData.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.weight_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.weight_back_description))
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                fuelState?.let { fuelMeasurement ->
                    // Main card with essential information
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.weight_fuel_monitor),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Gas cylinder visual centered
                            GasCylinderVisualizer(
                                fuelPercentage = fuelMeasurement.fuelPercentage
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Fuel information centered
                            Text(
                                text = fuelMeasurement.getFormattedFuelKilograms(),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = fuelMeasurement.getFormattedPercentage(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    fuelMeasurement.fuelPercentage > 50 -> MaterialTheme.colorScheme.primary
                                    fuelMeasurement.fuelPercentage > 20 -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.error
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Weight measured by sensor
                            Text(
                                text = stringResource(R.string.weight_measured_weight_format, fuelMeasurement.getFormattedTotalWeight()),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Measurement time centered
                            Text(
                                text = stringResource(R.string.weight_last_measurement_format, formatTimestamp(fuelMeasurement.timestamp)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Button to manually request data
                            Row(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { viewModel.requestWeightDataManually() },
                                    modifier = Modifier.weight(1f),
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
                                    Text(
                                        if (isRequestingData) stringResource(R.string.weight_requesting) else stringResource(
                                            R.string.weight_update_weight
                                        )
                                    )
                                }
                            }

                            if (!viewModel.isConnected()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.weight_sensor_not_connected_emoji),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else if (!viewModel.canMakeRequest()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.weight_wait_between_requests),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Current cylinder data centered
                            activeCylinder?.let { cylinder ->
                                Text(
                                    text = stringResource(R.string.weight_current_cylinder_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = stringResource(R.string.weight_cylinder_name_format, cylinder.name),
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Text(
                                    text = stringResource(R.string.weight_cylinder_capacity_format, cylinder.capacity),
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Text(
                                    text = stringResource(R.string.weight_cylinder_tare_format, cylinder.tare),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } ?: run {
                                Text(
                                    text = stringResource(R.string.weight_no_cylinder_configured_emoji),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } ?: run {
                    // Estado when no hay data
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.weight_waiting_sensor_data),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.weight_make_sure_connected),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Button to request data when no data is available
                        OutlinedButton(
                            onClick = { viewModel.requestWeightDataManually() },
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
                            Text(
                                if (isRequestingData) stringResource(R.string.weight_requesting) else stringResource(
                                    R.string.weight_request_data
                                )
                            )
                        }

                        if (!viewModel.isConnected()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.weight_connect_sensor_first),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else if (!viewModel.canMakeRequest()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.weight_wait_between_requests),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
