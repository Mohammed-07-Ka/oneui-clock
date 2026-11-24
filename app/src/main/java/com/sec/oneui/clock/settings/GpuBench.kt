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
import kotlin.math.ceil

private const val BENCH_TAG = "GpuBench"
private const val REPEATS_PER_VARIANT = 3
private const val EARLY_FRAME_COUNT = 12
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
        id = "X0",
        label = "X0 baseline",
        override = BenchOverride()
    ),
    BenchVariantSpec(
        id = "X1",
        label = "X1 ctr:noBlur",
        override = BenchOverride(containerGlassMask = GlassEffectMask(noBlur = true))
    ),
    BenchVariantSpec(
        id = "X2",
        label = "X2 ctr:noLens",
        override = BenchOverride(containerGlassMask = GlassEffectMask(noLens = true))
    ),
    BenchVariantSpec(
        id = "X3",
        label = "X3 ctr:noColor",
        override = BenchOverride(containerGlassMask = GlassEffectMask(noColor = true))
    ),
    BenchVariantSpec(
        id = "X4",
        label = "X4 inr:noBlur",
        override = BenchOverride(innerGlassMask = GlassEffectMask(noBlur = true))
    ),
    BenchVariantSpec(
        id = "X5",
        label = "X5 inr:noLens",
        override = BenchOverride(innerGlassMask = GlassEffectMask(noLens = true))
    ),
    BenchVariantSpec(
        id = "X6",
        label = "X6 inr:noColor",
        override = BenchOverride(innerGlassMask = GlassEffectMask(noColor = true))
    ),
    BenchVariantSpec(
        id = "X6a",
        label = "X6a ctr:noHi",
        override = BenchOverride(containerGlassMask = GlassEffectMask(noHighlight = true))
    ),
    BenchVariantSpec(
        id = "X6b",
        label = "X6b ctr:noScrim",
        override = BenchOverride(containerGlassMask = GlassEffectMask(noScrim = true))
    ),
    BenchVariantSpec(
        id = "X6c",
        label = "X6c inr:noHi",
        override = BenchOverride(innerGlassMask = GlassEffectMask(noHighlight = true))
    ),
    BenchVariantSpec(
        id = "X6d",
        label = "X6d inr:noScrim",
        override = BenchOverride(innerGlassMask = GlassEffectMask(noScrim = true))
    ),
    BenchVariantSpec(
        id = "X7",
        label = "X7 D6+D7",
        override = BenchOverride(sheetFlatContainer = true, sheetNoInnerGlass = true)
    ),
    BenchVariantSpec(
        id = "X8",
        label = "X8 baseline 2",
        override = BenchOverride()
    )
)

data class BenchFrameSample(
    val totalNs: Long,
    val animationNs: Long,
    val layoutMeasureNs: Long,
    val drawNs: Long,
    val syncNs: Long,
    val cmdIssueNs: Long,
    val swapNs: Long,
    val unknownDelayNs: Long,
    val gpuNs: Long?
)

