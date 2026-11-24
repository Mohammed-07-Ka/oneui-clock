package com.sec.oneui.clock.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.FrameMetrics
import android.view.Window
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sec.oneui.clock.theme.isLiquidGlassEnabled
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private const val BENCH_TAG = "GpuBench"
private const val WARMUP_CYCLES = 2
private const val BENCH_ROUNDS = 5
private const val POST_SETTLE_CAPTURE_MS = 250L
private const val WARMUP_SETTLED_HOLD_MS = 400L
private const val METRICS_FLUSH_MS = 40L
private const val BETWEEN_REPEATS_MS = 800L
private const val OVER_16_MS_NANOS = 16_700_000L

data class BenchMarkerEvent(
    val name: String,
    val nanoTime: Long
)

object GpuBenchMarks {
    @Volatile
    var active: Boolean = false
        private set

    private val lock = Any()
    private val events = mutableListOf<BenchMarkerEvent>()

    fun beginSession() {
        synchronized(lock) {
            events.clear()
            active = true
        }
    }

    fun endSession(): List<BenchMarkerEvent> {
        synchronized(lock) {
            active = false
            val copy = events.toList()
            events.clear()
            return copy
        }
    }

    fun stopWithoutDrain() {
        synchronized(lock) {
            active = false
            events.clear()
        }
    }

    fun mark(name: String) {
        if (!active) return
        val now = System.nanoTime()
        synchronized(lock) {
            if (active) {
                events.add(BenchMarkerEvent(name = name, nanoTime = now))
            }
        }
    }
}

data class BenchVariantSpec(
    val id: String,
    val label: String,
    val override: BenchOverride
)

val GPU_BENCH_VARIANTS = listOf(
    BenchVariantSpec(
        id = "A",
        label = "A glass ON",
        override = BenchOverride(liquidGlassOverride = true)
    ),
    BenchVariantSpec(
        id = "O",
        label = "O glass OFF",
        override = BenchOverride(liquidGlassOverride = false)
    )
)

data class BenchFrameSample(
    val round: Int,
    val frameIndex: Int,
    val intendedVsyncNs: Long,
    val msSinceT0: Double,
    val totalNs: Long,
    val animationNs: Long,
    val inputNs: Long,
    val layoutMeasureNs: Long,
    val drawNs: Long,
    val cmdIssueNs: Long,
    val swapNs: Long,
    val unknownDelayNs: Long,
    val gpuNs: Long?
)

data class OpeningMarkerPoint(
    val name: String,
    val msSinceT0: Double
)

data class OpeningCapture(
    val variantId: String,
    val variantLabel: String,
    val round: Int,
    val isGlassActive: Boolean,
    val refreshRateT0: Float,
    val refreshRateSettle: Float,
    val refreshRatePost: Float,
    val settleMsSinceT0: Double,
    val markers: List<OpeningMarkerPoint>,
    val frames: List<BenchFrameSample>
)

data class WindowStatsV6(
    val count: Int,
    val avgTotalMs: Double?,
    val maxTotalMs: Double?,
    val over16Count: Int,
    val avgGpuMs: Double?
)

data class TablePRow(
    val variantId: String,
    val round: Int,
    val isGlassActive: Boolean,
    val settleMs: Double,
    val frameCount: Int,
    val refreshRateT0: Float,
    val refreshRateSettle: Float,
    val refreshRatePost: Float
)

data class TableQRow(
    val variantId: String,
    val round: Int,
    val w0to200: WindowStatsV6,
    val w200toSettleMinus150: WindowStatsV6,
    val wSettleMinus150toSettle: WindowStatsV6,
    val wSettleToSettlePlus250: WindowStatsV6
)

data class TableRRow(
    val variantId: String,
    val label: String,
    val w0to200: WindowStatsV6,
    val w200toSettleMinus150: WindowStatsV6,
    val wSettleMinus150toSettle: WindowStatsV6,
    val wSettleToSettlePlus250: WindowStatsV6
)

data class VariantOStageAverages(
    val frameCount: Int,
    val over16Count: Int,
    val avgTotalMs: Double,
    val avgAnimMs: Double,
    val avgDrawMs: Double,
    val avgCmdIssueMs: Double,
    val avgSwapMs: Double,
    val avgGpuMs: Double?,
    val avgUnknownDelayMs: Double,
    val avgInputMs: Double,
    val avgLayoutMs: Double
)

data class BenchV6Report(
    val openings: List<OpeningCapture>,
    val tablePRows: List<TablePRow>,
    val tableQRows: List<TableQRow>,
    val tableRRows: List<TableRRow>,
    val top2VariantO: List<OpeningCapture>,
    val stageAveragesVariantO: VariantOStageAverages,
    val formattedText: String
)

