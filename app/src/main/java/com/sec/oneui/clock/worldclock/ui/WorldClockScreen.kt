package com.sec.oneui.clock.worldclock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sec.oneui.clock.theme.OneUiBlack
import com.sec.oneui.clock.theme.OneUiBlue
import com.sec.oneui.clock.theme.OneUiCard
import com.sec.oneui.clock.theme.OneUiCardHover
import com.sec.oneui.clock.theme.OneUiDivider
import com.sec.oneui.clock.theme.OneUiTextPrimary
import com.sec.oneui.clock.theme.OneUiTextSecondary
import com.sec.oneui.clock.worldclock.data.CityModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldClockScreen(
    viewModel: WorldClockViewModel,
    modifier: Modifier = Modifier
) {
    val cities by viewModel.cities.collectAsState()
    val sliderOffset by viewModel.sliderOffsetHours.collectAsState()

    var showSearchSheet by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OneUiBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "World Clock",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = OneUiTextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Time Zone Converter Slider Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = OneUiCard)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Time Converter",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OneUiTextPrimary
                        )

                        // Reset button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sliderOffset == 0f) OneUiCardHover else OneUiBlue)
                                .clickable { viewModel.resetSliderToLive() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (sliderOffset == 0f) "Live Time" else "Reset (${if (sliderOffset > 0) "+%.1f".format(sliderOffset) else "%.1f".format(sliderOffset)}h)",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = sliderOffset,
                        onValueChange = { viewModel.setSliderOffset(it) },
                        valueRange = -12f..12f,
                        steps = 47, // 30-minute intervals
                        colors = SliderDefaults.colors(
                            thumbColor = OneUiBlue,
                            activeTrackColor = OneUiBlue,
                            inactiveTrackColor = OneUiCardHover
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // City Cards List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(cities, key = { it.id }) { city ->
                    CityCard(
                        city = city,
                        sliderOffset = sliderOffset,
                        onDelete = { viewModel.removeCity(city) }
                    )
                }
            }
        }

        // Add City FAB
        FloatingActionButton(
            onClick = { showSearchSheet = true },
            containerColor = OneUiBlue,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 24.dp, bottom = 24.dp)
                .size(62.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add City", modifier = Modifier.size(32.dp))
        }

        if (showSearchSheet) {
            CitySearchSheet(
                allZones = viewModel.allAvailableZones,
                onDismiss = { showSearchSheet = false },
                onSelectCity = { selectedCity ->
                    viewModel.addCity(selectedCity)
                    showSearchSheet = false
                }
            )
        }
    }
}

@Composable
fun CityCard(
    city: CityModel,
    sliderOffset: Float,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = OneUiCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = city.cityName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OneUiTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = city.countryOrRegion,
                    fontSize = 13.sp,
                    color = OneUiTextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = city.calculateTimeDifference(sliderOffset),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = OneUiBlue
                )
            }

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = city.calculateDisplayTime(sliderOffset),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = OneUiTextPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = city.calculateAmPm(sliderOffset),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OneUiTextSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete City",
                    tint = OneUiTextSecondary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitySearchSheet(
    allZones: List<CityModel>,
    onDismiss: () -> Unit,
    onSelectCity: (CityModel) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filteredZones = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            allZones.take(50)
        } else {
            allZones.filter {
                it.cityName.contains(searchQuery, ignoreCase = true) ||
                        it.countryOrRegion.contains(searchQuery, ignoreCase = true)
            }.take(50)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OneUiCard,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select City",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = OneUiTextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = OneUiTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search city or region...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OneUiBlue,
                    unfocusedBorderColor = OneUiCardHover,
                    focusedTextColor = OneUiTextPrimary,
                    unfocusedTextColor = OneUiTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredZones, key = { it.id }) { city ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(OneUiBlack)
                            .clickable { onSelectCity(city) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(city.cityName, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = OneUiTextPrimary)
                            Text(city.countryOrRegion, fontSize = 13.sp, color = OneUiTextSecondary)
                        }
                        Text(
                            city.calculateDisplayTime(0f) + " " + city.calculateAmPm(0f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = OneUiBlue
                        )
                    }
                }
            }
        }
    }
}
