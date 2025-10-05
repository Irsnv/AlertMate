package com.example.alertmate.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.alertmate.databinding.FragmentHomeBinding
import com.example.alertmate.network.RetrofitInstance
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.lifecycleScope
import coil.load
import kotlinx.coroutines.launch
import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.roundToInt

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var lat = 0.0
    private var lon = 0.0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        val prefs = requireContext().getSharedPreferences("UserPrefs", 0)
        val userLocation = prefs.getString("location", "Klang") ?: "Klang"

        when (userLocation) {
            "Klang" -> {
                lat = 3.0392
                lon = 101.4419
            }
            "Shah Alam" -> {
                lat = 3.0734
                lon = 101.5217
            }
        }

        binding.textCurrentDate.text = getCurrentDate()

        fetchWeatherData(lat, lon, userLocation)

        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchWeatherData(lat, lon, userLocation)
        }

        return binding.root
    }


    private fun fetchWeatherData(lat: Double, lon: Double, cityName: String) {
        showLoading()
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getWeatherData(
                    lat = lat,
                    lon = lon,
                    apiKey = "99b611f04c37b4b65caf8185e8d6f3b4",
                    units = "metric"
                )

                Log.d("WeatherResponse", "Response code: ${response.code()} - ${response.message()}")

                if (response.isSuccessful) {
                    val data = response.body()
                    Log.d("WeatherResponse", "Data received: $data")

                    data?.let {
                        hideLoading()
                        updateUI(
                            city = cityName,
                            temp = it.current.temp,
                            humidity = it.current.humidity,
                            windSpeed = it.current.windSpeed,
                            iconCode = it.current.weather.first().icon,
                            chanceOfRain = ((it.daily.firstOrNull()?.pop ?: 0f) * 100).toInt()
                        )
                    }
                } else {
                    hideLoading()
                    Log.e("WeatherError", "API Error: ${response.errorBody()?.string()}")
                    binding.tvState.text = "Error: ${response.code()}"
                }
            } catch (e: Exception) {
                hideLoading()
                Log.e("WeatherError", "Exception: ${e.message}", e)
                binding.tvState.text = "Error: ${e.message}"
            }
        }
    }



    private fun updateUI(
        city: String,
        temp: Float,
        humidity: Int,
        windSpeed: Float,
        iconCode: String,
        chanceOfRain: Int
    ) {
        with(binding) {
            tvState.text = city
            tvTemperature.text = "${temp.roundToInt()}°C"
            tvWindSpeed.text = "${windSpeed} m/s"
            tvHumidity.text = "$humidity%"
            tvChaOfRain.text = "$chanceOfRain%"
            imgView.load("https://openweathermap.org/img/wn/${iconCode}@4x.png")
        }
    }


    private fun getCurrentDate(): String {
        val currentDate = Date()
        val formatter = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
        return "Today, ${formatter.format(currentDate)}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showLoading() {
        with(binding) {
            hourlyRecyclerView.visibility = View.VISIBLE
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun hideLoading() {
        with(binding) {
            hourlyRecyclerView.visibility = View.GONE
            swipeRefreshLayout.isRefreshing = false
        }
    }
}