@Stable
class GpuBenchRunner(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val leaveSettingsToAlarmTab: suspend () -> Unit,
    private val openSheetLikeFab: () -> Unit,
    private val closeSheetLikeCancel: () -> Unit,
    private val isSheetOpen: () -> Boolean,
    private val isSheetComposed: () -> Boolean,
    private val isSheetProgressRunning: () -> Boolean,
    private val sheetProgressValue: () -> Float,
    private val isGlassActive: () -> Boolean = { isLiquidGlassEnabled }
) {
    var isRunning by mutableStateOf(false)
        private set
    var isWarmingUp by mutableStateOf(false)
        private set
    var warmupCycleIndex by mutableIntStateOf(0)
        private set
    var currentCycleIndex by mutableIntStateOf(0)
        private set
    var totalCycles by mutableIntStateOf(BENCH_ROUNDS * GPU_BENCH_VARIANTS.size)
        private set
    var report by mutableStateOf<BenchV6Report?>(null)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var showResultsOverlay by mutableStateOf(false)
        private set

    private var runJob: Job? = null

    fun start() {
        if (isRunning) return
        runJob?.cancel()

        report = null
        errorMessage = null
        showResultsOverlay = false
        isWarmingUp = false
        warmupCycleIndex = 0
        currentCycleIndex = 0
        totalCycles = BENCH_ROUNDS * GPU_BENCH_VARIANTS.size
        isRunning = true

        runJob = coroutineScope.launch {
            var handlerThread: HandlerThread? = null
            var attachedWindow: Window? = null
            var attachedListener: Window.OnFrameMetricsAvailableListener? = null
            val sampleLock = Any()
            var capturing = false
            var activeCycleToken = 0
            var activeRound = 0
            var lastRecordedIntendedVsyncNs = -1L
            val recordedIntendedVsyncs = HashSet<Long>()
            val rawSamples = mutableListOf<RawFrameMetric>()

            fun detachListenerSafe() {
                synchronized(sampleLock) {
                    capturing = false
                }
                val w = attachedWindow
                val l = attachedListener
                if (w != null && l != null) {
                    try {
                        w.removeOnFrameMetricsAvailableListener(l)
                    } catch (_: Throwable) {
                    }
                }
                attachedWindow = null
                attachedListener = null
            }

            try {
                val activity = context.findComponentActivity()
                    ?: throw IllegalStateException("Cannot resolve ComponentActivity for FrameMetrics")
                val window = activity.window
                    ?: throw IllegalStateException("Activity window is null")

                val thread = HandlerThread("GpuBenchMetrics").apply { start() }
                handlerThread = thread
                val bgHandler = Handler(thread.looper)

                fun createCycleListener(cycleToken: Int): Window.OnFrameMetricsAvailableListener =
                    Window.OnFrameMetricsAvailableListener { _, frameMetrics, _ ->
                        val totalNs = frameMetrics.getMetric(FrameMetrics.TOTAL_DURATION)
                        if (totalNs < 0L) return@OnFrameMetricsAvailableListener
                        val intendedVsyncNs = frameMetrics.getMetric(FrameMetrics.INTENDED_VSYNC_TIMESTAMP)
                        val animationNs = frameMetrics.getMetric(FrameMetrics.ANIMATION_DURATION)
                        val inputNs = frameMetrics.getMetric(FrameMetrics.INPUT_HANDLING_DURATION)
                        val layoutNs = frameMetrics.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION)
                        val drawNs = frameMetrics.getMetric(FrameMetrics.DRAW_DURATION)
                        val cmdIssueNs = frameMetrics.getMetric(FrameMetrics.COMMAND_ISSUE_DURATION)
                        val swapNs = frameMetrics.getMetric(FrameMetrics.SWAP_BUFFERS_DURATION)
                        val unknownDelayNs = frameMetrics.getMetric(FrameMetrics.UNKNOWN_DELAY_DURATION)
                        val callbackNs = System.nanoTime()
                        val gpuNs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val v = frameMetrics.getMetric(FrameMetrics.GPU_DURATION)
                            if (v >= 0L) v else null
                        } else {
                            null
                        }
                        synchronized(sampleLock) {
                            if (!capturing || activeCycleToken != cycleToken) {
                                return@OnFrameMetricsAvailableListener
                            }
                            if (intendedVsyncNs >= 0L) {
                                if (intendedVsyncNs == lastRecordedIntendedVsyncNs ||
                                    !recordedIntendedVsyncs.add(intendedVsyncNs)
                                ) {
                                    return@OnFrameMetricsAvailableListener
                                }
                                lastRecordedIntendedVsyncNs = intendedVsyncNs
                            }
                            rawSamples.add(
                                RawFrameMetric(
                                    round = activeRound,
                                    intendedVsyncNs = intendedVsyncNs,
                                    fallbackNs = callbackNs,
                                    totalNs = totalNs,
                                    animationNs = animationNs,
                                    inputNs = inputNs,
                                    layoutMeasureNs = layoutNs,
                                    drawNs = drawNs,
                                    cmdIssueNs = cmdIssueNs,
                                    swapNs = swapNs,
                                    unknownDelayNs = unknownDelayNs,
                                    gpuNs = gpuNs
                                )
                            )
                        }
                    }

                // 1. Leave Settings, go to Alarm tab, ensure no sheet is open
                GpuBenchMarks.stopWithoutDrain()
                DiagFlags.updateBenchOverride(null)
                leaveSettingsToAlarmTab()
                delay(450L)

                if (isSheetOpen() || isSheetComposed() || sheetProgressValue() > 0.0001f) {
                    closeSheetLikeCancel()
                    snapshotFlow {
                        !isSheetOpen() && !isSheetComposed() && !isSheetProgressRunning() && sheetProgressValue() <= 0.0001f
                    }.first { it }
                    delay(300L)
                }

                // 2. TWO warm-up openings (discarded)
                isWarmingUp = true
                for (w in 1..WARMUP_CYCLES) {
                    warmupCycleIndex = w
                    DiagFlags.updateBenchOverride(BenchOverride())
                    repeat(2) { withFrameNanos {} }
                    openSheetLikeFab()
                    snapshotFlow {
                        isSheetOpen() && isSheetComposed() && !isSheetProgressRunning() && sheetProgressValue() >= 0.9999f
                    }.first { it }
                    delay(WARMUP_SETTLED_HOLD_MS)
                    closeSheetLikeCancel()
                    snapshotFlow {
                        !isSheetOpen() && !isSheetComposed() && !isSheetProgressRunning() && sheetProgressValue() <= 0.0001f
                    }.first { it }
                    delay(BETWEEN_REPEATS_MS)
                }
                isWarmingUp = false

                // 3. 5 interleaved rounds: A, O, A, O, A, O, A, O, A, O (10 measured openings)
                val openings = mutableListOf<OpeningCapture>()
                var cycleCounter = 0

                for (round in 1..BENCH_ROUNDS) {
                    for (spec in GPU_BENCH_VARIANTS) {
                        cycleCounter++
                        currentCycleIndex = cycleCounter

                        DiagFlags.updateBenchOverride(spec.override)
                        repeat(2) { withFrameNanos {} }
                        val activeGlassDuringRun = isGlassActive()

                        detachListenerSafe()
                        val cycleListener = createCycleListener(cycleCounter)
                        synchronized(sampleLock) {
                            rawSamples.clear()
                            recordedIntendedVsyncs.clear()
                            lastRecordedIntendedVsyncNs = -1L
                            activeCycleToken = cycleCounter
                            activeRound = round
                            capturing = true
                        }
                        attachedWindow = window
                        attachedListener = cycleListener
                        window.addOnFrameMetricsAvailableListener(cycleListener, bgHandler)

                        GpuBenchMarks.beginSession()
                        var marked05 = false
                        var marked09 = false
                        var marked099 = false
                        var marked09999 = false
                        val markerJob = launch {
                            snapshotFlow { sheetProgressValue() }.collect { p ->
                                if (!marked05 && p >= 0.5f) {
                                    marked05 = true
                                    GpuBenchMarks.mark("sheetProgress>=0.5")
                                }
                                if (!marked09 && p >= 0.9f) {
                                    marked09 = true
                                    GpuBenchMarks.mark("sheetProgress>=0.9")
                                }
                                if (!marked099 && p >= 0.99f) {
                                    marked099 = true
                                    GpuBenchMarks.mark("sheetProgress>=0.99")
                                }
                                if (!marked09999 && p >= 0.9999f) {
                                    marked09999 = true
                                    GpuBenchMarks.mark("sheetProgress>=0.9999")
                                }
                            }
                        }

                        val refreshRateT0 = readRefreshRate(activity)
                        val t0Ns = System.nanoTime()
                        openSheetLikeFab()

                        try {
                            snapshotFlow {
                                isSheetOpen() && isSheetComposed() && !isSheetProgressRunning() && sheetProgressValue() >= 0.9999f
                            }.first { it }
                        } finally {
                            markerJob.cancel()
                        }

                        val settleNs = System.nanoTime()
                        GpuBenchMarks.mark("settled")
                        val refreshRateSettle = readRefreshRate(activity)

                        // Stop collecting at settle + 250 ms
                        delay(POST_SETTLE_CAPTURE_MS)
                        val refreshRatePost = readRefreshRate(activity)

                        withFrameNanos {}
                        delay(METRICS_FLUSH_MS)

                        detachListenerSafe()
                        val rawMarkers = GpuBenchMarks.endSession()
                        val rawSnapshot: List<RawFrameMetric>
                        synchronized(sampleLock) {
                            rawSnapshot = rawSamples.toList()
                            rawSamples.clear()
                            recordedIntendedVsyncs.clear()
                        }

                        val samples = rawSnapshot
                            .mapIndexed { index, r ->
                                val tsNs = if (r.intendedVsyncNs >= 0L) r.intendedVsyncNs else r.fallbackNs
                                BenchFrameSample(
                                    round = r.round,
                                    frameIndex = index + 1,
                                    intendedVsyncNs = r.intendedVsyncNs,
                                    msSinceT0 = (tsNs - t0Ns) / 1_000_000.0,
                                    totalNs = r.totalNs,
                                    animationNs = r.animationNs,
                                    inputNs = r.inputNs,
                                    layoutMeasureNs = r.layoutMeasureNs,
                                    drawNs = r.drawNs,
                                    cmdIssueNs = r.cmdIssueNs,
                                    swapNs = r.swapNs,
                                    unknownDelayNs = r.unknownDelayNs,
                                    gpuNs = r.gpuNs
                                )
                            }
                            .sortedBy { it.msSinceT0 }
                            .mapIndexed { idx, s -> s.copy(frameIndex = idx + 1) }

                        val markers = rawMarkers
                            .map { OpeningMarkerPoint(name = it.name, msSinceT0 = (it.nanoTime - t0Ns) / 1_000_000.0) }
                            .sortedBy { it.msSinceT0 }

                        openings.add(
                            OpeningCapture(
                                variantId = spec.id,
                                variantLabel = spec.label,
                                round = round,
                                isGlassActive = activeGlassDuringRun,
                                refreshRateT0 = refreshRateT0,
                                refreshRateSettle = refreshRateSettle,
                                refreshRatePost = refreshRatePost,
                                settleMsSinceT0 = (settleNs - t0Ns) / 1_000_000.0,
                                markers = markers,
                                frames = samples
                            )
                        )

                        closeSheetLikeCancel()
                        snapshotFlow {
                            !isSheetOpen() && !isSheetComposed() && !isSheetProgressRunning() && sheetProgressValue() <= 0.0001f
                        }.first { it }

                        delay(BETWEEN_REPEATS_MS)
                    }
                }

                val built = buildV6Report(openings)
                built.formattedText.lineSequence().forEach { line ->
                    Log.i(BENCH_TAG, line)
                }
                report = built
                showResultsOverlay = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(BENCH_TAG, "Benchmark error", e)
                errorMessage = "${e::class.simpleName}: ${e.message ?: "Unknown error"}"
                showResultsOverlay = true
            } finally {
                withContext(NonCancellable) {
                    GpuBenchMarks.stopWithoutDrain()
                    detachListenerSafe()
                    handlerThread?.quitSafely()
                    handlerThread = null
                    DiagFlags.updateBenchOverride(null)
                    if (isSheetOpen()) {
                        closeSheetLikeCancel()
                    }
                    isWarmingUp = false
                    isRunning = false
                }
            }
        }
    }

    fun cancel() {
        runJob?.cancel()
        runJob = null
        GpuBenchMarks.stopWithoutDrain()
        DiagFlags.updateBenchOverride(null)
        if (isSheetOpen()) {
            closeSheetLikeCancel()
        }
        isWarmingUp = false
        isRunning = false
    }

    fun dismissResults() {
        showResultsOverlay = false
        errorMessage = null
    }
}

