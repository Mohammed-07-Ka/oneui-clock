package com.sec.oneui.clock.alarm.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Immutable
@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "Alarm",
    val daysOfWeek: String = "", // e.g. "1,2,3,4,5" (1=Sun, 2=Mon ... 7=Sat)
    val isEnabled: Boolean = true,
    val snoozeDurationMinutes: Int = 5,
    val snoozeDoneCount: Int = 0,
    val maxSnoozeCount: Int = 3,
    val vibrate: Boolean = true,
    val alertTimeMillis: Long = 0L,
    val ringtoneUri: String? = null,
    val ringtoneName: String = "Default",
    val scheduledDateMillis: Long? = null
) {
    fun isOneTime(): Boolean = daysOfWeek.isBlank()

    fun getDaysSet(): Set<Int> {
        if (daysOfWeek.isBlank()) return emptySet()
        return daysOfWeek.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
    }

    fun formattedTime(): String {
        val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        val m = String.format("%02d", minute)
        return "$h:$m"
    }

    fun amPm(): String {
        return if (hour >= 12) "PM" else "AM"
    }

    fun daysDescription(): String {
        val set = getDaysSet()
        if (set.isEmpty()) return "Once"
        if (set.size == 7) return "Every day"
        if (set == setOf(2, 3, 4, 5, 6)) return "Weekdays"
        if (set == setOf(1, 7)) return "Weekends"
        val dayNames = mapOf(
            1 to "Sun", 2 to "Mon", 3 to "Tue",
            4 to "Wed", 5 to "Thu", 6 to "Fri", 7 to "Sat"
        )
        return set.sorted().joinToString(" ") { dayNames[it] ?: "" }
    }

    fun formattedScheduledDate(): String? {
        val millis = scheduledDateMillis ?: return null
        val sdf = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
        return sdf.format(Date(millis))
    }
}
