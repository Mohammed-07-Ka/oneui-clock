package com.sec.oneui.clock.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val key: String, val displayName: String) {
    DARK_GEMINI("dark_gemini", "Dark Gemini"),
    LIGHT_PEARL("light_pearl", "Light Pearl Purple"),
    AMOLED("amoled", "AMOLED Black")
}

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("clock_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _language = MutableStateFlow(loadLanguage())
    val language: StateFlow<String> = _language.asStateFlow()

    private val _defaultVibrate = MutableStateFlow(prefs.getBoolean("default_vibrate", true))
    val defaultVibrate: StateFlow<Boolean> = _defaultVibrate.asStateFlow()

    private val _gradualVolume = MutableStateFlow(prefs.getBoolean("gradual_volume", false))
    val gradualVolume: StateFlow<Boolean> = _gradualVolume.asStateFlow()

    private val _animationEnabled = MutableStateFlow(prefs.getBoolean("bg_animation_enabled", true))
    val animationEnabled: StateFlow<Boolean> = _animationEnabled.asStateFlow()

    private val _animationSpeed = MutableStateFlow(prefs.getFloat("bg_animation_speed", 1.0f))
    val animationSpeed: StateFlow<Float> = _animationSpeed.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString("theme_mode", mode.key).apply()
        _themeMode.value = mode
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString("app_language", lang).apply()
        _language.value = lang
    }

    fun setDefaultVibrate(enabled: Boolean) {
        prefs.edit().putBoolean("default_vibrate", enabled).apply()
        _defaultVibrate.value = enabled
    }

    fun setGradualVolume(enabled: Boolean) {
        prefs.edit().putBoolean("gradual_volume", enabled).apply()
        _gradualVolume.value = enabled
    }

    fun setAnimationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("bg_animation_enabled", enabled).apply()
        _animationEnabled.value = enabled
    }

    fun setAnimationSpeed(speed: Float) {
        prefs.edit().putFloat("bg_animation_speed", speed).apply()
        _animationSpeed.value = speed
    }

    private fun loadThemeMode(): ThemeMode {
        val key = prefs.getString("theme_mode", ThemeMode.DARK_GEMINI.key)
        return ThemeMode.entries.firstOrNull { it.key == key } ?: ThemeMode.DARK_GEMINI
    }

    private fun loadLanguage(): String {
        return prefs.getString("app_language", "en") ?: "en"
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
