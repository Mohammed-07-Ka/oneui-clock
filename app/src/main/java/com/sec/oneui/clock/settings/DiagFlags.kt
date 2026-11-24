package com.sec.oneui.clock.settings

import android.content.Context
import android.content.SharedPreferences
import android.view.Choreographer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object DiagFlags {
    private const val PREFS_NAME = "diag_prefs"
    private var prefs: SharedPreferences? = null

    var noPageScaleAlpha by mutableStateOf(false)
        private set
    var snapTabBarFabGear by mutableStateOf(false)
        private set
    var noDimmer by mutableStateOf(false)
        private set
    var deferSheetContent by mutableStateOf(false)
        private set
    var frameTimingOverlay by mutableStateOf(false)
        private set

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
        noPageScaleAlpha = p.getBoolean("d1_no_page_scale_alpha", false)
        snapTabBarFabGear = p.getBoolean("d2_snap_tab_bar_fab_gear", false)
        noDimmer = p.getBoolean("d3_no_dimmer", false)
        deferSheetContent = p.getBoolean("d4_defer_sheet_content", false)
        frameTimingOverlay = p.getBoolean("d5_frame_timing_overlay", false)
    }

    fun setNoPageScaleAlpha(enabled: Boolean) {
        noPageScaleAlpha = enabled
        prefs?.edit()?.putBoolean("d1_no_page_scale_alpha", enabled)?.apply()
    }

    fun setSnapTabBarFabGear(enabled: Boolean) {
        snapTabBarFabGear = enabled
        prefs?.edit()?.putBoolean("d2_snap_tab_bar_fab_gear", enabled)?.apply()
    }

    fun setNoDimmer(enabled: Boolean) {
        noDimmer = enabled
        prefs?.edit()?.putBoolean("d3_no_dimmer", enabled)?.apply()
    }

    fun setDeferSheetContent(enabled: Boolean) {
        deferSheetContent = enabled
        prefs?.edit()?.putBoolean("d4_defer_sheet_content", enabled)?.apply()
    }

    fun setFrameTimingOverlay(enabled: Boolean) {
        frameTimingOverlay = enabled
        prefs?.edit()?.putBoolean("d5_frame_timing_overlay", enabled)?.apply()
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
