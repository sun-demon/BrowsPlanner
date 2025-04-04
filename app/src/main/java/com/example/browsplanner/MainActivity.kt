package com.example.browsplanner

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private val app by lazy { application as BrowserPlannerApplication }
    val tag = MainActivity::class.simpleName
    lateinit var urlEditor: EditText
    lateinit var timePicker: TimePicker
    lateinit var toggleButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        urlEditor = findViewById(R.id.urlEditor)
        timePicker = findViewById(R.id.timePicker)
        toggleButton = findViewById(R.id.toggleButton)

        app.isServiceRunning.observeForever {
            urlEditor.isEnabled = !it
            timePicker.isEnabled = !it
            toggleButton.text = if (it) "Stop service" else "Start service"

        }

        toggleButton.setOnClickListener {
            app.isServiceRunning.value?.let{
                if (!it) {
                    startBrowserLauncherService()
                } else {
                    stopBrowserLauncherService()
                }
            } ?: Log.w(tag, "isWork.value is null")
        }
    }

    private fun startBrowserLauncherService() {
        var url = urlEditor.text.trim().replace("\\s".toRegex(), "")
        if (!url.matches(Regex("^https?://.*"))) {
            url = "https://$url"
        }
        if (!Patterns.WEB_URL.matcher(url).matches()) {
            Toast.makeText(this, "Incorrect URL address", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(packageManager) != null) {
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, timePicker.hour)
                    set(Calendar.MINUTE, timePicker.minute)
                    set(Calendar.SECOND, 0)
                }
                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val intent = Intent(this, BrowserLauncherService::class.java).apply {
                        putExtra("url", url)
                        putExtra("triggerTime", calendar.timeInMillis)
                    }
                    // Start as foreground service immediately
                    startForegroundService(intent)
                } else {
                    Toast.makeText(this, "Required Android 8.0+", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this,"Browser not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error by opening browser", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun stopBrowserLauncherService() {
        val intent = Intent(this, BrowserLauncherService::class.java)
        stopService(intent)
    }
}