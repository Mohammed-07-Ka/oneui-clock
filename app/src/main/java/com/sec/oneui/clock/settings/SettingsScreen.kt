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
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
    val alarmCustomVolume by settingsManager.alarmCustomVolume.collectAsState()
    val gradualVolumeDuration by settingsManager.gradualVolumeDuration.collectAsState()
    val autoSilenceMinutes by settingsManager.autoSilenceMinutes.collectAsState()
    val volumeButtonsAction by settingsManager.volumeButtonsAction.collectAsState()
    val defaultVibrate by settingsManager.defaultVibrate.collectAsState()
    val animationEnabled by settingsManager.animationEnabled.collectAsState()
    val animationSpeed by settingsManager.animationSpeed.collectAsState()
    val upcomingAlarmMinutes by settingsManager.upcomingAlarmMinutes.collectAsState()

    var customGradualVolumeExpanded by remember { mutableStateOf(gradualVolumeDuration !in listOf(0, 3, 5, 10)) }

    val haptic = LocalHapticFeedback.current

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
                    .iosPressEffect(targetScale = 0.96f)
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
                        .iosClickable(targetScale = 0.96f) { settingsManager.setThemeMode(mode) }
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
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        settingsManager.setAnimationEnabled(it)
                    },
                    modifier = Modifier.iosPressEffect(targetScale = 0.96f),
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
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        settingsManager.setUse24HourFormat(it)
                    },
                    modifier = Modifier.iosPressEffect(targetScale = 0.96f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = palette.toggleActiveThumb,
                        checkedTrackColor = palette.toggleActiveTrack,
                        uncheckedThumbColor = subTextColor,
                        uncheckedTrackColor = palette.cardBackground
                    )
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 20.dp))

            // In-App Alarm Volume Slider (0% to 100%)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Alarm Volume", fontSize = 15.sp, color = textColor)
                        Text("In-app alarm volume control", fontSize = 13.sp, color = subTextColor)
                    }
                    Text(
                        text = "${(alarmCustomVolume * 100).toInt()}%",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryAccent
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (alarmCustomVolume == 0f) Icons.Default.VolumeOff else Icons.Default.VolumeDown,
                        contentDescription = "Volume Low",
                        tint = subTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Slider(
                        value = alarmCustomVolume,
                        onValueChange = { settingsManager.setAlarmCustomVolume(it) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = palette.primaryAccent,
                            activeTrackColor = palette.primaryAccent,
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Volume High",
                        tint = subTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val fadeOptions = listOf(
                        0 to "Off",
                        3 to "3s",
                        5 to "5s",
                        10 to "10s"
                    )
                    fadeOptions.forEach { (secVal, secLabel) ->
                        val isSelected = !customGradualVolumeExpanded && gradualVolumeDuration == secVal
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) palette.primaryAccent else palette.cardBackground)
                                .border(1.dp, if (isSelected) cardBorder else palette.cardBorderSubtle, RoundedCornerShape(14.dp))
                                .iosClickable(targetScale = 0.96f) {
                                    customGradualVolumeExpanded = false
                                    settingsManager.setGradualVolumeDuration(secVal)
                                }
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

                    // Custom Option
                    val isCustomSelected = customGradualVolumeExpanded || gradualVolumeDuration !in listOf(0, 3, 5, 10)
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isCustomSelected) palette.primaryAccent else palette.cardBackground)
                            .border(1.dp, if (isCustomSelected) cardBorder else palette.cardBorderSubtle, RoundedCornerShape(14.dp))
                            .iosClickable(targetScale = 0.96f) {
                                customGradualVolumeExpanded = true
                                if (gradualVolumeDuration in listOf(0, 3, 5, 10)) {
                                    settingsManager.setGradualVolumeDuration(7)
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Custom",
                            color = if (isCustomSelected) palette.onPrimaryAccent else subTextColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Custom Slider & Stepper Controls (1s to 30s)
                if (customGradualVolumeExpanded || gradualVolumeDuration !in listOf(0, 3, 5, 10)) {
                    val currentVal = gradualVolumeDuration.coerceIn(1, 30)
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Custom Duration (1s – 30s)",
                            fontSize = 13.sp,
                            color = subTextColor
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(palette.cardBackground)
                                    .border(1.dp, palette.cardBorderSubtle, RoundedCornerShape(8.dp))
                                    .iosClickable(targetScale = 0.94f) {
                                        val nextVal = (currentVal - 1).coerceIn(1, 30)
                                        settingsManager.setGradualVolumeDuration(nextVal)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                            }
                            Text(
                                text = "${currentVal}s",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.primaryAccent
                            )
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(palette.cardBackground)
                                    .border(1.dp, palette.cardBorderSubtle, RoundedCornerShape(8.dp))
                                    .iosClickable(targetScale = 0.94f) {
                                        val nextVal = (currentVal + 1).coerceIn(1, 30)
                                        settingsManager.setGradualVolumeDuration(nextVal)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Slider(
                        value = currentVal.toFloat(),
                        onValueChange = { newSec ->
                            settingsManager.setGradualVolumeDuration(newSec.toInt().coerceIn(1, 30))
                        },
                        valueRange = 1f..30f,
                        steps = 28,
                        colors = SliderDefaults.colors(
                            thumbColor = palette.primaryAccent,
                            activeTrackColor = palette.primaryAccent,
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
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

            // Upcoming Alarm Notification (Early Warning with Dismiss Early)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text("Upcoming Alarm Notification", fontSize = 15.sp, color = textColor)
                Text("Silent notification prior to alarm with Dismiss Early option", fontSize = 13.sp, color = subTextColor)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val upcomingOptions = listOf(
                        0 to "Off",
                        15 to "15m before",
                        30 to "30m before",
                        60 to "1h before"
                    )
                    upcomingOptions.forEach { (minsVal, minsLabel) ->
                        val isSelected = upcomingAlarmMinutes == minsVal
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) palette.primaryAccent else palette.cardBackground)
                                .border(1.dp, if (isSelected) cardBorder else palette.cardBorderSubtle, RoundedCornerShape(14.dp))
                                .iosClickable(targetScale = 0.96f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    settingsManager.setUpcomingAlarmMinutes(minsVal)
                                }
                                .padding(vertical = 10.dp, horizontal = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = minsLabel,
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
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        settingsManager.setDefaultVibrate(it)
                    },
                    modifier = Modifier.iosPressEffect(targetScale = 0.96f),
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
