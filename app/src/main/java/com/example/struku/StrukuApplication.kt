package com.example.struku

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class untuk Struku App.
 * 
 * Digunakan untuk inisialisasi dependency injection dengan Hilt.
 */
@HiltAndroidApp
class StrukuApplication : Application()
