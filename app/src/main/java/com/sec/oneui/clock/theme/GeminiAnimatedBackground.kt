package com.sec.oneui.clock.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sec.oneui.clock.settings.ThemeMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Luminous Fluid Aurora Mesh Background for Authentic Liquid Glass Refraction
 *
 * Supported Themes:
 * 1. Aurora Purple (Default): Deep midnight dark base (#0A0815) with vibrant, glowing
 *    royal purple (#7C3AED), electric violet (#9333EA), and luminous magenta/cyan flares
 *    that flow and refract directly behind glass surfaces and the floating navigation bar.
 * 2. Midnight Blue: Deep space dark base (#050816) with radiant sapphire azure (#2563EB)
 *    and glowing electric cyan (#06B6D4) mesh orbs.
 * 3. AMOLED Pitch: 100% Pure OLED Black (#000000) with no mesh.
 *
 * Performance:
 * - Animated values are read exclusively within DrawScope (drawBehind),
 *   bypassing Composition and Layout passes to guarantee 60-120 FPS rendering.
 * - Lifecycle-aware: pauses animation updates when stopped or backgrounded.
 * - Keyed by animation speed to immediately update cycle duration without app restart.
 * - Static gradient mode when mesh motion is disabled, consuming 0% CPU.
 */
@Composable
fun GeminiAnimatedBackground(
    modifier: Modifier = Modifier,
    themeMode: ThemeMode = ThemeMode.AURORA_PURPLE,
    animationEnabled: Boolean = true,
    animationSpeed: Float = 1.0f,
    hazeState: HazeState? = LocalHazeState.current,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var isResumed by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isResumed = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> isResumed = false
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Explicit and perceivable cycle durations:
    // Slow (Eco): 20,000ms
    // Normal: 8,000ms
    // Dynamic: 3,500ms
    val cycleDurationMs = when {
        animationSpeed <= 0.65f -> 20_000
        animationSpeed >= 1.35f -> 3_500
        else -> 8_000
    }

    // Only instantiate and run InfiniteTransition when animation is enabled AND screen is resumed
    val phaseState: State<Float>? = if (animationEnabled && isResumed) {
        key(animationSpeed) {
            val infiniteTransition = rememberInfiniteTransition(label = "auroraMeshClock")
            infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = (2 * PI).toFloat(),
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = cycleDurationMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "phase"
            )
        }
    } else {
        null
    }

    val targetGroundColor = when (themeMode) {
        ThemeMode.AURORA_PURPLE -> Color(0xFF0D0920)
        ThemeMode.MIDNIGHT_BLUE -> Color(0xFF060C1E)
        ThemeMode.AMOLED_PITCH -> Color(0xFF000000)
    }

    // Smooth 350ms theme switch transition
    val groundColorState = animateColorAsState(
        targetValue = targetGroundColor,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "groundColorTransition"
    )

    Box(modifier = modifier.fillMaxSize()) {
        val hazeSourceModifier = if (hazeState != null) {
            Modifier.haze(hazeState)
        } else {
            Modifier
        }

        // Sibling background canvas registered as Haze blur source
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val width = size.width
                    val height = size.height
                    if (width <= 0f || height <= 0f) return@drawBehind

                    // 1. Fill ground floor
                    drawRect(color = groundColorState.value)

                    // Pure black for AMOLED Pitch
                    if (themeMode == ThemeMode.AMOLED_PITCH) {
                        return@drawBehind
                    }

                    // 2. If animation is disabled, render static gradient brush with 0% CPU consumption
                    if (!animationEnabled) {
                        drawStaticMesh(themeMode, width, height)
                        return@drawBehind
                    }

                    // 3. Read animated phase exclusively in DrawScope -> ZERO Recompositions
                    val animatedPhase = phaseState?.value ?: 0.85f
                    drawAnimatedMesh(themeMode, width, height, animatedPhase)
                }
                .then(hazeSourceModifier)
        )

        // Sibling foreground content (Pager, floating liquid glass bars, cards)
        content()
    }
}

private fun DrawScope.drawStaticMesh(themeMode: ThemeMode, width: Float, height: Float) {
    when (themeMode) {
        ThemeMode.AURORA_PURPLE -> {
            val meshRadius = width * 0.85f

            // Top-left luminous orb
            val orb1X = width * 0.30f
            val orb1Y = height * 0.22f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF9333EA).copy(alpha = 0.65f),
                        Color(0xFF6B21A8).copy(alpha = 0.30f),
                        Color.Transparent
                    ),
                    center = Offset(orb1X, orb1Y),
                    radius = meshRadius
                ),
                radius = meshRadius,
                center = Offset(orb1X, orb1Y)
            )

            // Bottom-center ambient glow (directly behind the floating glass bar)
            val orb2X = width * 0.50f
            val orb2Y = height * 0.88f
            val navRadius = width * 0.90f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF7C3AED).copy(alpha = 0.65f),
                        Color(0xFF4C1D95).copy(alpha = 0.30f),
                        Color.Transparent
                    ),
                    center = Offset(orb2X, orb2Y),
                    radius = navRadius
                ),
                radius = navRadius,
                center = Offset(orb2X, orb2Y)
            )

            // Right-side warm highlight
            val orb3X = width * 0.75f
            val orb3Y = height * 0.52f
            val ambientRadius = width * 0.70f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFC026D3).copy(alpha = 0.50f),
                        Color(0xFF38BDF8).copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = Offset(orb3X, orb3Y),
                    radius = ambientRadius
                ),
                radius = ambientRadius,
                center = Offset(orb3X, orb3Y)
            )
        }

        ThemeMode.MIDNIGHT_BLUE -> {
            val localizedRadius = width * 0.85f
            val orb1X = width * 0.75f
            val orb1Y = height * 0.20f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2563EB).copy(alpha = 0.65f),
                        Color(0xFF1E40AF).copy(alpha = 0.30f),
                        Color.Transparent
                    ),
                    center = Offset(orb1X, orb1Y),
                    radius = localizedRadius
                ),
                radius = localizedRadius,
                center = Offset(orb1X, orb1Y)
            )

            // Bottom glow behind navigation bar
            val orb2X = width * 0.45f
            val orb2Y = height * 0.88f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0284C7).copy(alpha = 0.65f),
                        Color(0xFF0369A1).copy(alpha = 0.30f),
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

