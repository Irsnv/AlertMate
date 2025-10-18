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
import kotlin.math.roundToInt

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val apiKey = "8ae14fb0f147ecae151f258a8bfc482f"

    private val hourlyAdapter = HourlyAdapter(emptyList())
    private lateinit var dailyAdapter: DailyAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView setup - do once
        binding.hourlyRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
            adapter = hourlyAdapter
        }

        binding.dailyRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        // Set current date
        binding.textCurrentDate.text = getCurrentDate()

        // Get current user ID
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Fetch weather on load
        fetchUserLocation(userId) { lat, lon, cityName ->
            fetchWeatherData(lat, lon, cityName)
        }

        // Swipe to refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchUserLocation(userId) { lat, lon, cityName ->
                fetchWeatherData(lat, lon, cityName)
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
    private fun fetchUserLocation(userId: String, onResult: (Double, Double, String) -> Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val location = document.getString("location")
                    when (location) {
                        "Klang" -> onResult(3.0392, 101.4419, "Klang")
                        "Shah Alam" -> onResult(3.0734, 101.5217, "Shah Alam")
                        else -> onResult(3.0734, 101.5217, "Shah Alam")
                    }
                }
            }
            .addOnFailureListener {
                onResult(3.0734, 101.5217, "Shah Alam")
            }
    }

    // Fetch weather from OpenWeatherMap
    private fun fetchWeatherData(lat: Double, lon: Double, cityName: String) {
        showLoading()
        lifecycleScope.launch {
            try {
                // Call One Call API
                val response = RetrofitInstance.api.getOneCall(
                    lat = lat,
                    lon = lon,
                    exclude = "minutely,alerts",
                    units = "metric",
                    apiKey = apiKey
                )

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!

                    // ====== CURRENT ======
                    val current = data.current
                    binding.tvTemperature.text = "${current?.temp?.toInt()}°C"
                    binding.tvHumidity.text = "${current?.humidity ?: 0}%"
                    binding.tvWindSpeed.text = "${current?.wind_speed ?: 0.0} m/s"
                    binding.tvState.text = data.timezone ?: "Unknown"

                    binding.tvState.text = cityName

                    // Chance of Rain
                    val chanceOfRain = (data.hourly?.get(0)?.pop ?: 0.0) * 100
                    binding.tvChaOfRain.text = "${chanceOfRain.toInt()}%"

                    // Weather Icon
                    val icon = current?.weather?.get(0)?.icon ?: "01d"
                    binding.imgView.load("https://openweathermap.org/img/wn/${icon}@2x.png") {
                        crossfade(true)
                        placeholder(R.drawable.sunny)
                        error(R.drawable.sunny)
                    }

                    // ====== HOURLY ======
                    val hourlyListFromApi = data.hourly ?: emptyList()

// Choose how many hours to show (next 24 hours typical)
                    val hoursToShow = 24
                    val displayHourly = if (hourlyListFromApi.size > hoursToShow)
                        hourlyListFromApi.take(hoursToShow)
                    else
                        hourlyListFromApi

                    // --- find index for current hour (first dt >= now)
                    fun findCurrentHourIndex(hourly: List<com.example.alertmate.data.HourlyItem>): Int {
                        val nowSec = System.currentTimeMillis() / 1000
                        val idx = hourly.indexOfFirst { (it.dt ?: 0L) >= nowSec }
                        return if (idx == -1) {
                            // fallback: if all dt < now (unlikely), choose last item
                            maxOf(0, hourly.size - 1)
                        } else idx
                    }

                    if (displayHourly.isEmpty()) {
                        binding.hourlyRecyclerView.visibility = View.GONE
                    } else {
                        binding.hourlyRecyclerView.visibility = View.VISIBLE
                        // update adapter with list and selected index
                        val selectIndex = findCurrentHourIndex(displayHourly)
                        hourlyAdapter.updateList(displayHourly, selectIndex)

                        // Scroll so selected item is visible and slightly offset (center-ish)
                        binding.hourlyRecyclerView.post {
                            val lm = binding.hourlyRecyclerView.layoutManager as? LinearLayoutManager
                            if (lm != null) {
                                // compute center offset: center the selected item approximately
                                val recyclerWidth = binding.hourlyRecyclerView.width
                                val itemWidthDp = 130  // your item layout width in dp (you used 130dp)
                                val density = resources.displayMetrics.density
                                val itemWidthPx = (itemWidthDp * density).toInt()
                                val offset = (recyclerWidth / 2) - (itemWidthPx / 2)
                                lm.scrollToPositionWithOffset(selectIndex, offset)
                            } else {
                                binding.hourlyRecyclerView.scrollToPosition(selectIndex)
                            }
                        }

                        // Update top Today UI with the selected hour (or `current` if you want)
                        val topItem = displayHourly.getOrNull(selectIndex)
                        topItem?.let { item ->
                            // set big temperature
                            binding.tvTemperature.text = "${item.temp?.roundToInt() ?: 0}°C"

                            // set description label
                            val main = item.weather?.firstOrNull()?.main
                            val desc = item.weather?.firstOrNull()?.description
                            val (label, iconRes) = IconReplace.resolve(item.temp, main, desc, item.weather?.firstOrNull()?.icon)
                            binding.tvChaOfRain.text = label
                            binding.imgView.setImageResource(iconRes)

                        }
                    }

                    // ====== DAILY ======
                    val dailyListFromApi = data.daily ?: emptyList()

                    // Choose how many days to show (e.g., 7 days)
                    val daysToShow = 7
                    val displayDaily = if (dailyListFromApi.size > daysToShow)
                        dailyListFromApi.take(daysToShow)
                    else
                        dailyListFromApi

                    // If you haven't created adapter earlier, initialize it once
                    if (!::dailyAdapter.isInitialized) {
                        dailyAdapter = DailyAdapter(displayDaily)
                        binding.dailyRecyclerView.adapter = dailyAdapter
                        binding.dailyRecyclerView.layoutManager =
                            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                    } else {
                        dailyAdapter.updateList(displayDaily)
                    }

                    // Hide RecyclerView if list is empty
                    binding.dailyRecyclerView.visibility =
                        if (displayDaily.isEmpty()) View.GONE else View.VISIBLE

                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                hideLoading()
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
