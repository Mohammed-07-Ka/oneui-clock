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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Minimalist Xiaomi / HyperOS Alarm Ringing Screen:
 * - Top: Date & Day in soft gray (e.g. Nov 12 Thursday PM) + Large clean white time (e.g. 07:50)
 * - Center: Glassmorphic pill button for instant Snooze (Snooze for 5 mins)
 * - Bottom: Chevron arrow (^) with "Swipe up to stop" supporting 1-finger vertical swipe to dismiss
 */
@Composable
fun AlarmRingingScreen(
    alarm: AlarmEntity?,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    val currentDateTimeHeader = remember {
        SimpleDateFormat("MMM d EEEE a", Locale.ENGLISH).format(Date())
    }
    val currentTime = remember {
        SimpleDateFormat("hh:mm", Locale.getDefault()).format(Date())
    }
    val displayTime = alarm?.formattedTime() ?: currentTime

    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val dragOffsetAnimatable = remember { Animatable(0f) }
    var actionTriggered by remember { mutableStateOf(false) }

    // Subtle upward pulse animation for the chevron indicator
    val infiniteTransition = rememberInfiniteTransition(label = "chevronPulse")
    val arrowOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrowY"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050409))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val screenHeightPx = constraints.maxHeight.toFloat()
        val thresholdPx = (screenHeightPx * 0.22f).coerceAtLeast(140f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(actionTriggered) {
                    if (actionTriggered) return@pointerInput

                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (actionTriggered) return@detectVerticalDragGestures
                            val current = dragOffsetAnimatable.value
                            if (current <= -thresholdPx) {
                                actionTriggered = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                coroutineScope.launch {
                                    dragOffsetAnimatable.animateTo(
                                        -screenHeightPx * 1.15f,
                                        tween(200, easing = FastOutSlowInEasing)
                                    )
                                    onDismiss()
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
                                        -screenHeightPx * 1.15f,
                                        tween(200, easing = FastOutSlowInEasing)
                                    )
                                    onDismiss()
                                }
                            } else if (!actionTriggered) {
                                coroutineScope.launch {
                                    // Allow upward drag, with rubber-band resistance on downward drag
                                    val clamped = if (newOffset > 0f) newOffset * 0.15f else newOffset
                                    dragOffsetAnimatable.snapTo(clamped)
                                }
                            }
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .graphicsLayer {
                        val currentY = dragOffsetAnimatable.value.coerceAtMost(0f)
                        translationY = currentY
                        alpha = (1f - (abs(currentY) / (screenHeightPx * 0.8f))).coerceIn(0.1f, 1f)
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(48.dp))

                // ── TOP: Date & Day in soft gray + Large clean white time ──
                Text(
                    text = currentDateTimeHeader,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF9E9E9E),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = displayTime,
                    fontSize = 86.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp
                )

                if (!alarm?.label.isNullOrBlank() && alarm?.label != "Alarm") {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = alarm!!.label,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFFB0B0B0),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // ── CENTER: Glassmorphic Pill Button for instant Snooze ──
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(32.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!actionTriggered) {
                                actionTriggered = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSnooze()
                            }
                        }
                        .padding(horizontal = 28.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = "Snooze",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Snooze for ${alarm?.snoozeDurationMinutes ?: 5} mins",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // ── BOTTOM: Chevron (^) with "Swipe up to stop" ──
                Column(
                    modifier = Modifier.padding(bottom = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Swipe up to stop",
                        tint = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier
                            .size(26.dp)
                            .offset(y = arrowOffsetY.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Swipe up to stop",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.55f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
