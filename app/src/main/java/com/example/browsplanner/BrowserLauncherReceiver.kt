package com.example.browsplanner

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent

class BrowserLauncherReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            "CANCEL_ACTION" -> {
                val serviceIntent = Intent(context, BrowserLauncherService::class.java)
                context.stopService(serviceIntent)

                // Убираем уведомление
                val manager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                manager.cancel(1)
            }
        }
    }
}