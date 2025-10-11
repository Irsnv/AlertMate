package com.example.alertmate.data

data class OneCallResponse(
    val lat: Double?,
    val lon: Double?,
    val timezone: String?,
    val current: Current?,
    val minutely: List<MinutelyItem>?,
    val hourly: List<HourlyItem>?,
    val daily: List<DailyItem>?,
    val alerts: List<AlertItem>?
)
data class Current(
    val dt: Long?,
    val sunrise: Long?,
    val sunset: Long?,
    val temp: Double?,
    val feels_like: Double?,
    val pressure: Int?,
    val humidity: Int?,
    val dew_point: Double?,
    val uvi: Double?,
    val clouds: Int?,
    val visibility: Int?,
    val wind_speed: Double?,
    val wind_deg: Int?,
    val weather: List<Weather>?
)
data class MinutelyItem(
    val dt: Long?,
    val precipitation: Double?)
data class HourlyItem(
    val dt: Long?,
    val temp: Double?,
    val pop: Double?,
    val weather: List<Weather>?)
data class DailyItem(
    val dt: Long?,
    val temp: Temp?,
    val pop: Double?,
    val weather: List<Weather>?)
data class Temp(
    val day: Double?,
    val min: Double?,
    val max: Double?)
data class Weather(
    val id: Int?, val main: String?,
    val description: String?,
    val icon: String?)
data class AlertItem(
    val sender_name: String?,
    val event: String?,
    val start: Long?,
    val end: Long?,
    val description: String?)

