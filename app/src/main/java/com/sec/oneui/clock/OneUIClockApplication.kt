package com.sec.oneui.clock

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.sec.oneui.clock.alarm.data.AlarmDatabase
import com.sec.oneui.clock.alarm.service.AlarmForegroundService
import com.sec.oneui.clock.settings.DiagFlags

class OneUIClockApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize Diagnostic Flags
        DiagFlags.init(this)

        // Pre-initialize Room Database
        AlarmDatabase.getInstance(this)

        // Initialize Notification Channels
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmChannel = NotificationChannel(
                AlarmForegroundService.CHANNEL_ID,
                "One UI Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Active ringing alarms and timers"
                setSound(null, null)
                enableVibration(false)
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(alarmChannel)
        }
    }
}
