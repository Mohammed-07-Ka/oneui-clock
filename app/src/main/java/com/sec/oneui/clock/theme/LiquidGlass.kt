package com.sec.oneui.clock.theme

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
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
 * Applies the authentic Liquid Glass visual effect matching SimpMusic:
 * - Elevated floating drop shadow
 * - Translucent milky frosted glass substrate
 * - Internal top-edge curved lens refraction glow
 * - AGSL SDF Specular Highlight on API 33+ (surface normal & light reflection angle)
 * - Multi-stop specular fallback on API < 33
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(26.dp),
    cornerRadius: Dp = 26.dp,
    tintColor: Color? = null,
    borderWidth: Dp = 1.dp,
    angleDegrees: Float = 135f,
    elevation: Dp = 0.dp
): Modifier = composed {
    val palette = ClockTheme.palette
    val density = LocalDensity.current
    val cornerRadiusPx = with(density) { cornerRadius.toPx() }
    val borderWidthPx = with(density) { borderWidth.toPx() }

    // Authentic translucent frosted glass substrate:
    // Translucent dark glass with luminous violet/indigo tint
    val glassSubstrate = tintColor ?: if (palette.isBlueTheme) {
        Color(0x550F172A)
    } else {
        Color(0x521A152E)
    }

    val highlightColor = if (palette.isBlueTheme) Color(0xFFBAE6FD) else Color.White

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

    val shadowModifier = if (elevation > 0.dp) {
        Modifier.shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.40f),
            spotColor = Color.Black.copy(alpha = 0.65f)
        )
    } else Modifier

    this
        .then(shadowModifier)
        .clip(shape)
        .background(glassSubstrate)
        .drawWithCache {
            val width = size.width
            val height = size.height

            // 1. Frosted surface sheen gradient (milky top-to-bottom light falloff)
            val surfaceSheenBrush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.16f),
                    Color.White.copy(alpha = 0.04f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = height * 0.75f
            )

            // 2. Top-edge curved internal lens refraction glow
            val lensRefractionBrush = Brush.verticalGradient(
                colors = listOf(
                    highlightColor.copy(alpha = 0.32f),
                    highlightColor.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = minOf(height * 0.50f, cornerRadiusPx * 2f)
            )

            // 3. Fallback specular rim brush
            val fallbackSpecularBrush = Brush.linearGradient(
                colors = listOf(
                    highlightColor.copy(alpha = 0.75f),
                    highlightColor.copy(alpha = 0.25f),
                    Color.Transparent,
                    highlightColor.copy(alpha = 0.35f)
                ),
                start = Offset(0f, 0f),
                end = Offset(width, height)
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && runtimeShader != null) {
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
                runtimeShader.setFloatUniform("falloff", 1.2f)
                runtimeShader.setColorUniform(
                    "color",
                    android.graphics.Color.valueOf(
                        highlightColor.red,
                        highlightColor.green,
                        highlightColor.blue,
                        0.75f
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
                    // Draw milky surface sheen over background
                    drawRect(surfaceSheenBrush)

                    // Draw inner lens curvature reflection
                    drawRoundRect(
                        brush = lensRefractionBrush,
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        size = Size(width, minOf(height * 0.50f, cornerRadiusPx * 2f))
                    )

                    // Draw child content
                    drawContent()

                    // Draw AGSL Specular Highlight along outer border
                    drawPath(
                        path = roundRectPath,
                        brush = shaderBrush,
                        style = Stroke(width = borderWidthPx)
                    )
                }
            } else {
                onDrawWithContent {
                    drawRect(surfaceSheenBrush)

                    drawRoundRect(
                        brush = lensRefractionBrush,
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        size = Size(width, minOf(height * 0.50f, cornerRadiusPx * 2f))
                    )

                    drawContent()

                    drawRoundRect(
                        brush = fallbackSpecularBrush,
                        size = size,
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        style = Stroke(width = borderWidthPx)
                    )
                }
            }
        }
}

/**
 * Replicates the illuminated, glowing liquid glass pill indicator from SimpMusic ("Home" tab).
 * Features glowing translucent glass substrate, high-contrast specular borders, and soft illumination.
 */
fun Modifier.liquidGlassActivePill(
    shape: Shape = RoundedCornerShape(26.dp),
    cornerRadius: Dp = 26.dp,
    glowColor: Color? = null
): Modifier = composed {
    val palette = ClockTheme.palette
    val density = LocalDensity.current
    val cornerRadiusPx = with(density) { cornerRadius.toPx() }
    val tint = glowColor ?: if (palette.isBlueTheme) Color(0xFF38BDF8) else Color(0xFFA855F7)

    this
        .shadow(
            elevation = 6.dp,
            shape = shape,
            clip = false,
            ambientColor = tint.copy(alpha = 0.35f),
            spotColor = tint.copy(alpha = 0.55f)
        )
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.28f),
                    Color.White.copy(alpha = 0.14f),
                    tint.copy(alpha = 0.25f)
                )
            )
        )
        .drawWithCache {
            val width = size.width
            val height = size.height

            val rimBrush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.85f),
                    Color.White.copy(alpha = 0.30f),
                    tint.copy(alpha = 0.40f)
                )
            )

            val innerGlowBrush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.40f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = height * 0.55f
            )

            onDrawWithContent {
                // Top inner reflection arc
                drawRoundRect(
                    brush = innerGlowBrush,
                    size = Size(width, height * 0.55f),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                )

                drawContent()

                // Specular border
                drawRoundRect(
                    brush = rimBrush,
                    size = size,
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }
}
