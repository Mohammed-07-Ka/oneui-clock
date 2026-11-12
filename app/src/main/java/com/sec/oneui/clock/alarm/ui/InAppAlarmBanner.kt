package com.sec.oneui.clock.alarm.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import com.sec.oneui.clock.alarm.service.AlarmForegroundService
import com.sec.oneui.clock.theme.ClockTheme
import com.sec.oneui.clock.theme.OneUiRed
import com.sec.oneui.clock.theme.iosPressEffect

@Composable
fun InAppAlarmBanner(
    onDismiss: (Int) -> Unit,
    onSnooze: (Int) -> Unit
) {
    val ringingAlarm by AlarmForegroundService.currentRingingAlarm.collectAsState()
    val palette = ClockTheme.palette

    AnimatedVisibility(
        visible = ringingAlarm != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        ringingAlarm?.let { alarm ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(26.dp)),
                shape = RoundedCornerShape(26.dp),
                color = palette.cardBackground,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = "Alarm Ringing",
                            tint = palette.textPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = alarm.label.ifBlank { "Alarm Ringing" },
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "${alarm.formattedTime()} ${alarm.amPm()}",
                                color = palette.textSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onSnooze(alarm.id) },
                            modifier = Modifier
                                .weight(1f)
                                .iosPressEffect(targetScale = 0.95f),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = palette.cardBackground,
                                contentColor = palette.textPrimary
                            )
                        ) {
                            Text("Snooze (${alarm.snoozeDurationMinutes}m)", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { onDismiss(alarm.id) },
                            modifier = Modifier
                                .weight(1f)
                                .iosPressEffect(targetScale = 0.95f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OneUiRed,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Dismiss", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
