package com.sec.oneui.clock.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.sec.oneui.clock.alarm.data.AlarmDatabase
import com.sec.oneui.clock.alarm.scheduler.AlarmScheduler
import com.sec.oneui.clock.alarm.service.AlarmForegroundService
import com.sec.oneui.clock.alarm.ui.AlarmRingingActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "AlarmReceiver"
        const val ACTION_DISMISS = "com.sec.oneui.clock.ACTION_DISMISS_ALARM"
        const val ACTION_SNOOZE = "com.sec.oneui.clock.ACTION_SNOOZE_ALARM"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "onReceive: action=$action")

        when (action) {
            AlarmScheduler.ACTION_ALARM_TRIGGER -> {
                val alarmId = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
                if (alarmId == -1) return

                // 1. Start foreground alarm service for continuous ringtone and vibration
                val serviceIntent = Intent(context, AlarmForegroundService::class.java).apply {
                    this.action = AlarmForegroundService.ACTION_START_ALARM
                    putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }

                // 2. Open full-screen Alarm Ringing Activity
                val ringingIntent = Intent(context, AlarmRingingActivity::class.java).apply {
                    putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                context.startActivity(ringingIntent)
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
