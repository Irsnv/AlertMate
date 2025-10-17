package com.example.alertmate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ScrollView
import androidx.fragment.app.Fragment

class TipsFragment : Fragment() {

    private lateinit var btnTips: Button
    private lateinit var btnEmeCont: Button
    private lateinit var layoutTips: ScrollView
    private lateinit var layoutEmergency: ScrollView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tips, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnTips = view.findViewById(R.id.btnTips)
        btnEmeCont = view.findViewById(R.id.btnEmeCont)
        layoutTips = view.findViewById(R.id.layoutTips)
        layoutEmergency = view.findViewById(R.id.layoutEmergency)

        // Default visible: Tips
        layoutTips.visibility = View.VISIBLE
        layoutEmergency.visibility = View.GONE

        btnTips.setOnClickListener {
            layoutTips.visibility = View.VISIBLE
            layoutEmergency.visibility = View.GONE
        }

        btnEmeCont.setOnClickListener {
            layoutTips.visibility = View.GONE
            layoutEmergency.visibility = View.VISIBLE
        }
    }
}
