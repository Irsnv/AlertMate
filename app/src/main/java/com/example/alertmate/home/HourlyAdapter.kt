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

class HourlyAdapter(
    private var list: List<HourlyItem>,
    private var selectedIndex: Int = -1
) : RecyclerView.Adapter<HourlyAdapter.HourlyViewHolder>() {

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

        //time: format dt -> "1 PM"
        val ts = (item.dt ?: 0L) * 1000L
        val date = Date(ts)
        val sdf = SimpleDateFormat("h a", Locale.getDefault())
        holder.hourTxt.text = sdf.format(date)

        //temperature (units=metric assumed)
        val tempText = item.temp?.roundToInt()?.let { "$it°C" } ?: "--"
        holder.tempTxt.text = tempText

        //weather desc
        val main = item.weather?.firstOrNull()?.main
        val desc = item.weather?.firstOrNull()?.description
        val tempValue = item.temp

        //weathericon
        val iconCode = item.weather?.firstOrNull()?.icon ?: "01d"
        holder.pic.load("https://openweathermap.org/img/wn/${iconCode}@2x.png") {
            crossfade(true)
            placeholder(R.drawable.sunny)
            error(R.drawable.sunny)
        }
        holder.tempdescTxt.text = desc ?: main ?: "Unknown"

        //highlight selected hour
        holder.itemView.isSelected = (position == selectedIndex)

        holder.itemView.setOnClickListener {
            val old = selectedIndex
            selectedIndex = position
            notifyItemChanged(old)
            notifyItemChanged(selectedIndex)
        }
    }

    override fun getItemCount(): Int = list.size

    //replace adapter data and optionally pre-select an index
    fun updateList(newList: List<HourlyItem>, selectIndex: Int = -1) {
        list = newList
        selectedIndex = selectIndex
        notifyDataSetChanged() // ok for small lists; use DiffUtil for better perf
    }
}
