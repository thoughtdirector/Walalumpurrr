package com.example.notificacionesapp

import android.Manifest
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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.notificacionesapp.databinding.ActivityMainRedesignedBinding
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityMainRedesignedBinding
    private lateinit var navController: NavController
    lateinit var tts: TextToSpeech
    private val permissionRequestCode = 123

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

        // Configurar la navegación
        setupNavigation()

        // Inicializar Text-to-Speech
        tts = TextToSpeech(this, this)

        // Verificar permisos
        checkAndRequestPermissions()
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
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Conectar la navegación inferior con el controlador de navegación
        binding.bottomNavigation.setupWithNavController(navController)

        // Configurar listener para manejar selección de item
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> navController.navigate(R.id.homeFragment)
                R.id.nav_schedule -> navController.navigate(R.id.scheduleFragment)
                R.id.nav_history -> navController.navigate(R.id.historyFragment)
                R.id.nav_settings -> navController.navigate(R.id.settingsFragment)
                R.id.nav_account -> navController.navigate(R.id.accountFragment)
            }
            true
        }
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
                // Reiniciar TTS para asegurar que funciona con los nuevos permisos
                tts.stop()
                tts.shutdown()
                tts = TextToSpeech(this, this)
            }
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
            val filterTheme = IntentFilter("com.example.notificacionesapp.THEME_CHANGED")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(themeChangeReceiver, filterTheme, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(themeChangeReceiver, filterTheme)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error al registrar receivers: ${e.message}")
        }
    }

    override fun onPause() {
        super.onPause()
        try {
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
                Log.e("MainActivity", getString(R.string.spanish_unavailable))
            } else {
                // Prueba del TTS cuando se inicializa correctamente
                tts.speak(getString(R.string.notification_reading_system_initialized), TextToSpeech.QUEUE_FLUSH, null, "init_id")
            }
        } else {
            Log.e("MainActivity", getString(R.string.tts_initialization_error))
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