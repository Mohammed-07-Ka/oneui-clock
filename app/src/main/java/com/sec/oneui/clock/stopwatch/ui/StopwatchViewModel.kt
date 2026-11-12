package com.sec.oneui.clock.stopwatch.ui

import android.os.SystemClock
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable
data class LapItem(
    val lapIndex: Int,
    val lapTimeMillis: Long,
    val totalTimeMillis: Long
) {
    fun formatLapTime(): String = formatStopwatchTime(lapTimeMillis)
    fun formatTotalTime(): String = formatStopwatchTime(totalTimeMillis)

    companion object {
        fun formatStopwatchTime(millis: Long): String {
            val hours = millis / 3600000L
            val minutes = (millis % 3600000L) / 60000L
            val seconds = (millis % 60000L) / 1000L
            val centis = (millis % 1000L) / 10L

            return if (hours > 0) {
                String.format("%02d:%02d:%02d.%02d", hours, minutes, seconds, centis)
            } else {
                String.format("%02d:%02d.%02d", minutes, seconds, centis)
            }
        }
    }
}

@Immutable
data class StopwatchUiState(
    val stopwatchState: StopwatchState = StopwatchState.IDLE,
    val elapsedMillis: Long = 0L,
    val laps: List<LapItem> = emptyList()
)

enum class StopwatchState {
    IDLE, RUNNING, PAUSED
}

class StopwatchViewModel : ViewModel() {

    private val _stopwatchState = MutableStateFlow(StopwatchState.IDLE)
    val stopwatchState: StateFlow<StopwatchState> = _stopwatchState.asStateFlow()

    private val _elapsedMillis = MutableStateFlow(0L)
    val elapsedMillis: StateFlow<Long> = _elapsedMillis.asStateFlow()

    private val _laps = MutableStateFlow<List<LapItem>>(emptyList())
    val laps: StateFlow<List<LapItem>> = _laps.asStateFlow()

    private var baseTime = 0L
    private var pausedOffset = 0L
    private var lastLapMark = 0L
    private var tickerJob: Job? = null

    fun start() {
        if (_stopwatchState.value == StopwatchState.RUNNING) return
        baseTime = SystemClock.elapsedRealtime()
        pausedOffset = 0L
        lastLapMark = 0L
        _laps.value = emptyList()
        _stopwatchState.value = StopwatchState.RUNNING
        startTicker()
    }

    fun pause() {
        if (_stopwatchState.value != StopwatchState.RUNNING) return
        tickerJob?.cancel()
        pausedOffset = SystemClock.elapsedRealtime() - baseTime
        _elapsedMillis.value = pausedOffset
        _stopwatchState.value = StopwatchState.PAUSED
    }

    fun resume() {
        if (_stopwatchState.value != StopwatchState.PAUSED) return
        baseTime = SystemClock.elapsedRealtime() - pausedOffset
        _stopwatchState.value = StopwatchState.RUNNING
        startTicker()
    }

    fun reset() {
        tickerJob?.cancel()
        baseTime = 0L
        pausedOffset = 0L
        lastLapMark = 0L
        _elapsedMillis.value = 0L
        _laps.value = emptyList()
        _stopwatchState.value = StopwatchState.IDLE
    }

    fun addLap() {
        if (_stopwatchState.value != StopwatchState.RUNNING) return
        val currentTotal = SystemClock.elapsedRealtime() - baseTime
        val lapDelta = currentTotal - lastLapMark
        lastLapMark = currentTotal

        val newLap = LapItem(
            lapIndex = _laps.value.size + 1,
            lapTimeMillis = lapDelta,
            totalTimeMillis = currentTotal
        )
        // Insert at beginning for One UI top-most recent lap list
        _laps.value = listOf(newLap) + _laps.value
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (_stopwatchState.value == StopwatchState.RUNNING) {
                _elapsedMillis.value = SystemClock.elapsedRealtime() - baseTime
                delay(15) // ~60fps smooth tick rate
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
    }
}
