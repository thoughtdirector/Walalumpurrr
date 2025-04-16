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
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.notificacionesapp.databinding.ActivityMainRedesignedBinding
import com.example.notificacionesapp.fragments.AccountFragment
import com.example.notificacionesapp.fragments.HistoryFragment
import com.example.notificacionesapp.fragments.HomeFragment
import com.example.notificacionesapp.fragments.ScheduleFragment
import com.example.notificacionesapp.fragments.SettingsFragment
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityMainRedesignedBinding
    lateinit var tts: TextToSpeech
    lateinit var scheduleManager: ScheduleManager
    private val permissionRequestCode = 123

    // Fragmento actual visible
    private var currentFragment: Fragment? = null
    private var homeFragment: HomeFragment? = null

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == updateStatusAction) {
                // Extraer información adicional del intent
                val serviceState = intent.getBooleanExtra("service_state", NotificationService.isServiceActive)
                val scheduleActivated = intent.getBooleanExtra("schedule_activated", false)

                // Notificar al homeFragment si está visible
                homeFragment?.let {
                    it.updateServiceState(serviceState)

                    if (scheduleActivated) {
                        // Mostrar un Toast informativo sobre la activación/desactivación por horario
                        val message = if (serviceState) {
                            getString(R.string.service_activated_by_schedule)
                        } else {
                            getString(R.string.service_deactivated_by_schedule)
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }

                    it.updateScheduleInfo()
                }
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
        binding = ActivityMainRedesignedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar ScheduleManager
        scheduleManager = ScheduleManager(this)

        // Inicializar Text-to-Speech
        tts = TextToSpeech(this, this)

        // Verificar permisos
        checkAndRequestPermissions()

        // Configurar la navegación
        setupNavigation()

        // Por defecto, mostrar el fragmento home
        if (savedInstanceState == null) {
            homeFragment = HomeFragment()
            loadFragment(homeFragment!!)
            binding.bottomNavigation.selectedItemId = R.id.nav_home
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

    private fun setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            var fragment: Fragment? = null

            when (item.itemId) {
                R.id.nav_home -> {
                    if (homeFragment == null) {
                        homeFragment = HomeFragment()
                    }
                    fragment = homeFragment
                }
                R.id.nav_schedule -> fragment = ScheduleFragment()
                R.id.nav_history -> fragment = HistoryFragment()
                R.id.nav_settings -> fragment = SettingsFragment()
                R.id.nav_account -> fragment = AccountFragment()
            }

            if (fragment != null) {
                loadFragment(fragment)
                return@setOnItemSelectedListener true
            }

            false
        }
    }

    private fun loadFragment(fragment: Fragment) {
        currentFragment = fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
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

    fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(pkgName)
    }

    fun promptNotificationAccess() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_required))
            .setMessage(getString(R.string.notification_access_message))
            .setPositiveButton(getString(R.string.configure)) { _, _ ->
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    // Método para ser llamado desde los fragmentos para activar/desactivar el servicio
    fun toggleNotificationService(enable: Boolean) {
        val intent = Intent(this, NotificationService::class.java)
        intent.action = if (enable) {
            NotificationService.ACTION_START_SERVICE
        } else {
            NotificationService.ACTION_STOP_SERVICE
        }

        try {
            if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }

            NotificationService.isServiceActive = enable

            if (!enable) {
                tts.speak(getString(R.string.service_deactivated), TextToSpeech.QUEUE_FLUSH, null, "switch_off")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error al cambiar estado del servicio: ${e.message}")
            Toast.makeText(this, "Error al cambiar estado del servicio", Toast.LENGTH_SHORT).show()
        }
    }

    // Método para probar el TTS
    fun testTTS(text: String) {
        if (::tts.isInitialized) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "test_id")
        }
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

        // Actualizar la UI del fragmento home si está visible
        homeFragment?.updateUI()
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