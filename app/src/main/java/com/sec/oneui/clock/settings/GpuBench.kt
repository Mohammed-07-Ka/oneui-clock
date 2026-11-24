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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
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
private const val OPEN_SETTLED_HOLD_MS = 560L
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
        id = "V0",
        label = "V0 baseline",
        override = BenchOverride()
    ),
    BenchVariantSpec(
        id = "V1",
        label = "V1 D6 flat",
        override = BenchOverride(sheetFlatContainer = true)
    ),
    BenchVariantSpec(
        id = "V2",
        label = "V2 D7 no inner",
        override = BenchOverride(sheetNoInnerGlass = true)
    ),
    BenchVariantSpec(
        id = "V3",
        label = "V3 D8 no fx",
        override = BenchOverride(sheetNoEffects = true)
    ),
    BenchVariantSpec(
        id = "V4",
        label = "V4 D6+D7",
        override = BenchOverride(sheetFlatContainer = true, sheetNoInnerGlass = true)
    ),
    BenchVariantSpec(
        id = "V5",
        label = "V5 D1+D2+D3",
        override = BenchOverride(
            noPageScaleAlpha = true,
            snapTabBarFabGear = true,
            noDimmer = true
        )
    )
)

data class BenchFrameSample(
    val totalNs: Long,
    val cmdIssueNs: Long,
    val swapNs: Long,
    val drawNs: Long,
    val layoutMeasureNs: Long,
    val gpuNs: Long?
)

data class BenchVariantResult(
    val variantLabel: String,
    val frameCount: Int,
    val avgTotalMs: Double,
    val p95TotalMs: Double,
    val maxTotalMs: Double,
    val avgGpuMs: Double?,
    val avgCmdIssueMs: Double,
    val avgSwapMs: Double,
    val avgDrawMs: Double,
    val avgLayoutMeasureMs: Double,
    val framesOver16Ms: Int
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
                    val cmdIssueNs = frameMetrics.getMetric(FrameMetrics.COMMAND_ISSUE_DURATION)
                    val swapNs = frameMetrics.getMetric(FrameMetrics.SWAP_BUFFERS_DURATION)
                    val drawNs = frameMetrics.getMetric(FrameMetrics.DRAW_DURATION)
                    val layoutNs = frameMetrics.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION)
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
                                    cmdIssueNs = cmdIssueNs,
                                    swapNs = swapNs,
                                    drawNs = drawNs,
                                    layoutMeasureNs = layoutNs,
                                    gpuNs = gpuNs
                                )
                            )
                        }
                    }
                }

                // 1. Leave Settings, navigate to Alarm tab, ensure no sheet is open
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

                val collectedResults = mutableListOf<BenchVariantResult>()

                // 2. Execute V0..V5, 3 repeats each
                GPU_BENCH_VARIANTS.forEachIndexed { index, spec ->
                    currentVariantIndex = index + 1
                    DiagFlags.updateBenchOverride(spec.override)
                    repeat(2) { withFrameNanos {} }

                    val pooledSamples = mutableListOf<BenchFrameSample>()

                    for (repeatIdx in 1..REPEATS_PER_VARIANT) {
                        synchronized(sampleLock) {
                            currentSamples.clear()
                            capturing = true
                        }
                        attachedWindow = window
                        attachedListener = frameListener
                        window.addOnFrameMetricsAvailableListener(frameListener, bgHandler)

                        // Trigger sheet open via the exact FAB path
                        openSheetLikeFab()

                        // Wait until open animation (repeat(3) + sheetProgress.animateTo(1f)) fully settles
                        snapshotFlow {
                            isSheetOpen() && isSheetComposed() && !isSheetProgressRunning() && sheetProgressValue() >= 0.9999f
                        }.first { it }

                        // Allow async FrameMetrics callback for the final animation frame to flush
                        withFrameNanos {}
                        delay(METRICS_FLUSH_MS)

                        detachListenerSafe()
                        synchronized(sampleLock) {
                            pooledSamples.addAll(currentSamples)
                            currentSamples.clear()
                        }

                        // Hold open ~600ms total before closing
                        delay(OPEN_SETTLED_HOLD_MS)

                        // Close through exact Cancel path and wait until uncomposed
                        closeSheetLikeCancel()
                        snapshotFlow {
                            !isSheetOpen() && !isSheetComposed() && !isSheetProgressRunning() && sheetProgressValue() <= 0.0001f
                        }.first { it }

                        delay(BETWEEN_REPEATS_MS)
                    }

                    collectedResults.add(aggregateVariantSamples(spec.label, pooledSamples))
                }

                results = collectedResults
                val tableText = formatBenchmarkTable(collectedResults)
                tableText.lineSequence().forEach { line ->
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
        isRunning = false
    }

    fun dismissResults() {
        showResultsOverlay = false
        errorMessage = null
    }
}

