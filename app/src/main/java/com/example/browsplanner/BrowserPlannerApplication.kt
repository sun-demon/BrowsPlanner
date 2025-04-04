package com.example.browsplanner

import android.app.Application
import androidx.lifecycle.MutableLiveData

class BrowserPlannerApplication : Application() {
    val isServiceRunning = MutableLiveData<Boolean>(false)

    companion object {
        private lateinit var instance: BrowserPlannerApplication

        fun get(): BrowserPlannerApplication = instance
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}