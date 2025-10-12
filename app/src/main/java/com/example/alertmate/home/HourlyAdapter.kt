package com.example.alertmate.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
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

        // Time: format dt -> "1 PM"
        val ts = (item.dt ?: 0L) * 1000L
        val date = Date(ts)
        val sdf = SimpleDateFormat("h a", Locale.getDefault())
        holder.hourTxt.text = sdf.format(date)

        // Temperature (units=metric assumed)
        val tempText = item.temp?.roundToInt()?.let { "$it°C" } ?: "--"
        holder.tempTxt.text = tempText

        val main = item.weather?.firstOrNull()?.main
        val desc = item.weather?.firstOrNull()?.description
        val iconCode = item.weather?.firstOrNull()?.icon
        val tempValue = item.temp

        // Use combined resolver (temperature-aware)
        val (label, resId) = IconReplace.resolve(tempValue, main, desc, iconCode)
        holder.tempdescTxt.text = label
        holder.pic.setImageResource(resId)


        // Highlight selected hour (optional visual)
        holder.itemView.isSelected = (position == selectedIndex)

        // On click, mark selected and notify (keeps local selection UI)
        holder.itemView.setOnClickListener {
            val old = selectedIndex
            selectedIndex = position
            notifyItemChanged(old)
            notifyItemChanged(selectedIndex)
            // Optionally: you can add callback to notify fragment of this click
        }
    }

    override fun getItemCount(): Int = list.size

    // Replace adapter data and optionally pre-select an index
    fun updateList(newList: List<HourlyItem>, selectIndex: Int = -1) {
        list = newList
        selectedIndex = selectIndex
        notifyDataSetChanged() // ok for small lists; use DiffUtil for better perf
    }
}
