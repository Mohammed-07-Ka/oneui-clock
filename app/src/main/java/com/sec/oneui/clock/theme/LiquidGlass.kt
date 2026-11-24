package com.sec.oneui.clock.theme

import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.lerp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop as nativeBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.sec.oneui.clock.settings.GlassEffectMask
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlin.math.roundToInt
import kotlin.math.abs
import kotlin.math.sign

// =========================================================================
// 1. Compatibility & CompositionLocals
// =========================================================================

/**
 * True if device OS supports AGSL RuntimeShader (Android 13+ / API 33+).
 * On older Android versions (API 26..32), the UI automatically falls back
 * to the exact current look without crashing.
 */
val isLiquidGlassSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/**
 * Master boolean toggle for Liquid Glass.
 * Default: true on Android 13+ (API 33+), false on older Android versions.
 * Toggle this single variable to turn Liquid Glass on/off across the app.
 */
var isLiquidGlassEnabled: Boolean = isLiquidGlassSupported

/**
 * Global CompositionLocal for Liquid Glass. Defaults to [isLiquidGlassEnabled].
 */
val LocalLiquidGlassEnabled = staticCompositionLocalOf { isLiquidGlassEnabled }

/**
 * Backdrop layer typealias matching SimpMusic.
 */
typealias PlatformBackdrop = Backdrop

/**
 * CompositionLocal providing ambient PlatformBackdrop across composables.
 */
val LocalBackdrop = staticCompositionLocalOf<PlatformBackdrop?> { null }

/**
 * CompositionLocal providing ambient HazeState for other screens / cards.
 */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

/**
 * Modifier extension to mark a composable as the Haze blur source (legacy).
 */
fun Modifier.hazeSource(hazeState: HazeState): Modifier = this.haze(hazeState)

// =========================================================================
// 2. Liquid Glass Primitives (Ported 1:1 from SimpMusic)
// =========================================================================

/**
 * Marks a composable as the source layer that sibling glass surfaces sample and refract.
 * Replicates SimpMusic's LiquidGlass.kt:33
 */
fun Modifier.layerBackdrop(backdrop: LayerBackdrop): Modifier = this.nativeBackdrop(backdrop)

/**
 * Creates and remembers a LayerBackdrop with a solid/fallback color base.
 * Replicates SimpMusic's LiquidGlass.kt:36-40
 */
@Composable
fun rememberBackdrop(color: Color = Color.Transparent): LayerBackdrop =
    rememberLayerBackdrop {
        drawRect(color)
        drawContent()
    }

/**
 * Press/hold state holder for a single liquid-glass surface.
 * Replicates SimpMusic's LiquidGlassContainer.kt:206-238
 */
class GlassInteraction(
    private val animationScope: CoroutineScope,
) {
    private val pressSpec = spring<Float>(dampingRatio = 0.5f, stiffness = 300f, visibilityThreshold = 0.001f)
    private val pressAnimation = Animatable(0f, 0.001f)

    /** 0f at rest, animating to 1f while pressed. Read in draw/effect/layer blocks. */
    val pressProgress: Float get() = pressAnimation.value

    /** Local-space touch point used as the centre of the press glow. */
    var touchPosition by mutableStateOf(Offset.Zero)
        private set

    suspend fun detectPress(pointer: PointerInputScope) =
        with(pointer) {
            inspectDragGestures(
                onDragStart = { down ->
                    touchPosition = down.position
                    animationScope.launch { pressAnimation.animateTo(1f, pressSpec) }
                },
                onDragEnd = { animationScope.launch { pressAnimation.animateTo(0f, pressSpec) } },
                onDragCancel = { animationScope.launch { pressAnimation.animateTo(0f, pressSpec) } },
            ) { change, _ ->
                touchPosition = change.position
            }
        }
}

@Composable
fun rememberGlassInteraction(): GlassInteraction {
    val scope = rememberCoroutineScope()
    return remember(scope) { GlassInteraction(scope) }
}

private val DefaultBackdropShadow: () -> Shadow = { Shadow.Default }
private val EmptyBackdropEffects: BackdropEffectScope.() -> Unit = {}

/**
 * Draws the interactive liquid glass effect matching SimpMusic 1:1.
 * Replicates SimpMusic's LiquidGlassContainer.kt:255-346
 */
