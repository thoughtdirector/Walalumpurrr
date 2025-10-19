package com.example.notificacionesapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.notificacionesapp.NotificationService
import com.example.notificacionesapp.databinding.ActivityAppSettingsBinding

class AppSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppSettingsBinding
    private val appSettings = mutableMapOf<String, Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadAppSettings()
        setupRecyclerView()
        setupSaveButton()
    }

    private fun loadAppSettings() {
        try {
            val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            
            // Apps por defecto
            val defaultApps = listOf(
                "Nequi", "DaviPlata", "Bancolombia", "WhatsApp", 
                "BBVA", "Scotiabank", "Colpatria", "Itaú"
            )
            
            defaultApps.forEach { appName ->
                appSettings[appName] = prefs.getBoolean(appName, true)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error al cargar configuración: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        val adapter = AppSettingsAdapter(appSettings) { appName, isEnabled ->
            appSettings[appName] = isEnabled
        }
        
        binding.appsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@AppSettingsActivity)
            this.adapter = adapter
        }
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            saveAppSettings()
        }
    }

    private fun saveAppSettings() {
        try {
            val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            
            appSettings.forEach { (appName, isEnabled) ->
                editor.putBoolean(appName, isEnabled)
            }
            
            editor.apply()
            
            // Notificar al servicio sobre los cambios
            val serviceIntent = Intent(this, NotificationService::class.java)
            serviceIntent.action = NotificationService.ACTION_UPDATE_APP_SETTINGS
            startService(serviceIntent)
            
            Toast.makeText(this, "Configuración guardada", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al guardar configuración: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}