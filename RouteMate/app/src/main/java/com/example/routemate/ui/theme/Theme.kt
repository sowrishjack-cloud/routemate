package com.example.routemate.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.routemate.models.AppSettingsState

private val DarkColorScheme = darkColorScheme(
    primary = Cyan,
    onPrimary = DeepNavy,
    secondary = OceanBlue,
    onSecondary = Color.White,
    tertiary = AccentGreen,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = OceanBlue,
    onPrimary = Color.White,
    secondary = Cyan,
    onSecondary = DeepNavy,
    tertiary = AccentGreen,
    background = IceBlue,
    onBackground = DeepNavy,
    surface = Color.White,
    onSurface = DeepNavy,
    surfaceVariant = Color(0xFFE2EDFB),
    onSurfaceVariant = Color.Gray
)

@Composable
fun RouteMateTheme(
    darkTheme: Boolean = AppSettingsState.isDarkMode,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