fun Modifier.drawInteractiveGlass(
    isDark: Boolean,
    backdrop: PlatformBackdrop,
    layer: GraphicsLayer?,
    luminance: () -> Float,
    shape: Shape,
    interaction: GlassInteraction?,
    pressedScale: Float = 1.12f,
    highlight: Highlight = Highlight.Default,
    blurScale: Float = 1f,
    minScrim: Float = 0.12f,
    maxScrim: Float = 0.5f,
    scrimColor: Color? = null,
    enableShadow: Boolean = true,
    enableEffects: Boolean = true,
    effectMask: GlassEffectMask = GlassEffectMask.None,
): Modifier =
    this
        .drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            highlight = if (effectMask.noHighlight) null else ({ highlight }),
            shadow = if (enableShadow) DefaultBackdropShadow else null,
            effects = if (enableEffects) {
                {
                    val l = (luminance() * 2f - 1f).let { sign(it) * it * it }
                    val press = interaction?.pressProgress ?: 0f
                    if (!effectMask.noColor) {
                        vibrancy()
                        colorControls(
                            brightness = 0.05f,
                            contrast = 1f,
                            saturation = 1.5f,
                        )
                    }
                    if (!effectMask.noBlur) {
                        blur(
                            (
                                if (l > 0f) {
                                    lerp(8f.dp.toPx(), 16f.dp.toPx(), l)
                                } else {
                                    lerp(8f.dp.toPx(), 2f.dp.toPx(), -l)
                                }
                            ) * blurScale + 2f.dp.toPx() * press,
                        )
                    }
                    if (!effectMask.noLens && size.isSpecified && shape is CornerBasedShape) {
                        lens(
                            refractionHeight = size.minDimension / 4f + 2f.dp.toPx() * press,
                            refractionAmount = size.minDimension / 2f,
                            depthEffect = false,
                            chromaticAberration = false
                        )
                    }
                }
            } else {
                EmptyBackdropEffects
            },
            onDrawBackdrop = { drawBackdrop ->
                drawBackdrop()
                layer?.record { drawBackdrop() }
            },
            onDrawSurface = {
                if (!effectMask.noScrim) {
                    val darken = lerp(minScrim, maxScrim, ((luminance() - 0.3f) / 0.5f).coerceIn(0f, 1f))
                    val baseColor = scrimColor ?: (if (isDark) Color.Black else Color.White)
                    drawRect(baseColor.copy(alpha = darken))
                }
                val press = interaction?.pressProgress ?: 0f
                if (press > 0f) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.18f * press),
                                Color.Transparent,
                            ),
                            center = interaction?.touchPosition ?: Offset(size.width / 2f, size.height / 2f),
                            radius = size.minDimension * 1.2f,
                        ),
                        blendMode = BlendMode.Plus,
                    )
                }
            },
            layerBlock = if (interaction != null) {
                {
                    val scale = lerp(1f, pressedScale, interaction.pressProgress)
                    scaleX = scale
                    scaleY = scale
                }
            } else {
                null
            },
        ).then(
            if (interaction != null) {
                Modifier.pointerInput(interaction) { interaction.detectPress(this) }
            } else {
                Modifier
            },
        )

/**
 * Observe-only drag/press recogniser ported from Kyant's catalog.
 * Replicates SimpMusic's LiquidGlassContainer.kt:353-420
 */
internal suspend fun PointerInputScope.inspectDragGestures(
    onDragStart: (down: PointerInputChange) -> Unit = {},
    onDragEnd: (change: PointerInputChange) -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val down = awaitFirstDown(requireUnconsumed = false)
        onDragStart(down)
        onDrag(down, Offset.Zero)
        val upEvent = drag(
            pointerId = down.id,
            onDrag = { onDrag(it, it.positionChange()) },
        )
        if (upEvent == null) {
            onDragCancel()
        } else {
            onDragEnd(upEvent)
        }
    }
}

private suspend inline fun AwaitPointerEventScope.drag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit,
): PointerInputChange? {
    val isPointerUp = currentEvent.changes.fastFirstOrNull { it.id == pointerId }?.pressed != true
    if (isPointerUp) return null
    var pointer = pointerId
    while (true) {
        val change = awaitDragOrUp(pointer) ?: return null
        if (change.isConsumed) return null
        if (change.changedToUpIgnoreConsumed()) return change
        onDrag(change)
        pointer = change.id
    }
}

