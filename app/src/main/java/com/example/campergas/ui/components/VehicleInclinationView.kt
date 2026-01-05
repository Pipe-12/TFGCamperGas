package com.example.campergas.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.campergas.domain.model.VehicleType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import com.example.campergas.R

/**
 * Vehicle inclination visualization component.
 *
 * Displays vehicle tilt/pitch and roll angles using graphical representations
 * from side and rear perspectives. Supports both compact mode (side view only)
 * and full mode (both views).
 *
 * @param vehicleType Type of vehicle (CARAVAN or AUTOCARAVANA) to render
 * @param pitchAngle Pitch angle in degrees (forward/backward tilt)
 * @param rollAngle Roll angle in degrees (left/right tilt)
 * @param modifier Modifier for layout customization
 * @param compact If true, shows only side view; if false, shows both views
 */
@Composable
fun VehicleInclinationView(
    vehicleType: VehicleType,
    pitchAngle: Float,
    rollAngle: Float,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    if (compact) {
        // Compact mode: only show pitch (side view) for space-constrained displays
        VehicleInclinationSideView(
            vehicleType = vehicleType,
            pitchAngle = pitchAngle,
            modifier = modifier,
            compact = true
        )
    } else {
        // Full mode: show both pitch and roll views
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Side view - Vertical Leveling (Pitch)
            VehicleInclinationSideView(
                vehicleType = vehicleType,
                pitchAngle = pitchAngle,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Rear view - Horizontal Leveling (Roll)
            VehicleInclinationRearView(
                vehicleType = vehicleType,
                rollAngle = rollAngle,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Side view of vehicle inclination showing pitch (forward/backward tilt).
 *
 * Renders a 2D side profile of the vehicle with rotation based on pitch angle,
 * including ground reference line and angle indicator.
 *
 * @param vehicleType Type of vehicle to render
 * @param pitchAngle Pitch angle in degrees
 * @param modifier Modifier for layout customization
 * @param compact If true, uses compact layout without card wrapper
 */
@Composable
fun VehicleInclinationSideView(
    vehicleType: VehicleType,
    pitchAngle: Float,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (compact) stringResource(R.string.vehicle_inclination_compact) else stringResource(R.string.vehicle_inclination_side_view),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = if (compact) 4.dp else 8.dp)
            )

            Text(
                text = stringResource(R.string.vehicle_inclination_angle_format, pitchAngle),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (kotlin.math.abs(pitchAngle) <= 2f)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = if (compact) 16.dp else 0.dp) // Extra spacing in compact mode
            )

            Canvas(
                modifier = Modifier
                    .size(
                        width = if (compact) 280.dp else 320.dp,
                        height = if (compact) 100.dp else 140.dp
                    )
                    .padding(8.dp)
            ) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                val groundLevel = centerY + if (compact) 20f else 25f // Adjusted for compact mode

                // Draw horizontal reference line (ground level) with graduation
                drawLine(
                    color = Color.Gray.copy(alpha = 0.7f),
                    start = Offset(if (compact) 15f else 20f, groundLevel),
                    end = Offset(size.width - if (compact) 15f else 20f, groundLevel),
                    strokeWidth = 2.dp.toPx()
                )
                // Apply pitch rotation only, centered on ground level
                translate(centerX, groundLevel) {
                    rotate(pitchAngle, pivot = Offset.Zero) {
                        when (vehicleType) {
                            VehicleType.CARAVAN -> drawCaravanSideView(
                                primaryColor,
                                secondaryColor,
                                compact
                            )

                            VehicleType.AUTOCARAVANA -> drawMotorHomeSideView(
                                primaryColor,
                                secondaryColor,
                                compact
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VehicleInclinationRearView(
    vehicleType: VehicleType,
    rollAngle: Float,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.vehicle_inclination_rear_view),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = stringResource(R.string.vehicle_inclination_angle_format, rollAngle),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (kotlin.math.abs(rollAngle) <= 2f)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )

            Canvas(
                modifier = Modifier
                    .size(320.dp, 140.dp)
                    .padding(8.dp)
            ) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                val groundLevel = centerY + 25f // Ground level lower so wheels are on the line

                // Draw horizontal reference line (ground level) with graduation
                drawLine(
                    color = Color.Gray.copy(alpha = 0.7f),
                    start = Offset(20f, groundLevel),
                    end = Offset(size.width - 20f, groundLevel),
                    strokeWidth = 2.dp.toPx()
                )
                // Apply roll rotation only, centered on ground level
                translate(centerX, groundLevel) {
                    rotate(rollAngle, pivot = Offset.Zero) {
                        when (vehicleType) {
                            VehicleType.CARAVAN -> drawCaravanRearView(primaryColor)
                            VehicleType.AUTOCARAVANA -> drawMotorHomeRearView(
                                primaryColor,
                                secondaryColor
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawCaravanSideView(
    primaryColor: Color,
    secondaryColor: Color,
    compact: Boolean = false
) {
    val width = if (compact) 180f else 240f
    val height = if (compact) 75f else 100f
    val wheelRadius = if (compact) 12f else 16f
    val strokeWidth = if (compact) 1.5.dp.toPx() else 2.dp.toPx()
    val bodyOffsetY = -height - wheelRadius + if (compact) 6f else 8f

    // Vehicle shadow
    drawRoundRect(
        color = Color.Black.copy(alpha = if (compact) 0.15f else 0.2f),
        topLeft = Offset(-width / 2 + if (compact) 2 else 3, bodyOffsetY + if (compact) 2 else 3),
        size = Size(width, height),
        cornerRadius = CornerRadius(if (compact) 8f else 12f, if (compact) 8f else 12f)
    )

    // Main caravan body with rounded corners
    drawRoundRect(
        color = primaryColor,
        topLeft = Offset(-width / 2, bodyOffsetY),
        size = Size(width, height),
        cornerRadius = CornerRadius(if (compact) 8f else 12f, if (compact) 8f else 12f)
    )

    // Body outline
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.8f),
        topLeft = Offset(-width / 2, bodyOffsetY),
        size = Size(width, height),
        cornerRadius = CornerRadius(if (compact) 8f else 12f, if (compact) 8f else 12f),
        style = Stroke(width = strokeWidth)
    )

    // Side windows with frames (larger)
    val windowHeight = 35f
    val windowWidth = 50f

    // Front window
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(-width / 2 + 25, bodyOffsetY + 15),
        size = Size(windowWidth, windowHeight),
        cornerRadius = CornerRadius(6f, 6f)
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.6f),
        topLeft = Offset(-width / 2 + 25, bodyOffsetY + 15),
        size = Size(windowWidth, windowHeight),
        cornerRadius = CornerRadius(6f, 6f),
        style = Stroke(width = 1.5.dp.toPx())
    )

    // Middle window
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(-25f, bodyOffsetY + 15),
        size = Size(windowWidth, windowHeight),
        cornerRadius = CornerRadius(6f, 6f)
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.6f),
        topLeft = Offset(-25f, bodyOffsetY + 15),
        size = Size(windowWidth, windowHeight),
        cornerRadius = CornerRadius(6f, 6f),
        style = Stroke(width = 1.5.dp.toPx())
    )

    // Rear window
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(width / 2 - 75, bodyOffsetY + 15),
        size = Size(windowWidth, windowHeight),
        cornerRadius = CornerRadius(6f, 6f)
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.6f),
        topLeft = Offset(width / 2 - 75, bodyOffsetY + 15),
        size = Size(windowWidth, windowHeight),
        cornerRadius = CornerRadius(6f, 6f),
        style = Stroke(width = 1.5.dp.toPx())
    )

    // Side door (larger)
    drawRoundRect(
        color = primaryColor.copy(alpha = 0.8f),
        topLeft = Offset(30f, bodyOffsetY + 10),
        size = Size(50f, height - 30f),
        cornerRadius = CornerRadius(6f, 6f),
        style = Stroke(width = 2.dp.toPx())
    )

    // Manija of the puerta
    drawCircle(
        color = Color.Black.copy(alpha = 0.8f),
        radius = 3f,
        center = Offset(70f, bodyOffsetY + height / 2)
    )

    // Detailed wheels with rims - Positioned to touch reference line
    val wheelCenter = Offset(0f, 0f) // Height 0 to be on reference line

    // Sombra of the rueda
    drawCircle(
        color = Color.Black.copy(alpha = 0.3f),
        radius = wheelRadius + 2f,
        center = wheelCenter.copy(x = wheelCenter.x + 2f)
    )

    // Llanta exterior
    drawCircle(
        color = Color.Gray,
        radius = wheelRadius,
        center = wheelCenter
    )

    // Tire
    drawCircle(
        color = Color.Black,
        radius = wheelRadius - 2f,
        center = wheelCenter
    )

    // Llanta interior
    drawCircle(
        color = Color.LightGray,
        radius = wheelRadius - 6f,
        center = wheelCenter
    )

    // Rayos of the llanta
    for (i in 0..4) {
        val angle = (i * 72 * PI / 180).toFloat()
        val startRadius = 4f
        val endRadius = wheelRadius - 7f
        val startX = wheelCenter.x + startRadius * cos(angle)
        val startY = wheelCenter.y + startRadius * sin(angle)
        val endX = wheelCenter.x + endRadius * cos(angle)
        val endY = wheelCenter.y + endRadius * sin(angle)

        drawLine(
            color = Color.Gray,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 2.dp.toPx()
        )
    }

    // Front hitch/coupling
    val hitchWidth = 40f
    val hitchHeight = 18f

    drawRoundRect(
        color = secondaryColor,
        topLeft = Offset(-width / 2 - hitchWidth, -hitchHeight * 2),
        size = Size(hitchWidth, hitchHeight),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Hitch connection
    drawCircle(
        color = Color.Gray,
        radius = 8f,
        center = Offset(-width / 2 - hitchWidth + 5f, 0f)
    )

    // Front support wheel with stand
    val jockeyWheelCenter = Offset(-width / 2 - hitchWidth - 8f, 0f)

    // Support wheel stand
    drawLine(
        color = Color.Gray,
        start = Offset(-width / 2 - 15f, bodyOffsetY + height / 1.5f - 15f),
        end = jockeyWheelCenter,
        strokeWidth = 4.dp.toPx()
    )

    // Support wheel
    drawCircle(
        color = Color.Black,
        radius = 10f,
        center = jockeyWheelCenter
    )
    drawCircle(
        color = Color.Gray,
        radius = 8f,
        center = jockeyWheelCenter
    )

    // Detalles adicionales
    // Roof sefortion line
    drawLine(
        color = Color.Black.copy(alpha = 0.3f),
        start = Offset(-width / 2 + 15, bodyOffsetY),
        end = Offset(width / 2 - 15, bodyOffsetY),
        strokeWidth = 1.5.dp.toPx()
    )

    // Roof ventilation (larger)
    drawRoundRect(
        color = Color.Gray,
        topLeft = Offset(-20f, bodyOffsetY - 5f),
        size = Size(40f, 10f),
        cornerRadius = CornerRadius(5f, 5f)
    )

    // Franja decorativa lateral
    drawLine(
        color = secondaryColor.copy(alpha = 0.6f),
        start = Offset(-width / 2 + 20, bodyOffsetY + height / 2),
        end = Offset(width / 2 - 20, bodyOffsetY + height / 2),
        strokeWidth = 3.dp.toPx()
    )
}

private fun DrawScope.drawMotorHomeSideView(
    primaryColor: Color,
    secondaryColor: Color,
    compact: Boolean = false
) {
    val width = if (compact) 200f else 260f
    val height = if (compact) 80f else 110f
    val wheelRadius = if (compact) 12f else 16f
    val strokeWidth = if (compact) 1.5.dp.toPx() else 2.dp.toPx()
    val bodyOffsetY = -height - wheelRadius + if (compact) 6f else 8f

    // Vehicle shadow
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.2f),
        topLeft = Offset(-width / 2 + 3, bodyOffsetY + 3),
        size = Size(width, height),
        cornerRadius = CornerRadius(12f, 12f)
    )

    // Main body (cabin)
    drawRoundRect(
        color = primaryColor,
        topLeft = Offset(-width / 2, bodyOffsetY),
        size = Size(width - 70f, height),
        cornerRadius = CornerRadius(12f, 12f)
    )

    // Body outline principal
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.8f),
        topLeft = Offset(-width / 2, bodyOffsetY),
        size = Size(width - 70f, height),
        cornerRadius = CornerRadius(12f, 12f),
        style = Stroke(width = strokeWidth)
    )

    // Driver cabin with more realistic shape
    val cabinPath = Path().apply {
        moveTo(width / 2 - 70f, bodyOffsetY)
        lineTo(width / 2, bodyOffsetY + 20f)
        lineTo(width / 2, bodyOffsetY + height)
        lineTo(width / 2 - 70f, bodyOffsetY + height)
        close()
    }

    drawPath(
        path = cabinPath,
        color = secondaryColor
    )

    drawPath(
        path = cabinPath,
        color = Color.Black.copy(alpha = 0.8f),
        style = Stroke(width = strokeWidth)
    )

    // Cabin side windows (larger)
    val windowHeight = 40f
    val windowWidth = 55f

    // Main cabin window
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(-width / 2 + 25, bodyOffsetY + 20),
        size = Size(windowWidth, windowHeight),
        cornerRadius = CornerRadius(8f, 8f)
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.6f),
        topLeft = Offset(-width / 2 + 25, bodyOffsetY + 20),
        size = Size(windowWidth, windowHeight),
        cornerRadius = CornerRadius(8f, 8f),
        style = Stroke(width = 1.5.dp.toPx())
    )

    // Middle window
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(-15f, bodyOffsetY + 20),
        size = Size(windowWidth, windowHeight),
        cornerRadius = CornerRadius(8f, 8f)
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.6f),
        topLeft = Offset(-15f, bodyOffsetY + 20),
        size = Size(windowWidth, windowHeight),
        cornerRadius = CornerRadius(8f, 8f),
        style = Stroke(width = 1.5.dp.toPx())
    )

    // Cabin windshield (larger trapezoidal shape)
    val windshieldPath = Path().apply {
        moveTo(width / 2 - 60f, bodyOffsetY + 20f)
        lineTo(width / 2 - 15f, bodyOffsetY + 25f)
        lineTo(width / 2 - 15f, bodyOffsetY + height - 35f)
        lineTo(width / 2 - 60f, bodyOffsetY + height - 30f)
        close()
    }

    drawPath(
        path = windshieldPath,
        color = Color.White.copy(alpha = 0.9f)
    )

    drawPath(
        path = windshieldPath,
        color = Color.Black.copy(alpha = 0.6f),
        style = Stroke(width = 1.5.dp.toPx())
    )

    // Ventana lateral of the cabina
    drawRoundRect(
        color = Color.White.copy(alpha = 0.9f),
        topLeft = Offset(width / 2 - 55f, bodyOffsetY + height - 35f),
        size = Size(35f, 25f),
        cornerRadius = CornerRadius(6f, 6f)
    )

    // Cabin side door (larger)
    drawRoundRect(
        color = primaryColor.copy(alpha = 0.8f),
        topLeft = Offset(-width / 2 + 85f, bodyOffsetY + 15),
        size = Size(55f, height - 35f),
        cornerRadius = CornerRadius(8f, 8f),
        style = Stroke(width = 2.dp.toPx())
    )

    // Manija of the puerta
    drawCircle(
        color = Color.Black.copy(alpha = 0.8f),
        radius = 4f,
        center = Offset(-width / 2 + 125f, bodyOffsetY + height / 2)
    )

    // Detailed wheels - Positioned to touch reference line
    val rearWheelCenter = Offset(-width / 2 + 50f, 0f) // Height 0 to be on reference line
    val frontWheelCenter = Offset(width / 2 - 45f, 0f)

    // Rueda trasera
    drawDetailedWheel(rearWheelCenter, wheelRadius)
    // Rueda delantera
    drawDetailedWheel(frontWheelCenter, wheelRadius)

    // Detalles adicionales
    // Decorative side stripe (more visible)
    drawLine(
        color = secondaryColor.copy(alpha = 0.7f),
        start = Offset(-width / 2 + 15, bodyOffsetY + height / 2),
        end = Offset(width / 2 - 75f, bodyOffsetY + height / 2),
        strokeWidth = 5.dp.toPx()
    )

    // Roof ventilation (larger)
    drawRoundRect(
        color = Color.Gray,
        topLeft = Offset(-35f, bodyOffsetY - 6f),
        size = Size(70f, 12f),
        cornerRadius = CornerRadius(6f, 6f)
    )

    // Front headlights (larger)
    drawCircle(
        color = Color.Yellow.copy(alpha = 0.8f),
        radius = 8f,
        center = Offset(width / 2 - 8f, bodyOffsetY + height - 20f)
    )
    drawCircle(
        color = Color.White,
        radius = 6f,
        center = Offset(width / 2 - 8f, bodyOffsetY + height - 35f)
    )

    // Parrilla delantera
    drawRect(
        color = Color.Gray,
        topLeft = Offset(width / 2 - 5f, bodyOffsetY + height - 50f),
        size = Size(8f, 30f)
    )
}

