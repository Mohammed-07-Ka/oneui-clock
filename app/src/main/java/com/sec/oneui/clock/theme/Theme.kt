package com.sec.oneui.clock.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.sec.oneui.clock.settings.ThemeMode

data class ClockThemePalette(
    val themeMode: ThemeMode,
    val primaryAccent: Color,
    val onPrimaryAccent: Color,
    val secondaryAccent: Color,
    val isBlueTheme: Boolean,
    val cardBackground: Color,
    val cardBorder: Brush,
    val cardBorderSubtle: Brush,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val tabSelectedIndicator: Color,
    val toggleActiveTrack: Color,
    val toggleActiveThumb: Color
)

val AuroraPalette = ClockThemePalette(
    themeMode = ThemeMode.AURORA_PURPLE,
    primaryAccent = Color.White.copy(alpha = 0.92f),
    onPrimaryAccent = Color(0xFF000000),
    secondaryAccent = Color(0xFF7C3AED),
    isBlueTheme = false,
    cardBackground = LiquidGlassBackground,
    cardBorder = LiquidSpecularBorder,
    cardBorderSubtle = LiquidSpecularBorderSubtle,
    textPrimary = LiquidTextPrimary,
    textSecondary = LiquidTextSecondary,
    textMuted = LiquidTextMuted,
    tabSelectedIndicator = Color.White.copy(alpha = 0.12f),
    toggleActiveTrack = Color.White.copy(alpha = 0.92f),
    toggleActiveThumb = Color(0xFF000000)
)

val MidnightBluePalette = ClockThemePalette(
    themeMode = ThemeMode.MIDNIGHT_BLUE,
    primaryAccent = Color(0xFF2563EB),
    onPrimaryAccent = Color(0xFFFFFFFF),
    secondaryAccent = Color(0xFF4C1D95),
    isBlueTheme = true,
    cardBackground = Color.White.copy(alpha = 0.05f),
    cardBorder = BlueSpecularBorder,
    cardBorderSubtle = BlueSpecularBorderSubtle,
    textPrimary = LiquidTextPrimary,
    textSecondary = LiquidTextSecondary,
    textMuted = LiquidTextMuted,
    tabSelectedIndicator = Color(0xFF2563EB).copy(alpha = 0.28f),
    toggleActiveTrack = Color(0xFF2563EB),
    toggleActiveThumb = Color(0xFFFFFFFF)
)

val AmoledPitchPalette = ClockThemePalette(
    themeMode = ThemeMode.AMOLED_PITCH,
    primaryAccent = Color(0xFFFFFFFF),
    onPrimaryAccent = Color(0xFF000000),
    secondaryAccent = Color(0xFF333333),
    isBlueTheme = false,
    cardBackground = Color.White.copy(alpha = 0.05f),
    cardBorder = LiquidSpecularBorderSubtle,
    cardBorderSubtle = LiquidSpecularBorderSubtle,
    textPrimary = LiquidTextPrimary,
    textSecondary = LiquidTextSecondary,
    textMuted = LiquidTextMuted,
    tabSelectedIndicator = Color.White.copy(alpha = 0.12f),
    toggleActiveTrack = Color(0xFFFFFFFF),
    toggleActiveThumb = Color(0xFF000000)
)

val LocalClockPalette = staticCompositionLocalOf { AuroraPalette }

object ClockTheme {
    val palette: ClockThemePalette
        @Composable
        @ReadOnlyComposable
        get() = LocalClockPalette.current
}

private val AuroraColorScheme = darkColorScheme(
    primary = Color(0xFF7C3AED),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4C1D95),
    secondary = Color(0xFFCBD5E1),
    tertiary = Color(0xFFFFFFFF),
    background = Color(0xFF030207),
    surface = LiquidGlassBackground,
    surfaceVariant = LiquidGlassHover,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = LiquidTextSecondary,
    outline = Color.White.copy(alpha = 0.25f),
    error = OneUiRed
)

private val MidnightBlueColorScheme = darkColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF3B82F6),
    tertiary = Color(0xFF60A5FA),
    background = Color(0xFF000000),
    surface = LiquidGlassBackground,
    surfaceVariant = LiquidGlassHover,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = LiquidTextSecondary,
    outline = Color(0xFF2563EB).copy(alpha = 0.35f),
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
    val palette = when (themeMode) {
        ThemeMode.AURORA_PURPLE -> AuroraPalette
        ThemeMode.MIDNIGHT_BLUE -> MidnightBluePalette
        ThemeMode.AMOLED_PITCH -> AmoledPitchPalette
    }

    val colorScheme = when (themeMode) {
        ThemeMode.AURORA_PURPLE -> AuroraColorScheme
        ThemeMode.MIDNIGHT_BLUE -> MidnightBlueColorScheme
        ThemeMode.AMOLED_PITCH -> AmoledPitchColorScheme
    }

    CompositionLocalProvider(LocalClockPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

@Composable
fun OneUiClockTheme(
    content: @Composable () -> Unit
) = OneUIClockTheme(themeMode = ThemeMode.AURORA_PURPLE, content = content)
