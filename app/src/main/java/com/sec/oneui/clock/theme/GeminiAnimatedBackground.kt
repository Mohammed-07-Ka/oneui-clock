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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.sec.oneui.clock.settings.ThemeMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Gemini Purple & White Animated Mesh & Particle Background
 * Features:
 * - Fluid multi-layered radial mesh gradients moving slowly in 2D space.
 * - Dynamic 3D-undulating wave mesh of luminous particle dots and iconic 4-point Gemini stars (✦).
 * - Adapts to Dark Gemini, Light Pearl Purple, and AMOLED Black modes.
 * - Respects battery savings by pausing animation when disabled or scaling with animationSpeed.
 */
@Composable
fun GeminiAnimatedBackground(
    modifier: Modifier = Modifier,
    themeMode: ThemeMode = ThemeMode.DARK_GEMINI,
    animationEnabled: Boolean = true,
    animationSpeed: Float = 1.0f,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "geminiBgClock")

    // Base cycle animation: 20 seconds for normal speed (1.0f)
    val cycleDurationMs = remember(animationSpeed) {
        val safeSpeed = animationSpeed.coerceIn(0.2f, 3.0f)
        (20_000L / safeSpeed).toInt()
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
        remember { androidx.compose.runtime.mutableFloatStateOf(0.7f) }
    }

    // Pre-calculate pseudo-random deterministic wave particles (50 points)
    val particleOffsets = remember {
        List(48) { index ->
            val u = (index % 12) / 11f // horizontal slice 0..1
            val layer = index / 12     // 4 layers of depth
            val starVariant = (index * 7) % 5 // determines star vs dot
            ParticleSeed(u = u, layer = layer, isStar = starVariant == 0 || starVariant == 2)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            if (width <= 0f || height <= 0f) return@Canvas

            // 1. Draw Background Base
            val baseBgColor = when (themeMode) {
                ThemeMode.DARK_GEMINI -> GeminiDeepCosmos
                ThemeMode.LIGHT_PEARL -> GeminiLightPearl
                ThemeMode.AMOLED -> Color.Black
            }
            drawRect(color = baseBgColor)

            // 2. Smooth Animated Mesh Gradients (Only orbs in Dark/Light; subtle in AMOLED)
            val isAmoled = themeMode == ThemeMode.AMOLED
            val isLight = themeMode == ThemeMode.LIGHT_PEARL

            // Orb 1: Deep Violet (Top-Left / Center undulating)
            val orb1X = width * (0.28f + 0.16f * sin(animatedPhase))
            val orb1Y = height * (0.22f + 0.12f * cos(animatedPhase * 0.8f))
            val orb1Radius = width * 0.85f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (isLight) GeminiLavender.copy(alpha = 0.55f)
                        else if (isAmoled) GeminiDarkViolet.copy(alpha = 0.28f)
                        else GeminiDarkViolet.copy(alpha = 0.75f),
                        Color.Transparent
                    ),
                    center = Offset(orb1X, orb1Y),
                    radius = orb1Radius
                ),
                radius = orb1Radius,
                center = Offset(orb1X, orb1Y)
            )

            // Orb 2: Luminous Electric Purple (Top-Right / Mid undulating)
            val orb2X = width * (0.75f + 0.15f * cos(animatedPhase * 1.1f))
            val orb2Y = height * (0.35f + 0.14f * sin(animatedPhase * 0.9f))
            val orb2Radius = width * 0.70f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (isLight) GeminiPurple.copy(alpha = 0.28f)
                        else if (isAmoled) GeminiPurple.copy(alpha = 0.22f)
                        else GeminiPurple.copy(alpha = 0.50f),
                        Color.Transparent
                    ),
                    center = Offset(orb2X, orb2Y),
                    radius = orb2Radius
                ),
                radius = orb2Radius,
                center = Offset(orb2X, orb2Y)
            )

            // Orb 3: Radiant Lavender / Soft White Glow (Bottom-Center undulating)
            val orb3X = width * (0.50f + 0.22f * sin(animatedPhase * 0.7f + 1.2f))
            val orb3Y = height * (0.75f + 0.10f * cos(animatedPhase * 1.2f))
            val orb3Radius = width * 0.80f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (isLight) Color.White.copy(alpha = 0.80f)
                        else if (isAmoled) GeminiLavender.copy(alpha = 0.15f)
                        else GeminiLavender.copy(alpha = 0.35f),
                        Color.Transparent
                    ),
                    center = Offset(orb3X, orb3Y),
                    radius = orb3Radius
                ),
                radius = orb3Radius,
                center = Offset(orb3X, orb3Y)
            )

            // Orb 4: Ethereal White Core Sparkle (Center)
            if (!isAmoled) {
                val orb4X = width * (0.52f + 0.08f * cos(animatedPhase))
                val orb4Y = height * (0.48f + 0.08f * sin(animatedPhase))
                val orb4Radius = width * 0.45f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (isLight) Color.White.copy(alpha = 0.45f)
                            else GeminiWhiteGlow.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(orb4X, orb4Y),
                        radius = orb4Radius
                    ),
                    radius = orb4Radius,
                    center = Offset(orb4X, orb4Y)
                )
            }

            // 3. Gemini 3D Undulating Wave Field with Particle Dots & 4-Point Stars (✦)
            val waveBaseY = height * 0.74f

            particleOffsets.forEach { seed ->
                val x = seed.u * width
                val layerFactor = (seed.layer + 1) / 4f // 0.25 .. 1.0 (depth)
                val layerPhase = animatedPhase + seed.layer * 0.8f

                // 3D sin/cos wave calculation
                val waveOffset = (sin(seed.u * 3 * PI.toFloat() + layerPhase) * 28f * layerFactor) +
                        (cos(seed.u * 5 * PI.toFloat() - layerPhase * 0.6f) * 16f * layerFactor)

                val y = waveBaseY + (seed.layer * 22f) + waveOffset

                // Color tinting: mix of luminous white, electric lavender, and cyan sparkle
                val particleColor = when {
                    isLight -> when (seed.layer) {
                        0 -> GeminiPurple.copy(alpha = 0.35f * layerFactor)
                        1 -> GeminiDarkViolet.copy(alpha = 0.50f * layerFactor)
                        2 -> GeminiPurpleLight.copy(alpha = 0.65f * layerFactor)
                        else -> GeminiLavender.copy(alpha = 0.80f * layerFactor)
                    }
                    else -> when (seed.layer) {
                        0 -> GeminiLavender.copy(alpha = 0.40f * layerFactor)
                        1 -> GeminiWhiteGlow.copy(alpha = 0.55f * layerFactor)
                        2 -> GeminiCyanSparkle.copy(alpha = 0.70f * layerFactor)
                        else -> Color.White.copy(alpha = 0.85f * layerFactor)
                    }
                }

                if (seed.isStar) {
                    val starSize = (4f + seed.layer * 3.5f)
                    drawGeminiStar(center = Offset(x, y), size = starSize, color = particleColor)
                } else {
                    val dotRadius = (1.5f + seed.layer * 1.5f)
                    drawCircle(color = particleColor, radius = dotRadius, center = Offset(x, y))
                }
            }
        }

        // Host nested content above background
        content()
    }
}

/**
 * Draws the authentic Google Gemini 4-point concave star (✦)
 */
private fun DrawScope.drawGeminiStar(
    center: Offset,
    size: Float,
    color: Color
) {
    val path = Path().apply {
        moveTo(center.x, center.y - size)
        quadraticBezierTo(center.x, center.y, center.x + size, center.y)
        quadraticBezierTo(center.x, center.y, center.x, center.y + size)
        quadraticBezierTo(center.x, center.y, center.x - size, center.y)
        quadraticBezierTo(center.x, center.y, center.x, center.y - size)
        close()
    }
    drawPath(path = path, color = color)
}

private data class ParticleSeed(
    val u: Float,
    val layer: Int,
    val isStar: Boolean
)
