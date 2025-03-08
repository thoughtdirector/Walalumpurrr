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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var statusText: TextView
    private lateinit var serviceButton: MaterialButton
    private lateinit var powerSwitch: SwitchMaterial
    private lateinit var scheduleButton: MaterialButton
    private lateinit var tts: TextToSpeech
    private lateinit var scheduleManager: ScheduleManager
    private lateinit var descriptionText: TextView // Reutilizar este TextView existente

    private val permissionRequestCode = 123

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == updateStatusAction) {
                // Extraer información adicional del intent
                val serviceState = intent.getBooleanExtra("service_state", NotificationService.isServiceActive)
                val scheduleActivated = intent.getBooleanExtra("schedule_activated", false)

                // Actualizar la UI basado en el estado recibido
                updateSwitchWithoutTrigger(serviceState)

                if (scheduleActivated) {
                    // Mostrar un Toast informativo sobre la activación/desactivación por horario
                    val message = if (serviceState) {
                        "Servicio activado por horario programado"
                    } else {
                        "Servicio desactivado por horario programado"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }

                updateStatus()
                updateNextScheduleInfo()
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
        descriptionText = findViewById(R.id.descriptionText)

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
        updateNextScheduleInfo()
    }

    private fun updateNextScheduleInfo() {
        if (scheduleManager.isScheduleEnabled()) {
            val startTime = formatTime(scheduleManager.getStartHour(), scheduleManager.getStartMinute())
            val endTime = formatTime(scheduleManager.getEndHour(), scheduleManager.getEndMinute())

            val nextEvent = scheduleManager.getNextScheduledEvent()
            val nextEventTime = if (nextEvent != null) {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                dateFormat.format(nextEvent.time)
            } else {
                "No programado"
            }

            val daysString = buildEnabledDaysString()

            descriptionText.text = String.format(
                Locale.getDefault(),
                "Horario activo: %s a %s\nDías: %s\nPróxima actualización: %s",
                startTime, endTime, daysString, nextEventTime
            )
        } else {
            descriptionText.text = "Esta aplicación lee en voz alta las notificaciones de apps como Nequi y WhatsApp cuando las recibes."
        }
    }

    private fun buildEnabledDaysString(): String {
        val days = mutableListOf<String>()
        val dayNames = arrayOf("", "Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb")

        for (i in Calendar.SUNDAY..Calendar.SATURDAY) {
            if (scheduleManager.isDayEnabled(i)) {
                days.add(dayNames[i])
            }
        }

        return days.joinToString(", ")
    }

    private fun formatTime(hour: Int, minute: Int): String {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }

    private fun updateSwitchWithoutTrigger(checked: Boolean) {
        var ignoreNextChange = true

        val currentState = powerSwitch.isChecked

        if (currentState != checked) {
            // Establecer el nuevo estado
            powerSwitch.isChecked = checked
        }
    }

    private fun checkAndRequestPermissions() {
        val audioPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        val modifyAudioPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.MODIFY_AUDIO_SETTINGS)
        val notificationPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            PackageManager.PERMISSION_GRANTED
        }

        val permissionsNeeded = ArrayList<String>()

        if (audioPermission != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.RECORD_AUDIO)
        }

        if (modifyAudioPermission != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.MODIFY_AUDIO_SETTINGS)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            notificationPermission != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
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
            updateSwitchWithoutTrigger(NotificationService.isServiceActive)

            // Verificar el estado según el horario programado
            updateStatusText()
        } else {
            statusText.text = "❌ Servicio no disponible: concede permisos"
            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
            updateSwitchWithoutTrigger(false)
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

            // Usar un enfoque alternativo: Intent explícito (específico de la aplicación)
            // Esto evita la necesidad de flags de exportación
            registerReceiver(statusReceiver, filter)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error al registrar receiver: ${e.message}")
        }
        updateStatus()
        updateNextScheduleInfo()
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