package com.sec.oneui.clock

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sec.oneui.clock.alarm.ui.AlarmScreen
import com.sec.oneui.clock.alarm.ui.AlarmViewModel
import com.sec.oneui.clock.alarm.ui.InAppAlarmBanner
import com.sec.oneui.clock.settings.SettingsManager
import com.sec.oneui.clock.settings.SettingsScreen
import com.sec.oneui.clock.settings.ThemeMode
import com.sec.oneui.clock.stopwatch.ui.StopwatchScreen
import com.sec.oneui.clock.stopwatch.ui.StopwatchViewModel
import com.sec.oneui.clock.theme.GeminiAnimatedBackground
import com.sec.oneui.clock.theme.GeminiGlassBackgroundDark
import com.sec.oneui.clock.theme.GeminiGlassBackgroundLight
import com.sec.oneui.clock.theme.GeminiGlassBorderGradientDark
import com.sec.oneui.clock.theme.GeminiGlassBorderGradientLight
import com.sec.oneui.clock.theme.GeminiLavender
import com.sec.oneui.clock.theme.GeminiPurple
import com.sec.oneui.clock.theme.OneUIClockTheme
import com.sec.oneui.clock.timer.ui.TimerScreen
import com.sec.oneui.clock.timer.ui.TimerViewModel

class MainActivity : ComponentActivity() {

    private val alarmViewModel: AlarmViewModel by viewModels()
    private val stopwatchViewModel: StopwatchViewModel by viewModels()
    private val timerViewModel: TimerViewModel by viewModels()

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Notification permission result handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settingsManager = SettingsManager.getInstance(this)

        // 1. Request POST_NOTIFICATIONS on Android 13+ (Tiramisu)
        checkAndRequestNotificationPermission()

        // 2. Check SCHEDULE_EXACT_ALARM on Android 12+ (S)
        checkAndRequestExactAlarmPermission()

        setContent {
            val themeMode by settingsManager.themeMode.collectAsState()
            val animationEnabled by settingsManager.animationEnabled.collectAsState()
            val animationSpeed by settingsManager.animationSpeed.collectAsState()

            OneUIClockTheme(themeMode = themeMode) {
                GeminiAnimatedBackground(
                    themeMode = themeMode,
                    animationEnabled = animationEnabled,
                    animationSpeed = animationSpeed
                ) {
                    MainClockApp(
                        alarmViewModel = alarmViewModel,
                        stopwatchViewModel = stopwatchViewModel,
                        timerViewModel = timerViewModel,
                        settingsManager = settingsManager,
                        themeMode = themeMode
                    )
                }
            }
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun checkAndRequestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }
}

sealed class ClockTab(val index: Int, val title: String, val icon: ImageVector) {
    data object Alarm : ClockTab(0, "Alarm", Icons.Default.Alarm)
    data object Stopwatch : ClockTab(1, "Stopwatch", Icons.Default.Timer)
    data object Timer : ClockTab(2, "Timer", Icons.Default.HourglassBottom)

    companion object {
        val items = listOf(Alarm, Stopwatch, Timer)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainClockApp(
    alarmViewModel: AlarmViewModel,
    stopwatchViewModel: StopwatchViewModel,
    timerViewModel: TimerViewModel,
    settingsManager: SettingsManager,
    themeMode: ThemeMode
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    val isLight = themeMode == ThemeMode.LIGHT_PEARL
    val glassBg = if (isLight) GeminiGlassBackgroundLight else GeminiGlassBackgroundDark
    val glassBorder = if (isLight) GeminiGlassBorderGradientLight else GeminiGlassBorderGradientDark
    val navSelectedColor = if (isLight) GeminiPurple else Color.White
    val navUnselectedColor = if (isLight) Color(0xFF6B5887) else GeminiLavender.copy(alpha = 0.65f)

    if (showSettings) {
        SettingsScreen(
            settingsManager = settingsManager,
            onBack = { showSettings = false }
        )
        return
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "✦ Gemini Clock",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLight) Color(0xFF1E1035) else Color.White
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(glassBg)
                            .border(1.dp, glassBorder, RoundedCornerShape(16.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = if (isLight) GeminiPurple else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            // Glassmorphic Floating Bottom Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                NavigationBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .background(glassBg)
                        .border(1.dp, glassBorder, RoundedCornerShape(32.dp)),
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp
                ) {
                    ClockTab.items.forEach { tab ->
                        val isSelected = selectedTabIndex == tab.index
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTabIndex = tab.index },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = navSelectedColor,
                                selectedTextColor = navSelectedColor,
                                unselectedIconColor = navUnselectedColor,
                                unselectedTextColor = navUnselectedColor,
                                indicatorColor = GeminiPurple.copy(alpha = 0.22f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Screen Body
            when (selectedTabIndex) {
                0 -> AlarmScreen(viewModel = alarmViewModel)
                1 -> StopwatchScreen(viewModel = stopwatchViewModel)
                2 -> TimerScreen(viewModel = timerViewModel)
            }

            // In-App Ringing Banner floating at top
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            ) {
                InAppAlarmBanner(
                    onDismiss = { alarmViewModel.dismissAlarm(it) },
                    onSnooze = { alarmViewModel.snoozeAlarm(it) }
                )
            }
        }
    }
}
