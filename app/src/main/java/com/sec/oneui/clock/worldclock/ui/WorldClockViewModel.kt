package com.sec.oneui.clock.worldclock.ui

import androidx.lifecycle.ViewModel
import com.sec.oneui.clock.worldclock.data.CityModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.ZoneId

class WorldClockViewModel : ViewModel() {

    private val defaultCities = listOf(
        CityModel("America/New_York", "New York", "United States", "America/New_York"),
        CityModel("Europe/London", "London", "United Kingdom", "Europe/London"),
        CityModel("Europe/Paris", "Paris", "France", "Europe/Paris"),
        CityModel("Asia/Dubai", "Dubai", "United Arab Emirates", "Asia/Dubai"),
        CityModel("Africa/Cairo", "Cairo", "Egypt", "Africa/Cairo"),
        CityModel("Asia/Tokyo", "Tokyo", "Japan", "Asia/Tokyo"),
        CityModel("Australia/Sydney", "Sydney", "Australia", "Australia/Sydney")
    )

    private val _cities = MutableStateFlow(defaultCities)
    val cities: StateFlow<List<CityModel>> = _cities.asStateFlow()

    // Slider offset in hours from -12h to +12h (0 = live time)
    val sliderOffsetHours = MutableStateFlow(0f)

    // All available zones from Android java.time.ZoneId
    val allAvailableZones: List<CityModel> by lazy {
        ZoneId.getAvailableZoneIds()
            .filter { it.contains("/") && !it.startsWith("Etc/") }
            .sorted()
            .map { zoneId ->
                val parts = zoneId.split("/")
                val city = parts.last().replace("_", " ")
                val region = parts.first().replace("_", " ")
                CityModel(
                    id = zoneId,
                    cityName = city,
                    countryOrRegion = region,
                    timeZoneId = zoneId
                )
            }
    }

    fun addCity(city: CityModel) {
        if (_cities.value.none { it.id == city.id }) {
            _cities.value = _cities.value + city
        }
    }

    fun removeCity(city: CityModel) {
        _cities.value = _cities.value.filter { it.id != city.id }
    }

    fun setSliderOffset(offsetHours: Float) {
        sliderOffsetHours.value = offsetHours
    }

    fun resetSliderToLive() {
        sliderOffsetHours.value = 0f
    }
}