private data class RawFrameMetric(
    val round: Int,
    val intendedVsyncNs: Long,
    val fallbackNs: Long,
    val totalNs: Long,
    val animationNs: Long,
    val inputNs: Long,
    val layoutMeasureNs: Long,
    val drawNs: Long,
    val cmdIssueNs: Long,
    val swapNs: Long,
    val unknownDelayNs: Long,
    val gpuNs: Long?
)

@Suppress("DEPRECATION")
private fun readRefreshRate(activity: ComponentActivity): Float {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.display?.refreshRate ?: activity.windowManager.defaultDisplay.refreshRate
        } else {
            activity.windowManager.defaultDisplay.refreshRate
        }
    } catch (_: Throwable) {
        -1f
    }
}

private fun computeWindowStatsV6(samples: List<BenchFrameSample>): WindowStatsV6 {
    if (samples.isEmpty()) {
        return WindowStatsV6(count = 0, avgTotalMs = null, maxTotalMs = null, over16Count = 0, avgGpuMs = null)
    }
    val avgTotal = samples.map { it.totalNs }.average() / 1_000_000.0
    val maxTotal = samples.maxOf { it.totalNs } / 1_000_000.0
    val over16 = samples.count { it.totalNs > OVER_16_MS_NANOS }
    val gpuSamples = samples.mapNotNull { it.gpuNs }
    val avgGpu = if (gpuSamples.isNotEmpty()) gpuSamples.average() / 1_000_000.0 else null
    return WindowStatsV6(
        count = samples.size,
        avgTotalMs = avgTotal,
        maxTotalMs = maxTotal,
        over16Count = over16,
        avgGpuMs = avgGpu
    )
}

