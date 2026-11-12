package com.sec.oneui.clock.timer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sec.oneui.clock.MainActivity
import com.sec.oneui.clock.timer.ui.TimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

class TimerForegroundService : Service() {

    companion object {
        private const val TAG = "TimerService"

        const val CHANNEL_RUNNING_ID = "oneui_timer_running_channel"
        const val CHANNEL_ALERT_ID = "oneui_timer_alert_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START_TIMER = "com.sec.oneui.clock.timer.ACTION_START"
        const val ACTION_PAUSE_TIMER = "com.sec.oneui.clock.timer.ACTION_PAUSE"
        const val ACTION_RESUME_TIMER = "com.sec.oneui.clock.timer.ACTION_RESUME"
        const val ACTION_CANCEL_TIMER = "com.sec.oneui.clock.timer.ACTION_CANCEL"
        const val ACTION_DISMISS_ALARM = "com.sec.oneui.clock.timer.ACTION_DISMISS"
        const val ACTION_ADD_ONE_MINUTE = "com.sec.oneui.clock.timer.ACTION_ADD_MINUTE"

        const val EXTRA_DURATION_MILLIS = "extra_duration_millis"

        // Global single-source-of-truth flows for UI observers (e.g. TimerViewModel & TimerScreen)
        private val _timerState = MutableStateFlow(TimerState.IDLE)
        val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

        private val _remainingMillis = MutableStateFlow(0L)
        val remainingMillis: StateFlow<Long> = _remainingMillis.asStateFlow()

        private val _totalMillis = MutableStateFlow(0L)
        val totalMillis: StateFlow<Long> = _totalMillis.asStateFlow()

        fun startTimer(context: Context, durationMillis: Long) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_START_TIMER
                putExtra(EXTRA_DURATION_MILLIS, durationMillis)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun pauseTimer(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_PAUSE_TIMER
            }
            context.startService(intent)
        }

        fun resumeTimer(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_RESUME_TIMER
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun cancelTimer(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_CANCEL_TIMER
            }
            context.startService(intent)
        }

