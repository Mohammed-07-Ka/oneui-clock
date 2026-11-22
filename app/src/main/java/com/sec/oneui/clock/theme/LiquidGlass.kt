package com.sec.oneui.clock.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * CompositionLocal providing ambient HazeState across the application.
 */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

/**
 * Modifier extension to mark a composable as the Haze blur source.
 * Apply this to the BACKGROUND element (e.g., GeminiAnimatedBackground Box).
 */
fun Modifier.hazeSource(hazeState: HazeState): Modifier = this.haze(hazeState)

/**
 * Authentic Liquid Glass Capsule matching SimpMusic & Apple iOS 18:
 * - Real hardware background blur via Haze
 * - Elevated soft drop shadow
 * - Clean dark translucent glass substrate ("đục đen")
 * - Delicate, razor-sharp hairline specular border (no muddy or milky layers)
 */
fun Modifier.liquidGlass(
    hazeState: HazeState? = null,
    shape: Shape = CircleShape,
    cornerRadius: Dp = 32.dp,
    tintColor: Color? = null,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 16.dp
): Modifier = composed {
    val activeHaze = hazeState ?: LocalHazeState.current
    val palette = ClockTheme.palette

    // Clean, authentic dark translucent glass substrate
    val glassSubstrate = tintColor ?: if (palette.isBlueTheme) {
        Color(0x380A0E18)
    } else {
        Color(0x350F0D1A)
    }

    val shadowModifier = if (elevation > 0.dp) {
        Modifier.shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.45f),
            spotColor = Color.Black.copy(alpha = 0.70f)
        )
    } else Modifier

    val borderBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.28f),
            Color.White.copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.03f)
        )
    )

    val hazeModifier = if (activeHaze != null) {
        Modifier.hazeChild(
            state = activeHaze,
            style = HazeMaterials.ultraThin()
        )
    } else Modifier

    this
        .then(shadowModifier)
        .clip(shape)
        .then(hazeModifier)
        .background(glassSubstrate)
        .border(width = borderWidth, brush = borderBrush, shape = shape)
}

/**
 * Replicates the illuminated, glowing liquid glass pill indicator from SimpMusic ("Home" tab):
 * - Elevated soft glow shadow
 * - Translucent accent gradient with frosted blur
 * - High-contrast crisp specular border
 */
fun Modifier.liquidGlassActivePill(
    hazeState: HazeState? = null,
    shape: Shape = RoundedCornerShape(28.dp),
    cornerRadius: Dp = 28.dp,
    glowColor: Color? = null
): Modifier = composed {
    val activeHaze = hazeState ?: LocalHazeState.current
    val palette = ClockTheme.palette
    val accent = glowColor ?: if (palette.isBlueTheme) Color(0xFF38BDF8) else Color(0xFFA855F7)

    val shadowModifier = Modifier.shadow(
        elevation = 8.dp,
        shape = shape,
        clip = false,
        ambientColor = accent.copy(alpha = 0.35f),
        spotColor = accent.copy(alpha = 0.55f)
    )

    val borderBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.60f),
            accent.copy(alpha = 0.35f),
            Color.White.copy(alpha = 0.12f)
        )
    )

    val pillBackgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.18f),
            accent.copy(alpha = 0.24f),
            Color.Transparent
        )
    )

    val hazeModifier = if (activeHaze != null) {
        Modifier.hazeChild(
            state = activeHaze,
            style = HazeMaterials.thin()
        )
    } else Modifier

    this
        .then(shadowModifier)
        .clip(shape)
        .then(hazeModifier)
        .background(pillBackgroundBrush)
        .border(width = 1.dp, brush = borderBrush, shape = shape)
}

/**
 * Clean, modern frosted glass card for Alarm items:
 * - Subtle elevation shadow
 * - Real hardware background blur
 * - Crisp, delicate hairline border
 * - Zero muddy or milky overlays
 */
fun Modifier.liquidGlassCard(
    hazeState: HazeState? = null,
    shape: Shape = RoundedCornerShape(24.dp),
    cornerRadius: Dp = 24.dp
): Modifier = composed {
    val activeHaze = hazeState ?: LocalHazeState.current

    val shadowModifier = Modifier.shadow(
        elevation = 4.dp,
        shape = shape,
        clip = false,
        ambientColor = Color.Black.copy(alpha = 0.25f),
        spotColor = Color.Black.copy(alpha = 0.45f)
    )

    val borderBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.16f),
            Color.White.copy(alpha = 0.04f)
        )
    )

    val hazeModifier = if (activeHaze != null) {
        Modifier.hazeChild(
            state = activeHaze,
            style = HazeMaterials.ultraThin()
        )
    } else Modifier

    this
        .then(shadowModifier)
        .clip(shape)
        .then(hazeModifier)
        .background(Color.White.copy(alpha = 0.05f))
        .border(width = 1.dp, brush = borderBrush, shape = shape)
}