private fun aggregateVariantSamples(
    label: String,
    samples: List<BenchFrameSample>
): BenchVariantResult {
    if (samples.isEmpty()) {
        return BenchVariantResult(
            variantLabel = label,
            frameCount = 0,
            avgTotalMs = 0.0,
            p95TotalMs = 0.0,
            maxTotalMs = 0.0,
            avgGpuMs = null,
            avgCmdIssueMs = 0.0,
            avgSwapMs = 0.0,
            avgDrawMs = 0.0,
            avgLayoutMeasureMs = 0.0,
            framesOver16Ms = 0
        )
    }

    val totalsSorted = samples.map { it.totalNs }.sorted()
    val avgTotalMs = samples.map { it.totalNs }.average() / 1_000_000.0
    val p95Index = (ceil(totalsSorted.size * 0.95).toInt() - 1).coerceIn(0, totalsSorted.lastIndex)
    val p95TotalMs = totalsSorted[p95Index] / 1_000_000.0
    val maxTotalMs = totalsSorted.last() / 1_000_000.0

    val gpuValues = samples.mapNotNull { it.gpuNs }
    val avgGpuMs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && gpuValues.isNotEmpty()) {
        gpuValues.average() / 1_000_000.0
    } else {
        null
    }

    val validCmd = samples.mapNotNull { if (it.cmdIssueNs >= 0L) it.cmdIssueNs else null }
    val validSwap = samples.mapNotNull { if (it.swapNs >= 0L) it.swapNs else null }
    val validDraw = samples.mapNotNull { if (it.drawNs >= 0L) it.drawNs else null }
    val validLayout = samples.mapNotNull { if (it.layoutMeasureNs >= 0L) it.layoutMeasureNs else null }

    return BenchVariantResult(
        variantLabel = label,
        frameCount = samples.size,
        avgTotalMs = avgTotalMs,
        p95TotalMs = p95TotalMs,
        maxTotalMs = maxTotalMs,
        avgGpuMs = avgGpuMs,
        avgCmdIssueMs = if (validCmd.isNotEmpty()) validCmd.average() / 1_000_000.0 else 0.0,
        avgSwapMs = if (validSwap.isNotEmpty()) validSwap.average() / 1_000_000.0 else 0.0,
        avgDrawMs = if (validDraw.isNotEmpty()) validDraw.average() / 1_000_000.0 else 0.0,
        avgLayoutMeasureMs = if (validLayout.isNotEmpty()) validLayout.average() / 1_000_000.0 else 0.0,
        framesOver16Ms = samples.count { it.totalNs > OVER_16_MS_NANOS }
    )
}

fun formatBenchmarkTable(results: List<BenchVariantResult>): String {
    val sb = StringBuilder()
    sb.appendLine(
        "variant        | frames | avg total ms | p95 total ms | max total ms | avg GPU ms | avg cmd-issue ms | avg swap ms | frames > 16.7 ms"
    )
    results.forEach { row ->
        val gpuStr = row.avgGpuMs?.let { String.format(Locale.US, "%.2f", it) } ?: "n/a"
        sb.appendLine(
            String.format(
                Locale.US,
                "%-14s | %6d | %12.2f | %12.2f | %12.2f | %10s | %16.2f | %11.2f | %16d",
                row.variantLabel,
                row.frameCount,
                row.avgTotalMs,
                row.p95TotalMs,
                row.maxTotalMs,
                gpuStr,
                row.avgCmdIssueMs,
                row.avgSwapMs,
                row.framesOver16Ms
            )
        )
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
                Text(
                    text = "Benchmark running\u2026 variant ${runner.currentVariantIndex}/${runner.totalVariants}",
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
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xF210121A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (runner.errorMessage != null) "GPU Benchmark Error" else "GPU Benchmark (Add Alarm)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (runner.errorMessage != null) Color(0xFFF87171) else Color.White
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (runner.results.isNotEmpty() || runner.errorMessage != null) {
                            TextButton(
                                onClick = {
                                    val copyText = runner.errorMessage ?: formatBenchmarkTable(runner.results)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("GPU Benchmark", copyText))
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
                Spacer(modifier = Modifier.height(8.dp))

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
                    val hScroll = rememberScrollState()
                    val vScroll = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(vScroll)
                            .horizontalScroll(hScroll)
                    ) {
                        val headerLine =
                            "variant        | frames | avg total ms | p95 total ms | max total ms | avg GPU ms | avg cmd-issue ms | avg swap ms | frames > 16.7 ms"
                        Text(
                            text = headerLine,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF93C5FD),
                            softWrap = false,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
                        Spacer(modifier = Modifier.height(4.dp))

                        runner.results.forEachIndexed { idx, row ->
                            val gpuStr = row.avgGpuMs?.let { String.format(Locale.US, "%.2f", it) } ?: "n/a"
                            val line = String.format(
                                Locale.US,
                                "%-14s | %6d | %12.2f | %12.2f | %12.2f | %10s | %16.2f | %11.2f | %16d",
                                row.variantLabel,
                                row.frameCount,
                                row.avgTotalMs,
                                row.p95TotalMs,
                                row.maxTotalMs,
                                gpuStr,
                                row.avgCmdIssueMs,
                                row.avgSwapMs,
                                row.framesOver16Ms
                            )
                            Text(
                                text = line,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White,
                                softWrap = false,
                                maxLines = 1,
                                modifier = Modifier
                                    .background(
                                        if (idx % 2 == 1) Color.White.copy(alpha = 0.05f) else Color.Transparent
                                    )
                                    .padding(vertical = 3.dp)
                            )
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
