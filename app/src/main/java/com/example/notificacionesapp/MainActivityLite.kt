package com.example.notificacionesapp

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.notificacionesapp.databinding.ActivityMainLiteBinding
import com.example.notificacionesapp.fragments.HomeFragmentLite
import com.example.notificacionesapp.fragments.InfoFragmentLite
import com.example.notificacionesapp.fragments.SettingsFragmentLite
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivityLite : AppCompatActivity(), TextToSpeech.OnInitListener {

    lateinit var binding: ActivityMainLiteBinding
    lateinit var tts: TextToSpeech

    @Inject lateinit var sessionManager: SessionManager

    private val permissionRequestCode = 123

    private val ttsReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.example.notificacionesapp.TTS_SPEAK") {
                val text = intent.getStringExtra("text") ?: return
                if (::tts.isInitialized) {
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "svc_tts")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainLiteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tts = TextToSpeech(this, this)

        val filterTts = IntentFilter("com.example.notificacionesapp.TTS_SPEAK")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(ttsReceiver, filterTts, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(ttsReceiver, filterTts)
        }

        checkAndRequestPermissions()

        setupBottomNavigation()

        if (savedInstanceState == null) {
            loadFragment(HomeFragmentLite())
            binding.bottomNavigation.selectedItemId = R.id.nav_home
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_info -> {
                    loadFragment(InfoFragmentLite.newInstance())
                    true
                }
                R.id.nav_home -> {
                    loadFragment(HomeFragmentLite())
                    true
                }
                R.id.nav_settings -> {
                    loadFragment(SettingsFragmentLite.newInstance())
                    true
                }
                else -> false
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SCHEDULE_EXACT_ALARM)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.SCHEDULE_EXACT_ALARM)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), permissionRequestCode)
        }

        if (!isNotificationServiceEnabled()) {
            promptNotificationAccess()
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
            Log.e("MainActivityLite", "Error al cambiar estado del servicio: ${e.message}")
            Toast.makeText(this, "Error al cambiar estado del servicio", Toast.LENGTH_SHORT).show()
        }
    }

    fun testTTS(text: String) {
        if (::tts.isInitialized && tts != null) {
            try {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "test_id")
            } catch (e: Exception) {
                Log.e("MainActivityLite", "Error testing TTS: ${e.message}")
            }
        }
    }



    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Enrutar la voz por el stream de notificaciones para que el
            // deslizador de volumen del Home controle realmente el TTS.
            tts.setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )

            val result = tts.setLanguage(java.util.Locale("es", "ES"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, getString(R.string.spanish_unavailable), Toast.LENGTH_SHORT).show()
            }
            // No habla automáticamente al iniciar: el usuario no ha activado nada aún.
        } else {
            Toast.makeText(this, getString(R.string.tts_initialization_error), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        try { unregisterReceiver(ttsReceiver) } catch (_: Exception) {}
        super.onDestroy()
    }

    fun loadFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        private const val TAG = "MainActivityLite"
    }
}
