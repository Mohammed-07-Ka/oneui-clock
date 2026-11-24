package com.sec.oneui.clock.settings

import android.content.Context
import android.content.SharedPreferences
import android.view.Choreographer
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Immutable
data class GlassEffectMask(
    val noBlur: Boolean = false,
    val noLens: Boolean = false,
    val noColor: Boolean = false,
    val noHighlight: Boolean = false,
    val noScrim: Boolean = false
) {
    companion object {
        val None = GlassEffectMask()
    }
}

@Immutable
data class BenchOverride(
    val noPageScaleAlpha: Boolean = false,
    val snapTabBarFabGear: Boolean = false,
    val noDimmer: Boolean = false,
    val deferSheetContent: Boolean = false,
    val frameTimingOverlay: Boolean = false,
    val sheetFlatContainer: Boolean = false,
    val sheetNoInnerGlass: Boolean = false,
    val sheetNoEffects: Boolean = false,
    val containerGlassMask: GlassEffectMask = GlassEffectMask.None,
    val innerGlassMask: GlassEffectMask = GlassEffectMask.None
)

object DiagFlags {
    private const val PREFS_NAME = "diag_prefs"
    private var prefs: SharedPreferences? = null

    private var savedNoPageScaleAlpha by mutableStateOf(false)
    private var savedSnapTabBarFabGear by mutableStateOf(false)
    private var savedNoDimmer by mutableStateOf(false)
    private var savedDeferSheetContent by mutableStateOf(false)
    private var savedFrameTimingOverlay by mutableStateOf(false)
    private var savedSheetFlatContainer by mutableStateOf(false)
    private var savedSheetNoInnerGlass by mutableStateOf(false)
    private var savedSheetNoEffects by mutableStateOf(false)

    var benchOverride by mutableStateOf<BenchOverride?>(null)
        private set

    val noPageScaleAlpha: Boolean
        get() = benchOverride?.noPageScaleAlpha ?: savedNoPageScaleAlpha
    val snapTabBarFabGear: Boolean
        get() = benchOverride?.snapTabBarFabGear ?: savedSnapTabBarFabGear
    val noDimmer: Boolean
        get() = benchOverride?.noDimmer ?: savedNoDimmer
    val deferSheetContent: Boolean
        get() = benchOverride?.deferSheetContent ?: savedDeferSheetContent
    val frameTimingOverlay: Boolean
        get() = benchOverride?.frameTimingOverlay ?: savedFrameTimingOverlay
    val sheetFlatContainer: Boolean
        get() = benchOverride?.sheetFlatContainer ?: savedSheetFlatContainer
    val sheetNoInnerGlass: Boolean
        get() = benchOverride?.sheetNoInnerGlass ?: savedSheetNoInnerGlass
    val sheetNoEffects: Boolean
        get() = benchOverride?.sheetNoEffects ?: savedSheetNoEffects

    val sheetContainerMask: GlassEffectMask
        get() = benchOverride?.containerGlassMask ?: GlassEffectMask.None

    val sheetInnerMask: GlassEffectMask
        get() = benchOverride?.innerGlassMask ?: GlassEffectMask.None

    // Frame timing measurements for D5
    var worstFrameTimeMs by mutableFloatStateOf(0f)
        private set
    var framesOver16Ms by mutableIntStateOf(0)
        private set
    var framesOver25Ms by mutableIntStateOf(0)
        private set

    private var lastFrameTimeNanos = 0L
    private var isMonitoring = false

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!isMonitoring) return
            if (lastFrameTimeNanos > 0L) {
                val deltaMs = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000f
                if (deltaMs > worstFrameTimeMs) {
                    worstFrameTimeMs = deltaMs
                }
                if (deltaMs > 16.7f) {
                    framesOver16Ms++
                }
                if (deltaMs > 25.0f) {
                    framesOver25Ms++
                }
            }
            lastFrameTimeNanos = frameTimeNanos
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = p
        savedNoPageScaleAlpha = p.getBoolean("d1_no_page_scale_alpha", false)
        savedSnapTabBarFabGear = p.getBoolean("d2_snap_tab_bar_fab_gear", false)
        savedNoDimmer = p.getBoolean("d3_no_dimmer", false)
        savedDeferSheetContent = p.getBoolean("d4_defer_sheet_content", false)
        savedFrameTimingOverlay = p.getBoolean("d5_frame_timing_overlay", false)
        savedSheetFlatContainer = p.getBoolean("d6_sheet_flat_container", false)
        savedSheetNoInnerGlass = p.getBoolean("d7_sheet_no_inner_glass", false)
        savedSheetNoEffects = p.getBoolean("d8_sheet_no_effects", false)
    }

    fun updateBenchOverride(override: BenchOverride?) {
        benchOverride = override
    }

    fun updateNoPageScaleAlpha(enabled: Boolean) {
        savedNoPageScaleAlpha = enabled
        prefs?.edit()?.putBoolean("d1_no_page_scale_alpha", enabled)?.apply()
    }

    fun updateSnapTabBarFabGear(enabled: Boolean) {
        savedSnapTabBarFabGear = enabled
        prefs?.edit()?.putBoolean("d2_snap_tab_bar_fab_gear", enabled)?.apply()
    }

    fun updateNoDimmer(enabled: Boolean) {
        savedNoDimmer = enabled
        prefs?.edit()?.putBoolean("d3_no_dimmer", enabled)?.apply()
    }

    fun updateDeferSheetContent(enabled: Boolean) {
        savedDeferSheetContent = enabled
        prefs?.edit()?.putBoolean("d4_defer_sheet_content", enabled)?.apply()
    }

    fun updateFrameTimingOverlay(enabled: Boolean) {
        savedFrameTimingOverlay = enabled
        prefs?.edit()?.putBoolean("d5_frame_timing_overlay", enabled)?.apply()
    }

    fun updateSheetFlatContainer(enabled: Boolean) {
        savedSheetFlatContainer = enabled
        prefs?.edit()?.putBoolean("d6_sheet_flat_container", enabled)?.apply()
    }

    fun updateSheetNoInnerGlass(enabled: Boolean) {
        savedSheetNoInnerGlass = enabled
        prefs?.edit()?.putBoolean("d7_sheet_no_inner_glass", enabled)?.apply()
    }

    fun updateSheetNoEffects(enabled: Boolean) {
        savedSheetNoEffects = enabled
        prefs?.edit()?.putBoolean("d8_sheet_no_effects", enabled)?.apply()
    }

    fun startTimingSession() {
        worstFrameTimeMs = 0f
        framesOver16Ms = 0
        framesOver25Ms = 0
        lastFrameTimeNanos = 0L
        isMonitoring = true
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    fun stopTimingSession() {
        isMonitoring = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }
}