private fun partitionWindowsV6(
    settleMs: Double,
    frames: List<BenchFrameSample>
): List<List<BenchFrameSample>> {
    val w0to200 = mutableListOf<BenchFrameSample>()
    val w200toSettleMinus150 = mutableListOf<BenchFrameSample>()
    val wSettleMinus150toSettle = mutableListOf<BenchFrameSample>()
    val wSettleToSettlePlus250 = mutableListOf<BenchFrameSample>()

    val t1 = 200.0
    val t2 = (settleMs - 150.0).coerceAtLeast(t1)
    val t3 = settleMs
    val t4 = settleMs + 250.0

    for (f in frames) {
        val t = f.msSinceT0
        when {
            t <= t1 -> w0to200.add(f)
            t > t1 && t < t2 -> w200toSettleMinus150.add(f)
            t >= t2 && t <= t3 -> wSettleMinus150toSettle.add(f)
            t > t3 && t <= t4 -> wSettleToSettlePlus250.add(f)
        }
    }
    return listOf(w0to200, w200toSettleMinus150, wSettleMinus150toSettle, wSettleToSettlePlus250)
}

private const val TABLE_P_HEADER =
    "var | R | glass | settle ms | frames | refresh rate (T0 / settle / +250)"

private fun formatTablePRow(r: TablePRow): String {
    return String.format(
        Locale.US,
        " %-2s | %d | %-5b | %9.2f | %6d | %5.1f / %5.1f / %5.1f Hz",
        r.variantId,
        r.round,
        r.isGlassActive,
        r.settleMs,
        r.frameCount,
        r.refreshRateT0,
        r.refreshRateSettle,
        r.refreshRatePost
    )
}

