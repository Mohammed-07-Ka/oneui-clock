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
private const val BENCH_ROUNDS = 4
private const val EARLY_FRAME_COUNT = 12
private const val TIMELINE_MAX_FRAME = 16
private const val OPEN_SETTLED_HOLD_MS = 560L
private const val WARMUP_SETTLED_HOLD_MS = 400L
private const val METRICS_FLUSH_MS = 40L
private const val BETWEEN_REPEATS_MS = 800L
private const val OVER_16_MS_NANOS = 16_700_000L

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
    ),
    BenchVariantSpec(
        id = "C",
        label = "C D6 only",
        override = BenchOverride(sheetFlatContainer = true)
    ),
    BenchVariantSpec(
        id = "D",
        label = "D D7 only",
        override = BenchOverride(sheetNoInnerGlass = true)
    )
)

data class BenchFrameSample(
    val round: Int,
    val frameIndex: Int,
    val relativeIntendedVsyncMs: Double,
    val totalNs: Long,
    val animationNs: Long,
    val layoutMeasureNs: Long,
    val drawNs: Long,
    val syncNs: Long,
    val unknownDelayNs: Long,
    val cmdIssueNs: Long,
    val swapNs: Long,
    val gpuNs: Long?
)

data class StageBreakdown(
    val frameCount: Int,
    val animMs: Double,
    val layoutMs: Double,
    val drawMs: Double,
    val syncMs: Double,
    val unkDelayMs: Double,
    val cmdIssueMs: Double,
    val swapMs: Double,
    val gpuMs: Double?,
    val totalMs: Double
)

data class RoundDriftCell(
    val round: Int,
    val frameCount: Int,
    val avgTotalMs: Double,
    val over16Ms: Int
)

data class BenchVariantV3Report(
    val variantId: String,
    val variantLabel: String,
    val perFrameTotalMs: List<Double?>,
    val earlyStages: StageBreakdown,
    val lateStages: StageBreakdown,
    val roundDrift: List<RoundDriftCell>,
    val samplesByRound: Map<Int, List<BenchFrameSample>>
)

