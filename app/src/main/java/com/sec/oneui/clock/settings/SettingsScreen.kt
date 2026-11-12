package com.sec.oneui.clock.settings

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.sec.oneui.clock.theme.LiquidActiveText
import com.sec.oneui.clock.theme.LiquidGlassActive
import com.sec.oneui.clock.theme.LiquidGlassBackground
import com.sec.oneui.clock.theme.LiquidGlassHover
import com.sec.oneui.clock.theme.LiquidSpecularBorder
import com.sec.oneui.clock.theme.LiquidSpecularBorderSubtle
import com.sec.oneui.clock.theme.LiquidTextPrimary
import com.sec.oneui.clock.theme.LiquidTextSecondary

@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    onBack: () -> Unit
) {
    val themeMode by settingsManager.themeMode.collectAsState()
    val language by settingsManager.language.collectAsState()
    val defaultVibrate by settingsManager.defaultVibrate.collectAsState()
    val gradualVolume by settingsManager.gradualVolume.collectAsState()
    val animationEnabled by settingsManager.animationEnabled.collectAsState()
    val animationSpeed by settingsManager.animationSpeed.collectAsState()
    val context = LocalContext.current

    val textColor = LiquidTextPrimary
    val subTextColor = LiquidTextSecondary
    val cardColor = LiquidGlassBackground
    val cardBorder = LiquidSpecularBorder

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardColor)
                    .border(1.dp, LiquidSpecularBorderSubtle, RoundedCornerShape(14.dp))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = textColor
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Settings",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── 1. Theme Section ──
        SectionTitle("Theme Appearance", textColor = subTextColor)
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(cardColor)
                .border(1.dp, cardBorder, RoundedCornerShape(24.dp))
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
                            .size(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) LiquidGlassActive else LiquidGlassHover)
                            .border(1.dp, if (isSelected) cardBorder else LiquidSpecularBorderSubtle, RoundedCornerShape(12.dp))
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(LiquidActiveText)
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
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── 2. Animated Background Control (Battery Saver) ──
        SectionTitle("Background Ambient Glow", textColor = subTextColor)
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(cardColor)
                .border(1.dp, cardBorder, RoundedCornerShape(24.dp))
        ) {
            // Enable/Disable Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ambient Mesh Motion", fontSize = 16.sp, color = textColor)
                    Text("Turn off to maximize battery life", fontSize = 13.sp, color = subTextColor)
                }
                Switch(
                    checked = animationEnabled,
                    onCheckedChange = { settingsManager.setAnimationEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = LiquidActiveText,
                        checkedTrackColor = LiquidGlassActive,
                        uncheckedThumbColor = subTextColor,
                        uncheckedTrackColor = LiquidGlassHover
                    )
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 20.dp))

            // Speed Selector Chips
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text("Animation Speed", fontSize = 15.sp, color = textColor)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val speeds = listOf(
                        0.5f to "Slow (Eco)",
                        1.0f to "Normal",
                        1.5f to "Dynamic"
                    )
                    speeds.forEach { (spVal, spLabel) ->
                        val isSelected = animationSpeed == spVal
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) LiquidGlassActive else LiquidGlassHover)
                                .border(1.dp, if (isSelected) cardBorder else LiquidSpecularBorderSubtle, RoundedCornerShape(14.dp))
                                .clickable { settingsManager.setAnimationSpeed(spVal) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = spLabel,
                                color = if (isSelected) LiquidActiveText else subTextColor,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── 3. Language Section ──
        SectionTitle("Language / اللغة", textColor = subTextColor)
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(cardColor)
                .border(1.dp, cardBorder, RoundedCornerShape(24.dp))
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
                            .size(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) LiquidGlassActive else LiquidGlassHover)
                            .border(1.dp, if (isSelected) cardBorder else LiquidSpecularBorderSubtle, RoundedCornerShape(12.dp))
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(LiquidActiveText)
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
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── 4. Alarm Defaults Section ──
        SectionTitle("Alarm Defaults", textColor = subTextColor)
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(cardColor)
                .border(1.dp, cardBorder, RoundedCornerShape(24.dp))
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
                        checkedThumbColor = LiquidActiveText,
                        checkedTrackColor = LiquidGlassActive,
                        uncheckedThumbColor = subTextColor,
                        uncheckedTrackColor = LiquidGlassHover
                    )
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 20.dp))

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
                        checkedThumbColor = LiquidActiveText,
                        checkedTrackColor = LiquidGlassActive,
                        uncheckedThumbColor = subTextColor,
                        uncheckedTrackColor = LiquidGlassHover
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
