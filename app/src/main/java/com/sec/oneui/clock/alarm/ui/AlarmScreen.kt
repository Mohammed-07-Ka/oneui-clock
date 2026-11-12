@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.sec.oneui.clock.alarm.ui

import android.app.Activity
import com.sec.oneui.clock.ui.components.WheelPicker
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.statusBarsPadding
import com.sec.oneui.clock.alarm.data.AlarmEntity
import com.sec.oneui.clock.alarm.service.AlarmAudioPreviewer
import com.sec.oneui.clock.alarm.service.AlarmForegroundService
import kotlinx.coroutines.launch
import com.sec.oneui.clock.settings.SettingsManager
import com.sec.oneui.clock.settings.ThemeMode
import com.sec.oneui.clock.theme.ClockTheme
import com.sec.oneui.clock.theme.LiquidActiveText
import com.sec.oneui.clock.theme.LiquidGlassActive
import com.sec.oneui.clock.theme.LiquidGlassBackground
import com.sec.oneui.clock.theme.LiquidGlassHover
import com.sec.oneui.clock.theme.LiquidSpecularBorder
import com.sec.oneui.clock.theme.LiquidSpecularBorderSubtle
import com.sec.oneui.clock.theme.LiquidTextMuted
import com.sec.oneui.clock.theme.LiquidTextPrimary
import com.sec.oneui.clock.theme.LiquidTextSecondary
import com.sec.oneui.clock.theme.OneUiRed
import com.sec.oneui.clock.theme.iosClickable
import com.sec.oneui.clock.theme.iosPressEffect
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AlarmScreen(
    viewModel: AlarmViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager.getInstance(context) }
    val use24HourFormat by settingsManager.use24HourFormat.collectAsState()
    val defaultVibrate by settingsManager.defaultVibrate.collectAsState()

    val alarms by viewModel.alarms.collectAsState()
    val nearestRemaining by viewModel.nearestAlarmHeader.collectAsState()
    val editorState by viewModel.editorState.collectAsState()
    val palette = ClockTheme.palette

    // System Back Button closes Add/Edit Alarm sheet
    BackHandler(enabled = editorState.isOpen) {
        viewModel.dismissEditorSheet()
    }

    // Authentic iOS Modal Sheet background scale down and dimming
    val bgScale by animateFloatAsState(
        targetValue = if (editorState.isOpen) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "alarmBgScale"
    )
    val bgAlpha by animateFloatAsState(
        targetValue = if (editorState.isOpen) 0.7f else 1f,
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "alarmBgAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = bgScale
                    scaleY = bgScale
                    alpha = bgAlpha
                }
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Alarm",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = nearestRemaining,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = palette.textSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // In-App Ringing Banner (placed cleanly in natural column flow with dedicated spacing)
            val ringingAlarm by AlarmForegroundService.currentRingingAlarm.collectAsState()
            AnimatedVisibility(
                visible = ringingAlarm != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                ringingAlarm?.let { alarm ->
                    Column {
                        InAppAlarmBanner(
                            alarm = alarm,
                            onDismiss = { viewModel.dismissAlarm(it) },
                            onSnooze = { viewModel.snoozeAlarm(it) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            if (alarms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No alarms\nTap + to set an alarm",
                            fontSize = 17.sp,
                            color = palette.textSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(alarms, key = { it.id }) { alarm ->
                        AlarmCard(
                            alarm = alarm,
                            use24HourFormat = use24HourFormat,
                            onToggle = { viewModel.toggleAlarm(alarm) },
                            onClick = { viewModel.openEditorForExisting(alarm) },
                            onDelete = { viewModel.deleteAlarm(alarm) },
                            modifier = Modifier.animateItemPlacement(
                                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                            )
                        )
                    }
                }
            }
        }

        // iOS Liquid Glass Floating Action Button with Tactile Press Feedback
        AnimatedVisibility(
            visible = !editorState.isOpen,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp)
        ) {
            FloatingActionButton(
                onClick = { viewModel.openEditorForNew(defaultVibrate) },
                containerColor = palette.primaryAccent,
                contentColor = palette.onPrimaryAccent,
                shape = CircleShape,
                modifier = Modifier
                    .size(62.dp)
                    .clip(CircleShape)
                    .border(1.dp, palette.cardBorder, CircleShape)
                    .iosPressEffect(targetScale = 0.96f)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Alarm", modifier = Modifier.size(32.dp))
            }
        }

        // Edit Alarm Sheet rendered instantly in Frame 0 without delayed transitions
        if (editorState.isOpen) {
            AlarmEditSheet(
                editorState = editorState,
                use24HourFormat = use24HourFormat,
                onDismiss = { viewModel.dismissEditorSheet() },
                onSave = { viewModel.saveDraft() },
                onCancel = { viewModel.cancelDraft() },
                onTimeChanged = { h, m -> viewModel.updateEditorTime(h, m) },
                onLabelChanged = { viewModel.updateEditorLabel(it) },
                onDayToggled = { viewModel.toggleEditorDay(it) },
                onSnoozeChanged = { viewModel.updateEditorSnooze(it) },
                onVibrateChanged = { viewModel.updateEditorVibrate(it) },
                onRingtonePicked = { uri, name -> viewModel.updateEditorRingtone(uri, name) },
                onScheduledDateChanged = { viewModel.updateEditorScheduledDate(it) },
                onVolumeChanged = { viewModel.updateEditorVolume(it) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlarmCard(
    alarm: AlarmEntity,
    use24HourFormat: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = ClockTheme.palette
    val haptic = LocalHapticFeedback.current
    val cardInteractionSource = remember { MutableInteractionSource() }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .border(1.dp, palette.cardBorder, RoundedCornerShape(26.dp))
            .iosPressEffect(targetScale = 0.96f, interactionSource = cardInteractionSource)
            .clickable(interactionSource = cardInteractionSource, indication = null) { onClick() },
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = palette.cardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Top Row: Time on left, Alarm Label on upper right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        if (use24HourFormat) {
                            Text(
                                text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (alarm.isEnabled) palette.textPrimary else palette.textMuted
                            )
                        } else {
                            Text(
                                text = alarm.formattedTime(),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (alarm.isEnabled) palette.textPrimary else palette.textMuted
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = alarm.amPm(),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (alarm.isEnabled) palette.textSecondary else palette.textMuted,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = alarm.label.ifBlank { "Alarm" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (alarm.isEnabled) palette.textPrimary else palette.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Second line: Repeat & Date status without any emojis
                val repeatAndDate = buildString {
                    append(alarm.daysDescription())
                    alarm.formattedScheduledDate()?.let { dateStr ->
                        append(" • $dateStr")
                    }
                }
                Text(
                    text = repeatAndDate,
                    fontSize = 13.sp,
                    color = palette.textSecondary
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.iosPressEffect(targetScale = 0.96f)) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete Alarm",
                    tint = palette.textSecondary.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggle()
                },
                modifier = Modifier.iosPressEffect(targetScale = 0.96f),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = palette.toggleActiveThumb,
                    checkedTrackColor = palette.toggleActiveTrack,
                    uncheckedThumbColor = palette.textSecondary,
                    uncheckedTrackColor = palette.cardBackground
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditSheet(
    editorState: AlarmEditorState,
    use24HourFormat: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onTimeChanged: (hour: Int, minute: Int) -> Unit,
    onLabelChanged: (String) -> Unit,
    onDayToggled: (Int) -> Unit,
    onSnoozeChanged: (Int) -> Unit,
    onVibrateChanged: (Boolean) -> Unit,
    onRingtonePicked: (uri: String?, name: String) -> Unit,
    onScheduledDateChanged: (Long?) -> Unit,
    onVolumeChanged: (Float) -> Unit
) {
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }

    // System Back Button closes DatePickerDialog if open
    BackHandler(enabled = showDatePicker) {
        showDatePicker = false
    }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                val ringtone = RingtoneManager.getRingtone(context, uri)
                val ringtoneTitle = ringtone?.getTitle(context) ?: "Custom"
                onRingtonePicked(uri.toString(), ringtoneTitle)
            } else {
                onRingtonePicked(null, "Silent")
            }
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                Log.w("AlarmEditSheet", "takePersistableUriPermission failed: ${e.message}")
            }
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            val name = cursor?.use {
                val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (it.moveToFirst() && idx >= 0) it.getString(idx) else null
            } ?: "Custom audio"
            onRingtonePicked(uri.toString(), name)
        }
    }

    val palette = ClockTheme.palette
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        onDispose {
            AlarmAudioPreviewer.stopPreview()
        }
    }

    BackHandler { onCancel() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCancel
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding()
                .padding(top = 24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {},
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = if (palette.themeMode == ThemeMode.AMOLED_PITCH || palette.themeMode == ThemeMode.MIDNIGHT_BLUE) Color(0xFF07050C) else Color(0xFF0E0B18),
            border = androidx.compose.foundation.BorderStroke(1.dp, palette.cardBorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 32.dp)
            ) {
                // iOS Drag Handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(44.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                )

                Spacer(modifier = Modifier.height(14.dp))

                // iOS Sheet Header Bar: Cancel on left, Title in center, Save on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF60A5FA),
                        modifier = Modifier
                            .iosClickable(targetScale = 0.95f) { onCancel() }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    )

                    Text(
                        text = if (editorState.editingAlarmId == null) "Add Alarm" else "Edit Alarm",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )

                    Text(
                        text = "Save",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (palette.isBlueTheme) Color(0xFF60A5FA) else palette.primaryAccent,
                        modifier = Modifier
                            .iosClickable(targetScale = 0.95f) { onSave() }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

            // iOS Liquid Glass Wheel Time Picker
            if (use24HourFormat) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(palette.cardBackground)
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(26.dp))
                        .padding(vertical = 14.dp, horizontal = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hours (00..23)
                    WheelPicker(
                        items = (0..23).toList(),
                        initialIndex = editorState.hour.coerceIn(0, 23),
                        onItemSelected = { onTimeChanged(it, editorState.minute) },
                        modifier = Modifier.weight(1f),
                        formatItem = { String.format("%02d", it) },
                        isNumeric = true,
                        minValue = 0,
                        maxValue = 23
                    )

                    Text(
                        text = ":",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // Minutes (00..59)
                    WheelPicker(
                        items = (0..59).toList(),
                        initialIndex = editorState.minute.coerceIn(0, 59),
                        onItemSelected = { onTimeChanged(editorState.hour, it) },
                        modifier = Modifier.weight(1f),
                        formatItem = { String.format("%02d", it) },
                        isNumeric = true,
                        minValue = 0,
                        maxValue = 59
                    )
                }
            } else {
                val hour12 = when (val h = editorState.hour % 12) {
                    0 -> 12
                    else -> h
                }
                val isPm = editorState.hour >= 12

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(palette.cardBackground)
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(26.dp))
                        .padding(vertical = 14.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hours (1..12)
                    WheelPicker(
                        items = (1..12).toList(),
                        initialIndex = (hour12 - 1).coerceIn(0, 11),
                        onItemSelected = { newH12 ->
                            val newH24 = if (isPm) {
                                if (newH12 == 12) 12 else newH12 + 12
                            } else {
                                if (newH12 == 12) 0 else newH12
                            }
                            onTimeChanged(newH24, editorState.minute)
                        },
                        modifier = Modifier.weight(1f),
                        formatItem = { String.format("%02d", it) },
                        isNumeric = true,
                        minValue = 1,
                        maxValue = 12
                    )

                    Text(
                        text = ":",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Minutes (00..59)
                    WheelPicker(
                        items = (0..59).toList(),
                        initialIndex = editorState.minute.coerceIn(0, 59),
                        onItemSelected = { onTimeChanged(editorState.hour, it) },
                        modifier = Modifier.weight(1f),
                        formatItem = { String.format("%02d", it) },
                        isNumeric = true,
                        minValue = 0,
                        maxValue = 59
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // AM / PM
                    WheelPicker(
                        items = listOf("AM", "PM"),
                        initialIndex = if (isPm) 1 else 0,
                        onItemSelected = { newAmPm ->
                            val newIsPm = (newAmPm == "PM")
                            val newH24 = if (newIsPm) {
                                if (hour12 == 12) 12 else hour12 + 12
                            } else {
                                if (hour12 == 12) 0 else hour12
                            }
                            onTimeChanged(newH24, editorState.minute)
                        },
                        modifier = Modifier.weight(1f),
                        formatItem = { it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Day of Week selector
            Text("Repeat", fontSize = 15.sp, color = palette.textSecondary, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val days = listOf(
                    1 to "S", 2 to "M", 3 to "T", 4 to "W", 5 to "T", 6 to "F", 7 to "S"
                )
                days.forEach { (dayNum, dayLetter) ->
                    val isSelected = editorState.daysOfWeek.contains(dayNum)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) palette.primaryAccent else palette.cardBackground)
                            .border(
                                1.dp,
                                if (isSelected) palette.cardBorder else palette.cardBorderSubtle,
                                CircleShape
                            )
                            .iosClickable(targetScale = 0.96f) { onDayToggled(dayNum) }
                    ) {
                        Text(
                            text = dayLetter,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) palette.onPrimaryAccent else palette.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Calendar Date Picker Glass Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.cardBackground)
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                    .iosClickable(targetScale = 0.96f) { showDatePicker = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Pick Date",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Schedule Date", fontSize = 15.sp, color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (editorState.scheduledDateMillis != null) {
                            SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(Date(editorState.scheduledDateMillis))
                        } else "Not set (next occurrence)",
                        fontSize = 13.sp,
                        color = palette.textSecondary
                    )
                }
                if (editorState.scheduledDateMillis != null) {
                    Text(
                        text = "✕",
                        color = OneUiRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.clickable { onScheduledDateChanged(null) }.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Ringtone Picker Glass Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.cardBackground)
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                    .iosClickable(targetScale = 0.96f) {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Ringtone")
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            if (editorState.ringtoneUri != null) {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(editorState.ringtoneUri))
                            }
                        }
                        ringtoneLauncher.launch(intent)
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Ringtone",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Alarm Ringtone", fontSize = 15.sp, color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = editorState.ringtoneName,
                        fontSize = 13.sp,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Custom audio file picker
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { fileLauncher.launch(arrayOf("audio/*")) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Or pick from local audio files…",
                    fontSize = 13.sp,
                    color = palette.textPrimary.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Alarm Volume Glass Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.cardBackground)
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (editorState.volume == 0f) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Alarm Volume",
                            tint = palette.textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Alarm Volume",
                                fontSize = 15.sp,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Volume level: ${(editorState.volume * 100).toInt()}%",
                                fontSize = 13.sp,
                                color = palette.textSecondary
                            )
                        }
                    }
                    Text(
                        text = "${(editorState.volume * 100).toInt()}%",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (palette.isBlueTheme) Color(0xFF60A5FA) else palette.primaryAccent
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeDown,
                        contentDescription = "Volume Low",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Slider(
                        value = editorState.volume,
                        onValueChange = onVolumeChanged,
                        onValueChangeFinished = {
                            AlarmAudioPreviewer.playPreview(
                                context = context,
                                volumeFraction = editorState.volume,
                                customUri = editorState.ringtoneUri
                            )
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = if (palette.isBlueTheme) Color(0xFF60A5FA) else palette.primaryAccent,
                            activeTrackColor = if (palette.isBlueTheme) Color(0xFF60A5FA) else palette.primaryAccent,
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Volume High",
                        tint = palette.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Alarm Label TextField
            OutlinedTextField(
                value = editorState.label,
                onValueChange = onLabelChanged,
                label = { Text("Alarm Name", color = palette.textSecondary) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            coroutineScope.launch {
                                scrollState.animateScrollTo(scrollState.maxValue)
                            }
                        }
                    },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (palette.isBlueTheme) palette.primaryAccent else Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Snooze Interval Chips
            Text("Snooze Duration", fontSize = 15.sp, color = palette.textSecondary, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3, 5, 10, 15).forEach { mins ->
                    val isSelected = editorState.snoozeDurationMinutes == mins
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) palette.primaryAccent else palette.cardBackground)
                            .border(
                                1.dp,
                                if (isSelected) palette.cardBorder else palette.cardBorderSubtle,
                                RoundedCornerShape(12.dp)
                            )
                            .iosClickable(targetScale = 0.96f) { onSnoozeChanged(mins) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${mins}m",
                            color = if (isSelected) palette.onPrimaryAccent else palette.textSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

    // Material 3 DatePickerDialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = editorState.scheduledDateMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onScheduledDateChanged(datePickerState.selectedDateMillis)
                    showDatePicker = false
                }) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = LiquidTextSecondary)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = Color(0xFF121218)
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Surface(
                color = Color(0xFF121218),
                shape = RoundedCornerShape(28.dp)
            ) {
                DatePicker(
                    state = datePickerState,
                    colors = DatePickerDefaults.colors(
                        containerColor = Color(0xFF121218),
                        titleContentColor = Color.White,
                        headlineContentColor = Color.White,
                        weekdayContentColor = Color.White.copy(alpha = 0.7f),
                        subheadContentColor = Color.White.copy(alpha = 0.8f),
                        yearContentColor = Color.White,
                        currentYearContentColor = Color.White,
                        selectedYearContentColor = Color.White,
                        selectedYearContainerColor = Color(0xFF7C3AED),
                        dayContentColor = Color.White,
                        disabledDayContentColor = Color.White.copy(alpha = 0.38f),
                        selectedDayContentColor = Color.White,
                        selectedDayContainerColor = Color(0xFF7C3AED),
                        todayDateBorderColor = Color(0xFF7C3AED),
                        todayContentColor = Color.White
                    )
                )
            }
        }
    }
}
