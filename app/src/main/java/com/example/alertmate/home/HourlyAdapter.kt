package com.example.alertmate.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.alertmate.R
import com.example.alertmate.data.HourlyItem
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class HourlyAdapter(private val list: List<HourlyItem>) :
    RecyclerView.Adapter<HourlyAdapter.HourlyViewHolder>() {

    class HourlyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val hourTxt: TextView = view.findViewById(R.id.hourTxt)
        val pic: ImageView = view.findViewById(R.id.pic)
        val tempTxt: TextView = view.findViewById(R.id.tempTxt)
        val tempdescTxt: TextView = view.findViewById(R.id.tempdescTxt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourlyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.viewholder_hourly, parent, false)
        return HourlyViewHolder(view)
    }

    override fun onBindViewHolder(holder: HourlyViewHolder, position: Int) {
        val item = list[position]

        // Convert timestamp to hour
        val date = Date(item.dt * 1000)
        val sdf = SimpleDateFormat("ha", Locale.getDefault())
        holder.hourTxt.text = sdf.format(date)

        // Temperature in °C
        holder.tempTxt.text = "${item.main.temp.roundToInt()}°C"

        // Description
        holder.tempdescTxt.text = item.weather[0].description.capitalize()

        // Icon using OpenWeatherMap icon URL
        val iconUrl = "https://openweathermap.org/img/wn/${item.weather[0].icon}@2x.png"
        holder.pic.load(iconUrl)
    }

    override fun getItemCount(): Int = list.size
}
