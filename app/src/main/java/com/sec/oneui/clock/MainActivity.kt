package com.sec.oneui.clock

import android.Manifest
import android.app.AlarmManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import com.sec.oneui.clock.alarm.ui.AlarmEditSheet
import com.sec.oneui.clock.alarm.ui.AlarmScreen
import com.sec.oneui.clock.alarm.ui.AlarmViewModel
import com.sec.oneui.clock.settings.DiagFlags
import com.sec.oneui.clock.settings.GpuBenchMarks
import com.sec.oneui.clock.settings.GpuBenchOverlay
import com.sec.oneui.clock.settings.GpuBenchRunner
import com.sec.oneui.clock.settings.SettingsManager
import com.sec.oneui.clock.settings.SettingsScreen
import com.sec.oneui.clock.stopwatch.ui.StopwatchScreen
import com.sec.oneui.clock.stopwatch.ui.StopwatchViewModel
import com.sec.oneui.clock.settings.ThemeMode
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.sec.oneui.clock.theme.ClockMotion
import com.sec.oneui.clock.theme.ClockTheme
import com.sec.oneui.clock.theme.GeminiAnimatedBackground
import com.sec.oneui.clock.theme.GeminiBackgroundCanvas
import com.sec.oneui.clock.theme.LiquidGlassTabBar
import com.sec.oneui.clock.theme.LocalBackdrop
import com.sec.oneui.clock.theme.LocalHazeState
import com.sec.oneui.clock.theme.LocalLiquidGlassEnabled
import com.sec.oneui.clock.theme.OneUIClockTheme
import com.sec.oneui.clock.theme.PlatformBackdrop
import com.sec.oneui.clock.theme.drawInteractiveGlass
import com.sec.oneui.clock.theme.isLiquidGlassEnabled
import com.sec.oneui.clock.theme.isLiquidGlassSupported
import com.sec.oneui.clock.theme.iosPressEffect
import com.sec.oneui.clock.theme.layerBackdrop
import com.sec.oneui.clock.theme.liquidGlass
import com.sec.oneui.clock.theme.liquidGlassActivePill
import com.sec.oneui.clock.theme.rememberBackdrop
import com.sec.oneui.clock.timer.ui.TimerScreen
import com.sec.oneui.clock.timer.ui.TimerViewModel
import dev.chrisbanes.haze.HazeState
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
        CrashReporter.install(this) // TEMPORARY - remove after debugging
        super.onCreate(savedInstanceState)

        // TEMPORARY - remove after debugging
        val crashLog = CrashReporter.readCrash(this)
        if (crashLog != null) {
            showCrashScreen(crashLog)
            return
        }

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
        DiagFlags.init(this)

        // 3. Permissions
        checkAndRequestNotificationPermission()
        checkAndRequestExactAlarmPermission()

        setContent {
            val themeMode by settingsManager.themeMode.collectAsState()
            val animationEnabled by settingsManager.animationEnabled.collectAsState()
            val animationSpeed by settingsManager.animationSpeed.collectAsState()
            val liquidGlassEnabled by settingsManager.liquidGlassEnabled.collectAsState()
            val hazeState = remember { HazeState() }
            val isGlassActive = isLiquidGlassSupported && (DiagFlags.liquidGlassOverride ?: liquidGlassEnabled)
            isLiquidGlassEnabled = isGlassActive
            val bgBackdrop = if (isGlassActive) rememberBackdrop(Color.Transparent) else null
            val pagerBackdrop = if (isGlassActive) rememberBackdrop(Color.Transparent) else null
            val overlayBackdrop = if (isGlassActive && bgBackdrop != null && pagerBackdrop != null) {
                rememberCombinedBackdrop(bgBackdrop, pagerBackdrop)
            } else null

            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Ltr,
                LocalHazeState provides hazeState,
                LocalBackdrop provides bgBackdrop,
                LocalLiquidGlassEnabled provides isGlassActive
            ) {
                OneUIClockTheme(themeMode = themeMode) {
                    MainClockApp(
                        alarmViewModel = alarmViewModel,
                        stopwatchViewModel = stopwatchViewModel,
                        timerViewModel = timerViewModel,
                        settingsManager = settingsManager,
                        navTabFlow = navTabFlow,
                        themeMode = themeMode,
                        animationEnabled = animationEnabled,
                        animationSpeed = animationSpeed,
                        hazeState = hazeState,
                        bgBackdrop = bgBackdrop,
                        pagerBackdrop = pagerBackdrop,
                        overlayBackdrop = overlayBackdrop
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

    // TEMPORARY - remove after debugging
    private fun showCrashScreen(crashLog: String) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.BLACK)
            setPadding(32, 64, 32, 48)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val titleView = TextView(this).apply {
            text = "App Crash Detected"
            setTextColor(android.graphics.Color.RED)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 16)
        }
        root.addView(titleView)

        val buttonBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 16)
        }

        val copyButton = Button(this).apply {
            text = "Copy"
            setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Crash Log", crashLog)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this@MainActivity, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        }
        buttonBar.addView(copyButton)

        val spacer = android.view.View(this).apply {
            layoutParams = LinearLayout.LayoutParams(24, 1)
        }
        buttonBar.addView(spacer)

        val continueButton = Button(this).apply {
            text = "Continue"
            setOnClickListener {
                CrashReporter.clear(this@MainActivity)
                recreate()
            }
        }
        buttonBar.addView(continueButton)

        root.addView(buttonBar)

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            setBackgroundColor(android.graphics.Color.parseColor("#1E1E1E"))
            setPadding(16, 16, 16, 16)
        }

        val logView = TextView(this).apply {
            text = crashLog
            setTextColor(android.graphics.Color.WHITE)
            typeface = Typeface.MONOSPACE
            textSize = 11f
            setTextIsSelectable(true)
        }
        scrollView.addView(logView)

        root.addView(scrollView)

        setContentView(root)
    }
}

