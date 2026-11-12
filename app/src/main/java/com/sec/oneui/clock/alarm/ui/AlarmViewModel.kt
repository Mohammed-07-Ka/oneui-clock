package com.sec.oneui.clock.alarm.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
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
import java.util.Calendar

data class AlarmEditorState(
    val isOpen: Boolean = false,
    val isEditing: Boolean = false,
    val editingAlarmId: Int? = null,
    val hour: Int = 7,
    val minute: Int = 0,
    val label: String = "Alarm",
    val daysOfWeek: Set<Int> = emptySet(),
    val snoozeDurationMinutes: Int = 5,
    val vibrate: Boolean = true,
    val ringtoneUri: String? = null,
    val ringtoneName: String = "Default",
    val scheduledDateMillis: Long? = null
)

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext
    private val dao = AlarmDatabase.getInstance(context).alarmDao()

    val alarms: StateFlow<List<AlarmEntity>> = dao.getAllAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _tickFlow = MutableStateFlow(System.currentTimeMillis())

    private val _editorState = MutableStateFlow(AlarmEditorState())
    val editorState: StateFlow<AlarmEditorState> = _editorState.asStateFlow()

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

    fun getDefaultRingtone(): Pair<String?, String> {
        val prefs = context.getSharedPreferences("clock_settings", Context.MODE_PRIVATE)
        val savedUri = prefs.getString("default_ringtone_uri", null)
        val savedName = prefs.getString("default_ringtone_name", null)
        if (!savedName.isNullOrBlank()) {
            return Pair(savedUri, savedName)
        }
        val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val defaultRingtone = defaultUri?.let { RingtoneManager.getRingtone(context, it) }
        val title = defaultRingtone?.getTitle(context) ?: "Default Alarm"
        return Pair(defaultUri?.toString(), title)
    }

    fun openEditorForNew(defaultVibrate: Boolean) {
        val current = _editorState.value
        // If an existing unsaved draft for a new alarm is in progress, resume it!
        if (current.isEditing && current.editingAlarmId == null) {
            _editorState.value = current.copy(isOpen = true)
            return
        }

        val (defUri, defName) = getDefaultRingtone()
        val now = Calendar.getInstance()
        _editorState.value = AlarmEditorState(
            isOpen = true,
            isEditing = true,
            editingAlarmId = null,
            hour = now.get(Calendar.HOUR_OF_DAY),
            minute = now.get(Calendar.MINUTE),
            label = "Alarm",
            daysOfWeek = emptySet(),
            snoozeDurationMinutes = 5,
            vibrate = defaultVibrate,
            ringtoneUri = defUri,
            ringtoneName = defName,
            scheduledDateMillis = null
        )
    }

    fun openEditorForExisting(alarm: AlarmEntity) {
        val current = _editorState.value
        // If user was already editing this exact alarm, resume draft without resetting!
        if (current.isEditing && current.editingAlarmId == alarm.id) {
            _editorState.value = current.copy(isOpen = true)
            return
        }

        val (defUri, defName) = getDefaultRingtone()
        _editorState.value = AlarmEditorState(
            isOpen = true,
            isEditing = true,
            editingAlarmId = alarm.id,
            hour = alarm.hour,
            minute = alarm.minute,
            label = alarm.label,
            daysOfWeek = alarm.getDaysSet(),
            snoozeDurationMinutes = alarm.snoozeDurationMinutes,
            vibrate = alarm.vibrate,
            ringtoneUri = alarm.ringtoneUri ?: defUri,
            ringtoneName = if (alarm.ringtoneName != "Default") alarm.ringtoneName else defName,
            scheduledDateMillis = alarm.scheduledDateMillis
        )
    }

    fun updateEditorTime(hour: Int, minute: Int) {
        _editorState.value = _editorState.value.copy(hour = hour, minute = minute)
    }

    fun updateEditorHour(hour: Int) {
        _editorState.value = _editorState.value.copy(hour = hour)
    }

    fun updateEditorMinute(minute: Int) {
        _editorState.value = _editorState.value.copy(minute = minute)
    }

    fun updateEditorLabel(label: String) {
        _editorState.value = _editorState.value.copy(label = label)
    }

    fun toggleEditorDay(dayNum: Int) {
        val currentDays = _editorState.value.daysOfWeek
        val updatedDays = if (currentDays.contains(dayNum)) {
            currentDays - dayNum
        } else {
            currentDays + dayNum
        }
        _editorState.value = _editorState.value.copy(daysOfWeek = updatedDays)
    }

    fun updateEditorSnooze(minutes: Int) {
        _editorState.value = _editorState.value.copy(snoozeDurationMinutes = minutes)
    }

    fun updateEditorVibrate(vibrate: Boolean) {
        _editorState.value = _editorState.value.copy(vibrate = vibrate)
    }

    fun updateEditorRingtone(uri: String?, name: String) {
        _editorState.value = _editorState.value.copy(
            ringtoneUri = uri,
            ringtoneName = name
        )
        // Immediately persist as default ringtone for subsequent alarms
        val prefs = context.getSharedPreferences("clock_settings", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("default_ringtone_uri", uri)
            .putString("default_ringtone_name", name)
            .apply()
    }

    fun updateEditorScheduledDate(dateMillis: Long?) {
        _editorState.value = _editorState.value.copy(scheduledDateMillis = dateMillis)
    }

    fun saveDraft() {
        val draft = _editorState.value
        if (!draft.isEditing) return

        viewModelScope.launch {
            val daysString = draft.daysOfWeek.sorted().joinToString(",")
            val nextTime = AlarmScheduler.calculateNextTriggerTime(
                draft.hour, draft.minute, draft.daysOfWeek, draft.scheduledDateMillis
            )
            val entity = AlarmEntity(
                id = draft.editingAlarmId ?: 0,
                hour = draft.hour,
                minute = draft.minute,
                label = draft.label.ifBlank { "Alarm" },
                daysOfWeek = daysString,
                isEnabled = true,
                snoozeDurationMinutes = draft.snoozeDurationMinutes,
                snoozeDoneCount = 0,
                vibrate = draft.vibrate,
                alertTimeMillis = nextTime,
                ringtoneUri = draft.ringtoneUri,
                ringtoneName = draft.ringtoneName,
                scheduledDateMillis = draft.scheduledDateMillis
            )

            val savedId = if (draft.editingAlarmId == null || draft.editingAlarmId == 0) {
                dao.insertAlarm(entity).toInt()
            } else {
                dao.updateAlarm(entity)
                draft.editingAlarmId
            }

            AlarmScheduler.schedule(context, entity.copy(id = savedId))

            // Explicitly clear draft on save
            _editorState.value = AlarmEditorState(isOpen = false, isEditing = false)
        }
    }

    fun cancelDraft() {
        // Explicitly clear draft on cancel
        _editorState.value = AlarmEditorState(isOpen = false, isEditing = false)
    }

    fun dismissEditorSheet() {
        // Keep draft values intact, only close sheet
        _editorState.value = _editorState.value.copy(isOpen = false)
    }

    fun toggleAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            val updated = if (alarm.isEnabled) {
                AlarmScheduler.cancel(context, alarm.id)
                alarm.copy(isEnabled = false, snoozeDoneCount = 0)
            } else {
                val nextTime = AlarmScheduler.calculateNextTriggerTime(
                    alarm.hour, alarm.minute, alarm.getDaysSet(), alarm.scheduledDateMillis
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
        vibrate: Boolean,
        ringtoneUri: String? = null,
        ringtoneName: String = "Default",
        scheduledDateMillis: Long? = null
    ) {
        viewModelScope.launch {
            val daysString = daysSet.sorted().joinToString(",")
            val nextTime = AlarmScheduler.calculateNextTriggerTime(
                hour, minute, daysSet, scheduledDateMillis
            )
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
                alertTimeMillis = nextTime,
                ringtoneUri = ringtoneUri,
                ringtoneName = ringtoneName,
                scheduledDateMillis = scheduledDateMillis
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
