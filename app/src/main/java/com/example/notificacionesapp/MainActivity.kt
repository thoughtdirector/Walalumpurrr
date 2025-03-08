package com.example.notificacionesapp

import android.Manifest
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var statusText: TextView
    private lateinit var serviceButton: MaterialButton
    private lateinit var powerSwitch: SwitchMaterial
    private lateinit var scheduleButton: MaterialButton
    private lateinit var tts: TextToSpeech
    private lateinit var scheduleManager: ScheduleManager

    private val permissionRequestCode = 123

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == updateStatusAction) {
                updateStatus()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        serviceButton = findViewById(R.id.serviceButton)
        powerSwitch = findViewById(R.id.powerSwitch)
        scheduleButton = findViewById(R.id.scheduleButton)

        // Inicializar ScheduleManager
        scheduleManager = ScheduleManager(this)

        // Initialize Text-to-Speech
        tts = TextToSpeech(this, this)

        // Check permissions
        checkAndRequestPermissions()

        // Configure power switch
        powerSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!isNotificationServiceEnabled()) {
                    promptNotificationAccess()
                    powerSwitch.isChecked = false
                } else {
                    NotificationService.isServiceActive = true
                    updateStatusText()
                    tts.speak("Servicio de lectura activado", TextToSpeech.QUEUE_FLUSH, null, "switch_on")
                }
            } else {
                NotificationService.isServiceActive = false
                updateStatusText()
                tts.speak("Servicio de lectura desactivado", TextToSpeech.QUEUE_FLUSH, null, "switch_off")
            }
        }

        // Button to test service
        serviceButton.setOnClickListener {
            if (!isNotificationServiceEnabled()) {
                promptNotificationAccess()
            } else if (powerSwitch.isChecked) {
                Toast.makeText(this, "Probando servicio de voz...", Toast.LENGTH_SHORT).show()
                tts.speak("El servicio está funcionando correctamente y leerá tus notificaciones",
                    TextToSpeech.QUEUE_FLUSH, null, "test_id")
            } else {
                Toast.makeText(this, "Activa el servicio primero", Toast.LENGTH_SHORT).show()
            }
        }

        // Botón para configurar horario
        scheduleButton.setOnClickListener {
            try {
                val intent = Intent(this, ScheduleActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Error al abrir configuración: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }

        updateStatus()
    }

    private fun checkAndRequestPermissions() {
        val audioPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        val modifyAudioPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.MODIFY_AUDIO_SETTINGS)

        val permissionsNeeded = ArrayList<String>()

        if (audioPermission != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.RECORD_AUDIO)
        }

        if (modifyAudioPermission != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.MODIFY_AUDIO_SETTINGS)
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsNeeded.toTypedArray(),
                permissionRequestCode
            )
        }

        // Check notification access
        if (!isNotificationServiceEnabled()) {
            promptNotificationAccess()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == permissionRequestCode) {
            var allGranted = true

            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                    break
                }
            }

            if (allGranted) {
                Toast.makeText(this, "Todos los permisos concedidos", Toast.LENGTH_SHORT).show()
                // Reiniciar TTS para asegurar que funciona con los nuevos permisos
                tts.stop()
                tts.shutdown()
                tts = TextToSpeech(this, this)
            } else {
                Toast.makeText(
                    this,
                    "Se necesitan permisos de audio para el funcionamiento correcto",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun updateStatusText() {
        if (scheduleManager.isScheduleEnabled()) {
            val activeNow = scheduleManager.shouldServiceBeActive()
            if (activeNow && powerSwitch.isChecked) {
                statusText.text = "✅ Servicio activo según horario"
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
            } else if (!activeNow) {
                statusText.text = "⏰ Servicio en espera (fuera de horario)"
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_light))
            } else {
                statusText.text = "❌ Servicio inactivo: notificaciones silenciadas"
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
            }
            NotificationService.isServiceActive = activeNow && powerSwitch.isChecked
        } else {
            if (powerSwitch.isChecked) {
                statusText.text = "✅ Servicio activo: escuchando notificaciones"
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
            } else {
                statusText.text = "❌ Servicio inactivo: notificaciones silenciadas"
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
            }
        }
    }

    private fun updateStatus() {
        val notificationServiceEnabled = isNotificationServiceEnabled()

        if (notificationServiceEnabled) {
            powerSwitch.isEnabled = true

            // Restaurar el estado del switch si el servicio estaba activo
            powerSwitch.isChecked = NotificationService.isServiceActive

            // Verificar el estado según el horario programado
            updateStatusText()
        } else {
            statusText.text = "❌ Servicio no disponible: concede permisos"
            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
            powerSwitch.isChecked = false
            powerSwitch.isEnabled = false
            NotificationService.isServiceActive = false
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(pkgName)
    }

    private fun promptNotificationAccess() {
        AlertDialog.Builder(this)
            .setTitle("Permiso necesario")
            .setMessage("Esta app necesita acceso a tus notificaciones para funcionar. Por favor, activa el permiso para 'NotificacionesApp'.")
            .setPositiveButton("Configurar") { _, _ ->
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        try {
            val filter = IntentFilter(updateStatusAction)
            // Usar Intent con el paquete actual como destino (explícito)
            applicationContext.registerReceiver(statusReceiver, filter)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error al registrar receiver: ${e.message}")
        }
        updateStatus()
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(statusReceiver)
        } catch (e: Exception) {
            // Ignorar si el receptor no está registrado
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Set Spanish language
            val result = tts.setLanguage(Locale("es", "ES"))

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "El idioma español no está disponible", Toast.LENGTH_SHORT).show()
            } else {
                // Prueba del TTS cuando se inicializa correctamente
                tts.speak("Sistema de lectura de notificaciones iniciado", TextToSpeech.QUEUE_FLUSH, null, "init_id")
            }
        } else {
            Toast.makeText(this, "Error al inicializar TTS", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        // Shut down TTS
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }

    companion object {
        val updateStatusAction = "com.example.notificacionesapp.UPDATE_STATUS"
    }
}