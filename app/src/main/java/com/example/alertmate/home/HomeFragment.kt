package com.example.alertmate.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.alertmate.R
import com.example.alertmate.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val apiKey = "8ae14fb0f147ecae151f258a8bfc482f"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set current date
        binding.textCurrentDate.text = getCurrentDate()

        // Get current user ID
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Fetch weather on load
        fetchUserLocation(userId) { lat, lon ->
            fetchWeather(lat, lon)
            fetchHourlyForecast(lat, lon)
        }

        // Swipe to refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchUserLocation(userId) { lat, lon ->
                fetchWeather(lat, lon)
                fetchHourlyForecast(lat, lon)
            }
        }
    }

    // Get current date
    private fun getCurrentDate(): String {
        val currentDate = Date()
        val formatter = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
        return "Today, ${formatter.format(currentDate)}"
    }

    // Show/hide loading (optional)
    private fun showLoading() {
        binding.hourlyRecyclerView.visibility = View.GONE
        binding.swipeRefreshLayout.isRefreshing = true
    }

    private fun hideLoading() {
        binding.hourlyRecyclerView.visibility = View.VISIBLE
        binding.swipeRefreshLayout.isRefreshing = false
    }

    // Fetch user location from Firestore
    private fun fetchUserLocation(userId: String, onResult: (Double, Double) -> Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val location = document.getString("location")
                    when (location) {
                        "Klang" -> onResult(3.0392, 101.4419)
                        "Shah Alam" -> onResult(3.0734, 101.5217)
                        else -> onResult(3.0734, 101.5217)
                    }
                }
            }
            .addOnFailureListener {
                onResult(3.0734, 101.5217)
            }
    }

    // Fetch weather from OpenWeatherMap
    private fun fetchWeather(lat: Double, lon: Double) {
        showLoading()
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getCurrentWeather(lat, lon, apiKey)
                if (response.isSuccessful && response.body() != null) {
                    val weather = response.body()!!

                    // Update UI
                    binding.tvState.text = weather.name
                    binding.tvTemperature.text = "${weather.main.temp.toInt()}°C"
                    binding.tvHumidity.text = "${weather.main.humidity}%"
                    binding.tvWindSpeed.text = "${weather.wind.speed} m/s"

                    // Chance of Rain (replace with weather)
                    val weatherMain = weather.weather[0].main
                    binding.tvChaOfRain.text = when (weatherMain) {
                        "Rain" -> "High"
                        "Drizzle" -> "Medium"
                        "Thunderstorm" -> "Very High"
                        else -> "0%"
                    }

                    // Load weather icon with placeholder/error
                    val iconUrl = "https://openweathermap.org/img/wn/${weather.weather[0].icon}@2x.png"
                    binding.imgView.load(iconUrl) {
                        crossfade(true)
                        placeholder(R.drawable.sunny)
                        error(R.drawable.sunny)
                    }
                }
            } catch (e: HttpException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                hideLoading()
            }
        }
    }

    private fun fetchHourlyForecast(lat: Double, lon: Double) {
        lifecycleScope.launch {
            try {
                val response = RetrofitInstance.api.getHourlyWeather(lat, lon, apiKey)
                if (response.isSuccessful) {
                    val data = response.body()
                    data?.let {
                        binding.hourlyRecyclerView.apply {
                            layoutManager = LinearLayoutManager(
                                requireContext(),
                                LinearLayoutManager.HORIZONTAL,
                                false
                            )
                            adapter = HourlyAdapter(it.list)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
