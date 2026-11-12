package com.sec.oneui.clock.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * iOS Tactile Press Effect:
 * Scales smoothly to targetScale (0.96f) upon press,
 * and springs back using spring(dampingRatio = 0.75f, stiffness = 600f) within ~250ms.
 */
fun Modifier.iosPressEffect(
    targetScale: Float = 0.96f,
    interactionSource: MutableInteractionSource? = null
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1.0f,
        animationSpec = if (isPressed) {
            tween(durationMillis = 90, easing = FastOutSlowInEasing)
        } else {
            spring(dampingRatio = 0.75f, stiffness = 600f)
        },
        label = "iosPressEffect"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Clickable modifier with built-in iOS spring press scale feedback.
 */
fun Modifier.iosClickable(
    targetScale: Float = 0.96f,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) targetScale else 1.0f,
        animationSpec = if (isPressed) {
            tween(durationMillis = 90, easing = FastOutSlowInEasing)
        } else {
            spring(dampingRatio = 0.75f, stiffness = 600f)
        },
        label = "iosClickableScale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}
