package com.example.campergas.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.campergas.domain.model.VehicleType
import com.example.campergas.ui.screens.inclination.WheelElevations
import kotlin.math.abs
import com.example.campergas.R
/**
 * Displays calculated wheel elevation adjustments for vehicle leveling.
 *
 * Shows how much each wheel needs to be raised or lowered to level the vehicle
 * based on inclination sensor data. Supports both caravan (3 points) and
 * motorhome (4 wheels) configurations.
 *
 * @param vehicleType Type of vehicle (CARAVAN or AUTOCARAVANA)
 * @param wheelElevations Calculated elevation adjustments for each wheel/support point
 * @param modifier Modifier for layout customization
 */
@Composable
fun WheelElevationsDisplay(
    vehicleType: VehicleType,
    wheelElevations: WheelElevations,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.wheel_elevation_icon_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            when (vehicleType) {
                VehicleType.CARAVAN -> CaravanWheelElevations(wheelElevations)
                VehicleType.AUTOCARAVANA -> MotorHomeWheelElevations(wheelElevations)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.wheel_elevation_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CaravanWheelElevations(wheelElevations: WheelElevations) {
    Column {
        // Front support wheel
        WheelElevationItem(
            wheelName = "🛞 Front Support",
            elevation = wheelElevations.frontSupport,
            icon = "⬆️"
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Rear wheels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WheelElevationItem(
                wheelName = "🛞 Rear Left",
                elevation = wheelElevations.rearLeft,
                icon = "⬅️",
                modifier = Modifier.weight(1f)
            )

            WheelElevationItem(
                wheelName = "🛞 Rear Right",
                elevation = wheelElevations.rearRight,
                icon = "➡️",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MotorHomeWheelElevations(wheelElevations: WheelElevations) {
    Column {
        // Front wheels
        Text(
            text = stringResource(R.string.wheel_front_wheels),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WheelElevationItem(
                wheelName = "🛞 Front Left",
                elevation = wheelElevations.frontLeft,
                icon = "⬅️",
                modifier = Modifier.weight(1f)
            )

            WheelElevationItem(
                wheelName = "🛞 Front Right",
                elevation = wheelElevations.frontRight,
                icon = "➡️",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Rear wheels
        Text(
            text = stringResource(R.string.wheel_rear_wheels),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WheelElevationItem(
                wheelName = "🛞 Rear Left",
                elevation = wheelElevations.rearLeft,
                icon = "⬅️",
                modifier = Modifier.weight(1f)
            )

            WheelElevationItem(
                wheelName = "🛞 Rear Right",
                elevation = wheelElevations.rearRight,
                icon = "➡️",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WheelElevationItem(
    wheelName: String,
    elevation: Float,
    icon: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = when {
                abs(elevation) < 0.5f -> MaterialTheme.colorScheme.surfaceContainer
                elevation > 0 -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.errorContainer
            }
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = wheelName,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = stringResource(R.string.wheel_elevation_format, elevation),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = when {
                    abs(elevation) < 0.5f -> MaterialTheme.colorScheme.onSurfaceVariant
                    elevation > 0 -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onErrorContainer
                }
            )

            Text(
                text = when {
                    abs(elevation) < 0.5f -> "OK"
                    elevation > 0 -> "Raise"
                    else -> "Lower"
                },
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    abs(elevation) < 0.5f -> MaterialTheme.colorScheme.onSurfaceVariant
                    elevation > 0 -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onErrorContainer
                }
            )
        }
    }
}
