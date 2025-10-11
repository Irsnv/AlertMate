package com.example.alertmate.home

import android.util.Log
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

class HourlyAdapter(private var list: List<HourlyItem>) :
    RecyclerView.Adapter<HourlyAdapter.HourlyViewHolder>() {

    // Public helper to update the adapter's data without recreating the adapter
    fun updateList(newList: List<HourlyItem>) {
        list = newList
        notifyDataSetChanged()
    }

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

        // 1) Time formatting (Unix seconds -> readable hour)
        val ts = (item.dt ?: 0L) * 1000L
        val date = Date(ts)
        val sdf = SimpleDateFormat("h a", Locale.getDefault()) // e.g. "1 PM"
        holder.hourTxt.text = sdf.format(date)

        // 2) Temperature (safe, rounded)
        val tempText = item.temp?.roundToInt()?.let { "$it°C" } ?: "--"
        holder.tempTxt.text = tempText

        // 3) Description (safe)
        val desc = item.weather?.firstOrNull()?.description
            ?.replaceFirstChar { it.uppercaseChar() } ?: ""
        holder.tempdescTxt.text = desc

        Log.d("HourlyDebug", "Hour: ${item.dt}, Icon: ${item.weather?.firstOrNull()?.icon}")

        // 4) Icon (safe). If icon is null, fall back to "01d"
        val icon = item.weather?.firstOrNull()?.icon ?: "01d"
        val iconUrl = "https://openweathermap.org/img/wn/${icon}@2x.png"

        // inside onBindViewHolder (example)
        val popPercent = ((item.pop ?: 0.0) * 100).roundToInt()
        holder.tempdescTxt.text = "$popPercent% rain"


        holder.pic.load(iconUrl) {
            crossfade(true)
            placeholder(R.drawable.cloudy) // use a drawable in your project
            error(R.drawable.cloudy)
        }
    }

    override fun getItemCount(): Int = list.size
}
