package com.example.alertmate.data

import com.google.gson.annotations.SerializedName

data class RemoteWeatherData(
    val lat: Double,
    val lon: Double,
    val timezone: String,
    @SerializedName("timezone_offset") val timezoneOffset: Long,
    val current: CurrentWeatherRemote,
    val hourly: List<ForecastHourRemote>,
    val daily: List<ForecastDayRemote>
)

data class CurrentWeatherRemote(
    val dt: Long,
    val temp: Float,
    @SerializedName("feels_like") val feelsLike: Float,
    val pressure: Int,
    val humidity: Int,
    @SerializedName("wind_speed") val windSpeed: Float,
    @SerializedName("wind_deg") val windDeg: Int,
    val weather: List<WeatherConditionRemote>
)

data class WeatherConditionRemote(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
)

data class ForecastHourRemote(
    val dt: Long,
    val temp: Float,
    @SerializedName("feels_like") val feelsLike: Float,
    val pressure: Int,
    val humidity: Int,
    @SerializedName("wind_speed") val windSpeed: Float,
    val weather: List<WeatherConditionRemote>,
    val pop: Float
)

data class ForecastDayRemote(
    val dt: Long,
    val temp: TempRemote,
    @SerializedName("feels_like") val feelsLike: FeelsLikeRemote,
    val pressure: Int,
    val humidity: Int,
    @SerializedName("wind_speed") val windSpeed: Float,
    val weather: List<WeatherConditionRemote>,
    val clouds: Int,
    val pop: Float,
    val rain: Float?
)

data class TempRemote(
    val day: Float,
    val min: Float,
    val max: Float,
    val night: Float,
    val eve: Float,
    val morn: Float
)

data class FeelsLikeRemote(
    val day: Float,
    val night: Float,
    val eve: Float,
    val morn: Float
)
