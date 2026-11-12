package com.sec.oneui.clock.alarm.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.sec.oneui.clock.MainActivity
import com.sec.oneui.clock.alarm.data.AlarmEntity
import com.sec.oneui.clock.alarm.receiver.AlarmReceiver
import java.util.Calendar

object AlarmScheduler {
    const val ACTION_ALARM_TRIGGER = "com.sec.oneui.clock.ACTION_ALARM_TRIGGER"
    const val ACTION_UPCOMING_ALARM = "com.sec.oneui.clock.ACTION_UPCOMING_ALARM"
    const val ACTION_DISMISS_EARLY = "com.sec.oneui.clock.ACTION_DISMISS_EARLY"
    const val EXTRA_ALARM_ID = "extra_alarm_id"
    private const val TAG = "AlarmScheduler"

    /**
     * Reverse-engineered algorithm from Samsung Clock AlarmItem.calculateAlertTime.
     * Enhanced with scheduledDateMillis for calendar-specific scheduling.
     */
    fun calculateNextTriggerTime(
        hour: Int,
        minute: Int,
        daysOfWeek: Set<Int>,
        scheduledDateMillis: Long? = null
    ): Long {
        // If a specific date is chosen from the calendar picker
        if (scheduledDateMillis != null) {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = scheduledDateMillis
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return calendar.timeInMillis
        }

        val now = Calendar.getInstance()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (daysOfWeek.isEmpty()) {
            // One-time alarm
            if (calendar.timeInMillis <= now.timeInMillis) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            return calendar.timeInMillis
        }

        // Repeating alarm: check today first if time hasn't passed
        val currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK) // 1=Sunday .. 7=Saturday
        if (daysOfWeek.contains(currentDayOfWeek) && calendar.timeInMillis > now.timeInMillis) {
            return calendar.timeInMillis
        }

        // Find nearest future day
        for (i in 1..7) {
            val nextDay = (currentDayOfWeek - 1 + i) % 7 + 1
            if (daysOfWeek.contains(nextDay)) {
                calendar.add(Calendar.DAY_OF_YEAR, i)
                return calendar.timeInMillis
            }
        }

        // Fallback: 1 day later
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        return calendar.timeInMillis
    }

    /**
     * Schedules exact alarm via AlarmManager with AlarmClockInfo (One UI status bar integration)
     */
    fun schedule(context: Context, alarm: AlarmEntity, triggerTimeOverride: Long? = null) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerTime = triggerTimeOverride ?: calculateNextTriggerTime(
            alarm.hour, alarm.minute, alarm.getDaysSet(), alarm.scheduledDateMillis
        )

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            putExtra(EXTRA_ALARM_ID, alarm.id)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, alarm.id, intent, flags)

        val showIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val showPendingIntent = PendingIntent.getActivity(context, alarm.id, showIntent, flags)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    val clockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)
                    alarmManager.setAlarmClock(clockInfo, pendingIntent)
                    Log.d(TAG, "Scheduled exact alarm clock #${alarm.id} at $triggerTime")
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    Log.d(TAG, "Exact alarm denied, scheduled inexact alarm #${alarm.id}")
                }
            } else {
                val clockInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)
                alarmManager.setAlarmClock(clockInfo, pendingIntent)
                Log.d(TAG, "Scheduled alarm clock #${alarm.id} at $triggerTime")
            }

            // Schedule Upcoming Alarm Notification if enabled
            val upcomingMinutes = com.sec.oneui.clock.settings.SettingsManager.getInstance(context).upcomingAlarmMinutes.value
            if (upcomingMinutes > 0) {
                val upcomingTime = triggerTime - (upcomingMinutes * 60 * 1000L)
                if (upcomingTime > System.currentTimeMillis()) {
                    val upcomingIntent = Intent(context, AlarmReceiver::class.java).apply {
                        action = ACTION_UPCOMING_ALARM
                        putExtra(EXTRA_ALARM_ID, alarm.id)
                    }
                    val upcomingPending = PendingIntent.getBroadcast(
                        context,
                        100000 + alarm.id,
                        upcomingIntent,
                        flags
                    )
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, upcomingTime, upcomingPending)
                    Log.d(TAG, "Scheduled upcoming alarm notification #${alarm.id} at $upcomingTime")
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to schedule exact alarm: ${e.message}")
        }
    }

    /**
     * Cancels scheduled alarm and its upcoming notification trigger
     */
    fun cancel(context: Context, alarmId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, alarmId, intent, flags)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()

        // Also cancel upcoming notification trigger and active notification
        val upcomingIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_UPCOMING_ALARM
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val upcomingPending = PendingIntent.getBroadcast(
            context,
            100000 + alarmId,
            upcomingIntent,
            flags
        )
        alarmManager.cancel(upcomingPending)
        upcomingPending.cancel()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
        notificationManager?.cancel(200000 + alarmId)

        Log.d(TAG, "Cancelled alarm #$alarmId")
    }

    /**
     * Reverse-engineered algorithm from AlarmUtil.getAlertDiffFromCurrent
     */
    fun formatRemainingTime(targetMillis: Long): String {
        val now = System.currentTimeMillis()
        val currentTruncated = (now / 60000L) * 60000L
        val diff = targetMillis - currentTruncated

        if (diff <= 0) return "Alarm ringing soon"

        val minutes = (diff / 60000L) % 60L
        val totalHours = diff / 3600000L
        val days = totalHours / 24L
        val hours = totalHours % 24L

        return buildString {
            append("Alarm in ")
            if (days > 0) append("$days day${if (days > 1) "s" else ""}, ")
            if (hours > 0) append("$hours hr${if (hours > 1) "s" else ""}, ")
            append("$minutes min")
        }
    }
}