private const val TABLE_Q_HEADER =
    "var | R | [0-200ms] (avg/max/>16/gpu) | [200..set-150] (avg/max/>16/gpu) | [set-150..set] (avg/max/>16/gpu) | [set..set+250] (avg/max/>16/gpu)"

private fun formatWindowStatsV6Cell(w: WindowStatsV6): String {
    if (w.count == 0 || w.avgTotalMs == null) {
        return "  -  /   -  /  0 /   -"
    }
    val gpuStr = w.avgGpuMs?.let { String.format(Locale.US, "%4.1f", it) } ?: " n/a"
    return String.format(
        Locale.US,
        "%5.1f / %5.1f / %2d / %4s",
        w.avgTotalMs,
        w.maxTotalMs ?: 0.0,
        w.over16Count,
        gpuStr
    )
}

private fun formatTableQRow(r: TableQRow): String {
    return String.format(
        Locale.US,
        " %-2s | %d | %-27s | %-27s | %-27s | %s",
        r.variantId,
        r.round,
        formatWindowStatsV6Cell(r.w0to200),
        formatWindowStatsV6Cell(r.w200toSettleMinus150),
        formatWindowStatsV6Cell(r.wSettleMinus150toSettle),
        formatWindowStatsV6Cell(r.wSettleToSettlePlus250)
    )
}

private fun formatTableRRow(r: TableRRow): String {
    return String.format(
        Locale.US,
        " %-2s | P | %-27s | %-27s | %-27s | %s",
        r.variantId,
        formatWindowStatsV6Cell(r.w0to200),
        formatWindowStatsV6Cell(r.w200toSettleMinus150),
        formatWindowStatsV6Cell(r.wSettleMinus150toSettle),
        formatWindowStatsV6Cell(r.wSettleToSettlePlus250)
    )
}

private const val TABLE_S_HEADER =
    "idx | t0 ms   | total   | * | anim   | draw   | cmd-iss | swap   | GPU    | unk-dly"

private fun formatTableSFrameRow(s: BenchFrameSample): String {
    val flag = if (s.totalNs > OVER_16_MS_NANOS) "*" else " "
    val gpuStr = s.gpuNs?.let { String.format(Locale.US, "%6.2f", it / 1_000_000.0) } ?: "   n/a"
    return String.format(
        Locale.US,
        "%3d | %7.2f | %7.2f | %s | %6.2f | %6.2f | %7.2f | %6.2f | %6s | %7.2f",
        s.frameIndex,
        s.msSinceT0,
        s.totalNs / 1_000_000.0,
        flag,
        s.animationNs.coerceAtLeast(0L) / 1_000_000.0,
        s.drawNs.coerceAtLeast(0L) / 1_000_000.0,
        s.cmdIssueNs.coerceAtLeast(0L) / 1_000_000.0,
        s.swapNs.coerceAtLeast(0L) / 1_000_000.0,
        gpuStr,
        s.unknownDelayNs.coerceAtLeast(0L) / 1_000_000.0
    )
}

private sealed class TimelineLine {
    data class Frame(val frame: BenchFrameSample) : TimelineLine()
    data class Marker(val marker: OpeningMarkerPoint) : TimelineLine()
}

