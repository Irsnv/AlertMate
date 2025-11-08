package com.example.alertmate.home

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.alertmate.R
import com.example.alertmate.alert.AlertActivity
import com.example.alertmate.alert.AlertReceiver
import com.example.alertmate.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import com.google.android.material.snackbar.Snackbar // Correct for Views

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

        val db = FirebaseFirestore.getInstance()
        val user = FirebaseAuth.getInstance().currentUser ?: return

        // ✅ Check user's role before listening for alerts
        db.collection("users").document(user.uid).get().addOnSuccessListener { document ->
            val role = document.getString("role") ?: "user"

            fetchUserLocation(user.uid) { lat, lon, cityName ->
                fetchWeatherData(lat, lon, cityName)

                // ✅ Only normal users receive alerts
                if (role != "admin") {
                    listenForRealtimeAlerts()
                }
            }
        }

        // Swipe to refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchUserLocation(user.uid) { lat, lon, cityName ->
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
                        "Klang" -> {
                            subscribeToLocationTopic("Klang") // 👈 Add this line
                            onResult(3.0392, 101.4419, "Klang")
                        }
                        "Shah Alam" -> {
                            subscribeToLocationTopic("Shah Alam") // 👈 Add this line
                            onResult(3.0734, 101.5217, "Shah Alam")
                        }
                        else -> {
                            subscribeToLocationTopic("Shah Alam")
                            onResult(3.0734, 101.5217, "Shah Alam")
                        }
                    }
                }
            }
            .addOnFailureListener {
                subscribeToLocationTopic("Shah Alam")
                onResult(3.0734, 101.5217, "Shah Alam")
            }
    }

    // Subscribe user to location-based FCM topic
    private fun subscribeToLocationTopic(userLocation: String) {
        val topic = "alerts_${userLocation.replace(" ", "").lowercase()}" // e.g. alerts_klang
        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Optional: Log or show success
                    println("Subscribed to topic: $topic")
                } else {
                    println("Failed to subscribe to topic: $topic")
                }
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
                    Log.d("WeatherIcon", "Current weather icon code: $icon")
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
                            val iconCode = item.weather?.firstOrNull()?.icon ?: "01d"
                            Log.d("WeatherIcon", "Hourly icon code: $iconCode")
                            binding.imgView.load("https://openweathermap.org/img/wn/${iconCode}@2x.png") {
                                crossfade(true)
                                placeholder(R.drawable.sunny)
                                error(R.drawable.sunny)
                            }
                            binding.tvChaOfRain.text = desc ?: main ?: "N/A"


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

                    // === Detect thunderstorm 1 hour ahead ===
                    val nowSec = System.currentTimeMillis() / 1000
                    val oneHourAhead = nowSec + 3600

                    val upcomingThunderstorm = displayHourly.firstOrNull { hour ->
                        val dt = hour.dt ?: 0L
                        val main = hour.weather?.firstOrNull()?.main ?: ""
                        dt in nowSec..oneHourAhead && main.contains("Thunderstorm", ignoreCase = true)
                    }

                    // Use .let for safe handling of the nullable upcomingThunderstorm
                    upcomingThunderstorm?.let {
                        // This code only runs if 'it' (the thunderstorm) is not null
                        val stormTime = it.dt ?: return@let // Use return@let to exit only the 'let' block
                        val triggerTime = (stormTime - 3600) * 1000L // 1 hour before (in ms)
                        scheduleAlert(triggerTime)
                    }


                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                hideLoading()
            }
        }
    }
    private fun scheduleAlert(triggerAtMillis: Long) {
        val context = requireContext().applicationContext
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Check for permission on Android 12 (S) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Permission is not granted, guide the user to settings.
                Snackbar.make(
                    binding.root,
                    "Permission needed to show timely storm alerts.",
                    Snackbar.LENGTH_LONG
                ).setAction("Grant") {
                    // Use the correct Settings class
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    startActivity(intent)
                }.show()
                return // Don't schedule the alarm if permission is missing
            }
        }

        // --- If permission is granted (or on older OS), proceed with scheduling ---
        val intent = Intent(context, AlertReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0, // A unique request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // For testing, this schedules the alarm 10 seconds from now.
        // In production, you would use `triggerAtMillis`.
        val testTriggerTime = System.currentTimeMillis() + 10000

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            testTriggerTime, // Use your calculated `triggerAtMillis` here
            pendingIntent
        )

        Log.d("AlarmScheduler", "Exact alarm scheduled for a potential thunderstorm.")
    }

    // === Listen for location-based alerts ===
    private fun listenForAlerts(userLocation: String) {
        db.collection("alerts")
            .whereEqualTo("location", userLocation) // ✅ Only alerts for this location
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) {
                    Log.w("Firestore", "Listen failed.", e)
                    return@addSnapshotListener
                }

                for (dc in snapshots.documentChanges) {
                    if (dc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        val alertId = dc.document.id
                        val message = dc.document.getString("message") ?: continue
                        val location = dc.document.getString("location") ?: "Unknown"

                        // ✅ Show once only
                        if (!hasAlertBeenShownBefore(alertId)) {
                            showFullScreenPopup(message, location)
                            markAlertAsShown(alertId)
                        }
                    }
                }
            }
    }

    // === Show popup screen when alert received ===
    private fun showFullScreenPopup(message: String, location: String) {
        val intent = Intent(requireContext(), com.example.alertmate.alert.WarningPopupActivity::class.java)
        intent.putExtra("title", "Emergency Alert: $location")
        intent.putExtra("message", message)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    // === Prevent showing same alert twice ===
    private fun hasAlertBeenShownBefore(alertId: String): Boolean {
        val prefs = requireContext().getSharedPreferences("shown_alerts", Context.MODE_PRIVATE)
        return prefs.getBoolean(alertId, false)
    }

    private fun markAlertAsShown(alertId: String) {
        val prefs = requireContext().getSharedPreferences("shown_alerts", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(alertId, true).apply()
    }

    // === Listen for Realtime Database alerts ===
    private fun listenForRealtimeAlerts() {
        val dbRef = com.google.firebase.database.FirebaseDatabase
            .getInstance("https://alertmate-6eaf4-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("alerts")

        dbRef.addChildEventListener(object : com.google.firebase.database.ChildEventListener {
            override fun onChildAdded(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {
                val alertId = snapshot.key ?: return
                val message = snapshot.child("message").getValue(String::class.java)
                val location = snapshot.child("location").getValue(String::class.java) ?: "Unknown"

                // ✅ Show only if it matches user's location and not shown before
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
                fetchUserLocation(userId) { _, _, userCity ->
                    if (userCity == location && message != null && !hasAlertBeenShownBefore(alertId)) {
                        showFullScreenPopup(message, location)
                        markAlertAsShown(alertId)
                    }
                }
            }

            override fun onChildChanged(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: com.google.firebase.database.DataSnapshot) {}
            override fun onChildMoved(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Log.e("RealtimeDB", "Error listening for alerts", error.toException())
            }
        })
    }

}
