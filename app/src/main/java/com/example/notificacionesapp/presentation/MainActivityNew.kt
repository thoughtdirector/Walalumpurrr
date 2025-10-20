package com.example.notificacionesapp.presentation

import android.Manifest
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.notificacionesapp.R
import com.example.notificacionesapp.core.auth.AuthManager
import com.example.notificacionesapp.core.auth.AuthState
import com.example.notificacionesapp.core.permissions.PermissionManager
import com.example.notificacionesapp.databinding.ActivityMainRedesignedBinding
import com.example.notificacionesapp.fragments.AccountFragment
import com.example.notificacionesapp.fragments.HistoryFragment
import com.example.notificacionesapp.fragments.HomeFragment
import com.example.notificacionesapp.fragments.ManageEmployeesFragment
import com.example.notificacionesapp.fragments.ProfileFragment
import com.example.notificacionesapp.fragments.ScheduleFragment
import com.example.notificacionesapp.fragments.SettingsFragment
import com.example.notificacionesapp.presentation.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * Main Activity with improved architecture
 * Uses MVVM pattern with dependency injection
 */
@AndroidEntryPoint
class MainActivityNew : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityMainRedesignedBinding
    private lateinit var tts: TextToSpeech

    @Inject
    lateinit var authManager: AuthManager

    @Inject
    lateinit var permissionManager: PermissionManager

    @Inject
    lateinit var authViewModel: AuthViewModel

    // Fragment management
    private var currentFragment: Fragment? = null
    private var homeFragment: HomeFragment? = null
    private var profileFragment: ProfileFragment? = null

    // Broadcast receivers
    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == updateStatusAction) {
                val serviceState = intent.getBooleanExtra("service_state", false)
                val scheduleActivated = intent.getBooleanExtra("schedule_activated", false)

                homeFragment?.let { fragment ->
                    fragment.updateServiceState(serviceState)
                    if (scheduleActivated) {
                        val message = if (serviceState) {
                            getString(R.string.service_activated_by_schedule)
                        } else {
                            getString(R.string.service_deactivated_by_schedule)
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                    fragment.updateScheduleInfo()
                }
            }
        }
    }

    private val themeChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.example.notificacionesapp.THEME_CHANGED") {
                val isDarkMode = intent.getBooleanExtra("dark_mode", false)
                AppCompatDelegate.setDefaultNightMode(
                    if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
                    else AppCompatDelegate.MODE_NIGHT_NO
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply theme before setContentView
        applyTheme()

        super.onCreate(savedInstanceState)
        binding = ActivityMainRedesignedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Text-to-Speech
        tts = TextToSpeech(this, this)

        // Check and request permissions
        permissionManager.checkAndRequestPermissions(this)

        // Setup navigation
        setupNavigation()

        // Observe authentication state
        observeAuthState()

        // Check authentication state
        if (savedInstanceState == null) {
            lifecycleScope.launch {
                authManager.checkAuthState()
            }
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
            Log.e("MainActivity", "Error applying theme: ${e.message}")
        }
    }

    private fun observeAuthState() {
        lifecycleScope.launch {
            authManager.authState.collect { authState ->
                when (authState) {
                    is AuthState.Loading -> {
                        // Show loading state if needed
                    }
                    is AuthState.Authenticated -> {
                        // User is authenticated
                        if (homeFragment == null) {
                            homeFragment = HomeFragment()
                            loadFragment(homeFragment!!)
                            binding.bottomNavigation.selectedItemId = R.id.nav_home
                        }
                    }
                    is AuthState.Unauthenticated -> {
                        // User is not authenticated
                        val accountFragment = AccountFragment()
                        loadFragment(accountFragment)
                        binding.bottomNavigation.selectedItemId = R.id.nav_account
                    }
                    is AuthState.Error -> {
                        // Handle authentication error
                        Toast.makeText(this@MainActivityNew, authState.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
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
                R.id.nav_account -> {
                    // Check if user is authenticated
                    lifecycleScope.launch {
                        authManager.authState.collect { authState ->
                            if (authState is AuthState.Authenticated) {
                                if (profileFragment == null) {
                                    profileFragment = ProfileFragment()
                                }
                                fragment = profileFragment
                            } else {
                                fragment = AccountFragment()
                            }
                        }
                    }
                }
            }

            if (fragment != null) {
                // Check user role for navigation restrictions
                lifecycleScope.launch {
                    authManager.currentUser.collect { user ->
                        if (user?.isEmployee() == true && 
                            item.itemId != R.id.nav_home && 
                            item.itemId != R.id.nav_account) {
                            Toast.makeText(this@MainActivityNew, "Acceso restringido", Toast.LENGTH_SHORT).show()
                            return@collect
                        }
                        loadFragment(fragment!!)
                    }
                }
                return@setOnItemSelectedListener true
            }

            false
        }
    }

    fun loadFragment(fragment: Fragment) {
        currentFragment = fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }

    fun loadManageEmployeesFragment() {
        val fragment = ManageEmployeesFragment()
        loadFragment(fragment)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PermissionManager.PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            if (allGranted) {
                Toast.makeText(this, getString(R.string.all_permissions_granted), Toast.LENGTH_SHORT).show()
                // Restart TTS to ensure it works with new permissions
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
        return permissionManager.isNotificationServiceEnabled(this)
    }

    fun promptNotificationAccess() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_required))
            .setMessage(getString(R.string.notification_access_message))
            .setPositiveButton(getString(R.string.configure)) { _, _ ->
                permissionManager.openNotificationListenerSettings(this)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    fun toggleNotificationService(enable: Boolean) {
        val intent = Intent(this, com.example.notificacionesapp.NotificationService::class.java)
        intent.action = if (enable) {
            com.example.notificacionesapp.NotificationService.ACTION_START_SERVICE
        } else {
            com.example.notificacionesapp.NotificationService.ACTION_STOP_SERVICE
        }

        try {
            if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }

            com.example.notificacionesapp.NotificationService.isServiceActive = enable

            if (!enable) {
                tts.speak(getString(R.string.service_deactivated), TextToSpeech.QUEUE_FLUSH, null, "switch_off")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error changing service state: ${e.message}")
            Toast.makeText(this, "Error changing service state", Toast.LENGTH_SHORT).show()
        }
    }

    fun testTTS(text: String) {
        if (::tts.isInitialized && tts != null) {
            try {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "test_id")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error testing TTS: ${e.message}")
            }
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
            Log.e("MainActivity", "Error registering receivers: ${e.message}")
        }

        // Update UI of home fragment if visible
        homeFragment?.updateUI()
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(statusReceiver)
            unregisterReceiver(themeChangeReceiver)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error unregistering receivers: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Set Spanish language
            val result = tts.setLanguage(Locale("es", "ES"))

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, getString(R.string.spanish_unavailable), Toast.LENGTH_SHORT).show()
            } else {
                // Test TTS when initialized correctly
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
        private const val TAG = "MainActivity"
    }
}