private fun DrawScope.drawDetailedWheel(center: Offset, radius: Float) {
    // Sombra of the rueda
    drawCircle(
        color = Color.Black.copy(alpha = 0.3f),
        radius = radius + 2f,
        center = center.copy(x = center.x + 2f)
    )

    // Llanta exterior
    drawCircle(
        color = Color.Gray,
        radius = radius,
        center = center
    )

    // Tire
    drawCircle(
        color = Color.Black,
        radius = radius - 2f,
        center = center
    )

    // Llanta interior
    drawCircle(
        color = Color.LightGray,
        radius = radius - 5f,
        center = center
    )

    // Rayos of the llanta
    for (i in 0..4) {
        val angle = (i * 72 * PI / 180).toFloat()
        val startRadius = 3f
        val endRadius = radius - 6f
        val startX = center.x + startRadius * cos(angle)
        val startY = center.y + startRadius * sin(angle)
        val endX = center.x + endRadius * cos(angle)
        val endY = center.y + endRadius * sin(angle)

        drawLine(
            color = Color.Gray,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 1.5.dp.toPx()
        )
    }
}

private fun DrawScope.drawCaravanRearView(primaryColor: Color) {
    val width = 180f // Increased for more visible view
    val height = 110f // Aumentado
    val wheelRadius = 16f // Aumentado
    val strokeWidth = 2.dp.toPx()
    val bodyOffsetY = -height - wheelRadius + 8f // Adjusted for better position

    // Vehicle shadow
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.2f),
        topLeft = Offset(-width / 2 + 3, bodyOffsetY + 3),
        size = Size(width, height),
        cornerRadius = CornerRadius(12f, 12f)
    )

    // Cuerpo principal of the caravana (vista trasera)
    drawRoundRect(
        color = primaryColor,
        topLeft = Offset(-width / 2, bodyOffsetY),
        size = Size(width, height),
        cornerRadius = CornerRadius(12f, 12f)
    )

    // Body outline
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.8f),
        topLeft = Offset(-width / 2, bodyOffsetY),
        size = Size(width, height),
        cornerRadius = CornerRadius(12f, 12f),
        style = Stroke(width = strokeWidth)
    )

    // Rear window with frame (larger)
    val windowWidth = 100f
    val windowHeight = 45f
    drawRoundRect(
        color = Color.White.copy(alpha = 0.9f),
        topLeft = Offset(-windowWidth / 2, bodyOffsetY + 20),
        size = Size(windowWidth, windowHeight),
        cornerRadius = CornerRadius(8f, 8f)
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.6f),
        topLeft = Offset(-windowWidth / 2, bodyOffsetY + 20),
        size = Size(windowWidth, windowHeight),
        cornerRadius = CornerRadius(8f, 8f),
        style = Stroke(width = 1.5.dp.toPx())
    )

    // Puerta trasera con divisiones
    drawLine(
        color = Color.Black.copy(alpha = 0.4f),
        start = Offset(0f, bodyOffsetY + 12),
        end = Offset(0f, bodyOffsetY + height - 12),
        strokeWidth = 3.dp.toPx()
    )

    // Rear door handles (larger)
    drawCircle(
        color = Color.Black.copy(alpha = 0.8f),
        radius = 4f,
        center = Offset(-20f, bodyOffsetY + height / 2 + 8f)
    )
    drawCircle(
        color = Color.Black.copy(alpha = 0.8f),
        radius = 4f,
        center = Offset(20f, bodyOffsetY + height / 2 + 8f)
    )

    // Rectangular rear wheels - Positioned to touch reference line
    val leftWheelCenter = Offset(-width / 2 + 35, 0f) // Height 0 to be on reference line
    val rightWheelCenter = Offset(width / 2 - 35, 0f)

    drawRearViewWheel(leftWheelCenter)
    drawRearViewWheel(rightWheelCenter)

    // Rear lights (larger and more visible)
    val lightRadius = 7f

    // Brake/position lights
    drawCircle(
        color = Color.Red,
        radius = lightRadius,
        center = Offset(-width / 2 + 20, bodyOffsetY + height - 25f)
    )
    drawCircle(
        color = Color.Red,
        radius = lightRadius,
        center = Offset(width / 2 - 20, bodyOffsetY + height - 25f)
    )

    // Reverse lights
    drawCircle(
        color = Color.White,
        radius = lightRadius - 1f,
        center = Offset(-width / 2 + 20, bodyOffsetY + height - 8f)
    )
    drawCircle(
        color = Color.White,
        radius = lightRadius - 1f,
        center = Offset(width / 2 - 20, bodyOffsetY + height - 8f)
    )

    // Turn signals
    drawCircle(
        color = Color.Yellow,
        radius = lightRadius - 1f,
        center = Offset(-width / 2 + 45, bodyOffsetY + height - 25f)
    )
    drawCircle(
        color = Color.Yellow,
        radius = lightRadius - 1f,
        center = Offset(width / 2 - 45, bodyOffsetY + height - 25f)
    )

    // Contornos of thes luces
    for (lightCenter in listOf(
        Offset(-width / 2 + 20, bodyOffsetY + height - 25f),
        Offset(width / 2 - 20, bodyOffsetY + height - 25f),
        Offset(-width / 2 + 20, bodyOffsetY + height - 8f),
        Offset(width / 2 - 20, bodyOffsetY + height - 8f),
        Offset(-width / 2 + 45, bodyOffsetY + height - 25f),
        Offset(width / 2 - 45, bodyOffsetY + height - 25f)
    )) {
        drawCircle(
            color = Color.Black.copy(alpha = 0.6f),
            radius = lightRadius,
            center = lightCenter,
            style = Stroke(width = 1.5.dp.toPx())
        )
    }

    // License plate (larger)
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(-35f, bodyOffsetY + height - 2f),
        size = Size(70f, 16f),
        cornerRadius = CornerRadius(3f, 3f)
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.8f),
        topLeft = Offset(-35f, bodyOffsetY + height - 2f),
        size = Size(70f, 16f),
        cornerRadius = CornerRadius(3f, 3f),
        style = Stroke(width = 1.dp.toPx())
    )

    // Estabilizadores laterales
    drawLine(
        color = Color.Gray,
        start = Offset(-width / 2, bodyOffsetY + height - 12f),
        end = Offset(-width / 2 - 15f, -wheelRadius + 5f),
        strokeWidth = 4.dp.toPx()
    )
    drawLine(
        color = Color.Gray,
        start = Offset(width / 2, bodyOffsetY + height - 12f),
        end = Offset(width / 2 + 15f, -wheelRadius + 5f),
        strokeWidth = 4.dp.toPx()
    )

    // Reflectores laterales
    drawRect(
        color = Color.Red.copy(alpha = 0.7f),
        topLeft = Offset(-width / 2 - 2f, bodyOffsetY + height / 2),
        size = Size(4f, 15f)
    )
    drawRect(
        color = Color.Red.copy(alpha = 0.7f),
        topLeft = Offset(width / 2 - 2f, bodyOffsetY + height / 2),
        size = Size(4f, 15f)
    )
}

