package com.sec.oneui.clock.alarm.ui

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sec.oneui.clock.alarm.data.AlarmEntity
import com.sec.oneui.clock.theme.GeminiCyanSparkle
import com.sec.oneui.clock.theme.GeminiDarkViolet
import com.sec.oneui.clock.theme.GeminiGlassBackgroundDark
import com.sec.oneui.clock.theme.GeminiGlassBackgroundHover
import com.sec.oneui.clock.theme.GeminiGlassBorderGradientDark
import com.sec.oneui.clock.theme.GeminiLavender
import com.sec.oneui.clock.theme.GeminiPurple
import com.sec.oneui.clock.theme.GeminiPurpleLight
import com.sec.oneui.clock.theme.OneUiRed
import com.sec.oneui.clock.theme.OneUiTextPrimary
import com.sec.oneui.clock.theme.OneUiTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(
    viewModel: AlarmViewModel,
    modifier: Modifier = Modifier
) {
    val alarms by viewModel.alarms.collectAsState()
    val nearestRemaining by viewModel.nearestAlarmHeader.collectAsState()

    var showEditSheet by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<AlarmEntity?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Alarm",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = OneUiTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = nearestRemaining,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = GeminiLavender
            )
            Spacer(modifier = Modifier.height(20.dp))

            if (alarms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "✦",
                            fontSize = 38.sp,
                            color = GeminiPurpleLight
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No alarms\nTap + to set an alarm",
                            fontSize = 17.sp,
                            color = GeminiLavender,
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
                            onToggle = { viewModel.toggleAlarm(alarm) },
                            onClick = {
                                editingAlarm = alarm
                                showEditSheet = true
                            },
                            onDelete = { viewModel.deleteAlarm(alarm) }
                        )
                    }
                }
            }
        }

        // Floating Action Button with Glowing Gemini Gradient
        FloatingActionButton(
            onClick = {
                editingAlarm = null
                showEditSheet = true
            },
            containerColor = Color.Transparent,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp)
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(GeminiPurple, GeminiDarkViolet)
                    )
                )
                .border(1.dp, GeminiGlassBorderGradientDark, CircleShape)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Alarm", modifier = Modifier.size(34.dp))
        }

        if (showEditSheet) {
            AlarmEditSheet(
                existingAlarm = editingAlarm,
                onDismiss = { showEditSheet = false },
                onSave = { h, m, label, days, snooze, vib, rUri, rName, dateMs ->
                    viewModel.saveAlarm(
                        id = editingAlarm?.id ?: 0,
                        hour = h,
                        minute = m,
                        label = label,
                        daysSet = days,
                        snoozeDuration = snooze,
                        vibrate = vib,
                        ringtoneUri = rUri,
                        ringtoneName = rName,
                        scheduledDateMillis = dateMs
                    )
                    showEditSheet = false
                }
            )
        }
    }
}

