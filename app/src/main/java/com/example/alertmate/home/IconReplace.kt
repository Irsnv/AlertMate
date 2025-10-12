package com.example.alertmate.home

import com.example.alertmate.R
import java.util.*

object IconReplace {

    /**
     * Map temperature range (°C) to local weather label and icon.
     * Used for Malaysian context — no snow.
     */
    fun mapTempToLabelAndDrawable(temp: Double?): Pair<String, Int> {
        if (temp == null) return "Unknown" to R.drawable.unknown

        return when {
            temp >= 30 -> "Sunny" to R.drawable.sunny
            temp in 25.0..29.9 -> "Cloudy" to R.drawable.cloudy
            temp in 24.0..24.9 -> "Light Rain" to R.drawable.rain
            temp <= 23 -> "Thunderstorm" to R.drawable.thunderstorm
            else -> "Unknown" to R.drawable.unknown
        }
    }

    /**
     * Map icon code to drawable (backup when temperature not available).
     */
    fun mapIconCodeToDrawable(iconCode: String?): Int {
        if (iconCode.isNullOrBlank()) return R.drawable.unknown
        return when {
            iconCode.startsWith("01") -> R.drawable.sunny         // clear
            iconCode.startsWith("02") -> R.drawable.sunny         // few clouds → still sunny
            iconCode.startsWith("03") -> R.drawable.cloudy        // scattered
            iconCode.startsWith("04") -> R.drawable.cloudy        // overcast
            iconCode.startsWith("09") -> R.drawable.rain
            iconCode.startsWith("10") -> R.drawable.rain
            iconCode.startsWith("11") -> R.drawable.thunderstorm
            else -> R.drawable.unknown
        }
    }

    /**
     * Combined mapping: use temperature first, else use weather.main/icon.
     */
    fun resolve(temp: Double?, main: String?, description: String?, iconCode: String?): Pair<String, Int> {
        // Prefer temperature logic if valid
        val (label, iconRes) = mapTempToLabelAndDrawable(temp)
        if (label != "Unknown") return label to iconRes

        // Fallback: based on main/icon
        val fallbackLabel = when (main?.lowercase(Locale.getDefault())) {
            "clear" -> "Sunny"
            "clouds" -> "Cloudy"
            "rain", "drizzle" -> "Light Rain"
            "thunderstorm" -> "Thunderstorm"
            else -> description?.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            } ?: "Unknown"
        }
        val fallbackIcon = mapIconCodeToDrawable(iconCode)
        return fallbackLabel to fallbackIcon
    }
}
