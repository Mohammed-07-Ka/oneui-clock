package com.sec.oneui.clock.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * Central motion specifications approximating Apple's documented SwiftUI Spring presets:
 * Spring(duration:bounce:) formula with mass = 1, stiffness = (2 * PI / duration)^2, dampingRatio = 1 - bounce.
 *
 * For duration = 0.5s:
 *   angular frequency omega = 2 * PI / 0.5 = 4 * PI ≈ 12.56637
 *   stiffness = omega^2 ≈ 157.91f
 *
 * Note: These approximate SwiftUI presets (.smooth, .snappy, .bouncy) and are NOT the internal
 * private values of the UIKit system sheet.
 */
object ClockMotion {
    fun <T> smooth(): SpringSpec<T> = spring(
        dampingRatio = 1.0f,
        stiffness = 157.91f
    )

    fun <T> snappy(): SpringSpec<T> = spring(
        dampingRatio = 0.85f,
        stiffness = 157.91f
    )

    fun <T> bouncy(): SpringSpec<T> = spring(
        dampingRatio = 0.70f,
        stiffness = 157.91f
    )

    val Smooth: SpringSpec<Float> = spring(
        dampingRatio = 1.0f,
        stiffness = 157.91f
    )

    val Snappy: SpringSpec<Float> = spring(
        dampingRatio = 0.85f,
        stiffness = 157.91f
    )

    // Defined only, do not use yet
    val Bouncy: SpringSpec<Float> = spring(
        dampingRatio = 0.70f,
        stiffness = 157.91f
    )
}
