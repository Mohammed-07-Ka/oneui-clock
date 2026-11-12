package com.sec.oneui.clock.timer.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import com.sec.oneui.clock.timer.service.TimerForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class TimerState {
    IDLE, RUNNING, PAUSED, FINISHED
}

@Immutable
data class TimerUiState(
    val timerState: TimerState = TimerState.IDLE,
    val remainingMillis: Long = 0L,
    val totalMillis: Long = 0L
)

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext

    val timerState: StateFlow<TimerState> = TimerForegroundService.timerState
    val remainingMillis: StateFlow<Long> = TimerForegroundService.remainingMillis
    val totalMillis: StateFlow<Long> = TimerForegroundService.totalMillis

    var inputHours = MutableStateFlow(0)
    var inputMinutes = MutableStateFlow(5)
    var inputSeconds = MutableStateFlow(0)

    fun startTimer() {
        val configuredMillis = (inputHours.value * 3600L + inputMinutes.value * 60L + inputSeconds.value) * 1000L
        if (configuredMillis <= 0L) return
        TimerForegroundService.startTimer(context, configuredMillis)
    }

    fun pauseTimer() {
        TimerForegroundService.pauseTimer(context)
    }

    fun resumeTimer() {
        TimerForegroundService.resumeTimer(context)
    }

    fun resetTimer() {
        TimerForegroundService.cancelTimer(context)
    }

    fun addOneMinute() {
        if (timerState.value == TimerState.IDLE) {
            inputMinutes.value = (inputMinutes.value + 1) % 60
        } else {
            TimerForegroundService.addOneMinute(context)
        }
    }

    fun applyPreset(minutes: Int) {
        inputHours.value = 0
        inputMinutes.value = minutes
        inputSeconds.value = 0
        val millis = minutes * 60 * 1000L
        TimerForegroundService.startTimer(context, millis)
    }

    fun dismissFinishedAlarm() {
        TimerForegroundService.dismissAlarm(context)
    }
}