private fun DrawScope.drawAnimatedMesh(
    themeMode: ThemeMode,
    width: Float,
    height: Float,
    animatedPhase: Float
) {
    when (themeMode) {
        ThemeMode.AURORA_PURPLE -> {
            val meshRadius = width * 0.85f

            // Orb 1: Radiant Electric Violet (#9333EA) undulating top-left
            val orb1X = width * (0.28f + 0.14f * sin(animatedPhase))
            val orb1Y = height * (0.20f + 0.10f * cos(animatedPhase * 0.8f))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF9333EA).copy(alpha = 0.65f),
                        Color(0xFF6B21A8).copy(alpha = 0.30f),
                        Color.Transparent
                    ),
                    center = Offset(orb1X, orb1Y),
                    radius = meshRadius
                ),
                radius = meshRadius,
                center = Offset(orb1X, orb1Y)
            )

            // Orb 2: Radiant Royal Purple (#7C3AED) positioned right behind the floating bottom bar
            val orb2X = width * (0.50f + 0.16f * cos(animatedPhase * 1.1f))
            val orb2Y = height * (0.86f + 0.08f * sin(animatedPhase * 0.9f))
            val bottomGlowRadius = width * 0.92f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF7C3AED).copy(alpha = 0.65f),
                        Color(0xFF4C1D95).copy(alpha = 0.30f),
                        Color.Transparent
                    ),
                    center = Offset(orb2X, orb2Y),
                    radius = bottomGlowRadius
                ),
                radius = bottomGlowRadius,
                center = Offset(orb2X, orb2Y)
            )

            // Orb 3: Luminous Magenta / Cyan Accent (#C026D3) shifting mid-screen
            val orb3X = width * (0.72f + 0.15f * sin(animatedPhase * 0.7f + 1.2f))
            val orb3Y = height * (0.50f + 0.12f * cos(animatedPhase * 1.2f))
            val midRadius = width * 0.72f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFC026D3).copy(alpha = 0.50f),
                        Color(0xFF38BDF8).copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = Offset(orb3X, orb3Y),
                    radius = midRadius
                ),
                radius = midRadius,
                center = Offset(orb3X, orb3Y)
            )
        }

        ThemeMode.MIDNIGHT_BLUE -> {
            val localizedRadius = width * 0.85f

            // Radiant Sapphire Blue (#2563EB) in top-right
            val orb1X = width * (0.78f + 0.10f * sin(animatedPhase))
            val orb1Y = height * (0.20f + 0.08f * cos(animatedPhase * 0.8f))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2563EB).copy(alpha = 0.65f),
                        Color(0xFF1E40AF).copy(alpha = 0.30f),
                        Color.Transparent
                    ),
                    center = Offset(orb1X, orb1Y),
                    radius = localizedRadius
                ),
                radius = localizedRadius,
                center = Offset(orb1X, orb1Y)
            )

            // Electric Azure (#0284C7) directly behind bottom navigation bar
            val orb2X = width * (0.45f + 0.14f * cos(animatedPhase * 1.1f))
            val orb2Y = height * (0.86f + 0.08f * sin(animatedPhase * 0.9f))
            val bottomGlowRadius = width * 0.90f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0284C7).copy(alpha = 0.65f),
                        Color(0xFF0369A1).copy(alpha = 0.30f),
                        Color.Transparent
                    ),
                    center = Offset(orb2X, orb2Y),
                    radius = bottomGlowRadius
                ),
                radius = bottomGlowRadius,
                center = Offset(orb2X, orb2Y)
            )
        }

        ThemeMode.AMOLED_PITCH -> {}
    }
}