@Composable
fun AlarmCard(
    alarm: AlarmEntity,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .border(1.dp, GeminiGlassBorderGradientDark, RoundedCornerShape(26.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = GeminiGlassBackgroundDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = alarm.formattedTime(),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (alarm.isEnabled) Color.White else OneUiTextSecondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = alarm.amPm(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (alarm.isEnabled) GeminiLavender else OneUiTextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${alarm.label.ifBlank { "Alarm" }} · ${alarm.daysDescription()}",
                    fontSize = 14.sp,
                    color = GeminiLavender
                )
                // Show scheduled date if set
                alarm.formattedScheduledDate()?.let { dateStr ->
                    Text(
                        text = "📅 $dateStr",
                        fontSize = 13.sp,
                        color = GeminiCyanSparkle,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                // Show custom ringtone name
                if (alarm.ringtoneName != "Default") {
                    Text(
                        text = "🔔 ${alarm.ringtoneName}",
                        fontSize = 13.sp,
                        color = GeminiLavender.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete Alarm",
                    tint = GeminiLavender.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = GeminiPurple,
                    uncheckedThumbColor = GeminiLavender.copy(alpha = 0.5f),
                    uncheckedTrackColor = GeminiGlassBackgroundHover
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditSheet(
    existingAlarm: AlarmEntity?,
    onDismiss: () -> Unit,
    onSave: (
        hour: Int, minute: Int, label: String, days: Set<Int>,
        snooze: Int, vibrate: Boolean,
        ringtoneUri: String?, ringtoneName: String,
        scheduledDateMillis: Long?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    var hour by remember { mutableIntStateOf(existingAlarm?.hour ?: 7) }
    var minute by remember { mutableIntStateOf(existingAlarm?.minute ?: 0) }
    var label by remember { mutableStateOf(existingAlarm?.label ?: "Alarm") }
    var selectedDays by remember { mutableStateOf(existingAlarm?.getDaysSet() ?: emptySet()) }
    var snoozeMinutes by remember { mutableIntStateOf(existingAlarm?.snoozeDurationMinutes ?: 5) }
    var vibrate by remember { mutableStateOf(existingAlarm?.vibrate ?: true) }
    var ringtoneUri by remember { mutableStateOf(existingAlarm?.ringtoneUri) }
    var ringtoneName by remember { mutableStateOf(existingAlarm?.ringtoneName ?: "Default") }
    var scheduledDateMillis by remember { mutableStateOf(existingAlarm?.scheduledDateMillis) }
    var showDatePicker by remember { mutableStateOf(false) }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                ringtoneUri = uri.toString()
                val ringtone = RingtoneManager.getRingtone(context, uri)
                ringtoneName = ringtone?.getTitle(context) ?: "Custom"
            } else {
                ringtoneUri = null
                ringtoneName = "Silent"
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
            } catch (_: Exception) { }
            ringtoneUri = uri.toString()
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            val name = cursor?.use {
                val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (it.moveToFirst() && idx >= 0) it.getString(idx) else null
            } ?: "Custom audio"
            ringtoneName = name
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF130E29),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = if (existingAlarm == null) "✦ Add Alarm" else "✦ Edit Alarm",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Time Selector (Hour / Minute controls)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(GeminiGlassBackgroundDark)
                    .border(1.dp, GeminiGlassBorderGradientDark, RoundedCornerShape(24.dp))
                    .padding(vertical = 18.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hour Buttons
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("▲", color = GeminiPurple, modifier = Modifier.clickable { hour = (hour + 1) % 24 }.padding(8.dp))
                    Text(String.format("%02d", hour), fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("▼", color = GeminiPurple, modifier = Modifier.clickable { hour = if (hour == 0) 23 else hour - 1 }.padding(8.dp))
                }

                Text(" : ", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = GeminiLavender, modifier = Modifier.padding(horizontal = 16.dp))

                // Minute Buttons
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("▲", color = GeminiPurple, modifier = Modifier.clickable { minute = (minute + 1) % 60 }.padding(8.dp))
                    Text(String.format("%02d", minute), fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("▼", color = GeminiPurple, modifier = Modifier.clickable { minute = if (minute == 0) 59 else minute - 1 }.padding(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Day of Week selector
            Text("Repeat", fontSize = 15.sp, color = GeminiLavender, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val days = listOf(
                    1 to "S", 2 to "M", 3 to "T", 4 to "W", 5 to "T", 6 to "F", 7 to "S"
                )
                days.forEach { (dayNum, dayLetter) ->
                    val isSelected = selectedDays.contains(dayNum)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) GeminiPurple else GeminiGlassBackgroundDark)
                            .border(1.dp, if (isSelected) BorderStroke(1.dp, Color.White) else BorderStroke(1.dp, GeminiGlassBorderGradientDark), CircleShape)
                            .clickable {
                                selectedDays = if (isSelected) {
                                    selectedDays - dayNum
                                } else {
                                    selectedDays + dayNum
                                }
                            }
                    ) {
                        Text(
                            text = dayLetter,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else GeminiLavender
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
                    .background(GeminiGlassBackgroundDark)
                    .border(1.dp, GeminiGlassBorderGradientDark, RoundedCornerShape(18.dp))
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Pick Date",
                    tint = GeminiPurpleLight,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Schedule Date", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (scheduledDateMillis != null) {
                            SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(Date(scheduledDateMillis!!))
                        } else "Not set (next occurrence)",
                        fontSize = 13.sp,
                        color = GeminiLavender
                    )
                }
                if (scheduledDateMillis != null) {
                    Text(
                        text = "✕",
                        color = OneUiRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.clickable { scheduledDateMillis = null }.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Ringtone Picker Glass Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(GeminiGlassBackgroundDark)
                    .border(1.dp, GeminiGlassBorderGradientDark, RoundedCornerShape(18.dp))
                    .clickable {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Ringtone")
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            if (ringtoneUri != null) {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(ringtoneUri))
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
                    tint = GeminiPurpleLight,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Alarm Ringtone", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = ringtoneName,
                        fontSize = 13.sp,
                        color = GeminiLavender,
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
                    text = "✦ Or pick from local audio files…",
                    fontSize = 13.sp,
                    color = GeminiPurpleLight,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Alarm Label TextField
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Alarm Name", color = GeminiLavender) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GeminiPurple,
                    unfocusedBorderColor = GeminiLavender.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Snooze Interval Chips
            Text("Snooze Duration", fontSize = 15.sp, color = GeminiLavender, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3, 5, 10, 15).forEach { mins ->
                    val isSelected = snoozeMinutes == mins
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) GeminiPurple else GeminiGlassBackgroundDark)
                            .border(1.dp, GeminiGlassBorderGradientDark, RoundedCornerShape(12.dp))
                            .clickable { snoozeMinutes = mins }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${mins}m",
                            color = if (isSelected) Color.White else GeminiLavender,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Cancel and Save Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, GeminiLavender.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GeminiLavender)
                ) {
                    Text("Cancel", fontSize = 16.sp)
                }

                Button(
                    onClick = {
                        onSave(hour, minute, label, selectedDays, snoozeMinutes, vibrate, ringtoneUri, ringtoneName, scheduledDateMillis)
                    },
                    modifier = Modifier.weight(1f).height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GeminiPurple,
                        contentColor = Color.White
                    )
                ) {
                    Text("Save", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Material 3 DatePickerDialog with Gemini Purple theme
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = scheduledDateMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    scheduledDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) {
                    Text("OK", color = GeminiPurpleLight, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = GeminiLavender)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
