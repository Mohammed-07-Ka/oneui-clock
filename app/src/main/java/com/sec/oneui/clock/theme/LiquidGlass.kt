package com.sec.oneui.clock.theme

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Optical Liquid Glass engine based on SimpMusic / Kyant0 AndroidLiquidGlass.
 * Uses GPU-accelerated AGSL (Android Graphics Shading Language) on Android 13+ (API 33+)
 * with Signed Distance Field (SDF) rounded rectangle normal gradients and specular highlights.
 * Seamlessly falls back to multi-stop specular physics gradients on earlier Android versions.
 */
object LiquidGlassShaders {
    // Exact AGSL Shader from SimpMusic for directional specular rim illumination
    const val SPECULAR_HIGHLIGHT_AGSL = """
        uniform float2 size;
        uniform float4 cornerRadii;
        layout(color) uniform half4 color;
        uniform float angle;
        uniform float falloff;

        float radiusAt(float2 coord, float4 radii) {
            if (coord.x >= 0.0) {
                if (coord.y <= 0.0) return radii.y;
                else return radii.z;
            } else {
                if (coord.y <= 0.0) return radii.x;
                else return radii.w;
            }
        }

        float sdRoundedRect(float2 coord, float2 halfSize, float radius) {
            float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
            float outside = length(max(cornerCoord, 0.0)) - radius;
            float inside = min(max(cornerCoord.x, cornerCoord.y), 0.0);
            return outside + inside;
        }

        float2 gradSdRoundedRect(float2 coord, float2 halfSize, float radius) {
            float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
            if (cornerCoord.x >= 0.0 || cornerCoord.y >= 0.0) {
                return sign(coord) * normalize(max(cornerCoord, 0.0));
            } else {
                float gradX = step(cornerCoord.y, cornerCoord.x);
                return sign(coord) * float2(gradX, 1.0 - gradX);
            }
        }

        half4 main(float2 coord) {
            float2 halfSize = size * 0.5;
            float2 centeredCoord = coord - halfSize;
            float radius = radiusAt(coord, cornerRadii);
            
            float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
            float2 grad = gradSdRoundedRect(centeredCoord, halfSize, gradRadius);
            float2 normal = float2(cos(angle), sin(angle));
            float d = dot(grad, normal);
            float intensity = pow(abs(d), falloff);
            return color * intensity;
        }
    """
}

/**
 * Applies the true Liquid Glass visual effect:
 * - Ultra-translucent glass substrate
 * - AGSL SDF Specular Highlight on API 33+ (calculates true surface normal and light reflection angle)
 * - Multi-stop specular border fallback on API < 33
 * - Top-edge specular inner rim glow for genuine 3D glass refraction depth
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(26.dp),
    cornerRadius: Dp = 26.dp,
    tintColor: Color? = null,
    borderWidth: Dp = 1.dp,
    angleDegrees: Float = 135f // Light arriving from top-left (135 degrees)
): Modifier = composed {
    val palette = ClockTheme.palette
    val density = LocalDensity.current
    val cornerRadiusPx = with(density) { cornerRadius.toPx() }
    val borderWidthPx = with(density) { borderWidth.toPx() }

    val baseGlassColor = tintColor ?: palette.cardBackground
    val highlightColor = if (palette.isBlueTheme) Color(0xFF93C5FD) else Color.White

    // Cache the RuntimeShader on API 33+ to avoid recompilation
    val runtimeShader = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        remember {
            try {
                RuntimeShader(LiquidGlassShaders.SPECULAR_HIGHLIGHT_AGSL)
            } catch (e: Exception) {
                null
            }
        }
    } else null

    this
        .clip(shape)
        .background(baseGlassColor)
        .drawWithCache {
            val width = size.width
            val height = size.height

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && runtimeShader != null) {
                // Configure AGSL uniforms for current component dimensions
                runtimeShader.setFloatUniform("size", width, height)
                runtimeShader.setFloatUniform(
                    "cornerRadii",
                    cornerRadiusPx,
                    cornerRadiusPx,
                    cornerRadiusPx,
                    cornerRadiusPx
                )
                val angleRad = Math.toRadians(angleDegrees.toDouble()).toFloat()
                runtimeShader.setFloatUniform("angle", angleRad)
                runtimeShader.setFloatUniform("falloff", 1.4f)
                runtimeShader.setColorUniform(
                    "color",
                    android.graphics.Color.valueOf(
                        highlightColor.red,
                        highlightColor.green,
                        highlightColor.blue,
                        0.50f
                    )
                )

                val shaderBrush = ShaderBrush(runtimeShader)

                val roundRectPath = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = androidx.compose.ui.geometry.Rect(0f, 0f, width, height),
                            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                        )
                    )
                }

                onDrawWithContent {
                    // 1. Draw child content
                    drawContent()

                    // 2. Draw AGSL Specular Highlight along the outer border
                    drawPath(
                        path = roundRectPath,
                        brush = shaderBrush,
                        style = Stroke(width = borderWidthPx)
                    )

                    // 3. Draw soft inner top glass refraction glow
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                highlightColor.copy(alpha = 0.16f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = minOf(height * 0.45f, cornerRadiusPx * 2f)
                        ),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        size = Size(width, minOf(height * 0.45f, cornerRadiusPx * 2f))
                    )
                }
            } else {
                // Graceful fallback for API < 33: Multi-stop linear specular border + inner refraction glow
                val fallbackSpecularBrush = Brush.linearGradient(
                    colors = listOf(
                        highlightColor.copy(alpha = 0.38f),
                        highlightColor.copy(alpha = 0.10f),
                        Color.Transparent,
                        highlightColor.copy(alpha = 0.15f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(width, height)
                )

                onDrawWithContent {
                    // 1. Draw child content
                    drawContent()

                    // 2. Draw fallback specular border
                    drawRoundRect(
                        brush = fallbackSpecularBrush,
                        size = size,
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        style = Stroke(width = borderWidthPx)
                    )

                    // 3. Draw soft inner top refraction glow
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                highlightColor.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = minOf(height * 0.4f, cornerRadiusPx * 1.5f)
                        ),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        size = Size(width, minOf(height * 0.4f, cornerRadiusPx * 1.5f))
                    )
                }
            }
        }
}
