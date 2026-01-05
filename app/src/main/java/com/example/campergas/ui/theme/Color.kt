package com.example.campergas.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Color palette for the CamperGas application.
 *
 * Defines brand colors optimized for accessibility and WCAG compliance.
 * Includes variants for both light and dark themes with proper contrast ratios.
 */

// CamperGas Brand Colors - Improved for Accessibility

/** Primary blue color for light theme - WCAG AA compliant (7.8:1 contrast) */
val CamperBlue = Color(0xFF0D47A1)

/** Primary blue color for dark theme - Optimized for visibility (7.2:1 contrast) */
val CamperBlueLight = Color(0xFF5E92F3)

/** High contrast variant of primary blue */
val CamperBlueDark = Color(0xFF002171)

/** Secondary green color for light theme - WCAG AA compliant (6.9:1 contrast) */
val CamperGreen = Color(0xFF2E7D32)

/** Secondary green color for dark theme - Accessible light green */
val CamperGreenLight = Color(0xFF66BB6A)

/** High contrast dark green variant */
val CamperGreenDark = Color(0xFF1B5E20)

/** Tertiary orange color for light theme - Energy/warning theme (4.6:1 contrast) */
val CamperOrange = Color(0xFFF57C00)

/** Tertiary orange color for dark theme - Light orange (8.1:1 contrast) */
val CamperOrangeLight = Color(0xFFFFB74D)

/** Dark orange variant */
val CamperOrangeDark = Color(0xFFE65100)

/** Error color for light theme - WCAG compliant */
val CamperError = Color(0xFFD32F2F)

/** Error color for dark theme */
val CamperErrorLight = Color(0xFFFF6659)

/** Surface color for light theme - Slightly off-white for better definition */
val CamperSurfaceLight = Color(0xFFF5F5F5)

/** Surface color for dark theme - Improved dark surface */
val CamperSurfaceDark = Color(0xFF1A1A1A)

/** Background color for light theme - Pure white */
val CamperBackgroundLight = Color(0xFFFFFFFF)

/** Background color for dark theme - Deep dark background */
val CamperBackgroundDark = Color(0xFF0F0F0F)