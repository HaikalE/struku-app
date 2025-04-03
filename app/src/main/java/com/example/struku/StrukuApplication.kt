package com.example.struku

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class StruKuApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize any app-wide dependencies here
    }
}