private suspend inline fun AwaitPointerEventScope.awaitDragOrUp(pointerId: PointerId): PointerInputChange? {
    var pointer = pointerId
    while (true) {
        val event = awaitPointerEvent()
        val dragEvent = event.changes.fastFirstOrNull { it.id == pointer } ?: return null
        if (dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = event.changes.fastFirstOrNull { it.pressed }
            if (otherDown == null) {
                return dragEvent
            } else {
                pointer = otherDown.id
            }
        } else {
            val hasDragged = dragEvent.previousPosition != dragEvent.position
            if (hasDragged) return dragEvent
        }
    }
}

// =========================================================================
// 3. Tab Bar Physics & Motion (Ported 1:1 from SimpMusic)
// =========================================================================

/**
 * Spring-damped 1-D drag animation for sliding liquid tab indicator.
 * Replicates SimpMusic's LiquidGlassTabBar.android.kt:373-477
 */
class DampedDragAnimation(
    private val animationScope: CoroutineScope,
    val initialValue: Float,
    val valueRange: ClosedRange<Float>,
    val visibilityThreshold: Float,
    val initialScale: Float,
    val pressedScale: Float,
    val onDragStarted: DampedDragAnimation.(position: Offset) -> Unit,
    val onDragStopped: DampedDragAnimation.() -> Unit,
    val onDrag: DampedDragAnimation.(size: IntSize, dragAmount: Offset) -> Unit,
) {
    private val valueAnimationSpec = spring(1f, 1000f, visibilityThreshold)
    private val velocityAnimationSpec = spring(0.5f, 300f, visibilityThreshold * 10f)
    private val pressProgressAnimationSpec = spring(1f, 1000f, 0.001f)
    private val scaleXAnimationSpec = spring(0.6f, 250f, 0.001f)
    private val scaleYAnimationSpec = spring(0.7f, 250f, 0.001f)

    private val valueAnimation = Animatable(initialValue, visibilityThreshold)
    private val velocityAnimation = Animatable(0f, 5f)
    private val pressProgressAnimation = Animatable(0f, 0.001f)
    private val scaleXAnimation = Animatable(initialScale, 0.001f)
    private val scaleYAnimation = Animatable(initialScale, 0.001f)

    private val mutatorMutex = MutatorMutex()
    private val velocityTracker = VelocityTracker()

    val value: Float get() = valueAnimation.value
    val targetValue: Float get() = valueAnimation.targetValue
    val pressProgress: Float get() = pressProgressAnimation.value
    val scaleX: Float get() = scaleXAnimation.value
    val scaleY: Float get() = scaleYAnimation.value
    val velocity: Float get() = velocityAnimation.value

    val modifier: Modifier =
        Modifier.pointerInput(Unit) {
            inspectDragGestures(
                onDragStart = { down ->
                    onDragStarted(down.position)
                    press()
                },
                onDragEnd = {
                    onDragStopped()
                    release()
                },
                onDragCancel = {
                    onDragStopped()
                    release()
                },
            ) { _, dragAmount ->
                onDrag(size, dragAmount)
            }
        }

    fun press() {
        velocityTracker.resetTracking()
        animationScope.launch {
            launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(pressedScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(pressedScale, scaleYAnimationSpec) }
        }
    }

    fun release() {
        animationScope.launch {
            withFrameNanos {}
            if (value != targetValue) {
                val threshold = (valueRange.endInclusive - valueRange.start) * 0.025f
                snapshotFlow { valueAnimation.value }
                    .filter { abs(it - valueAnimation.targetValue) < threshold }
                    .first()
            }
            launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(initialScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(initialScale, scaleYAnimationSpec) }
        }
    }

    fun updateValue(value: Float) {
        val target = value.coerceIn(valueRange.start, valueRange.endInclusive)
        animationScope.launch {
            valueAnimation.animateTo(target, valueAnimationSpec) { updateVelocity() }
        }
    }

    fun animateToValue(value: Float) {
        animationScope.launch {
            mutatorMutex.mutate {
                press()
                val target = value.coerceIn(valueRange.start, valueRange.endInclusive)
                launch { valueAnimation.animateTo(target, valueAnimationSpec) }
                if (velocity != 0f) {
                    launch { velocityAnimation.animateTo(0f, velocityAnimationSpec) }
                }
                release()
            }
        }
    }

    private fun updateVelocity() {
        velocityTracker.addPosition(SystemClock.uptimeMillis(), Offset(value, 0f))
        val targetVelocity =
            velocityTracker.calculateVelocity().x / (valueRange.endInclusive - valueRange.start)
        animationScope.launch { velocityAnimation.animateTo(targetVelocity, velocityAnimationSpec) }
    }
}

