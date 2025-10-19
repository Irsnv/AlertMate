package com.example.alertmate.alert

import android.app.Activity
import android.os.Bundle
import com.example.alertmate.databinding.ActivityAlertBinding
import com.example.alertmate.R

class AlertActivity : Activity() {

    private lateinit var binding: ActivityAlertBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAlertBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Disable dismissal by outside touch or back button
        setFinishOnTouchOutside(false)

        // Stop button closes alert
        binding.btnStop.setOnClickListener {
            finish()
        }
    }
}