@Immutable
sealed class ClockTab(val index: Int, val title: String, val icon: ImageVector) {
    @Immutable
    data object Alarm : ClockTab(0, "Alarm", Icons.Default.Alarm)
    @Immutable
    data object Stopwatch : ClockTab(1, "Stopwatch", Icons.Default.Timer)
    @Immutable
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
    navTabFlow: kotlinx.coroutines.flow.StateFlow<Int?>,
    themeMode: ThemeMode = ThemeMode.MIDNIGHT_BLUE,
    animationEnabled: Boolean = true,
    animationSpeed: Float = 1.0f,
    hazeState: HazeState? = LocalHazeState.current,
    bgBackdrop: LayerBackdrop? = null,
    pagerBackdrop: LayerBackdrop? = null,
    overlayBackdrop: Backdrop? = null
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val editorState by alarmViewModel.editorState.collectAsState()
    val use24HourFormat by settingsManager.use24HourFormat.collectAsState()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { ClockTab.items.size })
    val coroutineScope = rememberCoroutineScope()
    val palette = ClockTheme.palette
    val activeHaze = hazeState ?: LocalHazeState.current ?: remember { HazeState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    val sheetProgress = remember { Animatable(0f) }
    var exitVelocity by remember { mutableFloatStateOf(0f) }
    var isSheetComposed by remember { mutableStateOf(false) }

    val gpuBenchRunner = remember(context, coroutineScope) {
        GpuBenchRunner(
            context = context,
            coroutineScope = coroutineScope,
            leaveSettingsToAlarmTab = {
                showSettings = false
                pagerState.scrollToPage(0)
            },
            openSheetLikeFab = {
                alarmViewModel.openEditorForNew(settingsManager.defaultVibrate.value)
            },
            closeSheetLikeCancel = {
                alarmViewModel.cancelDraft()
            },
            isSheetOpen = { alarmViewModel.editorState.value.isOpen },
            isSheetComposed = { isSheetComposed },
            isSheetProgressRunning = { sheetProgress.isRunning },
            sheetProgressValue = { sheetProgress.value },
            isGlassActive = { isLiquidGlassEnabled }
        )
    }

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
    Box(modifier = Modifier.fillMaxSize()) {
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
                Box(modifier = Modifier.fillMaxSize()) {
                    GeminiBackgroundCanvas(
                        themeMode = themeMode,
                        animationEnabled = animationEnabled,
                        animationSpeed = animationSpeed,
                        hazeState = activeHaze
                    )
                    SettingsScreen(
                        settingsManager = settingsManager,
                        onBack = { showSettings = false },
                        onRunGpuBenchmark = { gpuBenchRunner.start() }
                    )
                }
            } else {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                val instantFling = PagerDefaults.flingBehavior(
                    state = pagerState,
                    snapPositionalThreshold = 0.08f,
                    snapAnimationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessHigh
                    )
                )

                val isGlassActive = LocalLiquidGlassEnabled.current

                // 1. Sibling Backdrop Source Layer: Animated Background Canvas ONLY
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (isGlassActive && bgBackdrop != null) {
                                Modifier.layerBackdrop(bgBackdrop)
                            } else Modifier
                        )
                ) {
                    GeminiBackgroundCanvas(
                        themeMode = themeMode,
                        animationEnabled = animationEnabled,
                        animationSpeed = animationSpeed,
                        hazeState = activeHaze
                    )
                }

                // 2. Sibling Content Layer: Edge-to-Edge Content Pager
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (isGlassActive && pagerBackdrop != null) {
                                Modifier.layerBackdrop(pagerBackdrop)
                            } else Modifier
                        )
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = true,
                        beyondViewportPageCount = 1,
                        flingBehavior = instantFling
                    ) { pageIndex ->
                        when (pageIndex) {
                            0 -> AlarmScreen(viewModel = alarmViewModel)
                            1 -> StopwatchScreen(viewModel = stopwatchViewModel)
                            2 -> TimerScreen(viewModel = timerViewModel)
                        }
                    }
                }

                // 3. Settings Icon in Top-Right
                AnimatedVisibility(
                    visible = !editorState.isOpen,
                    enter = if (DiagFlags.snapTabBarFabGear) EnterTransition.None else fadeIn(tween(200)),
                    exit = if (DiagFlags.snapTabBarFabGear) ExitTransition.None else fadeOut(tween(200)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    DisposableEffect(Unit) {
                        onDispose {
                            if (GpuBenchMarks.active) GpuBenchMarks.mark("gear_disposed")
                        }
                    }
                    if (isGlassActive && overlayBackdrop != null) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .drawInteractiveGlass(
                                    isDark = true,
                                    backdrop = overlayBackdrop,
                                    layer = null,
                                    luminance = { 0.4f },
                                    shape = CircleShape,
                                    interaction = null
                                )
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { showSettings = true }
                                .iosPressEffect(targetScale = 0.95f),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { showSettings = true },
                            modifier = Modifier
                                .size(42.dp)
                                .liquidGlass(
                                    hazeState = hazeState,
                                    shape = CircleShape,
                                    cornerRadius = 21.dp,
                                    elevation = 8.dp
                                )
                                .iosPressEffect(targetScale = 0.95f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // 4. Floating Liquid Glass Action Button (Hoisted to MainActivity when glass is active)
                val defaultVibrate by settingsManager.defaultVibrate.collectAsState()
                AnimatedVisibility(
                    visible = isGlassActive && overlayBackdrop != null && pagerState.currentPage == 0 && !editorState.isOpen,
                    enter = if (DiagFlags.snapTabBarFabGear) EnterTransition.None else (fadeIn() + scaleIn()),
                    exit = if (DiagFlags.snapTabBarFabGear) ExitTransition.None else (fadeOut() + scaleOut()),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(end = 24.dp, bottom = 92.dp)
                ) {
                    DisposableEffect(Unit) {
                        onDispose {
                            if (GpuBenchMarks.active) GpuBenchMarks.mark("fab_disposed")
                        }
                    }
                    if (overlayBackdrop != null) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .drawInteractiveGlass(
                                    isDark = true,
                                    backdrop = overlayBackdrop,
                                    layer = null,
                                    luminance = { 0.4f },
                                    shape = CircleShape,
                                    interaction = null,
                                    scrimColor = palette.primaryAccent
                                )
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { alarmViewModel.openEditorForNew(defaultVibrate) }
                                .iosPressEffect(targetScale = 0.94f),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Alarm",
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                // 5. Floating Liquid Glass Navigation Bar (Overlaid on top of passing content)
                AnimatedVisibility(
                    visible = !editorState.isOpen,
                    enter = if (DiagFlags.snapTabBarFabGear) {
                        EnterTransition.None
                    } else {
                        slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f)
                        ) + fadeIn(tween(250))
                    },
                    exit = if (DiagFlags.snapTabBarFabGear) {
                        ExitTransition.None
                    } else {
                        slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f)
                        ) + fadeOut(tween(200))
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    DisposableEffect(Unit) {
                        onDispose {
                            if (GpuBenchMarks.active) GpuBenchMarks.mark("tabBar_disposed")
                        }
                    }
                    val activeColor = if (palette.isBlueTheme) Color(0xFF38BDF8) else Color(0xFFA855F7)

                    if (isGlassActive && overlayBackdrop != null) {
                        LiquidGlassTabBar(
                            tabCount = ClockTab.items.size,
                            selectedTab = pagerState.currentPage.coerceIn(0, ClockTab.items.size - 1),
                            backdrop = overlayBackdrop,
                            onTabSelected = { index ->
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        ) { position, isSelected ->
                            val tab = ClockTab.items[position]
                            val tabColor = if (isSelected) activeColor else Color.White.copy(alpha = 0.55f)
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = tabColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = tabColor,
                                    maxLines = 1
                                )
                            }
                        }
                    } else {
                        // Fallback: Exact current look completely unchanged for API < 33 or when disabled
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .liquidGlass(
                                    hazeState = activeHaze,
                                    shape = RoundedCornerShape(32.dp),
                                    cornerRadius = 32.dp,
                                    elevation = 16.dp
                                )
                                .padding(4.dp)
                        ) {
                            val tabCount = ClockTab.items.size
                            val tabWidth = maxWidth / tabCount
                            val selectedTab = pagerState.currentPage.coerceIn(0, tabCount - 1)
                            val pillOffset by animateDpAsState(
                                targetValue = tabWidth * selectedTab,
                                animationSpec = spring(dampingRatio = 0.72f, stiffness = 450f),
                                label = "activeTabPill"
                            )

                            // Current fallback active pill indicator
                            Box(
                                modifier = Modifier
                                    .offset(x = pillOffset)
                                    .width(tabWidth)
                                    .fillMaxHeight()
                                    .liquidGlassActivePill(
                                        hazeState = activeHaze,
                                        shape = RoundedCornerShape(28.dp),
                                        cornerRadius = 28.dp,
                                        glowColor = activeColor
                                    )
                            )

                            // Current fallback tab items row
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ClockTab.items.forEach { tab ->
                                    val isSelected = selectedTab == tab.index
                                    val tabColor = if (isSelected) activeColor else Color.White.copy(alpha = 0.55f)
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
                                            .iosPressEffect(targetScale = 0.94f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = tab.title,
                                                tint = tabColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = tab.title,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                color = tabColor,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. Edit Alarm Sheet (Hoisted above Pager and Tab Bar with unified iOS motion)
                var lastOpenEditorState by remember { mutableStateOf(editorState) }
                if (editorState.isOpen) {
                    lastOpenEditorState = editorState
                }

                if (editorState.isOpen) {
                    if (!isSheetComposed) {
                        isSheetComposed = true
                        if (GpuBenchMarks.active) GpuBenchMarks.mark("isSheetComposed=true")
                    }
                }

                LaunchedEffect(editorState.isOpen) {
                    if (editorState.isOpen) {
                        if (DiagFlags.frameTimingOverlay) {
                            DiagFlags.startTimingSession()
                        }
                        exitVelocity = 0f
                        repeat(3) {
                            withFrameNanos {}
                        }
                        sheetProgress.animateTo(1f, animationSpec = ClockMotion.Smooth)
                        if (DiagFlags.frameTimingOverlay) {
                            DiagFlags.stopTimingSession()
                        }
                    } else {
                        if (sheetProgress.value > 0.0001f) {
                            val v = exitVelocity
                            exitVelocity = 0f
                            sheetProgress.animateTo(0f, animationSpec = ClockMotion.Smooth, initialVelocity = v)
                        }
                        isSheetComposed = false
                    }
                }

                if (isSheetComposed) {
                    val displayState = if (editorState.isOpen) editorState else lastOpenEditorState
                    AlarmEditSheet(
                        editorState = displayState,
                        use24HourFormat = use24HourFormat,
                        sheetProgress = sheetProgress,
                        isSheetOpen = editorState.isOpen,
                        onDragDismiss = { velocity ->
                            exitVelocity = velocity
                            alarmViewModel.cancelDraft()
                        },
                        onDismiss = { alarmViewModel.dismissEditorSheet() },
                        onSave = { alarmViewModel.saveDraft() },
                        onCancel = { alarmViewModel.cancelDraft() },
                        onTimeChanged = { h, m -> alarmViewModel.updateEditorTime(h, m) },
                        onLabelChanged = { alarmViewModel.updateEditorLabel(it) },
                        onDayToggled = { alarmViewModel.toggleEditorDay(it) },
                        onSnoozeChanged = { alarmViewModel.updateEditorSnooze(it) },
                        onVibrateChanged = { alarmViewModel.updateEditorVibrate(it) },
                        onRingtonePicked = { uri, name -> alarmViewModel.updateEditorRingtone(uri, name) },
                        onScheduledDateChanged = { alarmViewModel.updateEditorScheduledDate(it) },
                        onVolumeChanged = { alarmViewModel.updateEditorVolume(it) },
                        sheetBackdrop = overlayBackdrop
                    )
                }
            }
        }
        }

        if (gpuBenchRunner.isRunning) {
            androidx.compose.runtime.key(isSheetComposed) {
                BackHandler(enabled = true) { gpuBenchRunner.cancel() }
            }
        }
        if (!gpuBenchRunner.isRunning && gpuBenchRunner.showResultsOverlay) {
            BackHandler(enabled = true) { gpuBenchRunner.dismissResults() }
        }

        GpuBenchOverlay(runner = gpuBenchRunner)
    }
}
