package com.sec.oneui.clock.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val key: String, val displayName: String) {
    AURORA_PURPLE("aurora_purple", "Aurora Purple"),
    SILVER_VIOLET("silver_violet", "Silver Violet"),
    AMOLED_PITCH("amoled_pitch", "AMOLED Pitch")
}

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("clock_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _use24HourFormat = MutableStateFlow(prefs.getBoolean("use_24_hour_format", false))
    val use24HourFormat: StateFlow<Boolean> = _use24HourFormat.asStateFlow()

    private val _gradualVolumeDuration = MutableStateFlow(prefs.getInt("gradual_volume_seconds", 0))
    val gradualVolumeDuration: StateFlow<Int> = _gradualVolumeDuration.asStateFlow()

    private val _autoSilenceMinutes = MutableStateFlow(prefs.getInt("auto_silence_minutes", 10))
    val autoSilenceMinutes: StateFlow<Int> = _autoSilenceMinutes.asStateFlow()

    private val _volumeButtonsAction = MutableStateFlow(prefs.getString("volume_buttons_action", "Snooze") ?: "Snooze")
    val volumeButtonsAction: StateFlow<String> = _volumeButtonsAction.asStateFlow()

    private val _defaultVibrate = MutableStateFlow(prefs.getBoolean("default_vibrate", true))
    val defaultVibrate: StateFlow<Boolean> = _defaultVibrate.asStateFlow()

    private val _animationEnabled = MutableStateFlow(prefs.getBoolean("bg_animation_enabled", true))
    val animationEnabled: StateFlow<Boolean> = _animationEnabled.asStateFlow()

    private val _animationSpeed = MutableStateFlow(prefs.getFloat("bg_animation_speed", 1.0f))
    val animationSpeed: StateFlow<Float> = _animationSpeed.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.key).apply()
        _themeMode.value = mode
    }

    fun setUse24HourFormat(enabled: Boolean) {
        prefs.edit().putBoolean("use_24_hour_format", enabled).apply()
        _use24HourFormat.value = enabled
    }

    fun setGradualVolumeDuration(seconds: Int) {
        prefs.edit().putInt("gradual_volume_seconds", seconds).apply()
        _gradualVolumeDuration.value = seconds
    }

    fun setAutoSilenceMinutes(minutes: Int) {
        prefs.edit().putInt("auto_silence_minutes", minutes).apply()
        _autoSilenceMinutes.value = minutes
    }

    fun setVolumeButtonsAction(action: String) {
        prefs.edit().putString("volume_buttons_action", action).apply()
        _volumeButtonsAction.value = action
    }

    fun setDefaultVibrate(enabled: Boolean) {
        prefs.edit().putBoolean("default_vibrate", enabled).apply()
        _defaultVibrate.value = enabled
    }

    fun setAnimationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("bg_animation_enabled", enabled).apply()
        _animationEnabled.value = enabled
    }

    fun setAnimationSpeed(speed: Float) {
        prefs.edit().putFloat("bg_animation_speed", speed).apply()
        _animationSpeed.value = speed
    }

    val defaultRingtoneUri: String?
        get() = prefs.getString("default_ringtone_uri", null)

    val defaultRingtoneName: String
        get() = prefs.getString("default_ringtone_name", "Default") ?: "Default"

    fun setDefaultRingtone(uri: String?, name: String) {
        prefs.edit()
            .putString("default_ringtone_uri", uri)
            .putString("default_ringtone_name", name)
            .apply()
    }

    private fun loadThemeMode(): ThemeMode {
        val key = prefs.getString("theme_mode", ThemeMode.AURORA_PURPLE.key)
        return ThemeMode.entries.firstOrNull { it.key == key } ?: ThemeMode.AURORA_PURPLE
    }

    companion object {
        @Volatile
        private var INSTANCE: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SettingsManager(context)
                INSTANCE = instance
                instance
            }
        }
    }
}
