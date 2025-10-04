package com.example.alertmate.home

data class WeatherResponse(
    val current: Current,
    val hourly: List<Hourly>,
    val daily: List<Daily>
)

data class Current(
    val dt: Long,
    val temp: Double,
    val humidity: Int,
    val wind_speed: Double,
    val weather: List<Weather>
)

data class Hourly(
    val dt: Long,
    val temp: Double,
    val weather: List<Weather>
)

data class Daily(
    val dt: Long,
    val temp: Temp,
    val weather: List<Weather>
)

data class Temp(
    val min: Double,
    val max: Double
)

data class Weather(
    val main: String,
    val description: String,
    val icon: String
)

