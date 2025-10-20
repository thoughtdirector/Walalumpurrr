package com.example.notificacionesapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for the NotificacionesApp
 * This class is required for Hilt dependency injection
 */
@HiltAndroidApp
class NotificacionesApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Initialize any application-level components here
    }
}
