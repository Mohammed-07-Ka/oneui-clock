package com.sec.oneui.clock.alarm.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sec.oneui.clock.alarm.data.AlarmEntity
import com.sec.oneui.clock.theme.ClockTheme
import com.sec.oneui.clock.theme.OneUiRed
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun AlarmRingingScreen(
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
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val dragOffsetAnimatable = remember { Animatable(0f) }
    var actionTriggered by remember { mutableStateOf(false) }

    // Infinite gentle pulse animation for directional indicators
    val infiniteTransition = rememberInfiniteTransition(label = "pulseIndicators")
    val pulseArrowOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrowFloat"
    )
    val ringGlowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringScale"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val screenHeightPx = constraints.maxHeight.toFloat()
        val thresholdPx = (screenHeightPx * 0.30f).coerceAtLeast(150f)
        val currentOffset = dragOffsetAnimatable.value

        val dismissProgress = (-currentOffset / thresholdPx).coerceIn(0f, 1f)
        val snoozeProgress = (currentOffset / thresholdPx).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(actionTriggered) {
                    if (actionTriggered) return@pointerInput

                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (actionTriggered) return@detectVerticalDragGestures
                            val curr = dragOffsetAnimatable.value
                            if (curr <= -thresholdPx) {
                                actionTriggered = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                coroutineScope.launch {
                                    dragOffsetAnimatable.animateTo(
                                        -screenHeightPx * 1.25f,
                                        tween(240, easing = FastOutSlowInEasing)
                                    )
                                    onDismiss()
                                }
                            } else if (curr >= thresholdPx) {
                                actionTriggered = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                coroutineScope.launch {
                                    dragOffsetAnimatable.animateTo(
                                        screenHeightPx * 1.25f,
                                        tween(240, easing = FastOutSlowInEasing)
                                    )
                                    onSnooze()
                                }
                            } else {
                                coroutineScope.launch {
                                    dragOffsetAnimatable.animateTo(
                                        0f,
                                        spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            if (!actionTriggered) {
                                coroutineScope.launch {
                                    dragOffsetAnimatable.animateTo(
                                        0f,
                                        spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            if (actionTriggered) return@detectVerticalDragGestures

                            val newOffset = dragOffsetAnimatable.value + dragAmount
                            if (newOffset <= -thresholdPx && !actionTriggered) {
                                actionTriggered = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                coroutineScope.launch {
                                    dragOffsetAnimatable.animateTo(
                                        -screenHeightPx * 1.25f,
                                        tween(240, easing = FastOutSlowInEasing)
                                    )
                                    onDismiss()
                                }
                            } else if (newOffset >= thresholdPx && !actionTriggered) {
                                actionTriggered = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                coroutineScope.launch {
                                    dragOffsetAnimatable.animateTo(
                                        screenHeightPx * 1.25f,
                                        tween(240, easing = FastOutSlowInEasing)
                                    )
                                    onSnooze()
                                }
                            } else if (!actionTriggered) {
                                coroutineScope.launch {
                                    dragOffsetAnimatable.snapTo(newOffset)
                                }
                            }
                        }
                    )
                }
        ) {
            // ── TOP ZONE: Swipe Up to Dismiss (MIUI/HyperOS Style) ──
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (dismissProgress > 0f) {
                                OneUiRed.copy(alpha = 0.2f + 0.5f * dismissProgress)
                            } else {
                                Color.White.copy(alpha = 0.1f)
                            }
                        )
                        .border(
                            1.dp,
                            if (dismissProgress > 0f) OneUiRed else Color.White.copy(alpha = 0.2f),
                            CircleShape
                        )
                        .offset(y = (-pulseArrowOffset).dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = if (dismissProgress > 0f) Color.White else palette.textPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.offset(y = (-pulseArrowOffset * 0.7f).dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = if (dismissProgress > 0f) OneUiRed else palette.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (dismissProgress > 0.6f) "Release to Stop" else "Swipe up to stop",
                        fontSize = 15.sp,
                        fontWeight = if (dismissProgress > 0f) FontWeight.Bold else FontWeight.Medium,
                        color = if (dismissProgress > 0f) Color.White else palette.textSecondary
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = if (dismissProgress > 0f) OneUiRed else palette.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // ── CENTER ZONE: Moving Time & HyperOS Ring ──
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = currentOffset
                        alpha = (1f - (abs(currentOffset) / (screenHeightPx * 0.8f))).coerceIn(0.2f, 1f)
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    // Central Glowing HyperOS Gesture Ring
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(140.dp)
                            .scale(if (abs(currentOffset) < 10f) ringGlowScale else 1f)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        palette.primaryAccent.copy(alpha = 0.28f),
                                        palette.cardBackground.copy(alpha = 0.85f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(
                                2.dp,
                                if (dismissProgress > 0f) OneUiRed.copy(alpha = 0.8f)
                                else if (snoozeProgress > 0f) Color(0xFF60A5FA)
                                else palette.cardBorder,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = "Alarm Ringing",
                            tint = if (dismissProgress > 0f) OneUiRed
                            else if (snoozeProgress > 0f) Color(0xFF60A5FA)
                            else palette.textPrimary,
                            modifier = Modifier.size(56.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Time Display
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = alarm?.formattedTime() ?: currentTime,
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = " " + (alarm?.amPm() ?: currentAmPm),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.textSecondary,
                            modifier = Modifier.padding(bottom = 14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Alarm Label
                    Text(
                        text = alarm?.label?.ifBlank { "Alarm" } ?: "Alarm",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textSecondary,
                        textAlign = TextAlign.Center
                    )

                    if (alarm?.ringtoneName != null && alarm.ringtoneName != "Default") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "🔔 ${alarm.ringtoneName}",
                            fontSize = 15.sp,
                            color = palette.textSecondary.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // ── BOTTOM ZONE: Swipe Down to Snooze (MIUI/HyperOS Style) ──
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.offset(y = (pulseArrowOffset * 0.7f).dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = if (snoozeProgress > 0f) Color(0xFF60A5FA) else palette.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (snoozeProgress > 0.6f) "Release to Snooze" else "Swipe down to snooze (${alarm?.snoozeDurationMinutes ?: 5}m)",
                        fontSize = 15.sp,
                        fontWeight = if (snoozeProgress > 0f) FontWeight.Bold else FontWeight.Medium,
                        color = if (snoozeProgress > 0f) Color(0xFF93C5FD) else palette.textSecondary
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = if (snoozeProgress > 0f) Color(0xFF60A5FA) else palette.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (snoozeProgress > 0f) {
                                Color(0xFF3B82F6).copy(alpha = 0.25f + 0.45f * snoozeProgress)
                            } else {
                                Color.White.copy(alpha = 0.1f)
                            }
                        )
                        .border(
                            1.dp,
                            if (snoozeProgress > 0f) Color(0xFF60A5FA) else Color.White.copy(alpha = 0.2f),
                            CircleShape
                        )
                        .offset(y = pulseArrowOffset.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Snooze",
                        tint = if (snoozeProgress > 0f) Color.White else palette.textPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