val CapsuleShape = RoundedCornerShape(percent = 50)
val BarHeight = 64.dp
val BlobHeight = 56.dp
val BarInset = 6.dp

/**
 * Authentic Liquid Glass Tab Bar replicated 1:1 from SimpMusic.
 * Features:
 * - Refractive, luminance-adaptive outer glass capsule (`drawInteractiveGlass`)
 * - Frosted glass blob indicator with heavy blur (+20dp), chromatic aberration lens,
 *   specular highlight, inner shadow, and velocity-damped spring squash/stretch.
 * - Crisp content on top with observe-only touch interaction.
 */
@Composable
fun LiquidGlassTabBar(
    tabCount: Int,
    selectedTab: Int,
    backdrop: PlatformBackdrop,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    tabContent: @Composable (position: Int, isSelected: Boolean) -> Unit
) {
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()
    val barInteraction = rememberGlassInteraction()
    val layer = rememberGraphicsLayer()
    val luminance = rememberGlassLuminance(layer, enabled = isLiquidGlassEnabled)

    var currentIndex by remember { mutableIntStateOf(selectedTab.coerceAtLeast(0)) }
    val draggedFlag = remember { booleanArrayOf(false) }

    val dampedDrag = remember(animationScope, tabCount) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedTab.coerceAtLeast(0).toFloat(),
            valueRange = 0f..(tabCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 76f / 56f,
            onDragStarted = { draggedFlag[0] = false },
            onDragStopped = {
                if (draggedFlag[0]) {
                    val target = targetValue.roundToInt().coerceIn(0, tabCount - 1)
                    currentIndex = target
                    animateToValue(target.toFloat())
                }
            },
            onDrag = { size, dragAmount ->
                if (dragAmount.x != 0f) draggedFlag[0] = true
                val widthPx = (size.width.toFloat() - with(density) { BarInset.toPx() } * 2) / tabCount
                if (widthPx > 0f) {
                    updateValue(
                        (targetValue + dragAmount.x / widthPx * if (isLtr) 1f else -1f)
                            .coerceIn(0f, (tabCount - 1).toFloat())
                    )
                }
            }
        )
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab >= 0 && currentIndex != selectedTab) {
            currentIndex = selectedTab
            dampedDrag.animateToValue(selectedTab.toFloat())
        }
    }

    LaunchedEffect(dampedDrag) {
        snapshotFlow { currentIndex }
            .drop(1)
            .collectLatest { index ->
                dampedDrag.animateToValue(index.toFloat())
                if (draggedFlag[0]) onTabSelected(index)
            }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(BarHeight)
            .pointerInput(barInteraction) { barInteraction.detectPress(this) },
        contentAlignment = Alignment.CenterStart
    ) {
        val totalWidth = maxWidth
        val tabWidth = (totalWidth - BarInset * 2) / tabCount
        val tabWidthPx = with(density) { tabWidth.toPx() }

        // 1) Dark frosted glass capsule (matching SimpMusic drawInteractiveGlass)
        Box(
            Modifier
                .matchParentSize()
                .drawInteractiveGlass(
                    isDark = isDark,
                    backdrop = backdrop,
                    layer = layer,
                    luminance = { luminance.value },
                    shape = CapsuleShape,
                    interaction = barInteraction,
                    pressedScale = 1.04f
                )
        )

        // 2) Frosted blob selection indicator — slides behind the icons
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .graphicsLayer {
                    translationX =
                        (if (isLtr) dampedDrag.value else (tabCount - 1) - dampedDrag.value) * tabWidthPx +
                        BarInset.toPx()
                }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { CapsuleShape },
                    effects = {
                        val l = (luminance.value * 2f - 1f).let { sign(it) * it * it }
                        val progress = dampedDrag.pressProgress
                        vibrancy()
                        colorControls(
                            brightness = 0.05f,
                            contrast = 1f,
                            saturation = 1.5f,
                        )
                        blur(
                            (if (l > 0f) lerp(8f.dp.toPx(), 16f.dp.toPx(), l) else lerp(8f.dp.toPx(), 2f.dp.toPx(), -l)) +
                                20f.dp.toPx(),
                        )
                        if (size.isSpecified && shape is CornerBasedShape) {
                            lens(
                                refractionHeight = 10f.dp.toPx() * progress,
                                refractionAmount = 14f.dp.toPx() * progress,
                                depthEffect = false,
                                chromaticAberration = true
                            )
                        }
                    },
                    highlight = { Highlight.Default.copy(alpha = 0.6f) },
                    shadow = { Shadow(radius = 4f.dp, alpha = 0.4f) },
                    innerShadow = {
                        val progress = dampedDrag.pressProgress
                        InnerShadow(radius = 8f.dp * progress, alpha = progress)
                    },
                    layerBlock = {
                        scaleX = dampedDrag.scaleX
                        scaleY = dampedDrag.scaleY
                        val velocity = dampedDrag.velocity / 10f
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val lumNorm = ((luminance.value - 0.3f) / 0.5f).coerceIn(0f, 1f)
                        val darken =
                            if (isDark) lerp(0.22f, 0.55f, lumNorm) else lerp(0.06f, 0.14f, lumNorm)
                        drawRect(Color.Black.copy(alpha = darken))
                    },
                )
                .width(tabWidth)
                .height(BlobHeight)
        )

        // 3) Crisp icons + labels on top, carrying the blob drag gesture
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = BarInset)
                .then(dampedDrag.modifier),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until tabCount) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CapsuleShape)
                        .clickable(
                            interactionSource = null,
                            indication = null,
                            role = Role.Tab,
                            onClick = {
                                if (i == currentIndex) {
                                    onTabSelected(i)
                                } else {
                                    currentIndex = i
                                    onTabSelected(i)
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    tabContent(i, currentIndex == i)
                }
            }
        }
    }
}

