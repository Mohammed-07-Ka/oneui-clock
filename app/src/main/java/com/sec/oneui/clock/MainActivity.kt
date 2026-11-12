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
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import kotlin.math.absoluteValue
import com.sec.oneui.clock.alarm.ui.AlarmScreen
import com.sec.oneui.clock.alarm.ui.AlarmViewModel
import com.sec.oneui.clock.settings.SettingsManager
import com.sec.oneui.clock.settings.SettingsScreen
import com.sec.oneui.clock.stopwatch.ui.StopwatchScreen
import com.sec.oneui.clock.stopwatch.ui.StopwatchViewModel
import com.sec.oneui.clock.theme.ClockTheme
import com.sec.oneui.clock.theme.GeminiAnimatedBackground
import com.sec.oneui.clock.theme.OneUIClockTheme
import com.sec.oneui.clock.theme.iosPressEffect
import com.sec.oneui.clock.timer.ui.TimerScreen
import com.sec.oneui.clock.timer.ui.TimerViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val alarmViewModel: AlarmViewModel by viewModels()
    private val stopwatchViewModel: StopwatchViewModel by viewModels()
    private val timerViewModel: TimerViewModel by viewModels()

    private val navTabFlow = kotlinx.coroutines.flow.MutableStateFlow<Int?>(null)

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Notification permission result handled
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNavIntent(intent)
    }

    private fun handleNavIntent(intent: Intent?) {
        val tab = intent?.getIntExtra("nav_tab", -1) ?: -1
        if (tab in 0..2) {
            navTabFlow.value = tab
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleNavIntent(intent)

        // 1. Full Edge-to-Edge with transparent system bars & light icons
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        // 2. Disable system contrast enforcement to remove navy / dark scrims
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

        // 3. Permissions
        checkAndRequestNotificationPermission()
        checkAndRequestExactAlarmPermission()

        setContent {
            val themeMode by settingsManager.themeMode.collectAsState()
            val animationEnabled by settingsManager.animationEnabled.collectAsState()
            val animationSpeed by settingsManager.animationSpeed.collectAsState()

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
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
                            navTabFlow = navTabFlow
                        )
                    }
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MainClockApp(
    alarmViewModel: AlarmViewModel,
    stopwatchViewModel: StopwatchViewModel,
    timerViewModel: TimerViewModel,
    settingsManager: SettingsManager,
    navTabFlow: kotlinx.coroutines.flow.StateFlow<Int?>
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val editorState by alarmViewModel.editorState.collectAsState()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { ClockTab.items.size })
    val coroutineScope = rememberCoroutineScope()
    val palette = ClockTheme.palette

    // Handle navigation tab changes (e.g. from notification clicks)
    val targetTab by navTabFlow.collectAsState()
    androidx.compose.runtime.LaunchedEffect(targetTab) {
        targetTab?.let { page ->
            if (page in 0 until ClockTab.items.size) {
                pagerState.scrollToPage(page)
            }
        }
    }

    // System Back Button Navigation
    BackHandler(enabled = showSettings) {
        showSettings = false
    }

    BackHandler(enabled = editorState.isOpen) {
        alarmViewModel.dismissEditorSheet()
    }

    // Smooth iOS-style vertical slide transition for Settings screen (320ms, easeOut)
    AnimatedContent(
        targetState = showSettings,
        transitionSpec = {
            if (targetState) {
                (slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(320, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(320, easing = FastOutSlowInEasing))).togetherWith(
                    slideOutVertically(
                        targetOffsetY = { -it / 4 },
                        animationSpec = tween(320, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(320, easing = FastOutSlowInEasing))
                )
            } else {
                (slideInVertically(
                    initialOffsetY = { -it / 4 },
                    animationSpec = tween(320, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(320, easing = FastOutSlowInEasing))).togetherWith(
                    slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(320, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(320, easing = FastOutSlowInEasing))
                )
            }
        },
        label = "settingsTransition"
    ) { isSettingsVisible ->
        if (isSettingsVisible) {
            SettingsScreen(
                settingsManager = settingsManager,
                onBack = { showSettings = false }
            )
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    // iOS Liquid Glass Floating Bottom Navigation Bar with Spring Pill Morph
                    AnimatedVisibility(
                        visible = !editorState.isOpen,
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f)
                        ) + fadeIn(tween(250)),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f)
                        ) + fadeOut(tween(200))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(palette.cardBackground)
                                    .border(1.dp, palette.cardBorder, RoundedCornerShape(32.dp))
                                    .padding(4.dp)
                            ) {
                                val tabCount = ClockTab.items.size
                                val tabWidth = maxWidth / tabCount
                                val selectedTab = pagerState.currentPage.coerceIn(0, tabCount - 1)
                                val pillOffset by animateDpAsState(
                                    targetValue = tabWidth * selectedTab,
                                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
                                    label = "activeTabPill"
                                )

                                // Physical sliding and stretching active pill indicator
                                Box(
                                    modifier = Modifier
                                        .offset(x = pillOffset)
                                        .width(tabWidth)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(28.dp))
                                        .background(palette.tabSelectedIndicator)
                                )

                                // Tab items row
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ClockTab.items.forEach { tab ->
                                        val isSelected = selectedTab == tab.index
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(28.dp))
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    coroutineScope.launch {
                                                        pagerState.animateScrollToPage(
                                                            page = tab.index,
                                                            animationSpec = spring(
                                                                dampingRatio = 0.75f,
                                                                stiffness = 500f
                                                            )
                                                        )
                                                    }
                                                }
                                                .iosPressEffect(targetScale = 0.96f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = tab.icon,
                                                    contentDescription = tab.title,
                                                    tint = if (isSelected) {
                                                        if (palette.isBlueTheme) Color(0xFF60A5FA) else palette.textPrimary
                                                    } else palette.textMuted,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = tab.title,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) {
                                                        if (palette.isBlueTheme) Color(0xFF60A5FA) else palette.textPrimary
                                                    } else palette.textMuted
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding())
                ) {
                    val instantFling = PagerDefaults.flingBehavior(
                        state = pagerState,
                        snapPositionalThreshold = 0.08f,
                        snapAnimationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessHigh
                        )
                    )

                    // Horizontal Pager with Left/Right Swiping between Alarm, Stopwatch, and Timer with instant zero-lag physics
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = true,
                        beyondBoundsPageCount = 2,
                        flingBehavior = instantFling
                    ) { pageIndex ->
                        when (pageIndex) {
                            0 -> AlarmScreen(viewModel = alarmViewModel)
                            1 -> StopwatchScreen(viewModel = stopwatchViewModel)
                            2 -> TimerScreen(viewModel = timerViewModel)
                        }
                    }

                    // Discreet Settings Icon in Top-Right (hidden when Add/Edit Alarm sheet is open)
                    AnimatedVisibility(
                        visible = !editorState.isOpen,
                        enter = fadeIn(tween(200)),
                        exit = fadeOut(tween(200)),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        IconButton(
                            onClick = { showSettings = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(palette.cardBackground)
                                .border(1.dp, palette.cardBorderSubtle, CircleShape)
                                .iosPressEffect(targetScale = 0.96f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = palette.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
