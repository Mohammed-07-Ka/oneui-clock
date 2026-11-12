package com.sec.oneui.clock.stopwatch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sec.oneui.clock.theme.GeminiCyanSparkle
import com.sec.oneui.clock.theme.GeminiGlassBackgroundDark
import com.sec.oneui.clock.theme.GeminiGlassBackgroundHover
import com.sec.oneui.clock.theme.GeminiGlassBorderGradientDark
import com.sec.oneui.clock.theme.GeminiLavender
import com.sec.oneui.clock.theme.GeminiPurple
import com.sec.oneui.clock.theme.OneUiGreen
import com.sec.oneui.clock.theme.OneUiRed
import com.sec.oneui.clock.theme.OneUiTextPrimary
import com.sec.oneui.clock.theme.OneUiTextSecondary

@Composable
fun StopwatchScreen(
    viewModel: StopwatchViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.stopwatchState.collectAsState()
    val elapsedMillis by viewModel.elapsedMillis.collectAsState()
    val laps by viewModel.laps.collectAsState()

    val formattedTime = LapItem.formatStopwatchTime(elapsedMillis)

    val fastestLapIndex = if (laps.size >= 2) laps.minByOrNull { it.lapTimeMillis }?.lapIndex else null
    val slowestLapIndex = if (laps.size >= 2) laps.maxByOrNull { it.lapTimeMillis }?.lapIndex else null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Stopwatch",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = OneUiTextPrimary
        )

        // Time Display Area with Glassmorphism
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.7f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(GeminiGlassBackgroundDark)
                    .border(1.dp, GeminiGlassBorderGradientDark, RoundedCornerShape(32.dp))
                    .padding(horizontal = 28.dp, vertical = 20.dp)
            ) {
                Text(
                    text = formattedTime,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White,
                    letterSpacing = (-1).sp
                )
            }
        }

        // Action Buttons Row with Gemini colors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (state) {
                StopwatchState.IDLE -> {
                    Button(
                        onClick = { viewModel.start() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(29.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GeminiPurple)
                    ) {
                        Text("Start", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                StopwatchState.RUNNING -> {
                    Button(
                        onClick = { viewModel.addLap() },
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp),
                        shape = RoundedCornerShape(29.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GeminiGlassBackgroundHover)
                    ) {
                        Text("Lap", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }

                    Button(
                        onClick = { viewModel.pause() },
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp),
                        shape = RoundedCornerShape(29.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OneUiRed)
                    ) {
                        Text("Stop", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                StopwatchState.PAUSED -> {
                    Button(
                        onClick = { viewModel.reset() },
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp),
                        shape = RoundedCornerShape(29.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GeminiGlassBackgroundHover)
                    ) {
                        Text("Reset", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = GeminiLavender)
                    }

                    Button(
                        onClick = { viewModel.resume() },
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

        // Lap Times Table Glass Container
        if (laps.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(GeminiGlassBackgroundDark)
                    .border(1.dp, GeminiGlassBorderGradientDark, RoundedCornerShape(24.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Lap", color = GeminiLavender, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Lap times", color = GeminiLavender, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("Overall time", color = GeminiLavender, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 60.dp)
                    ) {
                        items(laps, key = { it.lapIndex }) { lap ->
                            val lapColor = when (lap.lapIndex) {
                                fastestLapIndex -> OneUiGreen
                                slowestLapIndex -> OneUiRed
                                else -> Color.White
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = String.format("%02d", lap.lapIndex),
                                    color = GeminiLavender,
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
                                    color = GeminiLavender.copy(alpha = 0.8f),
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
