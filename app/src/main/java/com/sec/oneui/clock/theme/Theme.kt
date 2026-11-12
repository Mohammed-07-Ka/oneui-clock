package com.sec.oneui.clock.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.sec.oneui.clock.settings.ThemeMode

private val AmoledColorScheme = darkColorScheme(
    primary = OneUiBlue,
    onPrimary = Color.White,
    primaryContainer = OneUiBlueLight,
    secondary = OneUiOrange,
    tertiary = OneUiGreen,
    background = OneUiBlack,
    surface = OneUiSurface,
    surfaceVariant = OneUiCard,
    onBackground = OneUiTextPrimary,
    onSurface = OneUiTextPrimary,
    onSurfaceVariant = OneUiTextSecondary,
    outline = OneUiDivider,
    error = OneUiRed
)

private val DarkColorScheme = darkColorScheme(
    primary = OneUiBlue,
    onPrimary = Color.White,
    primaryContainer = OneUiBlueLight,
    secondary = OneUiOrange,
    tertiary = OneUiGreen,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2A2A2A),
    onBackground = OneUiTextPrimary,
    onSurface = OneUiTextPrimary,
    onSurfaceVariant = OneUiTextSecondary,
    outline = OneUiDivider,
    error = OneUiRed
)

private val LightColorScheme = lightColorScheme(
    primary = OneUiBlue,
    onPrimary = Color.White,
    primaryContainer = OneUiBlueLight,
    secondary = OneUiOrange,
    tertiary = OneUiGreen,
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    surfaceVariant = Color(0xFFE8E8ED),
    onBackground = Color(0xFF1C1C1E),
    onSurface = Color(0xFF1C1C1E),
    onSurfaceVariant = Color(0xFF6E6E73),
    outline = Color(0xFFD1D1D6),
    error = OneUiRed
)

@Composable
fun OneUIClockTheme(
    themeMode: ThemeMode = ThemeMode.AMOLED,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ThemeMode.AMOLED -> AmoledColorScheme
        ThemeMode.DARK -> DarkColorScheme
        ThemeMode.LIGHT -> LightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@Composable
fun OneUiClockTheme(
    content: @Composable () -> Unit
) = OneUIClockTheme(themeMode = ThemeMode.AMOLED, content = content)
