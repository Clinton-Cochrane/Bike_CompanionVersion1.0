package com.you.bikecompanion.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1B5E20),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA5D6A7),
    onPrimaryContainer = Color(0xFF002204),
    secondary = Color(0xFF52634F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5E8D0),
    onSecondaryContainer = Color(0xFF101F10),
    tertiary = Color(0xFF38656A),
    onTertiary = Color.White,
    background = Color(0xFFF6FBF4),
    onBackground = Color(0xFF191C19),
    surface = Color(0xFFF6FBF4),
    onSurface = Color(0xFF191C19),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8BC78E),
    onPrimary = Color(0xFF003908),
    primaryContainer = Color(0xFF00530D),
    onPrimaryContainer = Color(0xFFA5D6A7),
    secondary = Color(0xFFB9CCB4),
    onSecondary = Color(0xFF243423),
    secondaryContainer = Color(0xFF3A4B38),
    onSecondaryContainer = Color(0xFFD5E8D0),
    tertiary = Color(0xFF9CD0D5),
    onTertiary = Color(0xFF00363B),
    background = Color(0xFF191C19),
    onBackground = Color(0xFFE1E3DE),
    surface = Color(0xFF191C19),
    onSurface = Color(0xFFE1E3DE),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun BikeCompanionTheme(
    darkTheme: Boolean = true, // Default to dark mode; set to isSystemInDarkTheme() to follow system
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
