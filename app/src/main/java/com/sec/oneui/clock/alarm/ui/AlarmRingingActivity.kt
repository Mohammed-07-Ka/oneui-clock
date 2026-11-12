package com.sec.oneui.clock.alarm.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.sec.oneui.clock.alarm.data.AlarmDatabase
import com.sec.oneui.clock.alarm.data.AlarmEntity
import com.sec.oneui.clock.alarm.receiver.AlarmReceiver
import com.sec.oneui.clock.alarm.scheduler.AlarmScheduler
import com.sec.oneui.clock.settings.SettingsManager
import com.sec.oneui.clock.theme.ClockTheme
import com.sec.oneui.clock.theme.GeminiAnimatedBackground
import com.sec.oneui.clock.theme.OneUIClockTheme
import com.sec.oneui.clock.theme.OneUiRed
import com.sec.oneui.clock.theme.iosPressEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmRingingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        turnScreenOnAndKeyguard()

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val settingsManager = SettingsManager.getInstance(this)
        val alarmId = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)

        setContent {
            val themeMode by settingsManager.themeMode.collectAsState()
            val animationEnabled by settingsManager.animationEnabled.collectAsState()
            val animationSpeed by settingsManager.animationSpeed.collectAsState()

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                OneUIClockTheme(themeMode = themeMode) {
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
                        themeMode = themeMode,
                        animationEnabled = animationEnabled,
                        animationSpeed = animationSpeed
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
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            val prefs = getSharedPreferences("clock_settings", Context.MODE_PRIVATE)
            val action = prefs.getString("volume_buttons_action", "Snooze") ?: "Snooze"
            val alarmId = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)

            when (action) {
                "Snooze" -> {
                    val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
                        this.action = AlarmReceiver.ACTION_SNOOZE
                        putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                    }
                    sendBroadcast(snoozeIntent)
                    finish()
                    return true
                }
                "Dismiss" -> {
                    val dismissIntent = Intent(this, AlarmReceiver::class.java).apply {
                        this.action = AlarmReceiver.ACTION_DISMISS
                        putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                    }
                    sendBroadcast(dismissIntent)
                    finish()
                    return true
                }
                "Do Nothing" -> return true
            }
        }
        return super.onKeyDown(keyCode, event)
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
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
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

    val palette = ClockTheme.palette

    // Infinite breathing glow for ringing alarm icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Pulsing Alarm Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(palette.cardBackground)
                    .border(1.dp, palette.cardBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = "Ringing",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(60.dp)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Time text in iOS Liquid style
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = alarm?.formattedTime() ?: currentTime,
                    fontSize = 68.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Text(
                    text = " " + (alarm?.amPm() ?: currentAmPm),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = palette.textSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = alarm?.label?.ifBlank { "Alarm" } ?: "Alarm",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textSecondary
            )

            if (alarm?.ringtoneName != null && alarm.ringtoneName != "Default") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "🔔 ${alarm.ringtoneName}",
                    fontSize = 15.sp,
                    color = palette.textSecondary.copy(alpha = 0.8f)
                )
            }
        }

        // Action Buttons: Snooze in Dark Glass, Dismiss in Red with iOS Press Feedback
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
                    .height(60.dp)
                    .iosPressEffect(targetScale = 0.95f),
                shape = RoundedCornerShape(30.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = palette.cardBackground,
                    contentColor = palette.textPrimary
                )
            ) {
                Text("Snooze (${alarm?.snoozeDurationMinutes ?: 5}m)", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .iosPressEffect(targetScale = 0.95f),
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