data class BenchV3Report(
    val variants: List<BenchVariantV3Report>,
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
    var report by mutableStateOf<BenchV3Report?>(null)
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
            var activeRound = 0
            var firstIntendedVsyncNs = -1L
            val currentSamples = mutableListOf<BenchFrameSample>()

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

                val frameListener = Window.OnFrameMetricsAvailableListener { _, frameMetrics, _ ->
                    val totalNs = frameMetrics.getMetric(FrameMetrics.TOTAL_DURATION)
                    if (totalNs < 0L) return@OnFrameMetricsAvailableListener
                    val intendedVsyncNs = frameMetrics.getMetric(FrameMetrics.INTENDED_VSYNC_TIMESTAMP)
                    val animationNs = frameMetrics.getMetric(FrameMetrics.ANIMATION_DURATION)
                    val layoutNs = frameMetrics.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION)
                    val drawNs = frameMetrics.getMetric(FrameMetrics.DRAW_DURATION)
                    val syncNs = frameMetrics.getMetric(FrameMetrics.SYNC_DURATION)
                    val unknownDelayNs = frameMetrics.getMetric(FrameMetrics.UNKNOWN_DELAY_DURATION)
                    val cmdIssueNs = frameMetrics.getMetric(FrameMetrics.COMMAND_ISSUE_DURATION)
                    val swapNs = frameMetrics.getMetric(FrameMetrics.SWAP_BUFFERS_DURATION)
                    val gpuNs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val v = frameMetrics.getMetric(FrameMetrics.GPU_DURATION)
                        if (v >= 0L) v else null
                    } else {
                        null
                    }
                    synchronized(sampleLock) {
                        if (capturing) {
                            if (firstIntendedVsyncNs < 0L && intendedVsyncNs >= 0L) {
                                firstIntendedVsyncNs = intendedVsyncNs
                            }
                            val relMs = if (firstIntendedVsyncNs >= 0L && intendedVsyncNs >= 0L) {
                                (intendedVsyncNs - firstIntendedVsyncNs) / 1_000_000.0
                            } else {
                                0.0
                            }
                            currentSamples.add(
                                BenchFrameSample(
                                    round = activeRound,
                                    frameIndex = currentSamples.size + 1,
                                    relativeIntendedVsyncMs = relMs,
                                    totalNs = totalNs,
                                    animationNs = animationNs,
                                    layoutMeasureNs = layoutNs,
                                    drawNs = drawNs,
                                    syncNs = syncNs,
                                    unknownDelayNs = unknownDelayNs,
                                    cmdIssueNs = cmdIssueNs,
                                    swapNs = swapNs,
                                    gpuNs = gpuNs
                                )
                            )
                        }
                    }
                }

                // 1. Leave Settings, go to Alarm tab, ensure no sheet is open
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

                // 2. TWO warm-up cycles (frames discarded)
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

                // 3. Interleaved rounds: round 1..4 = A, B, C, D
                val samplesByVariantAndRound = LinkedHashMap<String, MutableMap<Int, List<BenchFrameSample>>>()
                GPU_BENCH_VARIANTS.forEach { spec ->
                    samplesByVariantAndRound[spec.id] = LinkedHashMap()
                }

                var cycleCounter = 0
                for (round in 1..BENCH_ROUNDS) {
                    for (spec in GPU_BENCH_VARIANTS) {
                        cycleCounter++
                        currentCycleIndex = cycleCounter

                        DiagFlags.updateBenchOverride(spec.override)
                        repeat(2) { withFrameNanos {} }

                        synchronized(sampleLock) {
                            currentSamples.clear()
                            activeRound = round
                            firstIntendedVsyncNs = -1L
                            capturing = true
                        }
                        attachedWindow = window
                        attachedListener = frameListener
                        window.addOnFrameMetricsAvailableListener(frameListener, bgHandler)

                        openSheetLikeFab()

                        snapshotFlow {
                            isSheetOpen() && isSheetComposed() && !isSheetProgressRunning() && sheetProgressValue() >= 0.9999f
                        }.first { it }

                        withFrameNanos {}
                        delay(METRICS_FLUSH_MS)

                        detachListenerSafe()
                        val roundSamples: List<BenchFrameSample>
                        synchronized(sampleLock) {
                            roundSamples = currentSamples.toList()
                            currentSamples.clear()
                        }
                        samplesByVariantAndRound[spec.id]?.put(round, roundSamples)

                        delay(OPEN_SETTLED_HOLD_MS)

                        closeSheetLikeCancel()
                        snapshotFlow {
                            !isSheetOpen() && !isSheetComposed() && !isSheetProgressRunning() && sheetProgressValue() <= 0.0001f
                        }.first { it }

                        delay(BETWEEN_REPEATS_MS)
                    }
                }

                val variantReports = GPU_BENCH_VARIANTS.map { spec ->
                    buildVariantV3Report(spec, samplesByVariantAndRound[spec.id].orEmpty())
                }
                val formatted = formatV3ReportText(variantReports)
                formatted.lineSequence().forEach { line ->
                    Log.i(BENCH_TAG, line)
                }
                report = BenchV3Report(variants = variantReports, formattedText = formatted)
                showResultsOverlay = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(BENCH_TAG, "Benchmark error", e)
                errorMessage = "${e::class.simpleName}: ${e.message ?: "Unknown error"}"
                showResultsOverlay = true
            } finally {
                withContext(NonCancellable) {
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

private fun avgValidMs(samples: List<BenchFrameSample>, selector: (BenchFrameSample) -> Long): Double {
    var sum = 0L
    var count = 0
    for (s in samples) {
        val v = selector(s)
        if (v >= 0L) {
            sum += v
            count++
        }
    }
    return if (count > 0) (sum.toDouble() / count.toDouble()) / 1_000_000.0 else 0.0
}

private fun computeStageBreakdown(samples: List<BenchFrameSample>): StageBreakdown {
    if (samples.isEmpty()) {
        return StageBreakdown(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, null, 0.0)
    }
    val gpuValues = samples.mapNotNull { it.gpuNs }
    val avgGpuMs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && gpuValues.isNotEmpty()) {
        gpuValues.average() / 1_000_000.0
    } else {
        null
    }
    return StageBreakdown(
        frameCount = samples.size,
        animMs = avgValidMs(samples) { it.animationNs },
        layoutMs = avgValidMs(samples) { it.layoutMeasureNs },
        drawMs = avgValidMs(samples) { it.drawNs },
        syncMs = avgValidMs(samples) { it.syncNs },
        unkDelayMs = avgValidMs(samples) { it.unknownDelayNs },
        cmdIssueMs = avgValidMs(samples) { it.cmdIssueNs },
        swapMs = avgValidMs(samples) { it.swapNs },
        gpuMs = avgGpuMs,
        totalMs = avgValidMs(samples) { it.totalNs }
    )
}

private fun buildVariantV3Report(
    spec: BenchVariantSpec,
    samplesByRound: Map<Int, List<BenchFrameSample>>
): BenchVariantV3Report {
    val allSamples = (1..BENCH_ROUNDS).flatMap { samplesByRound[it].orEmpty() }
    val earlySamples = allSamples.filter { it.frameIndex <= EARLY_FRAME_COUNT }
    val lateSamples = allSamples.filter { it.frameIndex > EARLY_FRAME_COUNT }

    val timeline = (1..TIMELINE_MAX_FRAME).map { idx ->
        val atIdx = allSamples.filter { it.frameIndex == idx }
        if (atIdx.isEmpty()) null else atIdx.map { it.totalNs }.average() / 1_000_000.0
    }

    val drift = (1..BENCH_ROUNDS).map { round ->
        val rs = samplesByRound[round].orEmpty()
        RoundDriftCell(
            round = round,
            frameCount = rs.size,
            avgTotalMs = if (rs.isNotEmpty()) rs.map { it.totalNs }.average() / 1_000_000.0 else 0.0,
            over16Ms = rs.count { it.totalNs > OVER_16_MS_NANOS }
        )
    }

    return BenchVariantV3Report(
        variantId = spec.id,
        variantLabel = spec.label,
        perFrameTotalMs = timeline,
        earlyStages = computeStageBreakdown(earlySamples),
        lateStages = computeStageBreakdown(lateSamples),
        roundDrift = drift,
        samplesByRound = samplesByRound
    )
}

private val TABLE_P_HEADER: String = buildString {
    append("variant    ")
    for (i in 1..TIMELINE_MAX_FRAME) {
        append(String.format(Locale.US, " | f%-4d", i))
    }
}

private const val TABLE_QR_HEADER =
    "variant     | anim   | layout | draw   | sync   | unk-dly | cmd-iss | swap   | GPU    | total"

private const val TABLE_S_HEADER =
    "variant     | R1 avg (>16.7)  | R2 avg (>16.7)  | R3 avg (>16.7)  | R4 avg (>16.7)"

private const val VARIANT_A_DETAIL_HEADER =
    "idx | vsync ms | total   | anim   | draw   | cmd-iss | swap   | GPU    | unk-dly"

private fun formatTablePRow(v: BenchVariantV3Report): String = buildString {
    append(String.format(Locale.US, "%-11s", v.variantLabel))
    v.perFrameTotalMs.forEach { ms ->
        val cell = ms?.let { String.format(Locale.US, "%5.1f", it) } ?: "  n/a"
        append(" | ").append(cell)
    }
}

private fun formatStageRow(label: String, s: StageBreakdown): String {
    val gpuStr = s.gpuMs?.let { String.format(Locale.US, "%.2f", it) } ?: "n/a"
    return String.format(
        Locale.US,
        "%-11s | %6.2f | %6.2f | %6.2f | %6.2f | %7.2f | %7.2f | %6.2f | %6s | %6.2f",
        label,
        s.animMs,
        s.layoutMs,
        s.drawMs,
        s.syncMs,
        s.unkDelayMs,
        s.cmdIssueMs,
        s.swapMs,
        gpuStr,
        s.totalMs
    )
}

private fun formatTableSRow(v: BenchVariantV3Report): String = buildString {
    append(String.format(Locale.US, "%-11s", v.variantLabel))
    v.roundDrift.forEach { d ->
        val cell = String.format(Locale.US, "%5.1f (%2d/%2d)", d.avgTotalMs, d.over16Ms, d.frameCount)
        append(" | ").append(String.format(Locale.US, "%-15s", cell))
    }
}

private fun formatSampleDetailRow(s: BenchFrameSample): String {
    val gpuStr = s.gpuNs?.let { String.format(Locale.US, "%.2f", it / 1_000_000.0) } ?: "n/a"
    return String.format(
        Locale.US,
        "%3d | %8.2f | %7.2f | %6.2f | %6.2f | %7.2f | %6.2f | %6s | %7.2f",
        s.frameIndex,
        s.relativeIntendedVsyncMs,
        s.totalNs / 1_000_000.0,
        s.animationNs.coerceAtLeast(0L) / 1_000_000.0,
        s.drawNs.coerceAtLeast(0L) / 1_000_000.0,
        s.cmdIssueNs.coerceAtLeast(0L) / 1_000_000.0,
        s.swapNs.coerceAtLeast(0L) / 1_000_000.0,
        gpuStr,
        s.unknownDelayNs.coerceAtLeast(0L) / 1_000_000.0
    )
}

private fun formatV3ReportText(variants: List<BenchVariantV3Report>): String {
    val sb = StringBuilder()
    sb.appendLine("=== Table P: Per-Frame Timeline 1..16 (avg TOTAL ms over 4 rounds) ===")
    sb.appendLine(TABLE_P_HEADER)
    variants.forEach { sb.appendLine(formatTablePRow(it)) }

    sb.appendLine()
    sb.appendLine("=== Table Q: Stage Breakdown EARLY (frames 1..12, avg ms) ===")
    sb.appendLine(TABLE_QR_HEADER)
    variants.forEach { sb.appendLine(formatStageRow(it.variantLabel, it.earlyStages)) }

    sb.appendLine()
    sb.appendLine("=== Table R: Stage Breakdown LATE (frames > 12, avg ms) ===")
    sb.appendLine(TABLE_QR_HEADER)
    variants.forEach { sb.appendLine(formatStageRow(it.variantLabel, it.lateStages)) }

    sb.appendLine()
    sb.appendLine("=== Table S: Drift by Round (avg TOTAL ms & >16.7 count) ===")
    sb.appendLine(TABLE_S_HEADER)
    variants.forEach { sb.appendLine(formatTableSRow(it)) }

    val variantA = variants.firstOrNull { it.variantId == "A" }
    if (variantA != null) {
        sb.appendLine()
        sb.appendLine("=== Variant A First 12 Frames per Round ===")
        for (round in 1..BENCH_ROUNDS) {
            sb.appendLine("-- Round $round --")
            sb.appendLine(VARIANT_A_DETAIL_HEADER)
            variantA.samplesByRound[round].orEmpty().take(EARLY_FRAME_COUNT).forEach { sample ->
                sb.appendLine(formatSampleDetailRow(sample))
            }
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
                        text = if (runner.errorMessage != null) "GPU Benchmark Error" else "GPU Benchmark v3 (Add Alarm)",
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
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("GPU Benchmark v3", copyText))
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
                    val variants = runner.report?.variants.orEmpty()
                    val vScroll = rememberScrollState()
                    val hScrollP = rememberScrollState()
                    val hScrollQ = rememberScrollState()
                    val hScrollR = rememberScrollState()
                    val hScrollS = rememberScrollState()
                    val hScrollA = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(vScroll)
                    ) {
                        TableSection(
                            title = "Table P: Timeline 1..16 (avg TOTAL ms)",
                            titleColor = Color(0xFF93C5FD),
                            header = TABLE_P_HEADER,
                            rows = variants.map { formatTablePRow(it) },
                            hScroll = hScrollP
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        TableSection(
                            title = "Table Q: First 12 Frames Stage Breakdown (avg ms)",
                            titleColor = Color(0xFF86EFAC),
                            header = TABLE_QR_HEADER,
                            rows = variants.map { formatStageRow(it.variantLabel, it.earlyStages) },
                            hScroll = hScrollQ
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        TableSection(
                            title = "Table R: Remaining Frames (>12) Stage Breakdown (avg ms)",
                            titleColor = Color(0xFFFCD34D),
                            header = TABLE_QR_HEADER,
                            rows = variants.map { formatStageRow(it.variantLabel, it.lateStages) },
                            hScroll = hScrollR
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        TableSection(
                            title = "Table S: Round Drift (avg TOTAL ms & >16.7 count)",
                            titleColor = Color(0xFFC4B5FD),
                            header = TABLE_S_HEADER,
                            rows = variants.map { formatTableSRow(it) },
                            hScroll = hScrollS
                        )

                        val variantA = variants.firstOrNull { it.variantId == "A" }
                        if (variantA != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Variant A First 12 Frames per Round",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF9A8D4)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Column(modifier = Modifier.horizontalScroll(hScrollA)) {
                                for (round in 1..BENCH_ROUNDS) {
                                    Text(
                                        text = "-- Round $round --",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFFF9A8D4),
                                        softWrap = false,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = VARIANT_A_DETAIL_HEADER,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF93C5FD),
                                        softWrap = false,
                                        maxLines = 1
                                    )
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
                                    val earlyRoundSamples = variantA.samplesByRound[round]
                                        .orEmpty()
                                        .take(EARLY_FRAME_COUNT)
                                    earlyRoundSamples.forEachIndexed { idx, sample ->
                                        Text(
                                            text = formatSampleDetailRow(sample),
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
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
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