private fun buildTableSRoundLines(opening: OpeningCapture): List<Pair<Boolean, String>> {
    val items = buildList {
        opening.frames.forEach { add(it.msSinceT0 to TimelineLine.Frame(it)) }
        opening.markers.forEach { add(it.msSinceT0 to TimelineLine.Marker(it)) }
    }.sortedWith(
        compareBy<Pair<Double, TimelineLine>> { it.first }
            .thenBy { if (it.second is TimelineLine.Marker) 0 else 1 }
    )

    return items.map { (_, line) ->
        when (line) {
            is TimelineLine.Marker -> true to String.format(
                Locale.US,
                "--- marker: %s @ %.2f ms ---",
                line.marker.name,
                line.marker.msSinceT0
            )
            is TimelineLine.Frame -> false to formatTableSFrameRow(line.frame)
        }
    }
}

private fun computeVariantOStageAverages(openings: List<OpeningCapture>): VariantOStageAverages {
    val frames = openings
        .filter { it.variantId == "O" }
        .flatMap { op ->
            val t2 = (op.settleMsSinceT0 - 150.0).coerceAtLeast(200.0)
            val t3 = op.settleMsSinceT0
            op.frames.filter { it.msSinceT0 in t2..t3 }
        }
    val n = frames.size
    val over16 = frames.count { it.totalNs > OVER_16_MS_NANOS }
    val totalAvg = if (n > 0) frames.map { it.totalNs }.average() / 1_000_000.0 else 0.0
    val animAvg = if (n > 0) frames.map { it.animationNs.coerceAtLeast(0L) }.average() / 1_000_000.0 else 0.0
    val drawAvg = if (n > 0) frames.map { it.drawNs.coerceAtLeast(0L) }.average() / 1_000_000.0 else 0.0
    val cmdAvg = if (n > 0) frames.map { it.cmdIssueNs.coerceAtLeast(0L) }.average() / 1_000_000.0 else 0.0
    val swapAvg = if (n > 0) frames.map { it.swapNs.coerceAtLeast(0L) }.average() / 1_000_000.0 else 0.0
    val gpuSamples = frames.mapNotNull { it.gpuNs }
    val gpuAvg = if (gpuSamples.isNotEmpty()) gpuSamples.average() / 1_000_000.0 else null
    val unkAvg = if (n > 0) frames.map { it.unknownDelayNs.coerceAtLeast(0L) }.average() / 1_000_000.0 else 0.0
    val inputAvg = if (n > 0) frames.map { it.inputNs.coerceAtLeast(0L) }.average() / 1_000_000.0 else 0.0
    val layoutAvg = if (n > 0) frames.map { it.layoutMeasureNs.coerceAtLeast(0L) }.average() / 1_000_000.0 else 0.0

    return VariantOStageAverages(
        frameCount = n,
        over16Count = over16,
        avgTotalMs = totalAvg,
        avgAnimMs = animAvg,
        avgDrawMs = drawAvg,
        avgCmdIssueMs = cmdAvg,
        avgSwapMs = swapAvg,
        avgGpuMs = gpuAvg,
        avgUnknownDelayMs = unkAvg,
        avgInputMs = inputAvg,
        avgLayoutMs = layoutAvg
    )
}

private fun formatStageAverages(avg: VariantOStageAverages): List<String> {
    val gpuStr = avg.avgGpuMs?.let { String.format(Locale.US, "%6.2f ms", it) } ?: "   n/a"
    return listOf(
        String.format(
            Locale.US,
            "=== Variant O Stage Averages over [settle-150, settle] (%d frames pooled, %d > 16.7ms) ===",
            avg.frameCount,
            avg.over16Count
        ),
        String.format(Locale.US, "total:     %6.2f ms", avg.avgTotalMs),
        String.format(Locale.US, "anim:      %6.2f ms", avg.avgAnimMs),
        String.format(Locale.US, "draw:      %6.2f ms", avg.avgDrawMs),
        String.format(Locale.US, "cmd-issue: %6.2f ms", avg.avgCmdIssueMs),
        String.format(Locale.US, "swap:      %6.2f ms", avg.avgSwapMs),
        String.format(Locale.US, "GPU:       %9s", gpuStr),
        String.format(Locale.US, "unk-delay: %6.2f ms", avg.avgUnknownDelayMs),
        String.format(Locale.US, "(input: %6.2f ms, layout: %6.2f ms)", avg.avgInputMs, avg.avgLayoutMs)
    )
}

