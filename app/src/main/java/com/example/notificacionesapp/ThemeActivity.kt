package com.example.notificacionesapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class ThemeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Obtener el modo oscuro desde el intent
        val isDarkMode = intent.getBooleanExtra("dark_mode", false)

        // Guardar en preferencias
        val themePrefs = getSharedPreferences("theme_settings", Context.MODE_PRIVATE)
        themePrefs.edit().putBoolean("dark_mode", isDarkMode).apply()

        // Aplicar el tema
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        // Esperar un momento y volver a la actividad principal
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }, 200)
    }
}