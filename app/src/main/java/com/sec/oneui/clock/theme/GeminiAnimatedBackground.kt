package com.sec.oneui.clock.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
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
 * Minimalist Aurora Mesh Background with iOS Fluid Animation
 *
 * Supported Themes:
 * 1. Aurora Purple (Default): Mid Purple + Deep Indigo + Soft Silver over dark violet (#090714).
 * 2. Midnight Blue & Purple: 65% Pure Black (#000000) with darker, subdued Deep Indigo Navy
 *    and Deep Violet glows occupying only 30-35% in corners.
 * 3. AMOLED Pitch: 100% Pure OLED Black (#000000) with no mesh.
 *
 * Smooth 350ms easeOut color transition when switching themes.
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

    val targetGroundColor = when (themeMode) {
        ThemeMode.AURORA_PURPLE -> Color(0xFF090714)
        ThemeMode.MIDNIGHT_BLUE -> Color(0xFF000000)
        ThemeMode.AMOLED_PITCH -> Color(0xFF000000)
    }

    // Smooth 350ms theme switch transition
    val groundColor by animateColorAsState(
        targetValue = targetGroundColor,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "groundColorTransition"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            if (width <= 0f || height <= 0f) return@Canvas

            // 1. Fill ground floor
            drawRect(color = groundColor)

            // Pure black for AMOLED Pitch
            if (themeMode == ThemeMode.AMOLED_PITCH) {
                return@Canvas
            }

            when (themeMode) {
                ThemeMode.AURORA_PURPLE -> {
                    val meshRadius = width * 0.95f

                    // Orb 1: Mid Purple (#7C3AED @ 0.42f) undulating top-left to center
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

                    // Orb 2: Deep Indigo Navy (#2563EB @ 0.32f) undulating bottom-right
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

                    // Orb 3: Soft Silver-White Glow (#E2E8F0 @ 0.16f) undulating mid-screen
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

                ThemeMode.MIDNIGHT_BLUE -> {
                    // Midnight Blue: Black is dominant (65%+).
                    // Subdued, darker deep blue & purple glows covering only 30-35% in corners.
                    val localizedRadius = width * 0.60f

                    // Deep Indigo Blue (#1D4ED8 @ 0.22f) in top-right corner
                    val orb1X = width * (0.82f + 0.08f * sin(animatedPhase))
                    val orb1Y = height * (0.22f + 0.08f * cos(animatedPhase * 0.8f))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1D4ED8).copy(alpha = 0.22f),
                                Color(0xFF1E3A8A).copy(alpha = 0.10f),
                                Color.Transparent
                            ),
                            center = Offset(orb1X, orb1Y),
                            radius = localizedRadius
                        ),
                        radius = localizedRadius,
                        center = Offset(orb1X, orb1Y)
                    )

                    // Deep Dark Purple (#4C1D95 @ 0.20f) in bottom-left corner
                    val orb2X = width * (0.20f + 0.08f * cos(animatedPhase * 1.1f))
                    val orb2Y = height * (0.78f + 0.08f * sin(animatedPhase * 0.9f))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF4C1D95).copy(alpha = 0.20f),
                                Color(0xFF3B0764).copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(orb2X, orb2Y),
                            radius = localizedRadius
                        ),
                        radius = localizedRadius,
                        center = Offset(orb2X, orb2Y)
                    )
                }

                ThemeMode.AMOLED_PITCH -> {}
            }
        }

        // Host content
        content()
    }
}