private fun buildV6Report(openings: List<OpeningCapture>): BenchV6Report {
    val tablePRows = openings.map { op ->
        TablePRow(
            variantId = op.variantId,
            round = op.round,
            isGlassActive = op.isGlassActive,
            settleMs = op.settleMsSinceT0,
            frameCount = op.frames.size,
            refreshRateT0 = op.refreshRateT0,
            refreshRateSettle = op.refreshRateSettle,
            refreshRatePost = op.refreshRatePost
        )
    }

    val tableQRows = openings.map { op ->
        val (w1, w2, w3, w4) = partitionWindowsV6(op.settleMsSinceT0, op.frames)
        TableQRow(
            variantId = op.variantId,
            round = op.round,
            w0to200 = computeWindowStatsV6(w1),
            w200toSettleMinus150 = computeWindowStatsV6(w2),
            wSettleMinus150toSettle = computeWindowStatsV6(w3),
            wSettleToSettlePlus250 = computeWindowStatsV6(w4)
        )
    }

    val tableRRows = GPU_BENCH_VARIANTS.map { spec ->
        val vOpenings = openings.filter { it.variantId == spec.id }
        val pooledW1 = mutableListOf<BenchFrameSample>()
        val pooledW2 = mutableListOf<BenchFrameSample>()
        val pooledW3 = mutableListOf<BenchFrameSample>()
        val pooledW4 = mutableListOf<BenchFrameSample>()
        for (op in vOpenings) {
            val (w1, w2, w3, w4) = partitionWindowsV6(op.settleMsSinceT0, op.frames)
            pooledW1.addAll(w1)
            pooledW2.addAll(w2)
            pooledW3.addAll(w3)
            pooledW4.addAll(w4)
        }
        TableRRow(
            variantId = spec.id,
            label = spec.label,
            w0to200 = computeWindowStatsV6(pooledW1),
            w200toSettleMinus150 = computeWindowStatsV6(pooledW2),
            wSettleMinus150toSettle = computeWindowStatsV6(pooledW3),
            wSettleToSettlePlus250 = computeWindowStatsV6(pooledW4)
        )
    }

    val top2VariantO = openings
        .filter { it.variantId == "O" }
        .sortedWith(
            compareByDescending<OpeningCapture> { op -> op.frames.count { it.totalNs > OVER_16_MS_NANOS } }
                .thenByDescending { op -> op.frames.maxOfOrNull { it.totalNs } ?: 0L }
                .thenBy { it.round }
        )
        .take(2)
        .sortedBy { it.round }

    val stageAverages = computeVariantOStageAverages(openings)

    val sb = StringBuilder()
    sb.appendLine("=== Table P: Per Opening (isGlassActive, Settle, Frames, Refresh Rate) ===")
    sb.appendLine(TABLE_P_HEADER)
    tablePRows.forEach { sb.appendLine(formatTablePRow(it)) }

    sb.appendLine()
    sb.appendLine("=== Table Q: Window Breakdown Per Opening (avg / max / >16.7ms / avg GPU) ===")
    sb.appendLine(TABLE_Q_HEADER)
    tableQRows.forEach { sb.appendLine(formatTableQRow(it)) }

    sb.appendLine()
    sb.appendLine("=== Table R: Pooled Window Breakdown by Variant (A and O) ===")
    sb.appendLine(TABLE_Q_HEADER)
    tableRRows.forEach { sb.appendLine(formatTableRRow(it)) }

    sb.appendLine()
    sb.appendLine("=== Table S: Variant O Top 2 Openings (Most Dropped Frames) Every Real Frame + Markers ===")
    if (top2VariantO.isEmpty()) {
        sb.appendLine("(no Variant O openings recorded)")
    } else {
        top2VariantO.forEach { op ->
            val over16 = op.frames.count { it.totalNs > OVER_16_MS_NANOS }
            sb.appendLine("-- Variant O Round ${op.round} (${op.frames.size} real frames, $over16 > 16.7ms, settle=${String.format(Locale.US, "%.2f", op.settleMsSinceT0)}ms) --")
            sb.appendLine(TABLE_S_HEADER)
            buildTableSRoundLines(op).forEach { (_, line) -> sb.appendLine(line) }
            sb.appendLine()
        }
    }

    formatStageAverages(stageAverages).forEach { sb.appendLine(it) }

    val formatted = sb.toString().trimEnd()

    return BenchV6Report(
        openings = openings,
        tablePRows = tablePRows,
        tableQRows = tableQRows,
        tableRRows = tableRRows,
        top2VariantO = top2VariantO,
        stageAveragesVariantO = stageAverages,
        formattedText = formatted
    )
}

