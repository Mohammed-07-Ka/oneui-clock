package com.sec.oneui.clock.stopwatch.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sec.oneui.clock.theme.ClockMotion
import com.sec.oneui.clock.theme.ClockTheme
import com.sec.oneui.clock.theme.LocalBackdrop
import com.sec.oneui.clock.theme.LocalLiquidGlassEnabled
import com.sec.oneui.clock.theme.drawInteractiveGlass
import com.sec.oneui.clock.theme.LiquidActiveText
import com.sec.oneui.clock.theme.LiquidGlassActive
import com.sec.oneui.clock.theme.LiquidGlassBackground
import com.sec.oneui.clock.theme.LiquidSpecularBorder
import com.sec.oneui.clock.theme.LiquidSpecularBorderSubtle
import com.sec.oneui.clock.theme.LiquidTextMuted
import com.sec.oneui.clock.theme.LiquidTextPrimary
import com.sec.oneui.clock.theme.LiquidTextSecondary
import com.sec.oneui.clock.theme.OneUiGreen
import com.sec.oneui.clock.theme.OneUiRed
import com.sec.oneui.clock.theme.iosPressEffect
import com.sec.oneui.clock.theme.liquidGlass

private const val STOP_GLASS_TINT_ALPHA = 0.5f

@Composable
fun StopwatchScreen(
    viewModel: StopwatchViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.stopwatchState.collectAsState()
    val elapsedMillis by viewModel.elapsedMillis.collectAsState()
    val laps by viewModel.laps.collectAsState()
    val palette = ClockTheme.palette
    val isGlassActive = LocalLiquidGlassEnabled.current && LocalBackdrop.current != null
    val backdrop = LocalBackdrop.current

    val formattedTime = LapItem.formatStopwatchTime(elapsedMillis)

    val fastestLapIndex = if (laps.size >= 2) laps.minByOrNull { it.lapTimeMillis }?.lapIndex else null
    val slowestLapIndex = if (laps.size >= 2) laps.maxByOrNull { it.lapTimeMillis }?.lapIndex else null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Stopwatch",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary
        )

        // Time Display Area with Glassmorphism
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.7f),
            contentAlignment = Alignment.Center
        ) {
            val displayCardShape = RoundedCornerShape(32.dp)
            Box(
                modifier = Modifier
                    .then(
                        if (isGlassActive && backdrop != null) {
                            Modifier
                                .drawInteractiveGlass(
                                    isDark = true,
                                    backdrop = backdrop,
                                    layer = null,
                                    luminance = { 0.4f },
                                    shape = displayCardShape,
                                    interaction = null
                                )
                                .clip(displayCardShape)
                        } else {
                            Modifier.liquidGlass(
                                shape = displayCardShape,
                                cornerRadius = 32.dp
                            )
                        }
                    )
                    .padding(horizontal = 28.dp, vertical = 20.dp)
            ) {
                Text(
                    text = formattedTime,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = palette.textPrimary,
                    letterSpacing = (-1).sp
                )
            }
        }

        // Action Buttons Row with iOS Liquid Glass Styling and Tactile Press
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                scaleIn(
                    initialScale = 0.96f,
                    animationSpec = ClockMotion.smooth()
                ).togetherWith(ExitTransition.None)
            },
            label = "stopwatchButtonsTransition",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) { targetState ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (targetState) {
                StopwatchState.IDLE -> {
                    if (isGlassActive && backdrop != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .drawInteractiveGlass(
                                    isDark = true,
                                    backdrop = backdrop,
                                    layer = null,
                                    luminance = { 0.4f },
                                    shape = RoundedCornerShape(29.dp),
                                    interaction = null,
                                    scrimColor = palette.primaryAccent
                                )
                                .clip(RoundedCornerShape(29.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { viewModel.start() }
                                .iosPressEffect(targetScale = 0.96f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Start", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.start() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .iosPressEffect(targetScale = 0.96f),
                            shape = RoundedCornerShape(29.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primaryAccent,
                                contentColor = palette.onPrimaryAccent
                            )
                        ) {
                            Text("Start", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                StopwatchState.RUNNING -> {
                    Button(
                        onClick = { viewModel.addLap() },
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .iosPressEffect(targetScale = 0.96f),
                        shape = RoundedCornerShape(29.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.cardBackground,
                            contentColor = palette.textPrimary
                        )
                    ) {
                        Text("Lap", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    }

                    if (isGlassActive && backdrop != null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp)
                                .drawInteractiveGlass(
                                    isDark = true,
                                    backdrop = backdrop,
                                    layer = null,
                                    luminance = { 0.4f },
                                    shape = RoundedCornerShape(29.dp),
                                    interaction = null,
                                    minScrim = STOP_GLASS_TINT_ALPHA,
                                    maxScrim = STOP_GLASS_TINT_ALPHA,
                                    scrimColor = OneUiRed
                                )
                                .clip(RoundedCornerShape(29.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { viewModel.pause() }
                                .iosPressEffect(targetScale = 0.96f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Stop", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.pause() },
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp)
                                .iosPressEffect(targetScale = 0.96f),
                            shape = RoundedCornerShape(29.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OneUiRed,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Stop", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                StopwatchState.PAUSED -> {
                    if (isGlassActive && backdrop != null) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp)
                                .drawInteractiveGlass(
                                    isDark = true,
                                    backdrop = backdrop,
                                    layer = null,
                                    luminance = { 0.4f },
                                    shape = RoundedCornerShape(29.dp),
                                    interaction = null
                                )
                                .clip(RoundedCornerShape(29.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { viewModel.reset() }
                                .iosPressEffect(targetScale = 0.96f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Reset", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp)
                                .drawInteractiveGlass(
                                    isDark = true,
                                    backdrop = backdrop,
                                    layer = null,
                                    luminance = { 0.4f },
                                    shape = RoundedCornerShape(29.dp),
                                    interaction = null,
                                    scrimColor = palette.primaryAccent
                                )
                                .clip(RoundedCornerShape(29.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { viewModel.resume() }
                                .iosPressEffect(targetScale = 0.96f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Resume", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.reset() },
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp)
                                .iosPressEffect(targetScale = 0.96f),
                            shape = RoundedCornerShape(29.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.cardBackground,
                                contentColor = palette.textPrimary
                            )
                        ) {
                            Text("Reset", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { viewModel.resume() },
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp)
                                .iosPressEffect(targetScale = 0.96f),
                            shape = RoundedCornerShape(29.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primaryAccent,
                                contentColor = palette.onPrimaryAccent
                            )
                        ) {
                            Text("Resume", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        }

        // Lap Times Table Glass Container
        if (laps.isNotEmpty()) {
            AnimatedVisibility(
                visible = laps.isNotEmpty(),
                enter = scaleIn(
                    initialScale = 0.96f,
                    animationSpec = ClockMotion.smooth()
                ),
                exit = ExitTransition.None,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val lapCardShape = RoundedCornerShape(24.dp)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                        if (isGlassActive && backdrop != null) {
                            Modifier
                                .drawInteractiveGlass(
                                    isDark = true,
                                    backdrop = backdrop,
                                    layer = null,
                                    luminance = { 0.4f },
                                    shape = lapCardShape,
                                    interaction = null
                                )
                                .clip(lapCardShape)
                        } else {
                            Modifier.liquidGlass(
                                shape = lapCardShape,
                                cornerRadius = 24.dp,
                                elevation = 8.dp
                            )
                        }
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Lap", color = LiquidTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Lap times", color = LiquidTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Overall time", color = LiquidTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 60.dp)
                    ) {
                        items(laps, key = { it.lapIndex }) { lap ->
                            val lapColor = when (lap.lapIndex) {
                                fastestLapIndex -> OneUiGreen
                                slowestLapIndex -> OneUiRed
                                else -> LiquidTextPrimary
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItem(
                                        placementSpec = ClockMotion.snappy(),
                                        fadeInSpec = ClockMotion.snappy(),
                                        fadeOutSpec = ClockMotion.snappy()
                                    )
                                    .padding(horizontal = 8.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = String.format("%02d", lap.lapIndex),
                                    color = LiquidTextSecondary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "+${lap.formatLapTime()}",
                                    color = lapColor,
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = lap.formatTotalTime(),
                                    color = LiquidTextSecondary.copy(alpha = 0.8f),
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                        }
                    }
                }
            }
            }
            Spacer(modifier = Modifier.height(112.dp))
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
