package com.example.campergas.domain.model

/**
 * Domain model representing vehicle inclination on two axes.
 *
 * This data class encapsulates inclination measurements obtained from the
 * accelerometer/gyroscope integrated in the BLE device. Provides information
 * about vehicle orientation on pitch (tilt forward/backward) and roll (tilt left/right) axes.
 *
 * Provides useful methods for:
 * - Determining if the vehicle is level on each axis
 * - Verifying if the vehicle is completely level
 * - Formatting the measurement timestamp
 *
 * Main uses:
 * - Vehicle stability monitoring
 * - Detection of irregular surfaces or slopes
 * - Safety alerts when the vehicle is not level
 * - Stability visualization widgets
 *
 * @property pitch Inclination on the pitch axis in degrees (-180° to +180°). Positive = tilt forward
 * @property roll Inclination on the roll axis in degrees (-180° to +180°). Positive = tilt to the right
 * @property timestamp Moment when the measurement was taken (Unix timestamp in milliseconds)
 * @author Felipe García Gómez
 */
data class Inclination(
    val pitch: Float,
    val roll: Float,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Calculates if the vehicle is level on the pitch axis.
     *
     * Considered level if inclination is within the tolerance
     * of ±2 degrees from horizontal. This tolerance allows small
     * variations that don't significantly affect stability.
     *
     * @return true if pitch is between -2° and +2°, false otherwise
     */
    val isLevelPitch: Boolean
        get() = kotlin.math.abs(pitch) <= 2.0f

    /**
     * Calculates if the vehicle is level on the roll axis.
     *
     * Considered level if inclination is within the tolerance
     * of ±2 degrees from horizontal. This tolerance allows small
     * variations that don't significantly affect stability.
     *
     * @return true if roll is between -2° and +2°, false otherwise
     */
    val isLevelRoll: Boolean
        get() = kotlin.math.abs(roll) <= 2.0f

    /**
     * Calculates if the vehicle is completely level on both axes.
     *
     * The vehicle is considered completely level only when both
     * pitch and roll are within their respective tolerances
     * of ±2 degrees. This indicates the vehicle is on a
     * relatively flat and horizontal surface.
     *
     * @return true if both axes are level, false otherwise
     */
    val isLevel: Boolean
        get() = isLevelPitch && isLevelRoll

    /**
     * Formats the measurement timestamp in human-readable time format.
     *
     * Converts the Unix timestamp to a string with format "HH:mm:ss"
     * using the device's locale configuration.
     *
     * @return String with formatted time (e.g., "14:35:22")
     */
    fun getFormattedTimestamp(): String {
        val date = java.util.Date(timestamp)
        val formatter = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        return formatter.format(date)
    }
}
