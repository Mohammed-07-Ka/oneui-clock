package com.sec.oneui.clock.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── Gemini Signature Colors ──
val GeminiDeepCosmos = Color(0xFF070510)     // Cosmic Dark Base
val GeminiDarkViolet = Color(0xFF4C1D95)     // Deep Violet Orb
val GeminiPurple = Color(0xFF8B5CF6)         // Electric / Luminous Purple
val GeminiPurpleLight = Color(0xFFA78BFA)    // Bright Violet
val GeminiLavender = Color(0xFFC4B5FD)       // Soft Radiant Lavender
val GeminiWhiteGlow = Color(0xFFF8FAFC)      // Pure Soft White Glow
val GeminiLightPearl = Color(0xFFF5F3FF)     // Pearl Lavender Light Background
val GeminiLightPearlSurface = Color(0xFFEDE9FE)
val GeminiCyanSparkle = Color(0xFF38BDF8)     // Subtle cosmic star sparkle

// ── Glassmorphism Colors & Gradients ──
val GeminiGlassBackgroundDark = Color.White.copy(alpha = 0.08f)
val GeminiGlassBackgroundHover = Color.White.copy(alpha = 0.14f)
val GeminiGlassBackgroundLight = Color.White.copy(alpha = 0.78f)

val GeminiGlassBorderGradientDark = Brush.linearGradient(
    listOf(
        Color.White.copy(alpha = 0.35f),
        GeminiPurple.copy(alpha = 0.40f),
        GeminiLavender.copy(alpha = 0.20f)
    )
)

val GeminiGlassBorderGradientLight = Brush.linearGradient(
    listOf(
        Color.White.copy(alpha = 0.90f),
        GeminiPurple.copy(alpha = 0.35f)
    )
)

// Seamless mapping of existing UI tokens to the Gemini Purple & White Theme
val OneUiBlack = GeminiDeepCosmos
val OneUiSurface = Color(0xFF0F0B1E)
val OneUiCard = GeminiGlassBackgroundDark
val OneUiCardHover = GeminiGlassBackgroundHover
val OneUiBlue = GeminiPurple
val OneUiBlueLight = GeminiPurpleLight
val OneUiOrange = Color(0xFFFF8A3D)
val OneUiGreen = Color(0xFF34D399)
val OneUiRed = Color(0xFFF43F5E)
val OneUiTextPrimary = Color(0xFFFFFFFF)
val OneUiTextSecondary = Color(0xFFC4B5FD)
val OneUiDivider = Color.White.copy(alpha = 0.12f)
val OneUiAccentGradientStart = GeminiDarkViolet
val OneUiAccentGradientEnd = GeminiPurple
