package com.sec.oneui.clock.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Debug
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs

private const val BENCH_TAG = "GpuBench"
private const val WARMUP_CYCLES = 2
private const val BENCH_ROUNDS = 4
private const val POST_SETTLE_CAPTURE_MS = 500L
private const val WARMUP_SETTLED_HOLD_MS = 400L
private const val METRICS_FLUSH_MS = 40L
private const val BETWEEN_REPEATS_MS = 800L
private const val PRE_SETTLE_WINDOW_MS = 150.0
private const val POST_SETTLE_WINDOW_MS = 100.0
private const val LATE_START_MS = 200.0
private const val MARKER_MATCH_WINDOW_MS = 30.0
private const val OVER_16_MS_NANOS = 16_700_000L
private const val OVER_25_MS_NANOS = 25_000_000L

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
        label = "A baseline",
        override = BenchOverride()
    ),
    BenchVariantSpec(
        id = "B",
        label = "B D6+D7",
        override = BenchOverride(sheetFlatContainer = true, sheetNoInnerGlass = true)
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

private data class GcSnapshot(
    val count: Long?,
    val timeMs: Long?
)

data class OpeningCapture(
    val variantId: String,
    val variantLabel: String,
    val round: Int,
    val settleMsSinceT0: Double,
    val gcCountDiff: Long?,
    val gcTimeDiffMs: Long?,
    val markers: List<OpeningMarkerPoint>,
    val frames: List<BenchFrameSample>
)

data class WindowStats(
    val realFrameCount: Int,
    val avgTotalMs: Double?,
    val maxTotalMs: Double?,
    val over16Ms: Int
)

data class TableXRow(
    val rowLabel: String,
    val preSettle: WindowStats,
    val postSettle: WindowStats,
    val restLate: WindowStats
)

data class TableYRow(
    val variantId: String,
    val round: Int,
    val sample: BenchFrameSample,
    val nearbyMarkers: String
)

data class BenchV5Report(
    val openings: List<OpeningCapture>,
    val tableXRows: List<TableXRow>,
    val tableYRows: List<TableYRow>,
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
    private val sheetProgressValue: () -> Float
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
    var report by mutableStateOf<BenchV5Report?>(null)
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

                // 3. 4 interleaved rounds: A, B, A, B, A, B, A, B (8 measured cycles)
                val openings = mutableListOf<OpeningCapture>()
                var cycleCounter = 0

                for (round in 1..BENCH_ROUNDS) {
                    for (spec in GPU_BENCH_VARIANTS) {
                        cycleCounter++
                        currentCycleIndex = cycleCounter

                        DiagFlags.updateBenchOverride(spec.override)
                        repeat(2) { withFrameNanos {} }

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

                        val gcStart = readGcStats()
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

                        // Keep FrameMetrics + markers active until 500 ms AFTER settle
                        delay(POST_SETTLE_CAPTURE_MS)
                        val gcEnd = readGcStats()

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

                        val gcCountDiff = if (gcStart.count != null && gcEnd.count != null) {
                            gcEnd.count - gcStart.count
                        } else {
                            null
                        }
                        val gcTimeDiffMs = if (gcStart.timeMs != null && gcEnd.timeMs != null) {
                            gcEnd.timeMs - gcStart.timeMs
                        } else {
                            null
                        }

                        openings.add(
                            OpeningCapture(
                                variantId = spec.id,
                                variantLabel = spec.label,
                                round = round,
                                settleMsSinceT0 = (settleNs - t0Ns) / 1_000_000.0,
                                gcCountDiff = gcCountDiff,
                                gcTimeDiffMs = gcTimeDiffMs,
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

                val built = buildV5Report(openings)
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

private fun readGcStats(): GcSnapshot {
    val count = try {
        Debug.getRuntimeStat("art.gc.gc-count")?.toLongOrNull()
    } catch (_: Throwable) {
        null
    }
    val timeMs = try {
        Debug.getRuntimeStat("art.gc.gc-time")?.toLongOrNull()
    } catch (_: Throwable) {
        null
    }
    return GcSnapshot(count = count, timeMs = timeMs)
}

private fun computeWindowStats(samples: List<BenchFrameSample>): WindowStats {
    if (samples.isEmpty()) {
        return WindowStats(realFrameCount = 0, avgTotalMs = null, maxTotalMs = null, over16Ms = 0)
    }
    val avgMs = samples.map { it.totalNs }.average() / 1_000_000.0
    val maxMs = samples.maxOf { it.totalNs } / 1_000_000.0
    val over16 = samples.count { it.totalNs > OVER_16_MS_NANOS }
    return WindowStats(
        realFrameCount = samples.size,
        avgTotalMs = avgMs,
        maxTotalMs = maxMs,
        over16Ms = over16
    )
}

private fun partitionOpeningWindows(opening: OpeningCapture): Triple<List<BenchFrameSample>, List<BenchFrameSample>, List<BenchFrameSample>> {
    val settleMs = opening.settleMsSinceT0
    val preStart = settleMs - PRE_SETTLE_WINDOW_MS
    val postEnd = settleMs + POST_SETTLE_WINDOW_MS
    val pre = mutableListOf<BenchFrameSample>()
    val post = mutableListOf<BenchFrameSample>()
    val restLate = mutableListOf<BenchFrameSample>()

    for (f in opening.frames) {
        when {
            f.msSinceT0 in preStart..settleMs -> pre.add(f)
            f.msSinceT0 > settleMs && f.msSinceT0 <= postEnd -> post.add(f)
            f.msSinceT0 > LATE_START_MS -> restLate.add(f)
        }
    }
    return Triple(pre, post, restLate)
}

private fun buildV5Report(openings: List<OpeningCapture>): BenchV5Report {
    val tableXRows = mutableListOf<TableXRow>()
    for (spec in GPU_BENCH_VARIANTS) {
        val variantOpenings = openings.filter { it.variantId == spec.id }.sortedBy { it.round }
        val pooledPre = mutableListOf<BenchFrameSample>()
        val pooledPost = mutableListOf<BenchFrameSample>()
        val pooledRest = mutableListOf<BenchFrameSample>()

        for (opening in variantOpenings) {
            val (pre, post, rest) = partitionOpeningWindows(opening)
            pooledPre.addAll(pre)
            pooledPost.addAll(post)
            pooledRest.addAll(rest)
            tableXRows.add(
                TableXRow(
                    rowLabel = "${spec.id} R${opening.round}",
                    preSettle = computeWindowStats(pre),
                    postSettle = computeWindowStats(post),
                    restLate = computeWindowStats(rest)
                )
            )
        }
        tableXRows.add(
            TableXRow(
                rowLabel = "${spec.id} POOLED",
                preSettle = computeWindowStats(pooledPre),
                postSettle = computeWindowStats(pooledPost),
                restLate = computeWindowStats(pooledRest)
            )
        )
    }

    val tableYRows = mutableListOf<TableYRow>()
    for (opening in openings) {
        for (frame in opening.frames) {
            if (frame.msSinceT0 > LATE_START_MS && frame.totalNs > OVER_25_MS_NANOS) {
                val nearby = opening.markers
                    .filter { abs(it.msSinceT0 - frame.msSinceT0) <= MARKER_MATCH_WINDOW_MS }
                    .joinToString(", ") { m ->
                        val delta = m.msSinceT0 - frame.msSinceT0
                        String.format(Locale.US, "%s(%+.1fms)", m.name, delta)
                    }
                    .ifEmpty { "none" }
                tableYRows.add(
                    TableYRow(
                        variantId = opening.variantId,
                        round = opening.round,
                        sample = frame,
                        nearbyMarkers = nearby
                    )
                )
            }
        }
    }

    val formatted = formatV5ReportText(openings, tableXRows, tableYRows)
    return BenchV5Report(
        openings = openings,
        tableXRows = tableXRows,
        tableYRows = tableYRows,
        formattedText = formatted
    )
}

private const val TABLE_W_HEADER =
    "idx | t0 ms   | total   | * | anim   | input  | layout | draw   | cmd-iss | swap   | GPU    | unk-dly"

private const val TABLE_X_HEADER =
    "var/rnd  | [settle-150..settle] n/avg/max/>16.7 | (settle..settle+100] n/avg/max/>16.7 | rest LATE>200 n/avg/max/>16.7"

private const val TABLE_Y_HEADER =
    "var | R | idx | t0 ms   | total   | anim   | input  | layout | draw   | cmd-iss | swap   | GPU    | unk-dly | markers (+-30ms)"

private fun formatTableWFrameRow(s: BenchFrameSample): String {
    val flag = if (s.totalNs > OVER_16_MS_NANOS) "*" else " "
    val gpuStr = s.gpuNs?.let { String.format(Locale.US, "%.2f", it / 1_000_000.0) } ?: "n/a"
    return String.format(
        Locale.US,
        "%3d | %7.2f | %7.2f | %s | %6.2f | %6.2f | %6.2f | %6.2f | %7.2f | %6.2f | %6s | %7.2f",
        s.frameIndex,
        s.msSinceT0,
        s.totalNs / 1_000_000.0,
        flag,
        s.animationNs.coerceAtLeast(0L) / 1_000_000.0,
        s.inputNs.coerceAtLeast(0L) / 1_000_000.0,
        s.layoutMeasureNs.coerceAtLeast(0L) / 1_000_000.0,
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

private fun buildTableWRoundLines(opening: OpeningCapture): List<Pair<Boolean, String>> {
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
            is TimelineLine.Frame -> false to formatTableWFrameRow(line.frame)
        }
    }
}

private fun formatWindowStatsCell(w: WindowStats): String {
    if (w.realFrameCount == 0 || w.avgTotalMs == null || w.maxTotalMs == null) {
        return " 0f   n/a   n/a  0"
    }
    return String.format(
        Locale.US,
        "%2df %5.1f %5.1f %2d",
        w.realFrameCount,
        w.avgTotalMs,
        w.maxTotalMs,
        w.over16Ms
    )
}

private fun formatTableXRow(row: TableXRow): String {
    val c1 = formatWindowStatsCell(row.preSettle)
    val c2 = formatWindowStatsCell(row.postSettle)
    val c3 = formatWindowStatsCell(row.restLate)
    return String.format(
        Locale.US,
        "%-8s | %-34s | %-35s | %s",
        row.rowLabel,
        c1,
        c2,
        c3
    )
}

private fun formatTableYRow(row: TableYRow): String {
    val s = row.sample
    val gpuStr = s.gpuNs?.let { String.format(Locale.US, "%.2f", it / 1_000_000.0) } ?: "n/a"
    return String.format(
        Locale.US,
        "%-3s | %d | %3d | %7.2f | %7.2f | %6.2f | %6.2f | %6.2f | %6.2f | %7.2f | %6.2f | %6s | %7.2f | %s",
        row.variantId,
        row.round,
        s.frameIndex,
        s.msSinceT0,
        s.totalNs / 1_000_000.0,
        s.animationNs.coerceAtLeast(0L) / 1_000_000.0,
        s.inputNs.coerceAtLeast(0L) / 1_000_000.0,
        s.layoutMeasureNs.coerceAtLeast(0L) / 1_000_000.0,
        s.drawNs.coerceAtLeast(0L) / 1_000_000.0,
        s.cmdIssueNs.coerceAtLeast(0L) / 1_000_000.0,
        s.swapNs.coerceAtLeast(0L) / 1_000_000.0,
        gpuStr,
        s.unknownDelayNs.coerceAtLeast(0L) / 1_000_000.0,
        row.nearbyMarkers
    )
}

private fun formatOpeningSummaryLines(opening: OpeningCapture): List<String> {
    val gcStr = if (opening.gcCountDiff != null && opening.gcTimeDiffMs != null) {
        "count=+${opening.gcCountDiff}, time=+${opening.gcTimeDiffMs}ms"
    } else {
        "n/a (Debug.getRuntimeStat unavailable)"
    }
    val hasBgScale = opening.markers.any { it.name == "bgScale_final" }
    val hasBgAlpha = opening.markers.any { it.name == "bgAlpha_final" }
    val markersJoined = buildString {
        append(
            opening.markers.joinToString(", ") { m ->
                String.format(Locale.US, "%s=%.2fms", m.name, m.msSinceT0)
            }
        )
        if (!hasBgScale) {
            append(if (isNotEmpty()) ", " else "")
            append("bgScale_final=n/a (did not reach target within settle+500ms)")
        }
        if (!hasBgAlpha) {
            append(if (isNotEmpty()) ", " else "")
            append("bgAlpha_final=n/a (did not reach target within settle+500ms)")
        }
        append(", wheelPicker_snap=n/a (rememberLazyListState starts at safeInitialIndex with no initial animated snap)")
    }
    return listOf(
        String.format(
            Locale.US,
            "%s R%d | real frames=%d | settle=%.2f ms | GC: %s",
            opening.variantId,
            opening.round,
            opening.frames.size,
            opening.settleMsSinceT0,
            gcStr
        ),
        "  markers: $markersJoined"
    )
}

private fun formatV5ReportText(
    openings: List<OpeningCapture>,
    tableXRows: List<TableXRow>,
    tableYRows: List<TableYRow>
): String {
    val sb = StringBuilder()

    sb.appendLine("=== Per-Opening Summary (Settle, Markers, GC) ===")
    openings.forEach { opening ->
        formatOpeningSummaryLines(opening).forEach { sb.appendLine(it) }
    }

    sb.appendLine()
    sb.appendLine("=== Table X: End-of-Animation & Late Windows (real frames / avg / max / >16.7) ===")
    sb.appendLine(TABLE_X_HEADER)
    tableXRows.forEach { sb.appendLine(formatTableXRow(it)) }

    sb.appendLine()
    sb.appendLine("=== Table Y: LATE Frames (>200ms since T0) with TOTAL > 25ms (A & B) ===")
    sb.appendLine(TABLE_Y_HEADER)
    if (tableYRows.isEmpty()) {
        sb.appendLine("(none)")
    } else {
        tableYRows.forEach { sb.appendLine(formatTableYRow(it)) }
    }

    sb.appendLine()
    sb.appendLine("=== Table W: Variant A Every Real Frame + Event Markers ===")
    openings.filter { it.variantId == "A" }.sortedBy { it.round }.forEach { opening ->
        sb.appendLine("-- Variant A Round ${opening.round} (${opening.frames.size} real frames, settle=${String.format(Locale.US, "%.2f", opening.settleMsSinceT0)}ms) --")
        sb.appendLine(TABLE_W_HEADER)
        buildTableWRoundLines(opening).forEach { (_, text) ->
            sb.appendLine(text)
        }
    }

    return sb.toString().trimEnd()
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
                    "Benchmark running\u2026 variant ${runner.currentCycleIndex}/${runner.totalCycles}"
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
                .heightIn(max = 580.dp),
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
                        text = if (runner.errorMessage != null) "GPU Benchmark Error" else "GPU Benchmark v5 (Add Alarm)",
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
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("GPU Benchmark v5", copyText))
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
                    val openings = rep?.openings.orEmpty()
                    val vScroll = rememberScrollState()
                    val hScrollSummary = rememberScrollState()
                    val hScrollX = rememberScrollState()
                    val hScrollY = rememberScrollState()
                    val hScrollW = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(vScroll)
                    ) {
                        Text(
                            text = "Per-Opening Summary (Settle, Markers, GC)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF86EFAC)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(modifier = Modifier.horizontalScroll(hScrollSummary)) {
                            openings.forEachIndexed { idx, opening ->
                                formatOpeningSummaryLines(opening).forEach { line ->
                                    Text(
                                        text = line,
                                        fontSize = 9.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (line.startsWith("  ")) Color(0xFFCBD5E1) else Color.White,
                                        softWrap = false,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .background(if (idx % 2 == 1) Color.White.copy(alpha = 0.04f) else Color.Transparent)
                                            .padding(vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        TableSection(
                            title = "Table X: End-of-Animation & Late Windows (real frames / avg / max / >16.7)",
                            titleColor = Color(0xFF93C5FD),
                            header = TABLE_X_HEADER,
                            rows = rep?.tableXRows.orEmpty().map { formatTableXRow(it) },
                            hScroll = hScrollX
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        TableSection(
                            title = "Table Y: LATE Frames (>200ms since T0) with TOTAL > 25ms",
                            titleColor = Color(0xFFFCD34D),
                            header = TABLE_Y_HEADER,
                            rows = rep?.tableYRows.orEmpty().let { rows ->
                                if (rows.isEmpty()) listOf("(none)") else rows.map { formatTableYRow(it) }
                            },
                            hScroll = hScrollY
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Table W: Variant A Every Real Frame + Event Markers",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF9A8D4)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(modifier = Modifier.horizontalScroll(hScrollW)) {
                            openings.filter { it.variantId == "A" }.sortedBy { it.round }.forEach { opening ->
                                Text(
                                    text = "-- Variant A Round ${opening.round} (${opening.frames.size} real frames, settle=${String.format(Locale.US, "%.2f", opening.settleMsSinceT0)}ms) --",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFF9A8D4),
                                    softWrap = false,
                                    maxLines = 1
                                )
                                Text(
                                    text = TABLE_W_HEADER,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF93C5FD),
                                    softWrap = false,
                                    maxLines = 1
                                )
                                HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
                                buildTableWRoundLines(opening).forEachIndexed { idx, (isMarker, line) ->
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
