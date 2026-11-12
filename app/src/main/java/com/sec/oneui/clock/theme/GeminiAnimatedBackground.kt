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
 * 85% Deep Black + 10% Royal Deep Purple Ambient Mesh Background
 *
 * Features:
 * - 85% True Black base (#000000) for OLED power efficiency and deep contrast.
 * - 10% Subtle ambient mesh glow using Deep Royal Purple (#1E0B36 and #2E1065) moving gently in the corners.
 * - Zero star symbols or distracting particle waves.
 * - Smooth, battery-conscious animation with speed scaling and toggle support.
 */
@Composable
fun GeminiAnimatedBackground(
    modifier: Modifier = Modifier,
    themeMode: ThemeMode = ThemeMode.DARK_GEMINI,
    animationEnabled: Boolean = true,
    animationSpeed: Float = 1.0f,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambientMeshClock")

    val cycleDurationMs = remember(animationSpeed) {
        val safeSpeed = animationSpeed.coerceIn(0.2f, 3.0f)
        (28_000L / safeSpeed).toInt()
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
        remember { androidx.compose.runtime.mutableFloatStateOf(0.5f) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            if (width <= 0f || height <= 0f) return@Canvas

            // 1. 85% Deep Black Solid Foundation (#000000)
            drawRect(color = LiquidBlack)

            // 2. 10% Royal Deep Purple Ambient Glow (Very subtle and muted)
            val isAmoled = themeMode == ThemeMode.AMOLED

            // Ambient Orb 1: Royal Deep Purple (#1E0B36) top-left
            val orb1X = width * (0.22f + 0.12f * sin(animatedPhase))
            val orb1Y = height * (0.18f + 0.10f * cos(animatedPhase * 0.8f))
            val orb1Radius = width * 0.95f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        LiquidDeepPurple.copy(alpha = if (isAmoled) 0.18f else 0.45f),
                        Color.Transparent
                    ),
                    center = Offset(orb1X, orb1Y),
                    radius = orb1Radius
                ),
                radius = orb1Radius,
                center = Offset(orb1X, orb1Y)
            )

            // Ambient Orb 2: Deep Purple (#2E1065) bottom-right
            val orb2X = width * (0.78f + 0.12f * cos(animatedPhase * 0.9f))
            val orb2Y = height * (0.82f + 0.10f * sin(animatedPhase * 0.7f))
            val orb2Radius = width * 0.90f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        LiquidRoyalPurple.copy(alpha = if (isAmoled) 0.14f else 0.35f),
                        Color.Transparent
                    ),
                    center = Offset(orb2X, orb2Y),
                    radius = orb2Radius
                ),
                radius = orb2Radius,
                center = Offset(orb2X, orb2Y)
            )
        }

        // Content layered on top
        content()
    }
}
