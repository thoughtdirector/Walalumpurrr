package com.example.notificacionesapp

import android.app.Application
import com.google.firebase.FirebaseApp

class NotificacionesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicializar Firebase
        FirebaseApp.initializeApp(this)
    }
}