private fun DrawScope.drawMotorHomeRearView(primaryColor: Color, secondaryColor: Color) {
    val width = 200f // Increased for more visible view
    val height = 120f // Aumentado
    val wheelRadius = 16f // Aumentado
    val strokeWidth = 2.dp.toPx()
    val bodyOffsetY = -height - wheelRadius + 8f // Adjusted for better position

    // Vehicle shadow
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.2f),
        topLeft = Offset(-width / 2 + 3, bodyOffsetY + 3),
        size = Size(width, height),
        cornerRadius = CornerRadius(12f, 12f)
    )

    // Cuerpo principal
    drawRoundRect(
        color = primaryColor,
        topLeft = Offset(-width / 2, bodyOffsetY),
        size = Size(width, height),
        cornerRadius = CornerRadius(12f, 12f)
    )

    // Body outline
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.8f),
        topLeft = Offset(-width / 2, bodyOffsetY),
        size = Size(width, height),
        cornerRadius = CornerRadius(12f, 12f),
        style = Stroke(width = strokeWidth)
    )

    // Rear window grande con marco
    val windowWidth = 120f
    val windowHeight = 50f
    drawRoundRect(
        color = Color.White.copy(alpha = 0.9f),
        topLeft = Offset(-windowWidth / 2, bodyOffsetY + 25),
        size = Size(windowWidth, windowHeight),
        cornerRadius = CornerRadius(10f, 10f)
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.6f),
        topLeft = Offset(-windowWidth / 2, bodyOffsetY + 25),
        size = Size(windowWidth, windowHeight),
        cornerRadius = CornerRadius(10f, 10f),
        style = Stroke(width = 2.dp.toPx())
    )

    // Puerta trasera con bisagra central
    drawLine(
        color = secondaryColor,
        start = Offset(0f, bodyOffsetY + 12),
        end = Offset(0f, bodyOffsetY + height - 12),
        strokeWidth = 5.dp.toPx()
    )

    // Paneles of the puerta trasera
    drawRoundRect(
        color = primaryColor.copy(alpha = 0.8f),
        topLeft = Offset(-width / 2 + 15, bodyOffsetY + height - 40f),
        size = Size(width / 2 - 20f, 35f),
        cornerRadius = CornerRadius(6f, 6f),
        style = Stroke(width = 2.dp.toPx())
    )
    drawRoundRect(
        color = primaryColor.copy(alpha = 0.8f),
        topLeft = Offset(5f, bodyOffsetY + height - 40f),
        size = Size(width / 2 - 20f, 35f),
        cornerRadius = CornerRadius(6f, 6f),
        style = Stroke(width = 2.dp.toPx())
    )

    // Rear door handles (larger)
    drawCircle(
        color = Color.Black.copy(alpha = 0.8f),
        radius = 5f,
        center = Offset(-25f, bodyOffsetY + height - 22f)
    )
    drawCircle(
        color = Color.Black.copy(alpha = 0.8f),
        radius = 5f,
        center = Offset(25f, bodyOffsetY + height - 22f)
    )

    // Rear step
    drawRoundRect(
        color = Color.Gray,
        topLeft = Offset(-width / 2 + 10, bodyOffsetY + height - 8f),
        size = Size(width - 20f, 12f),
        cornerRadius = CornerRadius(3f, 3f)
    )

    // Rectangular rear wheels - Positioned to touch reference line
    val leftWheelCenter = Offset(-width / 2 + 35, 0f) // Height 0 to be on reference line
    val rightWheelCenter = Offset(width / 2 - 35, 0f)

    drawRearViewWheel(leftWheelCenter)
    drawRearViewWheel(rightWheelCenter)

    // More complete and larger rear light system
    val lightRadius = 8f

    // Luces de freno principales
    drawCircle(
        color = Color.Red,
        radius = lightRadius,
        center = Offset(-width / 2 + 25, bodyOffsetY + height - 30f)
    )
    drawCircle(
        color = Color.Red,
        radius = lightRadius,
        center = Offset(width / 2 - 25, bodyOffsetY + height - 30f)
    )

    // Reverse lights
    drawCircle(
        color = Color.White,
        radius = lightRadius - 1f,
        center = Offset(-width / 2 + 25, bodyOffsetY + height - 12f)
    )
    drawCircle(
        color = Color.White,
        radius = lightRadius - 1f,
        center = Offset(width / 2 - 25, bodyOffsetY + height - 12f)
    )

    // Turn signals
    drawCircle(
        color = Color.Yellow,
        radius = lightRadius - 1f,
        center = Offset(-width / 2 + 50, bodyOffsetY + height - 30f)
    )
    drawCircle(
        color = Color.Yellow,
        radius = lightRadius - 1f,
        center = Offset(width / 2 - 50, bodyOffsetY + height - 30f)
    )

    // Position lights
    drawCircle(
        color = Color.Red.copy(alpha = 0.7f),
        radius = lightRadius - 2f,
        center = Offset(-width / 2 + 50, bodyOffsetY + height - 15f)
    )
    drawCircle(
        color = Color.Red.copy(alpha = 0.7f),
        radius = lightRadius - 2f,
        center = Offset(width / 2 - 50, bodyOffsetY + height - 15f)
    )

    // Contornos de todas las luces
    val allLights = listOf(
        Offset(-width / 2 + 25, bodyOffsetY + height - 30f),
        Offset(width / 2 - 25, bodyOffsetY + height - 30f),
        Offset(-width / 2 + 25, bodyOffsetY + height - 12f),
        Offset(width / 2 - 25, bodyOffsetY + height - 12f),
        Offset(-width / 2 + 50, bodyOffsetY + height - 30f),
        Offset(width / 2 - 50, bodyOffsetY + height - 30f),
        Offset(-width / 2 + 50, bodyOffsetY + height - 15f),
        Offset(width / 2 - 50, bodyOffsetY + height - 15f)
    )

    for (lightCenter in allLights) {
        drawCircle(
            color = Color.Black.copy(alpha = 0.6f),
            radius = lightRadius,
            center = lightCenter,
            style = Stroke(width = 1.5.dp.toPx())
        )
    }

    // License plate (larger)
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(-40f, bodyOffsetY + height + 2f),
        size = Size(80f, 18f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.8f),
        topLeft = Offset(-40f, bodyOffsetY + height + 2f),
        size = Size(80f, 18f),
        cornerRadius = CornerRadius(4f, 4f),
        style = Stroke(width = 1.5.dp.toPx())
    )

    // Side exhaust (more visible)
    drawCircle(
        color = Color.Gray,
        radius = 9f,
        center = Offset(width / 2 - 60f, -wheelRadius + 15f)
    )
    drawCircle(
        color = Color.Black,
        radius = 7f,
        center = Offset(width / 2 - 60f, -wheelRadius + 15f)
    )

    // Side ladder (larger)
    for (i in 0..2) {
        val stepY = bodyOffsetY + height / 2 + (i * 15f)
        drawLine(
            color = Color.Gray,
            start = Offset(width / 2 - 12f, stepY),
            end = Offset(width / 2 + 5f, stepY),
            strokeWidth = 3.dp.toPx()
        )
    }

    // Ladder support
    drawLine(
        color = Color.Gray,
        start = Offset(width / 2 - 12f, bodyOffsetY + height / 2),
        end = Offset(width / 2 - 12f, bodyOffsetY + height - 8f),
        strokeWidth = 4.dp.toPx()
    )

    // Reflectores laterales
    drawRect(
        color = Color.Red.copy(alpha = 0.7f),
        topLeft = Offset(-width / 2 - 3f, bodyOffsetY + height / 2),
        size = Size(6f, 20f)
    )
    drawRect(
        color = Color.Red.copy(alpha = 0.7f),
        topLeft = Offset(width / 2 - 3f, bodyOffsetY + height / 2),
        size = Size(6f, 20f)
    )
}

