package com.example.browsplanner

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri

class BrowserLauncherService : Service() {
    private val notificationId = 1
    private val channelId = "browser_launcher_channel"
    private lateinit var timer: CountDownTimer

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val url = it.getStringExtra("url") ?: ""
            val triggerTime = it.getLongExtra("triggerTime", 0L)

            if (url.isEmpty() || triggerTime <= 0) {
                stopSelf()
                return START_NOT_STICKY
            }

            // Start as foreground service
            startForegroundServiceWithNotification(url, triggerTime)
        }

        return START_STICKY
    }

    private fun startForegroundServiceWithNotification(url: String, triggerTime: Long) {
        // 1. Create the notification
        val notification = createNotification("Preparing to launch browser...")

        // 2. Start foreground service (different methods per API level)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(notificationId, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE)
            }
        } else {
            startForeground(notificationId, notification)
        }

        // 3. Start the countdown
        startTimer(url, triggerTime)
    }

    private fun startTimer(url: String, triggerTime: Long) {
        val delay = triggerTime - System.currentTimeMillis()

        timer = object : CountDownTimer(delay, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                updateNotification("Launching in ${millisUntilFinished / 1000} seconds")
            }

            override fun onFinish() {
                openBrowser(url)
                stopSelf()
            }
        }.start()
    }

    private fun openBrowser(url: String) {
        try {
            Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(this)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to open browser", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Browser Launcher")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_cancel,
                "Cancel",
                PendingIntent.getBroadcast(
                    this,
                    0,
                    Intent(this, BrowserLauncherReceiver::class.java).apply {
                        action = "CANCEL_ACTION"
                    },
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                channelId,
                "Browser Launcher",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for browser launch service"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }.also { channel ->
                val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }
        }
    }

    override fun onDestroy() {
        timer.cancel()
        stopForeground(true)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}