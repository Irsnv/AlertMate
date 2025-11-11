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
import com.google.android.material.snackbar.Snackbar

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

        //setup for scrolling list for Hourly Weather
        binding.hourlyRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            setHasFixedSize(true)
            adapter = hourlyAdapter
        }
        //setup for scrolling list for Daily Weather
        binding.dailyRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        //set current date
        binding.textCurrentDate.text = getCurrentDate()

        val db = FirebaseFirestore.getInstance()
        val user = FirebaseAuth.getInstance().currentUser ?: return
        // === Receive Alert from Admin Section ===
        //check user's role before listening for alerts
        db.collection("users").document(user.uid).get().addOnSuccessListener { document ->
            val role = document.getString("role") ?: "user"

            fetchUserLocation(user.uid) { lat, lon, cityName ->
                fetchWeatherData(lat, lon, cityName)

                //nly normal users receive alerts
                if (role != "admin") {
                    listenForRealtimeAlerts()
                }
            }
        }

        //swipe to refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchUserLocation(user.uid) { lat, lon, cityName ->
                fetchWeatherData(lat, lon, cityName)
            }
        }
    }


    //get and display current date
    private fun getCurrentDate(): String {
        val currentDate = Date()
        val formatter = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
        return "Today, ${formatter.format(currentDate)}"
    }
    //show and hide loading animation
    private fun showLoading() {
        binding.hourlyRecyclerView.visibility = View.GONE
        binding.swipeRefreshLayout.isRefreshing = true
    }
    private fun hideLoading() {
        binding.hourlyRecyclerView.visibility = View.VISIBLE
        binding.swipeRefreshLayout.isRefreshing = false
    }

    //fetch user location from Firestore
    private fun fetchUserLocation(userId: String, onResult: (Double, Double, String) -> Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val location = document.getString("location")
                    //convert loc into map coor
                    when (location) {
                        "Klang" -> {
                            subscribeToLocationTopic("Klang")
                            onResult(3.0392, 101.4419, "Klang")
                        }
                        "Shah Alam" -> {
                            subscribeToLocationTopic("Shah Alam")
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

    private fun subscribeToLocationTopic(userLocation: String) {
        val topic = "alerts_${userLocation.replace(" ", "").lowercase()}"
        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    println("Subscribed to topic: $topic")
                } else {
                    println("Failed to subscribe to topic: $topic")
                }
            }
    }
    //get weather data from OWM API
    private fun fetchWeatherData(lat: Double, lon: Double, cityName: String) {
        showLoading()
        lifecycleScope.launch {
            try {
                //call One Call API
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

                    //chance of rain
                    val chanceOfRain = (data.hourly?.get(0)?.pop ?: 0.0) * 100
                    binding.tvChaOfRain.text = "${chanceOfRain.toInt()}%"

                    //load weather icon
                    val icon = current?.weather?.get(0)?.icon ?: "01d"
                    Log.d("WeatherIcon", "Current weather icon code: $icon")
                    binding.imgView.load("https://openweathermap.org/img/wn/${icon}@2x.png") {
                        crossfade(true)
                        placeholder(R.drawable.sunny)
                        error(R.drawable.sunny)
                    }

                    // ==== HOURLY ====
                    val hourlyListFromApi = data.hourly ?: emptyList()
                    //show the next 24 hours
                    val hoursToShow = 24
                    val displayHourly = if (hourlyListFromApi.size > hoursToShow)
                        hourlyListFromApi.take(hoursToShow)
                    else
                        hourlyListFromApi

                    //find which hour is current
                    fun findCurrentHourIndex(hourly: List<com.example.alertmate.data.HourlyItem>): Int {
                        val nowSec = System.currentTimeMillis() / 1000
                        val idx = hourly.indexOfFirst { (it.dt ?: 0L) >= nowSec }
                        return if (idx == -1) {
                            maxOf(0, hourly.size - 1)
                        } else idx
                    }

                    if (displayHourly.isEmpty()) {
                        binding.hourlyRecyclerView.visibility = View.GONE
                    } else {
                        binding.hourlyRecyclerView.visibility = View.VISIBLE
                        //find which hour is the current
                        val selectIndex = findCurrentHourIndex(displayHourly)
                        hourlyAdapter.updateList(displayHourly, selectIndex)

                        //scroll so selected item is visible and slightly offset (center-ish)
                        binding.hourlyRecyclerView.post {
                            val lm = binding.hourlyRecyclerView.layoutManager as? LinearLayoutManager
                            if (lm != null) {
                                // compute center offset: center the selected item approximately
                                val recyclerWidth = binding.hourlyRecyclerView.width
                                val itemWidthDp = 130 //width each hourly item in dp
                                val density = resources.displayMetrics.density
                                val itemWidthPx = (itemWidthDp * density).toInt()
                                val offset = (recyclerWidth / 2) - (itemWidthPx / 2)
                                lm.scrollToPositionWithOffset(selectIndex, offset)
                            } else {
                                binding.hourlyRecyclerView.scrollToPosition(selectIndex)
                            }
                        }

                        //update top Today UI with the selected hour (or `current` if you want)
                        val topItem = displayHourly.getOrNull(selectIndex)
                        topItem?.let { item ->
                            //set temp num
                            binding.tvTemperature.text = "${item.temp?.roundToInt() ?: 0}°C"
                            // set desc label
                            val main = item.weather?.firstOrNull()?.main
                            val desc = item.weather?.firstOrNull()?.description
                            //showw weather icon
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

                    // ==== DAILY ====
                    val dailyListFromApi = data.daily ?: emptyList()
                    //show the next 7 days
                    val daysToShow = 7
                    val displayDaily = if (dailyListFromApi.size > daysToShow)
                        dailyListFromApi.take(daysToShow)
                    else
                        dailyListFromApi

                    //setup recyclerview
                    if (!::dailyAdapter.isInitialized) {
                        dailyAdapter = DailyAdapter(displayDaily)
                        binding.dailyRecyclerView.adapter = dailyAdapter
                        binding.dailyRecyclerView.layoutManager =
                            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                    } else {
                        dailyAdapter.updateList(displayDaily)
                    }
                    //hide daily recyclerview if there is no data
                    binding.dailyRecyclerView.visibility =
                        if (displayDaily.isEmpty()) View.GONE else View.VISIBLE

                    // === Detect thunderstorm 1 hour ahead ===
                    val nowSec = System.currentTimeMillis() / 1000 //current in sec
                    val oneHourAhead = nowSec + 3600 //time 1 hour ahead
                    //check the hourly list for thunderstorms in the next hour
                    val upcomingThunderstorm = displayHourly.firstOrNull { hour ->
                        val dt = hour.dt ?: 0L
                        val main = hour.weather?.firstOrNull()?.main ?: ""
                        dt in nowSec..oneHourAhead && main.contains("Thunderstorm", ignoreCase = true)
                    }

                    //if detect thunderstorm send the alert
                    upcomingThunderstorm?.let {
                        //only runs if 'it' (the thunderstorm) is not null
                        val stormTime = it.dt ?: return@let
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

    //=== function to send alert === in milisec
    private fun scheduleAlert(triggerAtMillis: Long) {
        //get app system AlarmManager
        val context = requireContext().applicationContext
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        //check for permission on Android 12  and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                //permission is not granted, guide the user to settings.
                Snackbar.make(
                    binding.root,
                    "Permission needed to show timely storm alerts.",
                    Snackbar.LENGTH_LONG
                ).setAction("Grant") {
                    //redirect and open system setting to grant permission
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    startActivity(intent)
                }.show()
                return
            }
        }

        //if permission is granted or on older Android vers proceed with scheduling
        //intent to trigger the AlertReceiver (broadcast receiver)
        val intent = Intent(context, AlertReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // For testing, this schedules the alarm 1 seconds from now.
        // In production, you would use `triggerAtMillis`.
        val testTriggerTime = System.currentTimeMillis() + 1000

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            testTriggerTime,
            pendingIntent
        )

        Log.d("AlarmScheduler", "Exact alarm scheduled for a potential thunderstorm.")
    }

    // === Listen for alerts for a specific location ===
    private fun listenForAlerts(userLocation: String) {
        //listen to the "alerts" collection in Firestore
        db.collection("alerts")
            .whereEqualTo("location", userLocation)
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) {
                    Log.w("Firestore", "Listen failed.", e)
                    return@addSnapshotListener
                }
                //loop through all changes in the collection
                for (dc in snapshots.documentChanges) {
                    //only process newly added alerts
                    if (dc.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        //get alert ID, message, and location
                        val alertId = dc.document.id
                        val message = dc.document.getString("message") ?: continue
                        val location = dc.document.getString("location") ?: "Unknown"

                        //show once only
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
        //intent to trigger the WarningPopupActivity
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
        //connect with realtime firebase
        val dbRef = com.google.firebase.database.FirebaseDatabase
            .getInstance("https://alertmate-6eaf4-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("alerts")

        dbRef.addChildEventListener(object : com.google.firebase.database.ChildEventListener {
            //triggered when a new alert is added"loc
            override fun onChildAdded(snapshot: com.google.firebase.database.DataSnapshot, previousChildName: String?) {
                val alertId = snapshot.key ?: return
                val message = snapshot.child("message").getValue(String::class.java)
                val location = snapshot.child("location").getValue(String::class.java) ?: "Unknown"

                //show only if it matches user's location and not shown before
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
