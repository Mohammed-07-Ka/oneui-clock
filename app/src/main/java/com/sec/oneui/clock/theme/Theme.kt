package com.sec.oneui.clock.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.sec.oneui.clock.settings.ThemeMode

private val DarkGeminiColorScheme = darkColorScheme(
    primary = GeminiPurple,
    onPrimary = Color.White,
    primaryContainer = GeminiDarkViolet,
    secondary = GeminiLavender,
    tertiary = GeminiCyanSparkle,
    background = GeminiDeepCosmos,
    surface = GeminiGlassBackgroundDark,
    surfaceVariant = GeminiGlassBackgroundHover,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = GeminiLavender,
    outline = GeminiLavender.copy(alpha = 0.35f),
    error = OneUiRed
)

private val LightPearlColorScheme = lightColorScheme(
    primary = GeminiPurple,
    onPrimary = Color.White,
    primaryContainer = GeminiLavender,
    secondary = GeminiDarkViolet,
    tertiary = GeminiCyanSparkle,
    background = GeminiLightPearl,
    surface = GeminiGlassBackgroundLight,
    surfaceVariant = GeminiLightPearlSurface,
    onBackground = Color(0xFF1E1035),
    onSurface = Color(0xFF1E1035),
    onSurfaceVariant = Color(0xFF6B5887),
    outline = GeminiPurple.copy(alpha = 0.30f),
    error = OneUiRed
)

private val AmoledColorScheme = darkColorScheme(
    primary = GeminiPurple,
    onPrimary = Color.White,
    primaryContainer = GeminiDarkViolet,
    secondary = GeminiLavender,
    tertiary = GeminiCyanSparkle,
    background = Color.Black,
    surface = Color(0xFF0B0816),
    surfaceVariant = Color(0xFF16112A),
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = GeminiLavender,
    outline = GeminiLavender.copy(alpha = 0.30f),
    error = OneUiRed
)

@Composable
fun OneUIClockTheme(
    themeMode: ThemeMode = ThemeMode.DARK_GEMINI,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ThemeMode.DARK_GEMINI -> DarkGeminiColorScheme
        ThemeMode.LIGHT_PEARL -> LightPearlColorScheme
        ThemeMode.AMOLED -> AmoledColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@Composable
fun OneUiClockTheme(
    content: @Composable () -> Unit
) = OneUIClockTheme(themeMode = ThemeMode.DARK_GEMINI, content = content)
