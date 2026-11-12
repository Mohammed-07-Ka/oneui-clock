package com.sec.oneui.clock.timer.ui

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class TimerState {
    IDLE, RUNNING, PAUSED, FINISHED
}

class TimerViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext

    private val _timerState = MutableStateFlow(TimerState.IDLE)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val _remainingMillis = MutableStateFlow(0L)
    val remainingMillis: StateFlow<Long> = _remainingMillis.asStateFlow()

    private val _totalMillis = MutableStateFlow(0L)
    val totalMillis: StateFlow<Long> = _totalMillis.asStateFlow()

    var inputHours = MutableStateFlow(0)
    var inputMinutes = MutableStateFlow(5)
    var inputSeconds = MutableStateFlow(0)

    private var targetEndTime = 0L
    private var tickerJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null

    fun startTimer() {
        val configuredMillis = (inputHours.value * 3600L + inputMinutes.value * 60L + inputSeconds.value) * 1000L
        if (configuredMillis <= 0L) return

        _totalMillis.value = configuredMillis
        _remainingMillis.value = configuredMillis
        targetEndTime = SystemClock.elapsedRealtime() + configuredMillis

        _timerState.value = TimerState.RUNNING
        startTicker()
    }

    fun pauseTimer() {
        if (_timerState.value != TimerState.RUNNING) return
        tickerJob?.cancel()
        val remain = targetEndTime - SystemClock.elapsedRealtime()
        _remainingMillis.value = if (remain > 0) remain else 0L
        _timerState.value = TimerState.PAUSED
    }

    fun resumeTimer() {
        if (_timerState.value != TimerState.PAUSED) return
        targetEndTime = SystemClock.elapsedRealtime() + _remainingMillis.value
        _timerState.value = TimerState.RUNNING
        startTicker()
    }

    fun resetTimer() {
        tickerJob?.cancel()
        stopAlarmSound()
        _remainingMillis.value = 0L
        _totalMillis.value = 0L
        _timerState.value = TimerState.IDLE
    }

    fun addOneMinute() {
        val extra = 60_000L
        _totalMillis.value += extra
        if (_timerState.value == TimerState.RUNNING) {
            targetEndTime += extra
            _remainingMillis.value = targetEndTime - SystemClock.elapsedRealtime()
        } else if (_timerState.value == TimerState.PAUSED) {
            _remainingMillis.value += extra
        } else if (_timerState.value == TimerState.IDLE) {
            inputMinutes.value = (inputMinutes.value + 1) % 60
        }
    }

    fun applyPreset(minutes: Int) {
        tickerJob?.cancel()
        stopAlarmSound()
        inputHours.value = 0
        inputMinutes.value = minutes
        inputSeconds.value = 0
        val millis = minutes * 60 * 1000L
        _totalMillis.value = millis
        _remainingMillis.value = millis
        targetEndTime = SystemClock.elapsedRealtime() + millis
        _timerState.value = TimerState.RUNNING
        startTicker()
    }

    fun dismissFinishedAlarm() {
        stopAlarmSound()
        resetTimer()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (_timerState.value == TimerState.RUNNING) {
                val remain = targetEndTime - SystemClock.elapsedRealtime()
                if (remain <= 0L) {
                    _remainingMillis.value = 0L
                    _timerState.value = TimerState.FINISHED
                    playAlarmSound()
                    break
                } else {
                    _remainingMillis.value = remain
                }
                delay(30) // Smooth circle and text update
            }
        }
    }

    private fun playAlarmSound() {
        try {
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, alertUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("TimerViewModel", "Error playing timer finish sound: ${e.message}")
        }
    }

    private fun stopAlarmSound() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e("TimerViewModel", "Error stopping audio: ${e.message}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
        stopAlarmSound()
    }
}
