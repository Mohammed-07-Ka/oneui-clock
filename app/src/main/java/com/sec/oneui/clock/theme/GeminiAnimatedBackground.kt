package com.sec.oneui.clock.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.sec.oneui.clock.settings.ThemeMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Visible Minimalist Aurora Mesh Background (iOS & Gemini Aurora Style)
 *
 * Smoothly blends 3 luminous colors in 2D space:
 * - Mid Purple (#7C3AED @ 0.42f)
 * - Deep Indigo Navy (#2563EB @ 0.32f)
 * - Soft Silver Glow (#E2E8F0 @ 0.16f)
 * over an elegant dark violet floor (#090714).
 *
 * Supports:
 * - Aurora Purple (Default)
 * - Silver Violet
 * - AMOLED Pitch (Pure OLED black)
 */
@Composable
fun GeminiAnimatedBackground(
    modifier: Modifier = Modifier,
    themeMode: ThemeMode = ThemeMode.AURORA_PURPLE,
    animationEnabled: Boolean = true,
    animationSpeed: Float = 1.0f,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "auroraMeshClock")

    val cycleDurationMs = remember(animationSpeed) {
        val safeSpeed = animationSpeed.coerceIn(0.2f, 3.0f)
        (22_000L / safeSpeed).toInt()
    }

    val animatedPhase by if (animationEnabled) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = cycleDurationMs, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase"
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(0.85f) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            if (width <= 0f || height <= 0f) return@Canvas

            // 1. Ground floor color
            val groundColor = when (themeMode) {
                ThemeMode.AURORA_PURPLE -> Color(0xFF090714)
                ThemeMode.SILVER_VIOLET -> Color(0xFF0D0B18)
                ThemeMode.AMOLED_PITCH -> Color(0xFF000000)
            }
            drawRect(color = groundColor)

            // If AMOLED Pitch, keep pure black without mesh glow
            if (themeMode == ThemeMode.AMOLED_PITCH) {
                return@Canvas
            }

            val meshRadius = width * 0.95f

            when (themeMode) {
                ThemeMode.AURORA_PURPLE -> {
                    // Orb 1: Mid Purple (#7C3AED, alpha = 0.42f) undulating top-left to center
                    val orb1X = width * (0.30f + 0.15f * sin(animatedPhase))
                    val orb1Y = height * (0.24f + 0.12f * cos(animatedPhase * 0.8f))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF7C3AED).copy(alpha = 0.42f),
                                Color.Transparent
                            ),
                            center = Offset(orb1X, orb1Y),
                            radius = meshRadius
                        ),
                        radius = meshRadius,
                        center = Offset(orb1X, orb1Y)
                    )

                    // Orb 2: Deep Indigo Navy (#2563EB, alpha = 0.32f) undulating bottom-right
                    val orb2X = width * (0.74f + 0.14f * cos(animatedPhase * 1.1f))
                    val orb2Y = height * (0.68f + 0.12f * sin(animatedPhase * 0.9f))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF2563EB).copy(alpha = 0.32f),
                                Color.Transparent
                            ),
                            center = Offset(orb2X, orb2Y),
                            radius = meshRadius
                        ),
                        radius = meshRadius,
                        center = Offset(orb2X, orb2Y)
                    )

                    // Orb 3: Soft Silver-White Glow (#E2E8F0, alpha = 0.16f) undulating mid-screen
                    val orb3X = width * (0.50f + 0.18f * sin(animatedPhase * 0.7f + 1.2f))
                    val orb3Y = height * (0.46f + 0.10f * cos(animatedPhase * 1.2f))
                    val silverRadius = width * 0.85f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFE2E8F0).copy(alpha = 0.16f),
                                Color.Transparent
                            ),
                            center = Offset(orb3X, orb3Y),
                            radius = silverRadius
                        ),
                        radius = silverRadius,
                        center = Offset(orb3X, orb3Y)
                    )
                }

                ThemeMode.SILVER_VIOLET -> {
                    // Luminous Lavender Violet
                    val orb1X = width * (0.28f + 0.14f * sin(animatedPhase))
                    val orb1Y = height * (0.26f + 0.10f * cos(animatedPhase * 0.8f))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFA855F7).copy(alpha = 0.38f),
                                Color.Transparent
                            ),
                            center = Offset(orb1X, orb1Y),
                            radius = meshRadius
                        ),
                        radius = meshRadius,
                        center = Offset(orb1X, orb1Y)
                    )

                    // Silver Slate
                    val orb2X = width * (0.72f + 0.14f * cos(animatedPhase * 1.0f))
                    val orb2Y = height * (0.70f + 0.12f * sin(animatedPhase * 0.9f))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF94A3B8).copy(alpha = 0.28f),
                                Color.Transparent
                            ),
                            center = Offset(orb2X, orb2Y),
                            radius = meshRadius
                        ),
                        radius = meshRadius,
                        center = Offset(orb2X, orb2Y)
                    )

                    // Crisp White Core
                    val orb3X = width * (0.50f + 0.16f * sin(animatedPhase * 0.75f + 1.0f))
                    val orb3Y = height * (0.48f + 0.08f * cos(animatedPhase * 1.1f))
                    val whiteRadius = width * 0.75f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color.Transparent
                            ),
                            center = Offset(orb3X, orb3Y),
                            radius = whiteRadius
                        ),
                        radius = whiteRadius,
                        center = Offset(orb3X, orb3Y)
                    )
                }

                ThemeMode.AMOLED_PITCH -> {}
            }
        }

        // Host content
        content()
    }
}
