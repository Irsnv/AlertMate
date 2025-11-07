package com.example.alertmate.alert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

class AlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.d("AlertReceiver", "Broadcast received - starting service")

        val serviceIntent = Intent(context, AlertService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)  // safer


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}