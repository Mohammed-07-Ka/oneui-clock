package com.sec.oneui.clock.settings

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.sec.oneui.clock.theme.OneUiBlack
import com.sec.oneui.clock.theme.OneUiBlue
import com.sec.oneui.clock.theme.OneUiCard
import com.sec.oneui.clock.theme.OneUiCardHover
import com.sec.oneui.clock.theme.OneUiDivider
import com.sec.oneui.clock.theme.OneUiTextPrimary
import com.sec.oneui.clock.theme.OneUiTextSecondary

@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    onBack: () -> Unit
) {
    val themeMode by settingsManager.themeMode.collectAsState()
    val language by settingsManager.language.collectAsState()
    val defaultVibrate by settingsManager.defaultVibrate.collectAsState()
    val gradualVolume by settingsManager.gradualVolume.collectAsState()
    val context = LocalContext.current

    val bgColor = when (themeMode) {
        ThemeMode.AMOLED -> OneUiBlack
        ThemeMode.DARK -> Color(0xFF121212)
        ThemeMode.LIGHT -> Color(0xFFF5F5F5)
    }
    val textColor = when (themeMode) {
        ThemeMode.LIGHT -> Color(0xFF1C1C1E)
        else -> OneUiTextPrimary
    }
    val subTextColor = when (themeMode) {
        ThemeMode.LIGHT -> Color(0xFF6E6E73)
        else -> OneUiTextSecondary
    }
    val cardColor = when (themeMode) {
        ThemeMode.LIGHT -> Color.White
        ThemeMode.DARK -> Color(0xFF1E1E1E)
        ThemeMode.AMOLED -> OneUiCard
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = textColor
                )
            }
            Text(
                text = "Settings",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Theme Section ──
        SectionTitle("Appearance", textColor = subTextColor)
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardColor)
        ) {
            ThemeMode.entries.forEach { mode ->
                val isSelected = themeMode == mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { settingsManager.setThemeMode(mode) }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (isSelected) OneUiBlue else OneUiCardHover)
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(Color.White)
                                    .align(Alignment.Center)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = mode.displayName,
                        fontSize = 16.sp,
                        color = textColor,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
                if (mode != ThemeMode.entries.last()) {
                    HorizontalDivider(color = OneUiDivider, modifier = Modifier.padding(horizontal = 20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Language Section ──
        SectionTitle("Language", textColor = subTextColor)
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardColor)
        ) {
            listOf("en" to "English", "ar" to "العربية").forEach { (code, name) ->
                val isSelected = language == code
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            settingsManager.setLanguage(code)
                            applyLanguage(context, code)
                        }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (isSelected) OneUiBlue else OneUiCardHover)
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(Color.White)
                                    .align(Alignment.Center)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = name,
                        fontSize = 16.sp,
                        color = textColor,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
                if (code == "en") {
                    HorizontalDivider(color = OneUiDivider, modifier = Modifier.padding(horizontal = 20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Alarm Defaults Section ──
        SectionTitle("Alarm Defaults", textColor = subTextColor)
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardColor)
        ) {
            // Vibration toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Default Vibration", fontSize = 16.sp, color = textColor)
                    Text("Vibrate when alarm rings", fontSize = 13.sp, color = subTextColor)
                }
                Switch(
                    checked = defaultVibrate,
                    onCheckedChange = { settingsManager.setDefaultVibrate(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = OneUiBlue,
                        uncheckedThumbColor = subTextColor,
                        uncheckedTrackColor = OneUiCardHover
                    )
                )
            }

            HorizontalDivider(color = OneUiDivider, modifier = Modifier.padding(horizontal = 20.dp))

            // Gradual Volume toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Gradual Volume Increase", fontSize = 16.sp, color = textColor)
                    Text("Volume rises over 30 seconds", fontSize = 13.sp, color = subTextColor)
                }
                Switch(
                    checked = gradualVolume,
                    onCheckedChange = { settingsManager.setGradualVolume(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = OneUiBlue,
                        uncheckedThumbColor = subTextColor,
                        uncheckedTrackColor = OneUiCardHover
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun SectionTitle(title: String, textColor: Color) {
    Text(
        text = title.uppercase(),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = textColor,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp)
    )
}

private fun applyLanguage(context: Context, langCode: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val localeManager = context.getSystemService(LocaleManager::class.java)
        localeManager.applicationLocales = LocaleList.forLanguageTags(langCode)
    } else {
        val appLocale = LocaleListCompat.forLanguageTags(langCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }
}
