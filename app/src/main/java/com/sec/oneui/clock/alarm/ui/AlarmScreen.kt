package com.sec.oneui.clock.alarm.ui

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sec.oneui.clock.alarm.data.AlarmEntity
import com.sec.oneui.clock.theme.OneUiBlack
import com.sec.oneui.clock.theme.OneUiBlue
import com.sec.oneui.clock.theme.OneUiCard
import com.sec.oneui.clock.theme.OneUiCardHover
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
            .background(OneUiBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // One UI Header
            Spacer(modifier = Modifier.height(24.dp))
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
                color = OneUiTextSecondary
            )
            Spacer(modifier = Modifier.height(20.dp))

            if (alarms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No alarms\nTap + to set an alarm",
                        fontSize = 18.sp,
                        color = OneUiTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
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

        // Floating Action Button
        FloatingActionButton(
            onClick = {
                editingAlarm = null
                showEditSheet = true
            },
            containerColor = OneUiBlue,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp)
                .size(62.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Alarm", modifier = Modifier.size(32.dp))
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
            .clickable { onClick() },
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = OneUiCard)
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
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (alarm.isEnabled) OneUiTextPrimary else OneUiTextSecondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = alarm.amPm(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (alarm.isEnabled) OneUiTextPrimary else OneUiTextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${alarm.label.ifBlank { "Alarm" }} · ${alarm.daysDescription()}",
                    fontSize = 14.sp,
                    color = OneUiTextSecondary
                )
                // Show scheduled date if set
                alarm.formattedScheduledDate()?.let { dateStr ->
                    Text(
                        text = "📅 $dateStr",
                        fontSize = 13.sp,
                        color = OneUiBlue,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                // Show ringtone name if custom
                if (alarm.ringtoneName != "Default") {
                    Text(
                        text = "🔔 ${alarm.ringtoneName}",
                        fontSize = 13.sp,
                        color = OneUiTextSecondary,
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
                    tint = OneUiTextSecondary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = OneUiBlue,
                    uncheckedThumbColor = OneUiTextSecondary,
                    uncheckedTrackColor = OneUiCardHover
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

    // Ringtone Picker launcher
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

    // Custom file picker launcher
    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            // Take persistable permission so the URI survives reboots
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) { }
            ringtoneUri = uri.toString()
            // Extract filename
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            val name = cursor?.use {
                val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (it.moveToFirst() && idx >= 0) it.getString(idx) else null
            } ?: "Custom file"
            ringtoneName = name
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OneUiCard,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = if (existingAlarm == null) "Add Alarm" else "Edit Alarm",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = OneUiTextPrimary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Time Selector (Hour / Minute controls)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OneUiBlack, RoundedCornerShape(20.dp))
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hour Buttons
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("▲", color = OneUiBlue, modifier = Modifier.clickable { hour = (hour + 1) % 24 }.padding(8.dp))
                    Text(String.format("%02d", hour), fontSize = 42.sp, fontWeight = FontWeight.Bold, color = OneUiTextPrimary)
                    Text("▼", color = OneUiBlue, modifier = Modifier.clickable { hour = if (hour == 0) 23 else hour - 1 }.padding(8.dp))
                }

                Text(" : ", fontSize = 42.sp, fontWeight = FontWeight.Bold, color = OneUiTextSecondary, modifier = Modifier.padding(horizontal = 16.dp))

                // Minute Buttons
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("▲", color = OneUiBlue, modifier = Modifier.clickable { minute = (minute + 1) % 60 }.padding(8.dp))
                    Text(String.format("%02d", minute), fontSize = 42.sp, fontWeight = FontWeight.Bold, color = OneUiTextPrimary)
                    Text("▼", color = OneUiBlue, modifier = Modifier.clickable { minute = if (minute == 0) 59 else minute - 1 }.padding(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Day of Week selector
            Text("Repeat", fontSize = 15.sp, color = OneUiTextSecondary, fontWeight = FontWeight.SemiBold)
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) OneUiBlue else OneUiCardHover)
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
                            color = if (isSelected) Color.White else OneUiTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Calendar Date Picker
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(OneUiCardHover)
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Pick Date",
                    tint = OneUiBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Schedule Date", fontSize = 15.sp, color = OneUiTextPrimary, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (scheduledDateMillis != null) {
                            SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(Date(scheduledDateMillis!!))
                        } else "Not set (next occurrence)",
                        fontSize = 13.sp,
                        color = OneUiTextSecondary
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

            // Ringtone Picker
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(OneUiCardHover)
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
                    tint = OneUiBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Alarm Ringtone", fontSize = 15.sp, color = OneUiTextPrimary, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = ringtoneName,
                        fontSize = 13.sp,
                        color = OneUiTextSecondary,
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
                    text = "Or pick from files…",
                    fontSize = 13.sp,
                    color = OneUiBlue,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Alarm Label
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Alarm Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OneUiBlue,
                    unfocusedBorderColor = OneUiCardHover,
                    focusedTextColor = OneUiTextPrimary,
                    unfocusedTextColor = OneUiTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Snooze Interval Chips (3m, 5m, 10m, 15m)
            Text("Snooze Duration", fontSize = 15.sp, color = OneUiTextSecondary, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3, 5, 10, 15).forEach { mins ->
                    val isSelected = snoozeMinutes == mins
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) OneUiBlue else OneUiCardHover)
                            .clickable { snoozeMinutes = mins }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${mins}m",
                            color = if (isSelected) Color.White else OneUiTextSecondary,
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
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OneUiTextSecondary)
                ) {
                    Text("Cancel", fontSize = 16.sp)
                }

                Button(
                    onClick = {
                        onSave(hour, minute, label, selectedDays, snoozeMinutes, vibrate, ringtoneUri, ringtoneName, scheduledDateMillis)
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OneUiBlue)
                ) {
                    Text("Save", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Material 3 DatePickerDialog
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
                    Text("OK", color = OneUiBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = OneUiTextSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
