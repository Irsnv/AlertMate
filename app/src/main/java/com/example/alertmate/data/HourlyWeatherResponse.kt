package com.example.alertmate.data

data class HourlyWeatherResponse(
    val list: List<HourlyItem>
)

data class HourlyItem(
    val dt: Long, // timestamp
    val main: HourlyMain,
    val weather: List<HourlyWeather>
)

data class HourlyMain(
    val temp: Double // Temperature in Kelvin
)

data class HourlyWeather(
    val description: String,
    val icon: String // For icon code
)