// Function to draw wheels in rear view (rectangular)
private fun DrawScope.drawRearViewWheel(center: Offset, width: Float = 20f, height: Float = 35f) {
    // Sombra of the rueda
    drawRect(
        color = Color.Black.copy(alpha = 0.3f),
        topLeft = Offset(center.x - width / 2 + 2f, center.y - height / 2 + 2f),
        size = Size(width, height)
    )

    // Main wheel (black vertical rectangle)
    drawRect(
        color = Color.Black,
        topLeft = Offset(center.x - width / 2, center.y - height / 2),
        size = Size(width, height)
    )

    // Metal rim edge
    drawRect(
        color = Color.Gray,
        topLeft = Offset(center.x - width / 2 + 2f, center.y - height / 2 + 2f),
        size = Size(width - 4f, height - 4f)
    )

    // Center line of tread
    drawLine(
        color = Color.DarkGray,
        start = Offset(center.x, center.y - height / 2 + 3f),
        end = Offset(center.x, center.y + height / 2 - 3f),
        strokeWidth = 1.5.dp.toPx()
    )

    // Contorno exterior
    drawRect(
        color = Color.Black,
        topLeft = Offset(center.x - width / 2, center.y - height / 2),
        size = Size(width, height),
        style = Stroke(width = 1.5.dp.toPx())
    )
}
