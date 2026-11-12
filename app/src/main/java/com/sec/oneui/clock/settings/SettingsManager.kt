package com.sec.oneui.clock.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val key: String, val displayName: String) {
    LIGHT("light", "Light Mode"),
    DARK("dark", "Dark Mode"),
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

    private fun loadThemeMode(): ThemeMode {
        val key = prefs.getString("theme_mode", ThemeMode.AMOLED.key)
        return ThemeMode.entries.firstOrNull { it.key == key } ?: ThemeMode.AMOLED
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
