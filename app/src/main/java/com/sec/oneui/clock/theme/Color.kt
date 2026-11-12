package com.sec.oneui.clock.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── 85% Deep Black + 10% Royal Deep Purple + 5% Crisp White ──
val LiquidBlack = Color(0xFF000000)
val LiquidDeepPurple = Color(0xFF1E0B36)
val LiquidRoyalPurple = Color(0xFF2E1065)
val LiquidTextPrimary = Color(0xFFFFFFFF)
val LiquidTextSecondary = Color(0xFF94A3B8)
val LiquidTextMuted = Color(0xFF64748B)

// ── iOS Liquid Glass Tokens ──
val LiquidGlassBackground = Color.White.copy(alpha = 0.06f)
val LiquidGlassHover = Color.White.copy(alpha = 0.10f)
val LiquidActiveWhite = Color.White.copy(alpha = 0.92f)
val LiquidGlassActive = LiquidActiveWhite
val LiquidActiveText = Color(0xFF000000)

// Specular Glass Border: top-left bright white highlight fading into subtle purple/dark rim
val LiquidSpecularBorder = Brush.linearGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.28f),
        Color.White.copy(alpha = 0.08f),
        LiquidRoyalPurple.copy(alpha = 0.08f)
    ),
    start = Offset(0f, 0f),
    end = Offset(400f, 600f)
)

val LiquidSpecularBorderSubtle = Brush.linearGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.20f),
        Color.White.copy(alpha = 0.04f)
    ),
    start = Offset(0f, 0f),
    end = Offset(200f, 400f)
)

// Legacy aliases mapped to the new Liquid Glass Dark aesthetic
val GeminiDeepCosmos = LiquidBlack
val GeminiDarkViolet = LiquidDeepPurple
val GeminiPurple = LiquidRoyalPurple
val GeminiPurpleLight = Color(0xFFA855F7)
val GeminiLavender = Color(0xFFCBD5E1)
val GeminiWhiteGlow = Color(0xFFFFFFFF)
val GeminiLightPearl = Color(0xFF000000)
val GeminiLightPearlSurface = Color(0xFF0F0B18)
val GeminiCyanSparkle = Color(0xFFFFFFFF)

val GeminiGlassBackgroundDark = LiquidGlassBackground
val GeminiGlassBackgroundHover = LiquidGlassHover
val GeminiGlassBackgroundLight = LiquidGlassBackground
val GeminiGlassBorderGradientDark = LiquidSpecularBorder
val GeminiGlassBorderGradientLight = LiquidSpecularBorder

val OneUiBlack = LiquidBlack
val OneUiSurface = LiquidBlack
val OneUiCard = LiquidGlassBackground
val OneUiCardHover = LiquidGlassHover
val OneUiBlue = LiquidActiveWhite
val OneUiBlueLight = Color(0xFFE2E8F0)
val OneUiOrange = Color(0xFFF97316)
val OneUiGreen = Color(0xFF22C55E)
val OneUiRed = Color(0xFFEF4444)
val OneUiTextPrimary = LiquidTextPrimary
val OneUiTextSecondary = LiquidTextSecondary
val OneUiDivider = Color.White.copy(alpha = 0.08f)
val OneUiAccentGradientStart = LiquidDeepPurple
val OneUiAccentGradientEnd = LiquidRoyalPurple
