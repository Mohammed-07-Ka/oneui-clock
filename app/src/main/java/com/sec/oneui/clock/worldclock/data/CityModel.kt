package com.sec.oneui.clock.worldclock.data

import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class CityModel(
    val id: String, // e.g. "Asia/Tokyo"
    val cityName: String, // e.g. "Tokyo"
    val countryOrRegion: String, // e.g. "Japan / Asia"
    val timeZoneId: String // e.g. "Asia/Tokyo"
) {
    fun calculateDisplayTime(sliderHourOffset: Float): String {
        val targetZone = try { ZoneId.of(timeZoneId) } catch (e: Exception) { ZoneId.systemDefault() }
        val now = ZonedDateTime.now(targetZone).plusMinutes((sliderHourOffset * 60).toLong())
        return now.format(DateTimeFormatter.ofPattern("hh:mm", Locale.getDefault()))
    }

    fun calculateAmPm(sliderHourOffset: Float): String {
        val targetZone = try { ZoneId.of(timeZoneId) } catch (e: Exception) { ZoneId.systemDefault() }
        val now = ZonedDateTime.now(targetZone).plusMinutes((sliderHourOffset * 60).toLong())
        return now.format(DateTimeFormatter.ofPattern("a", Locale.getDefault()))
    }

    fun calculateTimeDifference(sliderHourOffset: Float): String {
        val localZone = ZoneId.systemDefault()
        val targetZone = try { ZoneId.of(timeZoneId) } catch (e: Exception) { ZoneId.systemDefault() }

        val localTime = ZonedDateTime.now(localZone).plusMinutes((sliderHourOffset * 60).toLong())
        val targetTime = ZonedDateTime.now(targetZone).plusMinutes((sliderHourOffset * 60).toLong())

        val diffMinutes = Duration.between(localTime.toOffsetDateTime(), targetTime.toOffsetDateTime()).toMinutes()
        val diffHours = diffMinutes / 60
        val remainingMinutes = kotlin.math.abs(diffMinutes % 60)

        val dayDiff = when {
            targetTime.toLocalDate().isAfter(localTime.toLocalDate()) -> ", Tomorrow"
            targetTime.toLocalDate().isBefore(localTime.toLocalDate()) -> ", Yesterday"
            else -> ", Today"
        }

        return when {
            diffMinutes == 0L -> "Same as local time$dayDiff"
            diffMinutes > 0 -> {
                val minText = if (remainingMinutes > 0) " ${remainingMinutes}m" else ""
                "▲ ${diffHours}h$minText ahead$dayDiff"
            }
            else -> {
                val absHours = kotlin.math.abs(diffHours)
                val minText = if (remainingMinutes > 0) " ${remainingMinutes}m" else ""
                "▼ ${absHours}h$minText behind$dayDiff"
            }
        }
    }
}
