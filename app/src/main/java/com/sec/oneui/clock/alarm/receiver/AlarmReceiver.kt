package com.sec.oneui.clock.alarm.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sec.oneui.clock.MainActivity
import com.sec.oneui.clock.alarm.data.AlarmDatabase
import com.sec.oneui.clock.alarm.scheduler.AlarmScheduler
import com.sec.oneui.clock.alarm.service.AlarmForegroundService
import com.sec.oneui.clock.alarm.ui.AlarmRingingActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "AlarmReceiver"
        const val ACTION_DISMISS = "com.sec.oneui.clock.ACTION_DISMISS_ALARM"
        const val ACTION_SNOOZE = "com.sec.oneui.clock.ACTION_SNOOZE_ALARM"
        const val UPCOMING_CHANNEL_ID = "upcoming_alarms_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "onReceive: action=$action")

        when (action) {
            AlarmScheduler.ACTION_ALARM_TRIGGER -> {
                val alarmId = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
                if (alarmId == -1) return

                // Clear any upcoming notification
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.cancel(200000 + alarmId)

                // 1. Start foreground alarm service for continuous ringtone and vibration using ContextCompat
                val serviceIntent = Intent(context, AlarmForegroundService::class.java).apply {
                    this.action = AlarmForegroundService.ACTION_START_ALARM
                    putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                }
                ContextCompat.startForegroundService(context, serviceIntent)

                // 2. Open full-screen Alarm Ringing Activity
                val ringingIntent = Intent(context, AlarmRingingActivity::class.java).apply {
                    putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                context.startActivity(ringingIntent)
            }

            AlarmScheduler.ACTION_UPCOMING_ALARM -> {
                val alarmId = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
                if (alarmId == -1) return

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val dao = AlarmDatabase.getInstance(context).alarmDao()
                        val alarm = dao.getAlarmById(alarmId)
                        if (alarm != null && alarm.isEnabled) {
                            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                            if (nm != null) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    val channel = NotificationChannel(
                                        UPCOMING_CHANNEL_ID,
                                        "Upcoming Alarms",
                                        NotificationManager.IMPORTANCE_LOW
                                    ).apply {
                                        description = "Silent notification prior to alarm ringing with early dismiss"
                                        enableVibration(false)
                                        setSound(null, null)
                                    }
                                    nm.createNotificationChannel(channel)
                                }

                                val dismissEarlyIntent = Intent(context, AlarmReceiver::class.java).apply {
                                    this.action = AlarmScheduler.ACTION_DISMISS_EARLY
                                    putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                                }
                                val dismissEarlyPending = PendingIntent.getBroadcast(
                                    context,
                                    300000 + alarmId,
                                    dismissEarlyIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )

                                val contentIntent = Intent(context, MainActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                }
                                val contentPending = PendingIntent.getActivity(
                                    context,
                                    400000 + alarmId,
                                    contentIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )

                                val notification = NotificationCompat.Builder(context, UPCOMING_CHANNEL_ID)
                                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                                    .setContentTitle("Upcoming Alarm: ${alarm.formattedTime()} ${alarm.amPm()}")
                                    .setContentText("${alarm.label.ifBlank { "Alarm" }} · ${alarm.daysDescription()}")
                                    .setContentIntent(contentPending)
                                    .setPriority(NotificationCompat.PRIORITY_LOW)
                                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                                    .setAutoCancel(true)
                                    .setOngoing(false)
                                    .addAction(
                                        android.R.drawable.ic_menu_close_clear_cancel,
                                        "Dismiss Early",
                                        dismissEarlyPending
                                    )
                                    .build()

                                nm.notify(200000 + alarmId, notification)
                                Log.d(TAG, "Displayed upcoming alarm notification for alarm #$alarmId")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error posting upcoming alarm notification: ${e.message}")
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            AlarmScheduler.ACTION_DISMISS_EARLY -> {
                val alarmId = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
                if (alarmId == -1) return

                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.cancel(200000 + alarmId)

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val dao = AlarmDatabase.getInstance(context).alarmDao()
                        val alarm = dao.getAlarmById(alarmId)
                        if (alarm != null) {
                            if (alarm.isOneTime()) {
                                dao.updateAlarm(alarm.copy(isEnabled = false, snoozeDoneCount = 0))
                                AlarmScheduler.cancel(context, alarm.id)
                            } else {
                                // For repeating alarms, advance to the next day's occurrence
                                val now = Calendar.getInstance()
                                val nextTrigger = AlarmScheduler.calculateNextTriggerTime(
                                    alarm.hour, alarm.minute, alarm.getDaysSet(), null
                                )
                                // If calculated next trigger is today (within next hour), add 1 day to skip to next cycle
                                val effectiveNext = if (nextTrigger <= now.timeInMillis + 70 * 60 * 1000L) {
                                    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                                    AlarmScheduler.calculateNextTriggerTime(
                                        alarm.hour, alarm.minute, alarm.getDaysSet(), tomorrow.timeInMillis
                                    )
                                } else nextTrigger

                                val updated = alarm.copy(alertTimeMillis = effectiveNext, snoozeDoneCount = 0)
                                dao.updateAlarm(updated)
                                AlarmScheduler.schedule(context, updated)
                            }
                            Log.d(TAG, "Dismissed upcoming alarm #$alarmId early")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error dismissing upcoming alarm early: ${e.message}")
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_DISMISS -> {
                val alarmId = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
                val stopService = Intent(context, AlarmForegroundService::class.java).apply {
                    this.action = AlarmForegroundService.ACTION_STOP_ALARM
                }
                context.startService(stopService)

                // Update database in background
                if (alarmId != -1) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val dao = AlarmDatabase.getInstance(context).alarmDao()
                            val alarm = dao.getAlarmById(alarmId)
                            if (alarm != null) {
                                if (alarm.isOneTime()) {
                                    dao.updateAlarm(alarm.copy(isEnabled = false, snoozeDoneCount = 0))
                                } else {
                                    val nextTime = AlarmScheduler.calculateNextTriggerTime(
                                        alarm.hour, alarm.minute, alarm.getDaysSet()
                                    )
                                    dao.updateAlarm(alarm.copy(snoozeDoneCount = 0, alertTimeMillis = nextTime))
                                    AlarmScheduler.schedule(context, alarm)
                                }
                            }
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }

            ACTION_SNOOZE -> {
                val alarmId = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
                val stopService = Intent(context, AlarmForegroundService::class.java).apply {
                    this.action = AlarmForegroundService.ACTION_STOP_ALARM
                }
                context.startService(stopService)

                if (alarmId != -1) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val dao = AlarmDatabase.getInstance(context).alarmDao()
                            val alarm = dao.getAlarmById(alarmId)
                            if (alarm != null) {
                                val nextCount = alarm.snoozeDoneCount + 1
                                if (nextCount <= alarm.maxSnoozeCount) {
                                    val snoozeTime = System.currentTimeMillis() + (alarm.snoozeDurationMinutes * 60 * 1000L)
                                    val updatedAlarm = alarm.copy(snoozeDoneCount = nextCount, alertTimeMillis = snoozeTime)
                                    dao.updateAlarm(updatedAlarm)
                                    AlarmScheduler.schedule(context, updatedAlarm, snoozeTime)
                                } else {
                                    // Max snoozes reached
                                    if (alarm.isOneTime()) {
                                        dao.updateAlarm(alarm.copy(isEnabled = false, snoozeDoneCount = 0))
                                    } else {
                                        dao.updateAlarm(alarm.copy(snoozeDoneCount = 0))
                                        AlarmScheduler.schedule(context, alarm)
                                    }
                                }
                            }
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }

            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                // Reschedule all active alarms
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val dao = AlarmDatabase.getInstance(context).alarmDao()
                        val alarms = dao.getEnabledAlarms()
                        for (alarm in alarms) {
                            val nextTime = AlarmScheduler.calculateNextTriggerTime(
                                alarm.hour, alarm.minute, alarm.getDaysSet()
                            )
                            dao.updateAlarm(alarm.copy(alertTimeMillis = nextTime))
                            AlarmScheduler.schedule(context, alarm)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
