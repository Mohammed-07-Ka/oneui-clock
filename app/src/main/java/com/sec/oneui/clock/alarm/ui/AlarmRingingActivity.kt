package com.sec.oneui.clock.alarm.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sec.oneui.clock.alarm.data.AlarmDatabase
import com.sec.oneui.clock.alarm.data.AlarmEntity
import com.sec.oneui.clock.alarm.receiver.AlarmReceiver
import com.sec.oneui.clock.alarm.scheduler.AlarmScheduler
import com.sec.oneui.clock.settings.ThemeMode
import com.sec.oneui.clock.theme.GeminiAnimatedBackground
import com.sec.oneui.clock.theme.GeminiGlassBackgroundDark
import com.sec.oneui.clock.theme.GeminiGlassBorderGradientDark
import com.sec.oneui.clock.theme.GeminiLavender
import com.sec.oneui.clock.theme.GeminiPurple
import com.sec.oneui.clock.theme.GeminiPurpleLight
import com.sec.oneui.clock.theme.OneUIClockTheme
import com.sec.oneui.clock.theme.OneUiRed
import com.sec.oneui.clock.theme.OneUiTextPrimary
import com.sec.oneui.clock.theme.OneUiTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmRingingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        turnScreenOnAndKeyguard()

        val alarmId = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)

        setContent {
            OneUIClockTheme(themeMode = ThemeMode.DARK_GEMINI) {
                var alarm by remember { mutableStateOf<AlarmEntity?>(null) }

                LaunchedEffect(alarmId) {
                    if (alarmId != -1) {
                        val loaded = withContext(Dispatchers.IO) {
                            AlarmDatabase.getInstance(applicationContext).alarmDao().getAlarmById(alarmId)
                        }
                        alarm = loaded
                    }
                }

                GeminiAnimatedBackground(
                    themeMode = ThemeMode.DARK_GEMINI,
                    animationEnabled = true,
                    animationSpeed = 1.2f
                ) {
                    AlarmRingingContent(
                        alarm = alarm,
                        onDismiss = {
                            val dismissIntent = Intent(this@AlarmRingingActivity, AlarmReceiver::class.java).apply {
                                action = AlarmReceiver.ACTION_DISMISS
                                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                            }
                            sendBroadcast(dismissIntent)
                            finish()
                        },
                        onSnooze = {
                            val snoozeIntent = Intent(this@AlarmRingingActivity, AlarmReceiver::class.java).apply {
                                action = AlarmReceiver.ACTION_SNOOZE
                                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                            }
                            sendBroadcast(snoozeIntent)
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun turnScreenOnAndKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
}

@Composable
fun AlarmRingingContent(
    alarm: AlarmEntity?,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    val currentTime = remember {
        SimpleDateFormat("hh:mm", Locale.getDefault()).format(Date())
    }
    val currentAmPm = remember {
        SimpleDateFormat("a", Locale.getDefault()).format(Date())
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Pulsing Gemini Glowing Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(130.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(GeminiPurple.copy(alpha = 0.25f))
                    .border(2.dp, GeminiGlassBorderGradientDark, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = "Ringing",
                    tint = Color.White,
                    modifier = Modifier.size(68.dp)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Time text with Glowing Glass effect
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = alarm?.formattedTime() ?: currentTime,
                    fontSize = 68.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = " " + (alarm?.amPm() ?: currentAmPm),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = GeminiLavender,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = alarm?.label?.ifBlank { "Alarm" } ?: "Alarm",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = GeminiLavender
            )

            // Ringtone name indicator
            if (alarm?.ringtoneName != null && alarm.ringtoneName != "Default") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "🔔 ${alarm.ringtoneName}",
                    fontSize = 15.sp,
                    color = GeminiPurpleLight
                )
            }
        }

        // Action Buttons with Glassmorphic Styling
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
                shape = RoundedCornerShape(30.dp),
                border = BorderStroke(1.dp, GeminiLavender.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = GeminiGlassBackgroundDark,
                    contentColor = Color.White
                )
            ) {
                Text("Snooze (${alarm?.snoozeDurationMinutes ?: 5}m)", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OneUiRed,
                    contentColor = Color.White
                )
            ) {
                Text("Dismiss", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
