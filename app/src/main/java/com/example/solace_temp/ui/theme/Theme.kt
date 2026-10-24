/**
 * Theme.kt
 * 
 * Purpose: Defines the main Material 3 theme and color schemes for the application.
 * 
 * Needs Change/Issues:
 * - Hardcoded color values should ideally refer to primitives defined in Color.kt.
 */
package com.example.solace_temp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** The primary red color used for the app's brand identity. */
private val RedPrimary = Color(0xFFE53935)

/** A soft light blue background color used for main screens. */
private val LightBlueBackground = Color(0xFFF4F7FB)

/**
 * Defines the custom color scheme used when the application is in dark mode.
 */
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFEF5350),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB71C1C),
    onPrimaryContainer = Color(0xFFFFCDD2),
    secondary = Color(0xFF90CAF9),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFBDBDBD)
)

/**
 * Defines the custom color scheme used when the application is in light mode.
 */
private val LightColorScheme = lightColorScheme(
    primary = RedPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFCDD2),
    onPrimaryContainer = Color(0xFFB71C1C),
    secondary = Color(0xFF1976D2),
    background = LightBlueBackground,
    onBackground = Color(0xFF101418),
    surface = Color.White,
    onSurface = Color(0xFF101418),
    surfaceVariant = Color(0xFFE3E8EF),
    onSurfaceVariant = Color(0xFF43474E)
)

/**
 * The main theme composable for the Solace Campsite application.
 * Applies the correct [MaterialTheme] depending on system settings and OS version.
 * 
 * @param darkTheme Whether the dark color scheme should be used. Defaults to system setting.
 * @param dynamicColor Whether to use dynamic coloring (Android 12+). Default is false to preserve brand colors.
 * @param content The composable content that this theme will enclose.
 */
@Composable
fun Solace_tempTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set default to false to ensure our custom Red/Light Blue theme shows up!
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}