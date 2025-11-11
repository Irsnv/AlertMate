package com.example.alertmate.alert

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.example.alertmate.R


class WarningPopupActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_warning_popup)

        //show over lock screen and turn screen on
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        val title = intent.getStringExtra("title") ?: "Emergency Alert"
        val message = intent.getStringExtra("message") ?: "Please stay safe."

        findViewById<TextView>(R.id.txtTitle).text = title
        findViewById<TextView>(R.id.txtMessage).text = message

        findViewById<Button>(R.id.btnDismiss).setOnClickListener {
            finish()
        }
    }
}