@Composable
fun GpuBenchOverlay(
    runner: GpuBenchRunner,
    modifier: Modifier = Modifier
) {
    if (!runner.isRunning && !runner.showResultsOverlay) return

    val context = LocalContext.current

    if (runner.isRunning) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xE60B0D14))
                    .border(1.dp, Color(0xFF60A5FA).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val statusText = if (runner.isWarmingUp) {
                    "Benchmark running\u2026 warm-up ${runner.warmupCycleIndex}/$WARMUP_CYCLES"
                } else {
                    "Benchmark running\u2026 opening ${runner.currentCycleIndex}/${runner.totalCycles}"
                }
                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF93C5FD)
                )
                Text(
                    text = "Cancel",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF87171),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { runner.cancel() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        return
    }

    // Results or Error panel positioned BELOW status bar at the top
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xF210121A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (runner.errorMessage != null) "GPU Benchmark Error" else "GPU Benchmark v6 (Glass OFF vs ON)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (runner.errorMessage != null) Color(0xFFF87171) else Color.White
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val rep = runner.report
                        if (rep != null || runner.errorMessage != null) {
                            TextButton(
                                onClick = {
                                    val copyText = runner.errorMessage ?: rep?.formattedText.orEmpty()
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("GPU Benchmark v6", copyText))
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("Copy", color = Color(0xFF60A5FA), fontWeight = FontWeight.Bold)
                            }
                        }
                        TextButton(onClick = { runner.dismissResults() }) {
                            Text("Close", color = Color(0xFFF87171), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
                Spacer(modifier = Modifier.height(6.dp))

                val err = runner.errorMessage
                if (err != null) {
                    Text(
                        text = err,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFFCA5A5),
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                } else {
                    val rep = runner.report
                    val vScroll = rememberScrollState()
                    val hScrollP = rememberScrollState()
                    val hScrollQ = rememberScrollState()
                    val hScrollR = rememberScrollState()
                    val hScrollS = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(vScroll)
                    ) {
                        TableSection(
                            title = "Table P: Per Opening (isGlassActive, Settle, Frames, Refresh Rate)",
                            titleColor = Color(0xFF86EFAC),
                            header = TABLE_P_HEADER,
                            rows = rep?.tablePRows.orEmpty().map { formatTablePRow(it) },
                            hScroll = hScrollP
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        TableSection(
                            title = "Table Q: Window Breakdown Per Opening (avg / max / >16.7ms / avg GPU)",
                            titleColor = Color(0xFF93C5FD),
                            header = TABLE_Q_HEADER,
                            rows = rep?.tableQRows.orEmpty().map { formatTableQRow(it) },
                            hScroll = hScrollQ
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        TableSection(
                            title = "Table R: Pooled Window Breakdown by Variant (A and O)",
                            titleColor = Color(0xFFFCD34D),
                            header = TABLE_Q_HEADER,
                            rows = rep?.tableRRows.orEmpty().map { formatTableRRow(it) },
                            hScroll = hScrollR
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Table S: Variant O Top 2 Openings (Every Real Frame + Markers)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF9A8D4)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(modifier = Modifier.horizontalScroll(hScrollS)) {
                            val top2 = rep?.top2VariantO.orEmpty()
                            if (top2.isEmpty()) {
                                Text(
                                    text = "(no Variant O openings recorded)",
                                    fontSize = 9.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                )
                            } else {
                                top2.forEach { op ->
                                    val over16 = op.frames.count { it.totalNs > OVER_16_MS_NANOS }
                                    Text(
                                        text = "-- Variant O Round ${op.round} (${op.frames.size} real frames, $over16 > 16.7ms, settle=${String.format(Locale.US, "%.2f", op.settleMsSinceT0)}ms) --",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFFF9A8D4),
                                        softWrap = false,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = TABLE_S_HEADER,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF93C5FD),
                                        softWrap = false,
                                        maxLines = 1
                                    )
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
                                    buildTableSRoundLines(op).forEachIndexed { idx, (isMarker, line) ->
                                        Text(
                                            text = line,
                                            fontSize = 9.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = when {
                                                isMarker -> Color(0xFF67E8F9)
                                                line.contains(" | * | ") -> Color(0xFFFCA5A5)
                                                else -> Color.White
                                            },
                                            softWrap = false,
                                            maxLines = 1,
                                            modifier = Modifier
                                                .background(
                                                    if (idx % 2 == 1) Color.White.copy(alpha = 0.04f) else Color.Transparent
                                                )
                                                .padding(vertical = 1.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }

                        val stageAvg = rep?.stageAveragesVariantO
                        if (stageAvg != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Variant O Stage Averages over [settle-150, settle]",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6EE7B7)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            formatStageAverages(stageAvg).forEach { line ->
                                Text(
                                    text = line,
                                    fontSize = 9.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFCBD5E1),
                                    softWrap = false,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TableSection(
    title: String,
    titleColor: Color,
    header: String,
    rows: List<String>,
    hScroll: androidx.compose.foundation.ScrollState
) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = titleColor
    )
    Spacer(modifier = Modifier.height(4.dp))
    Column(modifier = Modifier.horizontalScroll(hScroll)) {
        Text(
            text = header,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = titleColor,
            softWrap = false,
            maxLines = 1
        )
        HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
        rows.forEachIndexed { idx, line ->
            Text(
                text = line,
                fontSize = 9.5.sp,
                fontFamily = FontFamily.Monospace,
                color = Color.White,
                softWrap = false,
                maxLines = 1,
                modifier = Modifier
                    .background(
                        if (idx % 2 == 1) Color.White.copy(alpha = 0.05f) else Color.Transparent
                    )
                    .padding(vertical = 2.dp)
            )
        }
    }
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}