        fun dismissAlarm(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_DISMISS_ALARM
            }
            context.startService(intent)
        }

        fun addOneMinute(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_ADD_ONE_MINUTE
            }
            context.startService(intent)
        }

        fun formatDuration(millis: Long): String {
            val totalSeconds = (millis + 999L) / 1000L
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tickerJob: Job? = null
    private var targetEndTime = 0L

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY
        Log.d(TAG, "onStartCommand: action=$action")

        when (action) {
            ACTION_START_TIMER -> {
                val duration = intent.getLongExtra(EXTRA_DURATION_MILLIS, 0L)
                if (duration > 0L) {
                    startTimerInternal(duration)
                }
            }
            ACTION_PAUSE_TIMER -> {
                pauseTimerInternal()
            }
            ACTION_RESUME_TIMER -> {
                resumeTimerInternal()
            }
            ACTION_CANCEL_TIMER -> {
                cancelTimerInternal()
            }
            ACTION_DISMISS_ALARM -> {
                dismissAlarmInternal()
            }
            ACTION_ADD_ONE_MINUTE -> {
                addOneMinuteInternal()
            }
        }

        return START_NOT_STICKY
    }

    private fun startTimerInternal(duration: Long) {
        stopAlertSoundAndVibration()
        _totalMillis.value = duration
        _remainingMillis.value = duration
        targetEndTime = SystemClock.elapsedRealtime() + duration
        _timerState.value = TimerState.RUNNING

        val notification = buildRunningNotification(duration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startTicker()
    }

    private fun pauseTimerInternal() {
        if (_timerState.value != TimerState.RUNNING) return
        tickerJob?.cancel()
        val remain = (targetEndTime - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        _remainingMillis.value = remain
        _timerState.value = TimerState.PAUSED

        val notification = buildPausedNotification(remain)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun resumeTimerInternal() {
        if (_timerState.value != TimerState.PAUSED) return
        val currentRemain = _remainingMillis.value
        if (currentRemain <= 0L) return

        targetEndTime = SystemClock.elapsedRealtime() + currentRemain
        _timerState.value = TimerState.RUNNING

        val notification = buildRunningNotification(currentRemain)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startTicker()
    }

    private fun cancelTimerInternal() {
        tickerJob?.cancel()
        tickerJob = null
        stopAlertSoundAndVibration()
        _timerState.value = TimerState.IDLE
        _remainingMillis.value = 0L
        _totalMillis.value = 0L

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun dismissAlarmInternal() {
        stopAlertSoundAndVibration()
        _timerState.value = TimerState.IDLE
        _remainingMillis.value = 0L
        _totalMillis.value = 0L

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun addOneMinuteInternal() {
        val extra = 60_000L
        _totalMillis.value += extra
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (_timerState.value) {
            TimerState.RUNNING -> {
                targetEndTime += extra
                val remain = (targetEndTime - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                _remainingMillis.value = remain
                nm.notify(NOTIFICATION_ID, buildRunningNotification(remain))
            }
            TimerState.PAUSED -> {
                val newRemain = _remainingMillis.value + extra
                _remainingMillis.value = newRemain
                nm.notify(NOTIFICATION_ID, buildPausedNotification(newRemain))
            }
            else -> {}
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            var lastPostedSecond = -1L

            while (_timerState.value == TimerState.RUNNING) {
                val now = SystemClock.elapsedRealtime()
                val remain = (targetEndTime - now).coerceAtLeast(0L)
                _remainingMillis.value = remain

                if (remain <= 0L) {
                    onTimerFinished()
                    break
                }

                // Update notification text roughly once per whole second
                val currentSecond = (remain + 999L) / 1000L
                if (currentSecond != lastPostedSecond) {
                    lastPostedSecond = currentSecond
                    nm.notify(NOTIFICATION_ID, buildRunningNotification(remain))
                }

                delay(40L) // ~25 updates per second for smooth Compose UI circular rendering
            }
        }
    }

    private fun onTimerFinished() {
        tickerJob?.cancel()
        tickerJob = null
        _remainingMillis.value = 0L
        _timerState.value = TimerState.FINISHED

        // Play alert audio and vibrate phone
        playAlertSound()
        startVibration()

        // Acquire partial wake lock to keep ringing until dismissed
        acquireWakeLock()

        // Post completion notification
        val notification = buildFinishedNotification()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun buildRunningNotification(remainMillis: Long): Notification {
        val timeStr = formatDuration(remainMillis)
        val openAppPendingIntent = createOpenAppPendingIntent()

        val pauseIntent = Intent(this, TimerForegroundService::class.java).apply {
            action = ACTION_PAUSE_TIMER
        }
        val pausePendingIntent = PendingIntent.getService(
            this, 201, pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, TimerForegroundService::class.java).apply {
            action = ACTION_CANCEL_TIMER
        }
        val cancelPendingIntent = PendingIntent.getService(
            this, 202, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_RUNNING_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Timer")
            .setContentText("Timer Running: $timeStr")
            .setSubText(timeStr)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)
            .build()
    }

    private fun buildPausedNotification(remainMillis: Long): Notification {
        val timeStr = formatDuration(remainMillis)
        val openAppPendingIntent = createOpenAppPendingIntent()

        val resumeIntent = Intent(this, TimerForegroundService::class.java).apply {
            action = ACTION_RESUME_TIMER
        }
        val resumePendingIntent = PendingIntent.getService(
            this, 203, resumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, TimerForegroundService::class.java).apply {
            action = ACTION_CANCEL_TIMER
        }
        val cancelPendingIntent = PendingIntent.getService(
            this, 204, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_RUNNING_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Timer")
            .setContentText("Timer Paused: $timeStr")
            .setSubText("Paused")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_media_play, "Resume", resumePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)
            .build()
    }

    private fun buildFinishedNotification(): Notification {
        val openAppPendingIntent = createOpenAppPendingIntent()

        val dismissIntent = Intent(this, TimerForegroundService::class.java).apply {
            action = ACTION_DISMISS_ALARM
        }
        val dismissPendingIntent = PendingIntent.getService(
            this, 205, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ALERT_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Timer")
            .setContentText("Time's up!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
            .build()
    }

    private fun createOpenAppPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("nav_tab", 2) // Timer tab
        }
        return PendingIntent.getActivity(
            this, 200, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun playAlertSound() {
        try {
            val prefs = applicationContext.getSharedPreferences("clock_settings", Context.MODE_PRIVATE)
            val customVolume = prefs.getFloat("alarm_custom_volume", 0.8f).coerceIn(0.01f, 1.0f)

            // Adjust stream volume
            try {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val maxStream = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                val calculatedStream = (maxStream * customVolume).roundToInt().coerceIn(1, maxStream)
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, calculatedStream, 0)
            } catch (e: Exception) {
                Log.w(TAG, "Cannot setStreamVolume: ${e.message}")
            }

            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alertUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                setVolume(customVolume, customVolume)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing alert tone: ${e.message}")
        }
    }

    private fun startVibration() {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            val pattern = longArrayOf(0, 800, 400, 800, 400)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error vibrating phone: ${e.message}")
        }
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "oneui:TimerAlertWakeLock").apply {
                    acquire(10 * 60 * 1000L) // 10 minutes timeout safety
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed acquiring wake lock: ${e.message}")
        }
    }

    private fun stopAlertSoundAndVibration() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player: ${e.message}")
        }

        try {
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling vibrator: ${e.message}")
        }

        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing wake lock: ${e.message}")
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val runningChannel = NotificationChannel(
                CHANNEL_RUNNING_ID,
                "Active Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing notification for running timer countdown"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }
            manager.createNotificationChannel(runningChannel)

            val alertChannel = NotificationChannel(
                CHANNEL_ALERT_ID,
                "Timer Alarm Alert",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notification when countdown reaches zero"
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(alertChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tickerJob?.cancel()
        serviceScope.cancel()
        stopAlertSoundAndVibration()
        _timerState.value = TimerState.IDLE
    }
}
