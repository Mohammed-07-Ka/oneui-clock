package com.sec.oneui.clock.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.sec.oneui.clock.settings.ThemeMode

private val AuroraColorScheme = darkColorScheme(
    primary = Color(0xFF7C3AED),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2563EB),
    secondary = Color(0xFFE2E8F0),
    tertiary = Color(0xFFFFFFFF),
    background = Color(0xFF090714),
    surface = LiquidGlassBackground,
    surfaceVariant = LiquidGlassHover,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = LiquidTextSecondary,
    outline = Color.White.copy(alpha = 0.25f),
    error = OneUiRed
)

private val SilverVioletColorScheme = darkColorScheme(
    primary = Color(0xFFA855F7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E1035),
    secondary = Color(0xFFCBD5E1),
    tertiary = Color(0xFFFFFFFF),
    background = Color(0xFF0D0B18),
    surface = LiquidGlassBackground,
    surfaceVariant = LiquidGlassHover,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = LiquidTextSecondary,
    outline = Color.White.copy(alpha = 0.30f),
    error = OneUiRed
)

private val AmoledPitchColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF111111),
    secondary = LiquidTextSecondary,
    tertiary = Color.White,
    background = Color.Black,
    surface = LiquidGlassBackground,
    surfaceVariant = LiquidGlassHover,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = LiquidTextSecondary,
    outline = Color.White.copy(alpha = 0.18f),
    error = OneUiRed
)

@Composable
fun OneUIClockTheme(
    themeMode: ThemeMode = ThemeMode.AURORA_PURPLE,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ThemeMode.AURORA_PURPLE -> AuroraColorScheme
        ThemeMode.SILVER_VIOLET -> SilverVioletColorScheme
        ThemeMode.AMOLED_PITCH -> AmoledPitchColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@Composable
fun OneUiClockTheme(
    content: @Composable () -> Unit
) = OneUIClockTheme(themeMode = ThemeMode.AURORA_PURPLE, content = content)
