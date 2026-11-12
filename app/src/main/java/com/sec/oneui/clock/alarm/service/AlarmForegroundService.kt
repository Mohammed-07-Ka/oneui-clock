package com.sec.oneui.clock.alarm.service

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
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sec.oneui.clock.alarm.data.AlarmDatabase
import com.sec.oneui.clock.alarm.data.AlarmEntity
import com.sec.oneui.clock.alarm.receiver.AlarmReceiver
import com.sec.oneui.clock.alarm.scheduler.AlarmScheduler
import kotlin.math.roundToInt
import com.sec.oneui.clock.alarm.ui.AlarmRingingActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlarmForegroundService : Service() {

    companion object {
        private const val TAG = "AlarmService"
        const val CHANNEL_ID = "oneui_alarm_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_ALARM = "com.sec.oneui.clock.service.START_ALARM"
        const val ACTION_STOP_ALARM = "com.sec.oneui.clock.service.STOP_ALARM"

        // StateFlow for In-App Ringing Banner
        private val _currentRingingAlarm = MutableStateFlow<AlarmEntity?>(null)
        val currentRingingAlarm = _currentRingingAlarm.asStateFlow()
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var gradualVolumeJob: Job? = null
    private var autoSilenceJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY
        val alarmId = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        Log.d(TAG, "onStartCommand: action=$action, alarmId=$alarmId")

        when (action) {
            ACTION_START_ALARM -> {
                startAlarm(alarmId)
            }
            ACTION_STOP_ALARM -> {
                stopAlarm()
            }
        }
        return START_NOT_STICKY
    }

    private fun startAlarm(alarmId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val alarm = if (alarmId != -1) {
                AlarmDatabase.getInstance(applicationContext).alarmDao().getAlarmById(alarmId)
            } else null

            _currentRingingAlarm.value = alarm ?: AlarmEntity(
                id = -1,
                hour = 7,
                minute = 0,
                label = "Alarm"
            )

            // 1. Build notification
            val notification = buildAlarmNotification(alarm)

            // 2. Start foreground with FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK on Android Q+ (API 29+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            // 3. Play audio tone (custom ringtone with fallback to system default)
            playRingtone(alarm?.ringtoneUri)

            // 4. Vibrate if enabled (check global preference too)
            val prefs = applicationContext.getSharedPreferences("clock_settings", Context.MODE_PRIVATE)
            val globalVibrate = prefs.getBoolean("default_vibrate", true)
            if (alarm?.vibrate != false && globalVibrate) {
                startVibration()
            }

            // 5. Auto-silence timer
            val autoSilenceMinutes = prefs.getInt("auto_silence_minutes", 10)
            if (autoSilenceMinutes > 0) {
                autoSilenceJob?.cancel()
                autoSilenceJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(autoSilenceMinutes * 60 * 1000L)
                    Log.d(TAG, "Auto-silence reached after $autoSilenceMinutes mins. Stopping alarm.")
                    stopAlarm()
                }
            }
        }
    }

    private fun stopAlarm() {
        _currentRingingAlarm.value = null
        autoSilenceJob?.cancel()
        autoSilenceJob = null
        gradualVolumeJob?.cancel()
        gradualVolumeJob = null

        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player: ${e.message}")
        } finally {
            try {
                mediaPlayer?.reset()
                mediaPlayer?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing media player: ${e.message}")
            }
            mediaPlayer = null
        }

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping vibration: ${e.message}")
        } finally {
            vibrator = null
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun playRingtone(customRingtoneUri: String?) {
        val prefs = applicationContext.getSharedPreferences("clock_settings", Context.MODE_PRIVATE)
        val gradualSeconds = prefs.getInt("gradual_volume_seconds", 0)
        val gradualVolume = gradualSeconds > 0
        val targetVolumeFraction = prefs.getFloat("alarm_custom_volume", 0.8f).coerceIn(0.01f, 1.0f)

        // Programmatically adjust system alarm stream volume based on custom volume percentage
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxStreamVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val calculatedStreamVolume = (maxStreamVolume * targetVolumeFraction).roundToInt().coerceIn(1, maxStreamVolume)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, calculatedStreamVolume, 0)
        } catch (e: Exception) {
            Log.w(TAG, "Failed setting alarm stream volume: ${e.message}")
        }

        fun createAndStartPlayer(uri: Uri): MediaPlayer? {
            val player = MediaPlayer()
            return try {
                player.setDataSource(applicationContext, uri)
                player.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                player.isLooping = true
                if (gradualVolume) {
                    val initialVol = (0.05f * targetVolumeFraction).coerceIn(0.01f, targetVolumeFraction)
                    player.setVolume(initialVol, initialVol)
                } else {
                    player.setVolume(targetVolumeFraction, targetVolumeFraction)
                }
                player.prepare()
                player.start()
                player
            } catch (e: Exception) {
                try {
                    player.release()
                } catch (_: Exception) {}
                null
            }
        }

        try {
            var playerStarted = false
            val defaultSavedUri = prefs.getString("default_ringtone_uri", null)
            val candidateUris = listOfNotNull(
                customRingtoneUri?.takeIf { it.isNotBlank() },
                defaultSavedUri?.takeIf { it.isNotBlank() }
            )

            for (uriStr in candidateUris) {
                try {
                    val p = createAndStartPlayer(Uri.parse(uriStr))
                    if (p != null) {
                        mediaPlayer = p
                        playerStarted = true
                        break
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed playing ringtone ($uriStr): ${e.message}")
                }
            }

            if (!playerStarted) {
                val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                if (defaultUri != null) {
                    try {
                        val p = createAndStartPlayer(defaultUri)
                        if (p != null) {
                            mediaPlayer = p
                            playerStarted = true
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed playing default alarm: ${e.message}")
                    }
                }
            }

            if (gradualVolume && playerStarted) {
                gradualVolumeJob = CoroutineScope(Dispatchers.Main).launch {
                    val totalDurationMs = (gradualSeconds.coerceIn(1, 120)) * 1000L
                    val stepIntervalMs = if (totalDurationMs <= 5000L) 100L else 250L
                    val totalSteps = (totalDurationMs / stepIntervalMs).toInt().coerceAtLeast(1)
                    val initialVol = (0.05f * targetVolumeFraction).coerceIn(0.01f, targetVolumeFraction)

                    for (step in 1..totalSteps) {
                        delay(stepIntervalMs)
                        val fraction = step.toFloat() / totalSteps
                        val vol = (initialVol + fraction * (targetVolumeFraction - initialVol)).coerceIn(initialVol, targetVolumeFraction)
                        try {
                            mediaPlayer?.setVolume(vol, vol)
                        } catch (_: Exception) {
                            break
                        }
                    }
                    try {
                        mediaPlayer?.setVolume(targetVolumeFraction, targetVolumeFraction)
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing ringtone: ${e.message}")
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

            val pattern = longArrayOf(0, 1000, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting vibration: ${e.message}")
        }
    }

    private fun buildAlarmNotification(alarm: AlarmEntity?): Notification {
        val alarmId = alarm?.id ?: -1
        val label = alarm?.label?.ifBlank { "Alarm" } ?: "Alarm"
        val timeStr = alarm?.formattedTime() ?: ""

        // Fullscreen intent
        val fullScreenIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, alarmId, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss action intent
        val dismissIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DISMISS
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this, alarmId * 10 + 1, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze action intent
        val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SNOOZE
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this, alarmId * 10 + 2, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(label)
            .setContentText("Ringing: $timeStr")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
            .addAction(android.R.drawable.ic_popup_reminder, "Snooze", snoozePendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "One UI Alarm ringtone and alert notifications"
                setSound(null, null)
                enableVibration(false) // Service handles vibration directly
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }
}
