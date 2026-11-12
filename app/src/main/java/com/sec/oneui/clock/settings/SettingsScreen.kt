package com.sec.oneui.clock.settings

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sec.oneui.clock.theme.ClockTheme
import com.sec.oneui.clock.theme.iosClickable
import com.sec.oneui.clock.theme.iosPressEffect

@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    onBack: () -> Unit
) {
    val themeMode by settingsManager.themeMode.collectAsState()
    val use24HourFormat by settingsManager.use24HourFormat.collectAsState()
    val gradualVolumeDuration by settingsManager.gradualVolumeDuration.collectAsState()
    val autoSilenceMinutes by settingsManager.autoSilenceMinutes.collectAsState()
    val volumeButtonsAction by settingsManager.volumeButtonsAction.collectAsState()
    val defaultVibrate by settingsManager.defaultVibrate.collectAsState()
    val animationEnabled by settingsManager.animationEnabled.collectAsState()
    val animationSpeed by settingsManager.animationSpeed.collectAsState()

    val palette = ClockTheme.palette
    val textColor = palette.textPrimary
    val subTextColor = palette.textSecondary
    val cardColor = palette.cardBackground
    val cardBorder = palette.cardBorder

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .statusBarsPadding()
            .navigationBarsPadding()
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
                    .border(1.dp, palette.cardBorderSubtle, RoundedCornerShape(14.dp))
                    .iosPressEffect(targetScale = 0.95f)
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
                        .iosClickable(targetScale = 0.97f) { settingsManager.setThemeMode(mode) }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) palette.primaryAccent else palette.cardBackground)
                            .border(1.dp, if (isSelected) cardBorder else palette.cardBorderSubtle, RoundedCornerShape(12.dp))
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(palette.onPrimaryAccent)
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

        // ── 2. Animated Background Control ──
        SectionTitle("Background Motion", textColor = subTextColor)
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
                    Text("Subtle animated color waves", fontSize = 13.sp, color = subTextColor)
                }
                Switch(
                    checked = animationEnabled,
                    onCheckedChange = { settingsManager.setAnimationEnabled(it) },
                    modifier = Modifier.iosPressEffect(targetScale = 0.95f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = palette.toggleActiveThumb,
                        checkedTrackColor = palette.toggleActiveTrack,
                        uncheckedThumbColor = subTextColor,
                        uncheckedTrackColor = palette.cardBackground
                    )
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 20.dp))

            // Speed Selector Chips (Equal Width with weight(1f), no text-wrapping)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text("Animation Speed", fontSize = 15.sp, color = textColor)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val speeds = listOf(
                        0.5f to "Slow (Eco)",
                        1.0f to "Normal",
                        1.5f to "Dynamic"
                    )
                    speeds.forEach { (spVal, spLabel) ->
                        val isSelected = animationSpeed == spVal
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) palette.primaryAccent else palette.cardBackground)
                                .border(1.dp, if (isSelected) cardBorder else palette.cardBorderSubtle, RoundedCornerShape(14.dp))
                                .iosClickable(targetScale = 0.96f) { settingsManager.setAnimationSpeed(spVal) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = spLabel,
                                color = if (isSelected) palette.onPrimaryAccent else subTextColor,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── 3. Clock & Alarm Behavior ──
        SectionTitle("Clock & Alarm Behavior", textColor = subTextColor)
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(cardColor)
                .border(1.dp, cardBorder, RoundedCornerShape(24.dp))
        ) {
            // 24-Hour Format Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Use 24-Hour Format", fontSize = 16.sp, color = textColor)
                    Text("Show 24-hour clock and time picker", fontSize = 13.sp, color = subTextColor)
                }
                Switch(
                    checked = use24HourFormat,
                    onCheckedChange = { settingsManager.setUse24HourFormat(it) },
                    modifier = Modifier.iosPressEffect(targetScale = 0.95f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = palette.toggleActiveThumb,
                        checkedTrackColor = palette.toggleActiveTrack,
                        uncheckedThumbColor = subTextColor,
                        uncheckedTrackColor = palette.cardBackground
                    )
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 20.dp))

            // Gradual Volume (Fade-in)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text("Gradual Volume (Fade-in)", fontSize = 15.sp, color = textColor)
                Text("Volume rises gently over time", fontSize = 13.sp, color = subTextColor)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val fadeOptions = listOf(
                        0 to "Off",
                        15 to "15s",
                        30 to "30s",
                        60 to "60s"
                    )
                    fadeOptions.forEach { (secVal, secLabel) ->
                        val isSelected = gradualVolumeDuration == secVal
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) palette.primaryAccent else palette.cardBackground)
                                .border(1.dp, if (isSelected) cardBorder else palette.cardBorderSubtle, RoundedCornerShape(14.dp))
                                .iosClickable(targetScale = 0.96f) { settingsManager.setGradualVolumeDuration(secVal) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = secLabel,
                                color = if (isSelected) palette.onPrimaryAccent else subTextColor,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 20.dp))

            // Auto-Silence After
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text("Auto-Silence After", fontSize = 15.sp, color = textColor)
                Text("Stop ringing automatically", fontSize = 13.sp, color = subTextColor)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val silenceOptions = listOf(
                        5 to "5m",
                        10 to "10m",
                        15 to "15m",
                        0 to "Never"
                    )
                    silenceOptions.forEach { (minVal, minLabel) ->
                        val isSelected = autoSilenceMinutes == minVal
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) palette.primaryAccent else palette.cardBackground)
                                .border(1.dp, if (isSelected) cardBorder else palette.cardBorderSubtle, RoundedCornerShape(14.dp))
                                .iosClickable(targetScale = 0.96f) { settingsManager.setAutoSilenceMinutes(minVal) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = minLabel,
                                color = if (isSelected) palette.onPrimaryAccent else subTextColor,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 20.dp))

            // Volume Buttons Action
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text("Volume Buttons Action", fontSize = 15.sp, color = textColor)
                Text("Hardware key behavior on alarm screen", fontSize = 13.sp, color = subTextColor)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val volumeActions = listOf("Snooze", "Dismiss", "Do Nothing")
                    volumeActions.forEach { action ->
                        val isSelected = volumeButtonsAction == action
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) palette.primaryAccent else palette.cardBackground)
                                .border(1.dp, if (isSelected) cardBorder else palette.cardBorderSubtle, RoundedCornerShape(14.dp))
                                .iosClickable(targetScale = 0.96f) { settingsManager.setVolumeButtonsAction(action) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = action,
                                color = if (isSelected) palette.onPrimaryAccent else subTextColor,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 20.dp))

            // Vibrate by Default Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Vibrate by Default", fontSize = 16.sp, color = textColor)
                    Text("Enable vibration for newly created alarms", fontSize = 13.sp, color = subTextColor)
                }
                Switch(
                    checked = defaultVibrate,
                    onCheckedChange = { settingsManager.setDefaultVibrate(it) },
                    modifier = Modifier.iosPressEffect(targetScale = 0.95f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = palette.toggleActiveThumb,
                        checkedTrackColor = palette.toggleActiveTrack,
                        uncheckedThumbColor = subTextColor,
                        uncheckedTrackColor = palette.cardBackground
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
