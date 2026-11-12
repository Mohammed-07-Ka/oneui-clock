package com.sec.oneui.clock.timer.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sec.oneui.clock.theme.GeminiGlassBackgroundDark
import com.sec.oneui.clock.theme.GeminiGlassBackgroundHover
import com.sec.oneui.clock.theme.GeminiGlassBorderGradientDark
import com.sec.oneui.clock.theme.GeminiLavender
import com.sec.oneui.clock.theme.GeminiPurple
import com.sec.oneui.clock.theme.GeminiPurpleLight
import com.sec.oneui.clock.theme.OneUiRed
import com.sec.oneui.clock.theme.OneUiTextPrimary

@Composable
fun TimerScreen(
    viewModel: TimerViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.timerState.collectAsState()
    val remainingMillis by viewModel.remainingMillis.collectAsState()
    val totalMillis by viewModel.totalMillis.collectAsState()

    val inputHours by viewModel.inputHours.collectAsState()
    val inputMinutes by viewModel.inputMinutes.collectAsState()
    val inputSeconds by viewModel.inputSeconds.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Timer",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = OneUiTextPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        when (state) {
            TimerState.IDLE -> {
                // Time Input Glass Dial
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(32.dp))
                                .background(GeminiGlassBackgroundDark)
                                .border(1.dp, GeminiGlassBorderGradientDark, RoundedCornerShape(32.dp))
                                .padding(horizontal = 26.dp, vertical = 22.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TimerDigitColumn("h", inputHours, onInc = { viewModel.inputHours.value = (inputHours + 1) % 100 }, onDec = { viewModel.inputHours.value = if (inputHours == 0) 99 else inputHours - 1 })
                            Text(":", fontSize = 38.sp, color = GeminiLavender, modifier = Modifier.padding(horizontal = 12.dp))
                            TimerDigitColumn("m", inputMinutes, onInc = { viewModel.inputMinutes.value = (inputMinutes + 1) % 60 }, onDec = { viewModel.inputMinutes.value = if (inputMinutes == 0) 59 else inputMinutes - 1 })
                            Text(":", fontSize = 38.sp, color = GeminiLavender, modifier = Modifier.padding(horizontal = 12.dp))
                            TimerDigitColumn("s", inputSeconds, onInc = { viewModel.inputSeconds.value = (inputSeconds + 1) % 60 }, onDec = { viewModel.inputSeconds.value = if (inputSeconds == 0) 59 else inputSeconds - 1 })
                        }

                        Spacer(modifier = Modifier.height(30.dp))

                        // Quick Presets
                        Text("Presets", color = GeminiLavender, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(5, 10, 15, 30).forEach { mins ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(GeminiGlassBackgroundDark)
                                        .border(1.dp, GeminiGlassBorderGradientDark, RoundedCornerShape(16.dp))
                                        .clickable { viewModel.applyPreset(mins) }
                                        .padding(horizontal = 18.dp, vertical = 10.dp)
                                ) {
                                    Text("${mins}m", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }

                // Start Button
                Button(
                    onClick = { viewModel.startTimer() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 96.dp)
                        .height(58.dp),
                    shape = RoundedCornerShape(29.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GeminiPurple)
                ) {
                    Text("Start", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            TimerState.RUNNING, TimerState.PAUSED -> {
                // Circular Progress countdown display with Gemini Purple Glow
                val progress = if (totalMillis > 0) (remainingMillis.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f) else 0f
                val h = (remainingMillis / 3600000L)
                val m = (remainingMillis % 3600000L) / 60000L
                val s = (remainingMillis % 60000L) / 1000L
                val formattedRemain = if (h > 0) {
                    String.format("%02d:%02d:%02d", h, m, s)
                } else {
                    String.format("%02d:%02d", m, s)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(270.dp)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.08f),
                                radius = size.minDimension / 2 - 12.dp.toPx(),
                                style = Stroke(width = 12.dp.toPx())
                            )
                            drawArc(
                                color = GeminiPurple,
                                startAngle = -90f,
                                sweepAngle = 360f * progress,
                                useCenter = false,
                                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = formattedRemain,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            // Active +1 min glass button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(GeminiGlassBackgroundDark)
                                    .border(1.dp, GeminiGlassBorderGradientDark, RoundedCornerShape(14.dp))
                                    .clickable { viewModel.addOneMinute() }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("+1 min", color = GeminiPurpleLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Action Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { viewModel.resetTimer() },
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp),
                        shape = RoundedCornerShape(29.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GeminiGlassBackgroundHover)
                    ) {
                        Text("Cancel", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = GeminiLavender)
                    }

                    if (state == TimerState.RUNNING) {
                        Button(
                            onClick = { viewModel.pauseTimer() },
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp),
                            shape = RoundedCornerShape(29.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GeminiPurple)
                        ) {
                            Text("Pause", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.resumeTimer() },
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp),
                            shape = RoundedCornerShape(29.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GeminiPurple)
                        ) {
                            Text("Resume", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            TimerState.FINISHED -> {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 0.95f,
                    targetValue = 1.08f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseScale"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.scale(pulseScale)
                    ) {
                        Text(
                            text = "00:00",
                            fontSize = 62.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = OneUiRed
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "✦ Time's up!",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Button(
                    onClick = { viewModel.dismissFinishedAlarm() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 96.dp)
                        .height(58.dp),
                    shape = RoundedCornerShape(29.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OneUiRed)
                ) {
                    Text("Dismiss", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun TimerDigitColumn(
    unit: String,
    value: Int,
    onInc: () -> Unit,
    onDec: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("▲", color = GeminiPurple, modifier = Modifier.clickable { onInc() }.padding(6.dp))
        Text(String.format("%02d", value), fontSize = 42.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(unit, fontSize = 12.sp, color = GeminiLavender, fontWeight = FontWeight.Medium)
        Text("▼", color = GeminiPurple, modifier = Modifier.clickable { onDec() }.padding(6.dp))
    }
}
