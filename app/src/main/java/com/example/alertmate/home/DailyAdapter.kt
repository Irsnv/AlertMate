package com.example.alertmate.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.alertmate.R
import com.example.alertmate.data.DailyItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

class DailyAdapter(
    private var items: List<DailyItem> = listOf(),
    private val timezoneId: String = "Asia/Kuala_Lumpur" // set to user's timezone if needed
) : RecyclerView.Adapter<DailyAdapter.DailyVH>() {

    inner class DailyVH(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        val tvDay: TextView  = view.findViewById(R.id.tvDay)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvTemp: TextView = view.findViewById(R.id.tvTemp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.viewholder_daily, parent, false)
        return DailyVH(view)
    }

    override fun onBindViewHolder(holder: DailyVH, position: Int) {
        val daily = items[position]

        // --- Date formatting ---
        val dtSeconds = daily.dt ?: 0L
        val date = Date(dtSeconds * 1000L)
        val daySdf = SimpleDateFormat("EEE", Locale.getDefault())
        val dateSdf = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        val tz = TimeZone.getTimeZone(timezoneId)
        daySdf.timeZone = tz
        dateSdf.timeZone = tz
        holder.tvDay.text = daySdf.format(date)
        holder.tvDate.text = dateSdf.format(date)

        // --- Temperature ---
        val tempMax = daily.temp?.max
        val tempMin = daily.temp?.min
        val tempDay = daily.temp?.day
        holder.tvTemp.text = when {
            tempMax != null && tempMin != null ->
                "${tempMax.roundToInt()}° / ${tempMin.roundToInt()}°"
            tempDay != null -> "${tempDay.roundToInt()}°"
            else -> "--°"
        }

        // --- Icon ---
        val iconCode = daily.weather?.firstOrNull()?.icon
        if (!iconCode.isNullOrEmpty()) {
            holder.ivIcon.load("https://openweathermap.org/img/wn/${iconCode}@2x.png") {
                placeholder(R.drawable.windy)
                error(R.drawable.windy)
            }
        } else {
            holder.ivIcon.setImageResource(R.drawable.windy)
        }
    }

    override fun getItemCount(): Int = items.size

    // helper to update list
    fun updateList(newItems: List<DailyItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
