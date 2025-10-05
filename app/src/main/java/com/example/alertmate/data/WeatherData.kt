package com.example.alertmate.data

sealed class WeatherData

data class CurrentWeather(
    val icon: String,
    val temperature: Double,
    val windSpeed: Double,
    val humidity: Int,
    val description: String
) : WeatherData()

data class HourlyWeather(
    val time: Long,
    val temperature: Double,
    val icon: String
) : WeatherData()

data class DailyWeather(
    val date: Long,
    val minTemp: Double,
    val maxTemp: Double,
    val icon: String
) : WeatherData()