// =========================================================================
// 4. Background Luminance Sampling (Ported 1:1 from SimpMusic)
// =========================================================================

private const val SAMPLE_SIZE = 5
private const val SAMPLE_INTERVAL_MS = 2_500L
private const val LUMINANCE_CHANGE_THRESHOLD = 0.02f

/**
 * Samples a small GPU-rendered copy to dynamically adapt the glass darkening.
 * Replicates SimpMusic's GlassLuminance.kt:31-86
 */
@Composable
fun rememberGlassLuminance(
    source: GraphicsLayer,
    enabled: Boolean = true,
): State<Float> {
    val sample = rememberGraphicsLayer()
    val luminance = remember { Animatable(0f) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    LaunchedEffect(source, sample, enabled, lifecycle, density, layoutDirection) {
        if (!enabled) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val pixels = IntArray(SAMPLE_SIZE * SAMPLE_SIZE)
            while (isActive) {
                val sourceSize = source.size
                if (sourceSize.width > 0 && sourceSize.height > 0) {
                    val target =
                        try {
                            sample.record(density, layoutDirection, IntSize(SAMPLE_SIZE, SAMPLE_SIZE)) {
                                scale(
                                    scaleX = SAMPLE_SIZE.toFloat() / sourceSize.width,
                                    scaleY = SAMPLE_SIZE.toFloat() / sourceSize.height,
                                    pivot = Offset.Zero,
                                ) {
                                    drawLayer(source)
                                }
                            }
                            sample.toImageBitmap().readPixels(pixels)
                            val average =
                                pixels.sumOf { color ->
                                    val r = (color shr 16 and 0xFF) / 255.0
                                    val g = (color shr 8 and 0xFF) / 255.0
                                    val b = (color and 0xFF) / 255.0
                                    0.2126 * r + 0.7152 * g + 0.0722 * b
                                } / pixels.size
                            average.toFloat().coerceIn(0.3f, 0.8f)
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            Log.e("GlassLuminance", "Unable to sample glass background", error)
                            return@repeatOnLifecycle
                        }
                    if (abs(target - luminance.targetValue) >= LUMINANCE_CHANGE_THRESHOLD) {
                        luminance.animateTo(target, tween(500))
                    }
                }
                delay(SAMPLE_INTERVAL_MS)
            }
        }
    }
    return luminance.asState()
}

// =========================================================================
// 5. Preserved Fallbacks & Card Modifiers
// =========================================================================

/**
 * Fallback Liquid Glass for API < 33 or when liquid glass is disabled.
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
 * Fallback Active Pill indicator for API < 33.
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
 * Frosted glass card for Alarm items in lists (unchanged).
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