data class BenchVariantResult(
    val variantLabel: String,
    val frameCount: Int,
    val avgTotalMs: Double,
    val p95TotalMs: Double,
    val maxTotalMs: Double,
    val framesOver16Ms: Int,
    val earlyFrameCount: Int,
    val earlyAvgTotalMs: Double,
    val earlyMaxTotalMs: Double,
    val earlyOver16Ms: Int,
    val lateFrameCount: Int,
    val lateAvgTotalMs: Double,
    val lateMaxTotalMs: Double,
    val lateOver16Ms: Int,
    val avgAnimationMs: Double,
    val avgLayoutMeasureMs: Double,
    val avgDrawMs: Double,
    val avgSyncMs: Double,
    val avgCmdIssueMs: Double,
    val avgSwapMs: Double,
    val avgGpuMs: Double?,
    val avgUnknownDelayMs: Double
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
    var currentVariantIndex by mutableIntStateOf(0)
        private set
    var totalVariants by mutableIntStateOf(GPU_BENCH_VARIANTS.size)
        private set
    var results by mutableStateOf<List<BenchVariantResult>>(emptyList())
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var showResultsOverlay by mutableStateOf(false)
        private set

    private var runJob: Job? = null

    fun start() {
        if (isRunning) return
        runJob?.cancel()

        results = emptyList()
        errorMessage = null
        showResultsOverlay = false
        isWarmingUp = false
        currentVariantIndex = 0
        totalVariants = GPU_BENCH_VARIANTS.size
        isRunning = true

        runJob = coroutineScope.launch {
            var handlerThread: HandlerThread? = null
            var attachedWindow: Window? = null
            var attachedListener: Window.OnFrameMetricsAvailableListener? = null
            val sampleLock = Any()
            var capturing = false
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
                    val animationNs = frameMetrics.getMetric(FrameMetrics.ANIMATION_DURATION)
                    val layoutNs = frameMetrics.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION)
                    val drawNs = frameMetrics.getMetric(FrameMetrics.DRAW_DURATION)
                    val syncNs = frameMetrics.getMetric(FrameMetrics.SYNC_DURATION)
                    val cmdIssueNs = frameMetrics.getMetric(FrameMetrics.COMMAND_ISSUE_DURATION)
                    val swapNs = frameMetrics.getMetric(FrameMetrics.SWAP_BUFFERS_DURATION)
                    val unknownDelayNs = frameMetrics.getMetric(FrameMetrics.UNKNOWN_DELAY_DURATION)
                    val gpuNs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val v = frameMetrics.getMetric(FrameMetrics.GPU_DURATION)
                        if (v >= 0L) v else null
                    } else {
                        null
                    }
                    synchronized(sampleLock) {
                        if (capturing) {
                            currentSamples.add(
                                BenchFrameSample(
                                    totalNs = totalNs,
                                    animationNs = animationNs,
                                    layoutMeasureNs = layoutNs,
                                    drawNs = drawNs,
                                    syncNs = syncNs,
                                    cmdIssueNs = cmdIssueNs,
                                    swapNs = swapNs,
                                    unknownDelayNs = unknownDelayNs,
                                    gpuNs = gpuNs
                                )
                            )
                        }
                    }
                }

                // 1. Leave Settings, go to Alarm tab, make sure no sheet is open
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

                // 2. One WARM-UP cycle whose frames are discarded
                isWarmingUp = true
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
                isWarmingUp = false

                val collectedResults = mutableListOf<BenchVariantResult>()

                // 3. Execute variants in order, 3 repeats each
                GPU_BENCH_VARIANTS.forEachIndexed { index, spec ->
                    currentVariantIndex = index + 1
                    DiagFlags.updateBenchOverride(spec.override)
                    repeat(2) { withFrameNanos {} }

                    val pooledAll = mutableListOf<BenchFrameSample>()
                    val pooledEarly = mutableListOf<BenchFrameSample>()
                    val pooledLate = mutableListOf<BenchFrameSample>()

                    for (repeatIdx in 1..REPEATS_PER_VARIANT) {
                        synchronized(sampleLock) {
                            currentSamples.clear()
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
                        val repeatSnapshot: List<BenchFrameSample>
                        synchronized(sampleLock) {
                            repeatSnapshot = currentSamples.toList()
                            currentSamples.clear()
                        }

                        pooledAll.addAll(repeatSnapshot)
                        pooledEarly.addAll(repeatSnapshot.take(EARLY_FRAME_COUNT))
                        if (repeatSnapshot.size > EARLY_FRAME_COUNT) {
                            pooledLate.addAll(repeatSnapshot.drop(EARLY_FRAME_COUNT))
                        }

                        delay(OPEN_SETTLED_HOLD_MS)

                        closeSheetLikeCancel()
                        snapshotFlow {
                            !isSheetOpen() && !isSheetComposed() && !isSheetProgressRunning() && sheetProgressValue() <= 0.0001f
                        }.first { it }

                        delay(BETWEEN_REPEATS_MS)
                    }

                    collectedResults.add(
                        aggregateVariantSamples(
                            label = spec.label,
                            allSamples = pooledAll,
                            earlySamples = pooledEarly,
                            lateSamples = pooledLate
                        )
                    )
                }

                results = collectedResults
                val combinedText = formatBothTables(collectedResults)
                combinedText.lineSequence().forEach { line ->
                    Log.i(BENCH_TAG, line)
                }
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

private fun aggregateVariantSamples(
    label: String,
    allSamples: List<BenchFrameSample>,
    earlySamples: List<BenchFrameSample>,
    lateSamples: List<BenchFrameSample>
): BenchVariantResult {
    if (allSamples.isEmpty()) {
        return BenchVariantResult(
            variantLabel = label,
            frameCount = 0,
            avgTotalMs = 0.0,
            p95TotalMs = 0.0,
            maxTotalMs = 0.0,
            framesOver16Ms = 0,
            earlyFrameCount = 0,
            earlyAvgTotalMs = 0.0,
            earlyMaxTotalMs = 0.0,
            earlyOver16Ms = 0,
            lateFrameCount = 0,
            lateAvgTotalMs = 0.0,
            lateMaxTotalMs = 0.0,
            lateOver16Ms = 0,
            avgAnimationMs = 0.0,
            avgLayoutMeasureMs = 0.0,
            avgDrawMs = 0.0,
            avgSyncMs = 0.0,
            avgCmdIssueMs = 0.0,
            avgSwapMs = 0.0,
            avgGpuMs = null,
            avgUnknownDelayMs = 0.0
        )
    }

    val totalsSorted = allSamples.map { it.totalNs }.sorted()
    val avgTotalMs = totalsSorted.average() / 1_000_000.0
    val p95Index = (ceil(totalsSorted.size * 0.95).toInt() - 1).coerceIn(0, totalsSorted.lastIndex)
    val p95TotalMs = totalsSorted[p95Index] / 1_000_000.0
    val maxTotalMs = totalsSorted.last() / 1_000_000.0

    val gpuValues = allSamples.mapNotNull { it.gpuNs }
    val avgGpuMs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && gpuValues.isNotEmpty()) {
        gpuValues.average() / 1_000_000.0
    } else {
        null
    }

    val earlyTotals = earlySamples.map { it.totalNs }
    val earlyAvgMs = if (earlyTotals.isNotEmpty()) earlyTotals.average() / 1_000_000.0 else 0.0
    val earlyMaxMs = if (earlyTotals.isNotEmpty()) earlyTotals.max() / 1_000_000.0 else 0.0

    val lateTotals = lateSamples.map { it.totalNs }
    val lateAvgMs = if (lateTotals.isNotEmpty()) lateTotals.average() / 1_000_000.0 else 0.0
    val lateMaxMs = if (lateTotals.isNotEmpty()) lateTotals.max() / 1_000_000.0 else 0.0

    return BenchVariantResult(
        variantLabel = label,
        frameCount = allSamples.size,
        avgTotalMs = avgTotalMs,
        p95TotalMs = p95TotalMs,
        maxTotalMs = maxTotalMs,
        framesOver16Ms = allSamples.count { it.totalNs > OVER_16_MS_NANOS },
        earlyFrameCount = earlySamples.size,
        earlyAvgTotalMs = earlyAvgMs,
        earlyMaxTotalMs = earlyMaxMs,
        earlyOver16Ms = earlySamples.count { it.totalNs > OVER_16_MS_NANOS },
        lateFrameCount = lateSamples.size,
        lateAvgTotalMs = lateAvgMs,
        lateMaxTotalMs = lateMaxMs,
        lateOver16Ms = lateSamples.count { it.totalNs > OVER_16_MS_NANOS },
        avgAnimationMs = avgValidMs(allSamples) { it.animationNs },
        avgLayoutMeasureMs = avgValidMs(allSamples) { it.layoutMeasureNs },
        avgDrawMs = avgValidMs(allSamples) { it.drawNs },
        avgSyncMs = avgValidMs(allSamples) { it.syncNs },
        avgCmdIssueMs = avgValidMs(allSamples) { it.cmdIssueNs },
        avgSwapMs = avgValidMs(allSamples) { it.swapNs },
        avgGpuMs = avgGpuMs,
        avgUnknownDelayMs = avgValidMs(allSamples) { it.unknownDelayNs }
    )
}

