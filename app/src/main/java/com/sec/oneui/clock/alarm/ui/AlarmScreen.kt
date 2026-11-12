package com.sec.oneui.clock.alarm.ui

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sec.oneui.clock.alarm.data.AlarmEntity
import com.sec.oneui.clock.settings.SettingsManager
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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
                color = LiquidTextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = nearestRemaining,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = LiquidTextSecondary
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
                            color = LiquidTextSecondary,
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

        // iOS Liquid Glass Floating Action Button
        FloatingActionButton(
            onClick = {
                editingAlarm = null
                showEditSheet = true
            },
            containerColor = LiquidGlassActive,
            contentColor = LiquidActiveText,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp)
                .size(62.dp)
                .clip(CircleShape)
                .border(1.dp, LiquidSpecularBorder, CircleShape)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Alarm", modifier = Modifier.size(32.dp))
        }

        if (showEditSheet) {
            AlarmEditSheet(
                existingAlarm = editingAlarm,
                use24HourFormat = use24HourFormat,
                defaultVibrate = defaultVibrate,
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
    use24HourFormat: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .border(1.dp, LiquidSpecularBorder, RoundedCornerShape(26.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = LiquidGlassBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    if (use24HourFormat) {
                        Text(
                            text = String.format("%02d:%02d", alarm.hour, alarm.minute),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (alarm.isEnabled) LiquidTextPrimary else LiquidTextMuted
                        )
                    } else {
                        Text(
                            text = alarm.formattedTime(),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (alarm.isEnabled) LiquidTextPrimary else LiquidTextMuted
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = alarm.amPm(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (alarm.isEnabled) LiquidTextSecondary else LiquidTextMuted,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${alarm.label.ifBlank { "Alarm" }} · ${alarm.daysDescription()}",
                    fontSize = 14.sp,
                    color = LiquidTextSecondary
                )
                // Show scheduled date if set
                alarm.formattedScheduledDate()?.let { dateStr ->
                    Text(
                        text = "📅 $dateStr",
                        fontSize = 13.sp,
                        color = LiquidTextPrimary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                // Show custom ringtone name
                if (alarm.ringtoneName != "Default") {
                    Text(
                        text = "🔔 ${alarm.ringtoneName}",
                        fontSize = 13.sp,
                        color = LiquidTextSecondary,
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
                    tint = LiquidTextSecondary.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = LiquidActiveText,
                    checkedTrackColor = LiquidGlassActive,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                    uncheckedTrackColor = LiquidGlassHover
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditSheet(
    existingAlarm: AlarmEntity?,
    use24HourFormat: Boolean,
    defaultVibrate: Boolean,
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

    // Initialize with current device time when creating a new alarm
    val now = remember { Calendar.getInstance() }
    val initialHour24 = existingAlarm?.hour ?: now.get(Calendar.HOUR_OF_DAY)
    val initialMinute = existingAlarm?.minute ?: now.get(Calendar.MINUTE)

    val initialHour12 = when (val h = initialHour24 % 12) {
        0 -> 12
        else -> h
    }
    val initialIsPm = initialHour24 >= 12

    var hour24 by remember { mutableIntStateOf(initialHour24) }
    var selectedHour12 by remember { mutableIntStateOf(initialHour12) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute) }
    var isPm by remember { mutableStateOf(initialIsPm) }

    var label by remember { mutableStateOf(existingAlarm?.label ?: "Alarm") }
    var selectedDays by remember { mutableStateOf(existingAlarm?.getDaysSet() ?: emptySet()) }
    var snoozeMinutes by remember { mutableIntStateOf(existingAlarm?.snoozeDurationMinutes ?: 5) }
    var vibrate by remember { mutableStateOf(existingAlarm?.vibrate ?: defaultVibrate) }
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
        containerColor = Color(0xFF0C0914),
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
                text = if (existingAlarm == null) "Add Alarm" else "Edit Alarm",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = LiquidTextPrimary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // iOS Liquid Glass Wheel Time Picker (Adapts to 24-hour / 12-hour format)
            if (use24HourFormat) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(LiquidGlassBackground)
                        .border(1.dp, LiquidSpecularBorder, RoundedCornerShape(26.dp))
                        .padding(vertical = 14.dp, horizontal = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hours (00..23)
                    WheelPicker(
                        items = (0..23).toList(),
                        initialIndex = hour24.coerceIn(0, 23),
                        onItemSelected = { hour24 = it },
                        modifier = Modifier.weight(1f),
                        formatItem = { String.format("%02d", it) }
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
                        initialIndex = selectedMinute.coerceIn(0, 59),
                        onItemSelected = { selectedMinute = it },
                        modifier = Modifier.weight(1f),
                        formatItem = { String.format("%02d", it) }
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(LiquidGlassBackground)
                        .border(1.dp, LiquidSpecularBorder, RoundedCornerShape(26.dp))
                        .padding(vertical = 14.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hours (1..12)
                    WheelPicker(
                        items = (1..12).toList(),
                        initialIndex = (selectedHour12 - 1).coerceIn(0, 11),
                        onItemSelected = { selectedHour12 = it },
                        modifier = Modifier.weight(1f),
                        formatItem = { String.format("%02d", it) }
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
                        initialIndex = selectedMinute.coerceIn(0, 59),
                        onItemSelected = { selectedMinute = it },
                        modifier = Modifier.weight(1f),
                        formatItem = { String.format("%02d", it) }
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // AM / PM
                    WheelPicker(
                        items = listOf("AM", "PM"),
                        initialIndex = if (isPm) 1 else 0,
                        onItemSelected = { isPm = (it == "PM") },
                        modifier = Modifier.weight(1f),
                        formatItem = { it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Day of Week selector
            Text("Repeat", fontSize = 15.sp, color = LiquidTextSecondary, fontWeight = FontWeight.SemiBold)
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
                            .background(if (isSelected) LiquidGlassActive else LiquidGlassBackground)
                            .border(
                                1.dp,
                                if (isSelected) LiquidSpecularBorder else LiquidSpecularBorderSubtle,
                                CircleShape
                            )
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
                            color = if (isSelected) LiquidActiveText else LiquidTextSecondary
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
                    .background(LiquidGlassBackground)
                    .border(1.dp, LiquidSpecularBorder, RoundedCornerShape(18.dp))
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Pick Date",
                    tint = LiquidTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Schedule Date", fontSize = 15.sp, color = LiquidTextPrimary, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (scheduledDateMillis != null) {
                            SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(Date(scheduledDateMillis!!))
                        } else "Not set (next occurrence)",
                        fontSize = 13.sp,
                        color = LiquidTextSecondary
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
                    .background(LiquidGlassBackground)
                    .border(1.dp, LiquidSpecularBorder, RoundedCornerShape(18.dp))
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
                    tint = LiquidTextPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Alarm Ringtone", fontSize = 15.sp, color = LiquidTextPrimary, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = ringtoneName,
                        fontSize = 13.sp,
                        color = LiquidTextSecondary,
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
                    color = LiquidTextPrimary.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Alarm Label TextField
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Alarm Name", color = LiquidTextSecondary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Snooze Interval Chips
            Text("Snooze Duration", fontSize = 15.sp, color = LiquidTextSecondary, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3, 5, 10, 15).forEach { mins ->
                    val isSelected = snoozeMinutes == mins
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) LiquidGlassActive else LiquidGlassBackground)
                            .border(
                                1.dp,
                                if (isSelected) LiquidSpecularBorder else LiquidSpecularBorderSubtle,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { snoozeMinutes = mins }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${mins}m",
                            color = if (isSelected) LiquidActiveText else LiquidTextSecondary,
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
                // Cancel Button: Dark Glass
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(LiquidGlassBackground)
                        .border(1.dp, LiquidSpecularBorderSubtle, RoundedCornerShape(18.dp))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cancel", fontSize = 16.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Medium)
                }

                // Save Button: Luminous White Liquid Glass with Black text
                Button(
                    onClick = {
                        val finalHour24 = if (use24HourFormat) {
                            hour24
                        } else {
                            if (isPm) {
                                if (selectedHour12 == 12) 12 else selectedHour12 + 12
                            } else {
                                if (selectedHour12 == 12) 0 else selectedHour12
                            }
                        }
                        onSave(finalHour24, selectedMinute, label, selectedDays, snoozeMinutes, vibrate, ringtoneUri, ringtoneName, scheduledDateMillis)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LiquidGlassActive,
                        contentColor = LiquidActiveText
                    )
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
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = LiquidTextSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * Vertical Touch Scroll Wheel Picker for Hours, Minutes, and AM/PM.
 * Uses Compose snapping with center item highlight and fading adjacent items.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> WheelPicker(
    items: List<T>,
    initialIndex: Int,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    visibleItemsCount: Int = 3,
    itemHeight: Dp = 48.dp,
    formatItem: (T) -> String = { it.toString() }
) {
    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.toPx() }
    val safeInitialIndex = initialIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = safeInitialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val currentIndex by remember {
        derivedStateOf {
            val offset = listState.firstVisibleItemScrollOffset
            val firstIndex = listState.firstVisibleItemIndex
            val index = if (offset > itemHeightPx / 2f) firstIndex + 1 else firstIndex
            index.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        }
    }

    LaunchedEffect(currentIndex) {
        if (currentIndex in items.indices) {
            onItemSelected(items[currentIndex])
        }
    }

    Box(
        modifier = modifier
            .height(itemHeight * visibleItemsCount)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Selection highlight bar in iOS glass style
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
        )

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight * (visibleItemsCount / 2)),
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(items) { index, item ->
                val isSelected = index == currentIndex
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatItem(item),
                        fontSize = if (isSelected) 36.sp else 22.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.35f)
                    )
                }
            }
        }
    }
}
