package com.example.campergas.ui.screens.bleconnect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

import androidx.navigation.NavController
import com.example.campergas.R
import com.example.campergas.domain.model.BleDevice
import com.example.campergas.ui.components.BluetoothDisabledDialog
import com.example.campergas.ui.theme.CamperGreenLight

/**
 * Screen for scanning and connecting to BLE gas cylinder sensors.
 *
 * Provides UI for:
 * - Scanning for nearby BLE devices
 * - Displaying available CamperGas sensors
 * - Connecting to selected devices
 * - Showing connection status
 * - Handling Bluetooth permissions and enable/disable states
 *
 * @param navController Navigation controller for screen navigation
 * @param viewModel ViewModel managing BLE connection state and operations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleConnectScreen(
    navController: NavController,
    viewModel: BleConnectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    // State to control Bluetooth dialog
    var showBluetoothDialog by remember { mutableStateOf(false) }

    // Verify permissions when entering the screen
    LaunchedEffect(Unit) {
        if (!viewModel.isBluetoothEnabled()) {
            showBluetoothDialog = true
        }
    }

    // Dialog to enable Bluetooth
    if (showBluetoothDialog) {
        BluetoothDisabledDialog(
            onAccept = {
            },
            onDismiss = {
            }
        )
    }

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
                text = stringResource(R.string.ble_connection_title),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        // Header with connection state
        ConnectionStatusCard(
            isConnected = connectionState,
            isScanning = uiState.isScanning,
            connectedDevice = uiState.connectedDevice,
            onDisconnect = { viewModel.disconnectDevice() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.startScan() },
                modifier = Modifier.weight(1f),
                enabled = !uiState.isScanning && !connectionState
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.ble_search_button))
            }

            OutlinedButton(
                onClick = { viewModel.stopScan() },
                modifier = Modifier.weight(1f),
                enabled = uiState.isScanning
            ) {
                Text(stringResource(R.string.ble_stop_button))
            }
        }

        // Filter toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.ble_only_compatible_devices),
                style = MaterialTheme.typography.bodyMedium
            )
            Switch(
                checked = uiState.showOnlyCompatibleDevices,
                onCheckedChange = { viewModel.toggleCompatibleDevicesFilter() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Loading indicator
        if (uiState.isScanning) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.ble_searching_devices),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Error display
        uiState.error?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ble_error_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { viewModel.clearError() }
                    ) {
                        Text(stringResource(R.string.ble_close_button))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Available devices section (only if not connected)
        if (!connectionState) {
            Text(
                text = stringResource(
                    R.string.ble_available_devices,
                    uiState.availableDevices.size
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (uiState.availableDevices.isEmpty() && !uiState.isScanning) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.ble_no_devices_found),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.ble_press_search_to_scan),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(uiState.availableDevices) { device ->
                        AvailableDeviceCard(
                            device = device,
                            isConnecting = uiState.isConnecting == device.address,
                            onConnect = { viewModel.connectToDevice(device) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AvailableDeviceCard(
    device: BleDevice,
    isConnecting: Boolean,
    onConnect: () -> Unit
) {
    val isCompatible = device.isCompatibleWithCamperGas
    val cardColors = if (isCompatible) {
        CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    } else {
        CardDefaults.elevatedCardColors()
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isCompatible) 6.dp else 2.dp
        ),
        colors = cardColors,
        onClick = if (!isConnecting && (isCompatible || device.isConnectable)) onConnect else {
            {}
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Compatibility badge
            if (isCompatible) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.ble_compatible_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = device.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Signal strength indicator
                    Icon(
                        imageVector = getSignalIcon(device.rssi),
                        contentDescription = stringResource(R.string.ble_signal_strength_format, device.signalStrength),
                        modifier = Modifier.size(20.dp),
                        tint = getSignalColor(device.rssi)
                    )

                    if (isConnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Device details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.ble_rssi_display_format, device.rssi),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = device.signalStrength,
                    style = MaterialTheme.typography.bodySmall,
                    color = getSignalColor(device.rssi),
                    fontWeight = FontWeight.Medium
                )
            }

            if (device.services.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.ble_services_format, device.services.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Compatibility information
            if (device.isCompatibleWithCamperGas) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = device.deviceType,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.ble_not_compatible_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun getSignalIcon(rssi: Int): ImageVector {
    return when {
        rssi >= -50 -> Icons.Default.Star
        rssi >= -70 -> Icons.Default.Info
        rssi >= -85 -> Icons.Default.Settings
        else -> Icons.Default.Warning
    }
}

@Composable
private fun getSignalColor(rssi: Int): Color {
    return when {
        rssi >= -50 -> MaterialTheme.colorScheme.secondary // Excellent signal - green
        rssi >= -70 -> CamperGreenLight // Good signal - light green
        rssi >= -85 -> MaterialTheme.colorScheme.tertiary // Fair signal - orange
        else -> MaterialTheme.colorScheme.error // Poor signal - red
    }
}

@Composable
fun ConnectionStatusCard(
    isConnected: Boolean,
    isScanning: Boolean,
    connectedDevice: BleDevice?,
    onDisconnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Settings,
                    contentDescription = null,
                    tint = if (isConnected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isConnected) stringResource(R.string.ble_sensor_connected) else stringResource(R.string.ble_bluetooth_connection),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            isConnected && connectedDevice != null -> stringResource(R.string.ble_connected_device_info, connectedDevice.name)
                            isScanning -> stringResource(R.string.ble_scanning_devices)
                            else -> stringResource(R.string.ble_search_devices)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isConnected) {
                    OutlinedButton(
                        onClick = onDisconnect,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.ble_disconnect_text))
                    }
                }
            }
        }
    }
}