private const val TABLE_A_HEADER =
    "variant         | frames | avg tot | p95 tot | max tot | >16.7 | E frm | E avg  | E max  | E>16.7 | L frm | L avg  | L max  | L>16.7"

private const val TABLE_B_HEADER =
    "variant         | anim   | layout | draw   | sync   | cmd-iss | swap   | GPU    | unk-dly"

private fun formatTableARow(row: BenchVariantResult): String = String.format(
    Locale.US,
    "%-15s | %6d | %7.2f | %7.2f | %7.2f | %5d | %5d | %6.2f | %6.2f | %6d | %5d | %6.2f | %6.2f | %6d",
    row.variantLabel,
    row.frameCount,
    row.avgTotalMs,
    row.p95TotalMs,
    row.maxTotalMs,
    row.framesOver16Ms,
    row.earlyFrameCount,
    row.earlyAvgTotalMs,
    row.earlyMaxTotalMs,
    row.earlyOver16Ms,
    row.lateFrameCount,
    row.lateAvgTotalMs,
    row.lateMaxTotalMs,
    row.lateOver16Ms
)

private fun formatTableBRow(row: BenchVariantResult): String {
    val gpuStr = row.avgGpuMs?.let { String.format(Locale.US, "%.2f", it) } ?: "n/a"
    return String.format(
        Locale.US,
        "%-15s | %6.2f | %6.2f | %6.2f | %6.2f | %7.2f | %6.2f | %6s | %7.2f",
        row.variantLabel,
        row.avgAnimationMs,
        row.avgLayoutMeasureMs,
        row.avgDrawMs,
        row.avgSyncMs,
        row.avgCmdIssueMs,
        row.avgSwapMs,
        gpuStr,
        row.avgUnknownDelayMs
    )
}

