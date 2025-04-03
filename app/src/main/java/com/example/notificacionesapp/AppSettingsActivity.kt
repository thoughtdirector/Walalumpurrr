package com.example.notificacionesapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial

class AppSettingsActivity : AppCompatActivity() {

    private lateinit var nequiSwitch: SwitchMaterial
    private lateinit var daviplataSwitch: SwitchMaterial
    private lateinit var bancolombiaSwitch: SwitchMaterial
    private lateinit var whatsappSwitch: SwitchMaterial
    private lateinit var darkModeSwitch: SwitchMaterial
    private lateinit var saveButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_settings)

        // Inicializar vistas
        nequiSwitch = findViewById(R.id.nequiSwitch)
        daviplataSwitch = findViewById(R.id.daviplataSwitch)
        bancolombiaSwitch = findViewById(R.id.bancolombiaSwitch)
        whatsappSwitch = findViewById(R.id.whatsappSwitch)
        darkModeSwitch = findViewById(R.id.darkModeSwitch)
        saveButton = findViewById(R.id.saveButton)

        // Cargar configuración guardada
        loadSettings()

        // Configurar botón de guardar
        saveButton.setOnClickListener {
            saveSettings()
        }
    }

    private fun loadSettings() {
        try {
            val appPrefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val themePrefs = getSharedPreferences("theme_settings", Context.MODE_PRIVATE)

            // Cargar configuración de apps
            nequiSwitch.isChecked = appPrefs.getBoolean("app_nequi", true)
            daviplataSwitch.isChecked = appPrefs.getBoolean("app_daviplata", true)
            bancolombiaSwitch.isChecked = appPrefs.getBoolean("app_bancolombia", true)
            whatsappSwitch.isChecked = appPrefs.getBoolean("app_whatsapp", true)

            // Cargar configuración de tema
            darkModeSwitch.isChecked = themePrefs.getBoolean("dark_mode", false)
        } catch (e: Exception) {
            Toast.makeText(this, "Error al cargar configuración: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveSettings() {
        try {
            val appPrefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val themePrefs = getSharedPreferences("theme_settings", Context.MODE_PRIVATE)

            // Guardar configuración de apps
            appPrefs.edit().apply {
                putBoolean("app_nequi", nequiSwitch.isChecked)
                putBoolean("app_daviplata", daviplataSwitch.isChecked)
                putBoolean("app_bancolombia", bancolombiaSwitch.isChecked)
                putBoolean("app_whatsapp", whatsappSwitch.isChecked)
            }.apply()

            // Guardar configuración de tema
            val isDarkMode = darkModeSwitch.isChecked
            themePrefs.edit().apply {
                putBoolean("dark_mode", isDarkMode)
            }.apply()

            // Notificar al servicio sobre los cambios
            val serviceIntent = Intent(this, NotificationService::class.java)
            serviceIntent.action = NotificationService.ACTION_UPDATE_APP_SETTINGS
            startService(serviceIntent)

            // Aplicar el tema solo si ha cambiado
            val oldDarkMode = themePrefs.getBoolean("dark_mode_applied", false)
            if (oldDarkMode != isDarkMode) {
                themePrefs.edit().putBoolean("dark_mode_applied", isDarkMode).apply()

                // Usar la actividad puente para cambiar el tema de manera segura
                val themeIntent = Intent(this, ThemeActivity::class.java)
                themeIntent.putExtra("dark_mode", isDarkMode)
                startActivity(themeIntent)
                finish() // Finalizar esta actividad
                return // Salir del método para evitar mostrar el Toast aquí
            }

            Toast.makeText(this, "Configuración guardada", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al guardar configuración: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}