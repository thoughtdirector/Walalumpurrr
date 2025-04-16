package com.example.notificacionesapp

import android.Manifest
import android.animation.AnimatorInflater
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var statusText: TextView
    private lateinit var activeTimeText: TextView
    private lateinit var nextUpdateText: TextView
    private lateinit var nextScheduleInfoText: TextView
    private lateinit var serviceButton: MaterialButton
    private lateinit var powerSwitch: SwitchMaterial
    private lateinit var scheduleButton: MaterialButton
    private lateinit var appSettingsButton: MaterialButton
    private lateinit var daysChipGroup: ChipGroup
    private lateinit var statusIcon: ImageView
    private lateinit var appIcon: ImageView
    private lateinit var tts: TextToSpeech
    private lateinit var scheduleManager: ScheduleManager

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
                        getString(R.string.service_activated_by_schedule)
                    } else {
                        getString(R.string.service_deactivated_by_schedule)
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }

                updateStatus()
                updateNextScheduleInfo()
            }
        }
    }

    private val themeChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.example.notificacionesapp.THEME_CHANGED") {
                val isDarkMode = intent.getBooleanExtra("dark_mode", false)
                // Aplicar el tema sin reiniciar la actividad
                AppCompatDelegate.setDefaultNightMode(
                    if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
                    else AppCompatDelegate.MODE_NIGHT_NO
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplicar tema antes de setContentView
        applyTheme()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar vistas
        initViews()

        // Aplicar animación al ícono
        applyIconAnimation()

        // Inicializar ScheduleManager
        scheduleManager = ScheduleManager(this)

        // Inicializar Text-to-Speech
        tts = TextToSpeech(this, this)

        // Verificar permisos
        checkAndRequestPermissions()

        // Configurar el switch de encendido
        setupPowerSwitch()

        // Configurar botones
        setupButtons()

        // Actualizar información en la UI
        updateStatus()
        updateNextScheduleInfo()
        updateDaysChips()
    }

    private fun initViews() {
        statusText = findViewById(R.id.statusText)
        activeTimeText = findViewById(R.id.activeTimeText)
        nextUpdateText = findViewById(R.id.nextUpdateText)
        nextScheduleInfoText = findViewById(R.id.nextScheduleInfoText)
        serviceButton = findViewById(R.id.serviceButton)
        powerSwitch = findViewById(R.id.powerSwitch)
        scheduleButton = findViewById(R.id.scheduleButton)
        appSettingsButton = findViewById(R.id.appSettingsButton)
        daysChipGroup = findViewById(R.id.daysChipGroup)
        statusIcon = findViewById(R.id.statusIcon)
        appIcon = findViewById(R.id.appIcon)
    }

    private fun applyIconAnimation() {
        try {
            val pulseAnimator = AnimatorInflater.loadAnimator(this, R.animator.pulse_animation)
            pulseAnimator.setTarget(appIcon)
            pulseAnimator.start()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error al aplicar animación: ${e.message}")
        }
    }

    private fun applyTheme() {
        try {
            val themePrefs = getSharedPreferences("theme_settings", Context.MODE_PRIVATE)
            val isDarkMode = themePrefs.getBoolean("dark_mode", false)

            AppCompatDelegate.setDefaultNightMode(
                if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        } catch (e: Exception) {
            Log.e("MainActivity", "Error al aplicar tema: ${e.message}")
        }
    }

    private fun setupPowerSwitch() {
        powerSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!isNotificationServiceEnabled()) {
                    promptNotificationAccess()
                    powerSwitch.isChecked = false
                } else {
                    // Iniciar servicio explícitamente
                    val intent = Intent(this, NotificationService::class.java)
                    intent.action = NotificationService.ACTION_START_SERVICE

                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(intent)
                        } else {
                            startService(intent)
                        }

                        NotificationService.isServiceActive = true
                        updateStatusText()
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error al iniciar servicio: ${e.message}")
                        Toast.makeText(this, "Error al iniciar servicio", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Detener servicio explícitamente
                val intent = Intent(this, NotificationService::class.java)
                intent.action = NotificationService.ACTION_STOP_SERVICE

                try {
                    startService(intent)

                    NotificationService.isServiceActive = false
                    updateStatusText()
                    tts.speak(getString(R.string.service_deactivated), TextToSpeech.QUEUE_FLUSH, null, "switch_off")
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error al detener servicio: ${e.message}")
                }
            }
        }
    }
    private fun setupButtons() {
        findViewById<MaterialButton>(R.id.historyButton).setOnClickListener {
            try {
                val intent = Intent(this, HistoryActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Error al abrir historial: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }

        // Botón para probar servicio
        serviceButton.setOnClickListener {
            if (!isNotificationServiceEnabled()) {
                promptNotificationAccess()
            } else if (powerSwitch.isChecked) {
                Toast.makeText(this, getString(R.string.testing_voice_service), Toast.LENGTH_SHORT).show()
                tts.speak(getString(R.string.service_working_correctly),
                    TextToSpeech.QUEUE_FLUSH, null, "test_id")
            } else {
                Toast.makeText(this, getString(R.string.activate_service_first), Toast.LENGTH_SHORT).show()
            }
        }

        // Botón para configurar horario
        scheduleButton.setOnClickListener {
            try {
                val intent = Intent(this, ScheduleActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.error_opening_config, e.message), Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }

        // Botón para configuración de apps
        appSettingsButton.setOnClickListener {
            try {
                val intent = Intent(this, AppSettingsActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Error al abrir configuración de apps: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
        findViewById<MaterialButton>(R.id.amountSettingsButton).setOnClickListener {
            try {
                val intent = Intent(this, AmountSettingsActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Error al abrir configuración de montos: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }

    private fun updateDaysChips() {
        // No permitimos la interacción directa con los chips en MainActivity
        // Solo los mostramos con el estado activo/inactivo según la configuración

        val chipLun = findViewById<Chip>(R.id.chipLun)
        val chipMar = findViewById<Chip>(R.id.chipMar)
        val chipMie = findViewById<Chip>(R.id.chipMie)
        val chipJue = findViewById<Chip>(R.id.chipJue)
        val chipVie = findViewById<Chip>(R.id.chipVie)
        val chipSab = findViewById<Chip>(R.id.chipSab)
        val chipDom = findViewById<Chip>(R.id.chipDom)

        // Actualizar el estado de los chips según la configuración actual
        chipLun.isChecked = scheduleManager.isDayEnabled(Calendar.MONDAY)
        chipMar.isChecked = scheduleManager.isDayEnabled(Calendar.TUESDAY)
        chipMie.isChecked = scheduleManager.isDayEnabled(Calendar.WEDNESDAY)
        chipJue.isChecked = scheduleManager.isDayEnabled(Calendar.THURSDAY)
        chipVie.isChecked = scheduleManager.isDayEnabled(Calendar.FRIDAY)
        chipSab.isChecked = scheduleManager.isDayEnabled(Calendar.SATURDAY)
        chipDom.isChecked = scheduleManager.isDayEnabled(Calendar.SUNDAY)

        // Desactivar la interacción con los chips (solo informativos)
        chipLun.isClickable = false
        chipMar.isClickable = false
        chipMie.isClickable = false
        chipJue.isClickable = false
        chipVie.isClickable = false
        chipSab.isClickable = false
        chipDom.isClickable = false
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        // Permisos necesarios para Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SCHEDULE_EXACT_ALARM)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.SCHEDULE_EXACT_ALARM)
            }
        }

        // Permisos necesarios para Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Verificar otros permisos necesarios
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.MODIFY_AUDIO_SETTINGS)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.MODIFY_AUDIO_SETTINGS)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
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
                Toast.makeText(this, getString(R.string.all_permissions_granted), Toast.LENGTH_SHORT).show()
                // Reiniciar TTS para asegurar que funciona con los nuevos permisos
                tts.stop()
                tts.shutdown()
                tts = TextToSpeech(this, this)
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.audio_permissions_needed),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun updateStatusText() {
        try {
            if (scheduleManager.isScheduleEnabled()) {
                val activeNow = scheduleManager.shouldServiceBeActive()
                if (activeNow && powerSwitch.isChecked) {
                    statusText.text = getString(R.string.service_active_scheduled)
                    statusText.setTextColor(ContextCompat.getColor(this, R.color.status_active))
                    statusIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_active))
                } else if (!activeNow) {
                    statusText.text = getString(R.string.service_waiting)
                    statusText.setTextColor(ContextCompat.getColor(this, R.color.status_waiting))
                    statusIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_waiting))
                } else {
                    statusText.text = getString(R.string.service_inactive)
                    statusText.setTextColor(ContextCompat.getColor(this, R.color.status_inactive))
                    statusIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_inactive))
                }
                NotificationService.isServiceActive = activeNow && powerSwitch.isChecked
            } else {
                if (powerSwitch.isChecked) {
                    statusText.text = getString(R.string.service_active_listening)
                    statusText.setTextColor(ContextCompat.getColor(this, R.color.status_active))
                    statusIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_active))
                } else {
                    statusText.text = getString(R.string.service_inactive)
                    statusText.setTextColor(ContextCompat.getColor(this, R.color.status_inactive))
                    statusIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_inactive))
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error en updateStatusText: ${e.message}")
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
            statusText.text = getString(R.string.service_unavailable)
            statusText.setTextColor(ContextCompat.getColor(this, R.color.status_inactive))
            statusIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_inactive))
            updateSwitchWithoutTrigger(false)
            powerSwitch.isEnabled = false
            NotificationService.isServiceActive = false
        }
    }

    private fun updateNextScheduleInfo() {
        if (scheduleManager.isScheduleEnabled()) {
            val startTime = formatTime(scheduleManager.getStartHour(), scheduleManager.getStartMinute())
            val endTime = formatTime(scheduleManager.getEndHour(), scheduleManager.getEndMinute())

            activeTimeText.text = "$startTime a $endTime"

            val nextEvent = scheduleManager.getNextScheduledEvent()
            val nextEventTime = if (nextEvent != null) {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                dateFormat.format(nextEvent.time)
            } else {
                getString(R.string.no_scheduled)
            }

            nextUpdateText.text = nextEventTime

            // Actualizar el texto detallado
            nextScheduleInfoText.text = "Próxima actualización: $nextEventTime"
        } else {
            activeTimeText.text = "No programado"
            nextUpdateText.text = "No programado"
            nextScheduleInfoText.text = ""
        }

        // Actualizar el estado de los chips de días
        updateDaysChips()
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
        val currentState = powerSwitch.isChecked

        if (currentState != checked) {
            // Desactivar temporalmente el listener
            powerSwitch.setOnCheckedChangeListener(null)

            // Establecer el nuevo estado
            powerSwitch.isChecked = checked

            // Reactivar el listener original
            setupPowerSwitch()
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(pkgName)
    }

    private fun promptNotificationAccess() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_required))
            .setMessage(getString(R.string.notification_access_message))
            .setPositiveButton(getString(R.string.configure)) { _, _ ->
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        try {
            val filterStatus = IntentFilter(updateStatusAction)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(statusReceiver, filterStatus, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(statusReceiver, filterStatus)
            }

            val filterTheme = IntentFilter("com.example.notificacionesapp.THEME_CHANGED")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(themeChangeReceiver, filterTheme, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(themeChangeReceiver, filterTheme)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error al registrar receivers: ${e.message}")
        }
        updateStatus()
        updateNextScheduleInfo()
        updateDaysChips()
    }




    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(statusReceiver)
            unregisterReceiver(themeChangeReceiver)
        } catch (e: Exception) {
            // Ignorar si los receptores no están registrados
            Log.e("MainActivity", "Error al desregistrar receivers: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Set Spanish language
            val result = tts.setLanguage(Locale("es", "ES"))

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, getString(R.string.spanish_unavailable), Toast.LENGTH_SHORT).show()
            } else {
                // Prueba del TTS cuando se inicializa correctamente
                tts.speak(getString(R.string.notification_reading_system_initialized), TextToSpeech.QUEUE_FLUSH, null, "init_id")
            }
        } else {
            Toast.makeText(this, getString(R.string.tts_initialization_error), Toast.LENGTH_SHORT).show()
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