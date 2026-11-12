package com.sec.oneui.clock.alarm.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sec.oneui.clock.alarm.data.AlarmDatabase
import com.sec.oneui.clock.alarm.data.AlarmEntity
import com.sec.oneui.clock.alarm.receiver.AlarmReceiver
import com.sec.oneui.clock.alarm.scheduler.AlarmScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext
    private val dao = AlarmDatabase.getInstance(context).alarmDao()

    val alarms: StateFlow<List<AlarmEntity>> = dao.getAllAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _tickFlow = MutableStateFlow(System.currentTimeMillis())

    init {
        // Ticker to refresh remaining time display every 30 seconds
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                _tickFlow.value = System.currentTimeMillis()
            }
        }
    }

    val nearestAlarmHeader: StateFlow<String> = combine(alarms, _tickFlow) { alarmList, _ ->
        val enabledAlarms = alarmList.filter { it.isEnabled }
        if (enabledAlarms.isEmpty()) {
            "All alarms are off"
        } else {
            val nearest = enabledAlarms.minByOrNull { it.alertTimeMillis }
            if (nearest != null) {
                AlarmScheduler.formatRemainingTime(nearest.alertTimeMillis)
            } else {
                "No upcoming alarms"
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "No upcoming alarms")

    fun toggleAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            val updated = if (alarm.isEnabled) {
                AlarmScheduler.cancel(context, alarm.id)
                alarm.copy(isEnabled = false, snoozeDoneCount = 0)
            } else {
                val nextTime = AlarmScheduler.calculateNextTriggerTime(
                    alarm.hour, alarm.minute, alarm.getDaysSet()
                )
                val newAlarm = alarm.copy(isEnabled = true, alertTimeMillis = nextTime, snoozeDoneCount = 0)
                AlarmScheduler.schedule(context, newAlarm)
                newAlarm
            }
            dao.updateAlarm(updated)
        }
    }

    fun saveAlarm(
        id: Int = 0,
        hour: Int,
        minute: Int,
        label: String,
        daysSet: Set<Int>,
        snoozeDuration: Int,
        vibrate: Boolean
    ) {
        viewModelScope.launch {
            val daysString = daysSet.sorted().joinToString(",")
            val nextTime = AlarmScheduler.calculateNextTriggerTime(hour, minute, daysSet)
            val entity = AlarmEntity(
                id = id,
                hour = hour,
                minute = minute,
                label = label.ifBlank { "Alarm" },
                daysOfWeek = daysString,
                isEnabled = true,
                snoozeDurationMinutes = snoozeDuration,
                snoozeDoneCount = 0,
                vibrate = vibrate,
                alertTimeMillis = nextTime
            )

            val savedId = if (id == 0) {
                dao.insertAlarm(entity).toInt()
            } else {
                dao.updateAlarm(entity)
                id
            }

            AlarmScheduler.schedule(context, entity.copy(id = savedId))
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            AlarmScheduler.cancel(context, alarm.id)
            dao.deleteAlarm(alarm)
        }
    }

    fun dismissAlarm(alarmId: Int) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DISMISS
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        context.sendBroadcast(intent)
    }

    fun snoozeAlarm(alarmId: Int) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SNOOZE
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        context.sendBroadcast(intent)
    }
}
