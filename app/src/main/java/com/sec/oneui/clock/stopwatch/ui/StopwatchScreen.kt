package com.sec.oneui.clock.stopwatch.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sec.oneui.clock.theme.OneUiBlack
import com.sec.oneui.clock.theme.OneUiBlue
import com.sec.oneui.clock.theme.OneUiCard
import com.sec.oneui.clock.theme.OneUiCardHover
import com.sec.oneui.clock.theme.OneUiDivider
import com.sec.oneui.clock.theme.OneUiGreen
import com.sec.oneui.clock.theme.OneUiOrange
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
            .background(OneUiBlack)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Stopwatch",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = OneUiTextPrimary
        )

        // Time Display Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.7f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = formattedTime,
                fontSize = 58.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                color = OneUiTextPrimary,
                letterSpacing = (-1).sp
            )
        }

        // Action Buttons Row
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
                        colors = ButtonDefaults.buttonColors(containerColor = OneUiOrange)
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
                        colors = ButtonDefaults.buttonColors(containerColor = OneUiCardHover)
                    ) {
                        Text("Lap", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = OneUiTextPrimary)
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
                        colors = ButtonDefaults.buttonColors(containerColor = OneUiCardHover)
                    ) {
                        Text("Reset", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = OneUiTextPrimary)
                    }

                    Button(
                        onClick = { viewModel.resume() },
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp),
                        shape = RoundedCornerShape(29.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OneUiOrange)
                    ) {
                        Text("Resume", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Lap Times Table
        if (laps.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Lap", color = OneUiTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Lap times", color = OneUiTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Overall time", color = OneUiTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            HorizontalDivider(color = OneUiDivider)

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(laps, key = { it.lapIndex }) { lap ->
                    val lapColor = when (lap.lapIndex) {
                        fastestLapIndex -> OneUiGreen
                        slowestLapIndex -> OneUiRed
                        else -> OneUiTextPrimary
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format("%02d", lap.lapIndex),
                            color = OneUiTextSecondary,
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
                            color = OneUiTextSecondary,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    HorizontalDivider(color = OneUiDivider)
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
