package com.sec.oneui.clock.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
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

@Composable
fun OneUIClockTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
