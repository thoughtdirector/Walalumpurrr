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
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.notificacionesapp.core.auth.AuthManager
import com.example.notificacionesapp.core.auth.AuthState
import com.example.notificacionesapp.core.domain.Result
import com.example.notificacionesapp.databinding.ActivityMainRedesignedBinding
import com.example.notificacionesapp.fragments.AccountFragment
import com.example.notificacionesapp.fragments.HistoryFragment
import com.example.notificacionesapp.fragments.HomeFragment
import com.example.notificacionesapp.fragments.ManageEmployeesFragment
import com.example.notificacionesapp.fragments.ProfileFragment
import com.example.notificacionesapp.fragments.ScheduleFragment
import com.example.notificacionesapp.fragments.SettingsFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    lateinit var binding: ActivityMainRedesignedBinding
    lateinit var tts: TextToSpeech
    lateinit var scheduleManager: ScheduleManager
    private val permissionRequestCode = 123

    @Inject lateinit var authManager: AuthManager
    @Inject lateinit var sessionManager: SessionManager

    private var currentFragment: Fragment? = null
    var homeFragment: HomeFragment? = null
    var profileFragment: ProfileFragment? = null

    var userRole: String? = null

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == updateStatusAction) {
                val serviceState = intent.getBooleanExtra("service_state", NotificationService.isServiceActive)
                val scheduleActivated = intent.getBooleanExtra("schedule_activated", false)

                homeFragment?.let {
                    it.updateServiceState(serviceState)

                    if (scheduleActivated) {
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
                AppCompatDelegate.setDefaultNightMode(
                    if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
                    else AppCompatDelegate.MODE_NIGHT_NO
                )
            }
        }
    }

    private val ttsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.example.notificacionesapp.TTS_SPEAK") {
                val text = intent.getStringExtra("text") ?: return
                Log.d("TTS_DEBUG", "[PASO6] Broadcast recibido en MainActivity: $text")
                if (::tts.isInitialized) {
                    val result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "svc_tts")
                    Log.d("TTS_DEBUG", "[PASO7] tts.speak() retornó: $result")
                    if (result != TextToSpeech.SUCCESS) {
                        Log.e("TTS_DEBUG", "[ERROR] tts.speak falló con código: $result")
                    }
                } else {
                    Log.e("TTS_DEBUG", "[ERROR] TTS no inicializado en MainActivity")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyTheme()

        super.onCreate(savedInstanceState)
        binding = ActivityMainRedesignedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        scheduleManager = ScheduleManager(this)
        tts = TextToSpeech(this, this)

        // Receiver para TTS desde NotificationService (funciona en background)
        val filterTts = IntentFilter("com.example.notificacionesapp.TTS_SPEAK")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(ttsReceiver, filterTts, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(ttsReceiver, filterTts)
        }

        checkAndRequestPermissions()
        setupNavigation()

        if (savedInstanceState == null) {
            checkAuthState()
        }
    }

    private fun checkAuthState() {
        lifecycleScope.launch {
            // Primero revisar Supabase — tiene prioridad sobre SharedPrefs
            authManager.checkAuthState()
            when (val state = authManager.authState.value) {
                is AuthState.Authenticated -> {
                    val user = state.user
                    userRole = user.role.name.lowercase()
                    sessionManager.createLoginSession(user.id, user.email, userRole!!, user.adminId)
                    Log.d(TAG, "Sesión activa en Supabase. UserID: ${user.id}, Role: $userRole")
                    homeFragment = HomeFragment()
                    loadFragment(homeFragment!!)
                    binding.bottomNavigation.selectedItemId = R.id.nav_home
                }
                else -> {
                    // Si Supabase no tiene sesión, revisar SharedPrefs como fallback
                    if (sessionManager.isLoggedIn()) {
                        val userId = sessionManager.getUserId()
                        userRole = sessionManager.getUserRole()
                        Log.d(TAG, "Sesión recuperada de SharedPrefs. UserID: $userId, Role: $userRole")
                        homeFragment = HomeFragment()
                        loadFragment(homeFragment!!)
                        binding.bottomNavigation.selectedItemId = R.id.nav_home
                    } else {
                        Log.d(TAG, "No hay sesión. Mostrando pantalla de login.")
                        val accountFragment = AccountFragment()
                        loadFragment(accountFragment)
                        binding.bottomNavigation.selectedItemId = R.id.nav_account
                    }
                }
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
                R.id.nav_account -> {
                    if (sessionManager.isLoggedIn()) {
                        if (profileFragment == null) {
                            profileFragment = ProfileFragment()
                        }
                        fragment = profileFragment
                    } else {
                        fragment = AccountFragment()
                    }
                }
            }

            if (fragment != null) {
                loadFragment(fragment)
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
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                permissionRequestCode
            )
        }

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
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            if (allGranted) {
                Toast.makeText(this, getString(R.string.all_permissions_granted), Toast.LENGTH_SHORT).show()
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
            Log.e("MainActivity", "Error al registrar receivers: ${e.message}")
        }

        homeFragment?.updateUI()
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(statusReceiver)
            unregisterReceiver(themeChangeReceiver)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error al desregistrar receivers: ${e.message}")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("es", "ES"))

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, getString(R.string.spanish_unavailable), Toast.LENGTH_SHORT).show()
            } else {
                tts.speak(getString(R.string.notification_reading_system_initialized), TextToSpeech.QUEUE_FLUSH, null, "init_id")
            }
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

    fun createUserSession(userId: String, email: String, role: String, adminId: String? = null) {
        sessionManager.createLoginSession(userId, email, role, adminId)
        userRole = role
        setupNavigation()
    }

    fun logoutUser() {
        lifecycleScope.launch {
            authManager.signOut()
            sessionManager.logoutUser()
            userRole = null

            Toast.makeText(this@MainActivity, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()

            homeFragment = HomeFragment()
            loadFragment(homeFragment!!)
            binding.bottomNavigation.selectedItemId = R.id.nav_home
        }
    }

    fun createEmployeeAccount(
        email: String,
        firstName: String,
        lastName: String,
        phone: String,
        birthDate: String
    ) {
        showAdminPasswordDialog(email, firstName, lastName, phone, birthDate)
    }

    private fun showAdminPasswordDialog(
        email: String,
        firstName: String,
        lastName: String,
        phone: String,
        birthDate: String
    ) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirmar Contraseña de Administrador")
        builder.setMessage("Para crear la cuenta del empleado, necesitamos confirmar tu contraseña de administrador:")

        val input = android.widget.EditText(this)
        input.hint = "Contraseña de administrador"
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        builder.setView(input)

        builder.setPositiveButton("Crear Empleado") { dialog, _ ->
            val adminPassword = input.text.toString()
            if (adminPassword.isNotEmpty()) {
                createEmployeeWithPasswordConfirmation(email, firstName, lastName, phone, birthDate, adminPassword)
            } else {
                Toast.makeText(this, "Debes ingresar tu contraseña de administrador", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun createEmployeeWithPasswordConfirmation(
        email: String,
        firstName: String,
        lastName: String,
        phone: String,
        birthDate: String,
        adminPassword: String
    ) {
        val password = generateRandomPassword()

        val adminSessionData = sessionManager.getUserDetails()
        val adminUid = adminSessionData[SessionManager.KEY_USER_ID]
        val adminEmail = adminSessionData[SessionManager.KEY_USER_EMAIL]
        val adminRole = adminSessionData[SessionManager.KEY_USER_ROLE]

        lifecycleScope.launch {
            val result = authManager.createEmployeeAccount(
                email, password, firstName, lastName, phone, birthDate, adminUid
            )

            if (result is Result.Success) {
                restoreAdminSession(adminEmail, adminPassword, adminSessionData)
                showEmployeeCredentials(email, password)
            } else {
                restoreAdminSession(adminEmail, adminPassword, adminSessionData)
                val errorMsg = (result as? Result.Error)?.exception?.message ?: "Error al crear la cuenta del empleado"
                Toast.makeText(this@MainActivity, errorMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun restoreAdminSession(
        adminEmail: String?,
        adminPassword: String,
        adminSessionData: HashMap<String, String?>
    ) {
        if (adminEmail != null) {
            authManager.signOut()
            val signInResult = authManager.signInWithEmailAndPassword(adminEmail, adminPassword)
            if (signInResult is Result.Success) {
                val adminUid = adminSessionData[SessionManager.KEY_USER_ID]
                val adminRole = adminSessionData[SessionManager.KEY_USER_ROLE]
                val adminId = adminSessionData[SessionManager.KEY_ADMIN_ID]

                if (adminUid != null) {
                    sessionManager.createLoginSession(adminUid, adminEmail, adminRole ?: "admin", adminId)
                    userRole = adminRole
                    setupNavigation()
                    currentFragment?.let { fragment -> loadFragment(fragment) }
                }
            } else {
                Log.e(TAG, "Failed to restore admin session: ${(signInResult as? Result.Error)?.exception?.message}")
                Toast.makeText(this@MainActivity, "Error al restaurar sesión de administrador", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEmployeeCredentials(email: String, password: String) {
        val message = "Email: $email\nContraseña: $password\n\n¡Guarda estas credenciales de forma segura y comunícaselas al empleado!"

        AlertDialog.Builder(this)
            .setTitle("Credenciales del Empleado")
            .setMessage(message)
            .setPositiveButton("Copiar Email") { dialog, _ ->
                copyToClipboard("Email del Empleado", email)
                dialog.dismiss()
            }
            .setNeutralButton("Copiar Contraseña") { dialog, _ ->
                copyToClipboard("Contraseña del Empleado", password)
                dialog.dismiss()
            }
            .setNegativeButton("Copiar Todo") { dialog, _ ->
                copyToClipboard("Credenciales del Empleado", "Email: $email\nContraseña: $password")
                dialog.dismiss()
            }
            .show()
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "$label copiado al portapapeles", Toast.LENGTH_SHORT).show()
    }

    private fun generateRandomPassword(length: Int = 12): String {
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (0 until length).map { allowedChars.random() }.joinToString("")
    }

    companion object {
        val updateStatusAction = "com.example.notificacionesapp.UPDATE_STATUS"
        private const val TAG = "MainActivity"
    }
}