fun formatBothTables(results: List<BenchVariantResult>): String {
    val sb = StringBuilder()
    sb.appendLine("=== Table A (Totals & Early/Late, ms) ===")
    sb.appendLine(TABLE_A_HEADER)
    results.forEach { sb.appendLine(formatTableARow(it)) }
    sb.appendLine()
    sb.appendLine("=== Table B (Stage Breakdown, avg ms) ===")
    sb.appendLine(TABLE_B_HEADER)
    results.forEach { sb.appendLine(formatTableBRow(it)) }
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
                    "Benchmark running\u2026 warm-up (0/${runner.totalVariants})"
                } else {
                    "Benchmark running\u2026 variant ${runner.currentVariantIndex}/${runner.totalVariants}"
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
                .heightIn(max = 560.dp),
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
                        text = if (runner.errorMessage != null) "GPU Benchmark Error" else "GPU Benchmark v2 (Add Alarm)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (runner.errorMessage != null) Color(0xFFF87171) else Color.White
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (runner.results.isNotEmpty() || runner.errorMessage != null) {
                            TextButton(
                                onClick = {
                                    val copyText = runner.errorMessage ?: formatBothTables(runner.results)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("GPU Benchmark v2", copyText))
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
                    val vScroll = rememberScrollState()
                    val hScrollA = rememberScrollState()
                    val hScrollB = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(vScroll)
                    ) {
                        Text(
                            text = "Table A: Total & Early(1..12)/Late ms",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF93C5FD)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(modifier = Modifier.horizontalScroll(hScrollA)) {
                            Text(
                                text = TABLE_A_HEADER,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF93C5FD),
                                softWrap = false,
                                maxLines = 1
                            )
                            HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
                            runner.results.forEachIndexed { idx, row ->
                                Text(
                                    text = formatTableARow(row),
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

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Table B: Stage Breakdown (avg ms)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF86EFAC)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(modifier = Modifier.horizontalScroll(hScrollB)) {
                            Text(
                                text = TABLE_B_HEADER,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF86EFAC),
                                softWrap = false,
                                maxLines = 1
                            )
                            HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
                            runner.results.forEachIndexed { idx, row ->
                                Text(
                                    text = formatTableBRow(row),
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
                }
            }
        }
    }
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}
