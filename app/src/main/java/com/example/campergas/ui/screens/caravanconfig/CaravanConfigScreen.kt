package com.example.campergas.ui.screens.caravanconfig

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

import androidx.navigation.NavController
import com.example.campergas.R
import com.example.campergas.domain.model.VehicleType

/**
 * Screen for configuring vehicle physical dimensions.
 *
 * Allows users to configure:
 * - Vehicle type (Caravan or Motorhome)
 * - Distance between rear wheels (lateral width)
 * - Distance to front support point (caravan) or wheelbase (motorhome)
 *
 * Configuration is used for accurate stability calculations and
 * wheel elevation recommendations.
 *
 * @param navController Navigation controller for screen navigation
 * @param viewModel ViewModel managing vehicle configuration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaravanConfigScreen(
    navController: NavController,
    viewModel: CaravanConfigViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // Back button and title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.ble_back_button)
                )
            }
            Text(
                text = stringResource(R.string.caravan_config_title),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Vehicle type
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .selectableGroup()
            ) {
                Text(
                    text = stringResource(R.string.caravan_config_vehicle_type),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                VehicleType.entries.forEach { vehicleType ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (uiState.selectedVehicleType == vehicleType),
                                onClick = { viewModel.updateVehicleType(vehicleType) }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (uiState.selectedVehicleType == vehicleType),
                            onClick = { viewModel.updateVehicleType(vehicleType) }
                        )
                        Text(
                            text = when (vehicleType) {
                                VehicleType.CARAVAN -> stringResource(R.string.vehicle_type_caravan)
                                VehicleType.AUTOCARAVANA -> stringResource(R.string.vehicle_type_motorhome)
                            },
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }

        // Dimensiones
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.caravan_config_dimensions),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Distance between rear wheels (common for both types)
                OutlinedTextField(
                    value = if (uiState.distanceBetweenWheels == 0f) "" else uiState.distanceBetweenWheels.toString(),
                    onValueChange = { value ->
                        value.toFloatOrNull()?.let { viewModel.updateDistanceBetweenWheels(it) }
                    },
                    label = { Text(stringResource(R.string.caravan_config_distance_between_rear_wheels_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                // Specific field according to vehicle type
                when (uiState.selectedVehicleType) {
                    VehicleType.CARAVAN -> {
                        OutlinedTextField(
                            value = if (uiState.distanceToFrontSupport == 0f) "" else uiState.distanceToFrontSupport.toString(),
                            onValueChange = { value ->
                                value.toFloatOrNull()
                                    ?.let { viewModel.updateDistanceToFrontSupport(it) }
                            },
                            label = { Text(stringResource(R.string.caravan_config_distance_to_front_support_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    VehicleType.AUTOCARAVANA -> {
                        OutlinedTextField(
                            value = if (uiState.distanceToFrontSupport == 0f) "" else uiState.distanceToFrontSupport.toString(),
                            onValueChange = { value ->
                                value.toFloatOrNull()
                                    ?.let { viewModel.updateDistanceToFrontSupport(it) }
                            },
                            label = { Text(stringResource(R.string.caravan_config_distance_between_front_wheels_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Indicador de carga
        if (uiState.isLoading || uiState.isSaving) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Mensaje de error
        uiState.error?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.weight(1f),
                enabled = !uiState.isSaving
            ) {
                Text(stringResource(R.string.caravan_config_cancel))
            }

            Button(
                onClick = { viewModel.saveConfiguration() },
                modifier = Modifier.weight(1f),
                enabled = !uiState.isSaving && !uiState.isLoading
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.caravan_config_save))
                }
            }
        }
    }
}
