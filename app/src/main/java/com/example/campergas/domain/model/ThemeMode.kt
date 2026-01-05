package com.example.campergas.domain.model

/**
 * Application visual theme mode.
 *
 * The application supports light, dark, and system themes. The theme affects background
 * colors, text, UI components, and system bars.
 *
 * Available themes:
 * - LIGHT: Light theme with light backgrounds and dark text
 * - DARK: Dark theme with dark/black backgrounds and light text
 * - SYSTEM: Uses the system theme (Android 10+)
 *
 * @author Felipe García Gómez
 */
enum class ThemeMode {
    /** Light theme mode with light backgrounds and dark text */
    LIGHT,

    /** Dark theme mode with dark backgrounds and light text */
    DARK,

    /** Uses the operating system theme */
    SYSTEM
}
