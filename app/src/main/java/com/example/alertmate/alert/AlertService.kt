package com.example.alertmate.alert

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.alertmate.R

class AlertService : Service() {

    override fun onCreate() {
        super.onCreate()
        Log.d("AlertService", "Service onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("AlertService", "Service started")

        val channelId = "alert_channel"
        val channel = NotificationChannel(channelId, "Weather Alert", NotificationManager.IMPORTANCE_HIGH)
        channel.enableVibration(true)
        channel.vibrationPattern = longArrayOf(0, 1000, 1000, 1000)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        // Full-screen intent
        val fullScreenIntent = Intent(this, AlertActivity::class.java)
        fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0, fullScreenIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.notification_bell)
            .setContentTitle("Thunderstorm Alert")
            .setContentText("A thunderstorm is approaching soon!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(0, 1000, 1000, 1000))
            .build()

        startForeground(1, notification)

        // Do NOT call startActivity() from service
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}