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
import com.example.notificacionesapp.fragments.ManageEmployeesFragment
import com.example.notificacionesapp.fragments.ProfileFragment
import com.example.notificacionesapp.fragments.ScheduleFragment
import com.example.notificacionesapp.fragments.SettingsFragment
import com.example.notificacionesapp.util.NotificationCleanupWorker
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import com.example.notificacionesapp.model.FirestoreUser
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.messaging.FirebaseMessaging
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    lateinit var binding: ActivityMainRedesignedBinding
    lateinit var tts: TextToSpeech
    lateinit var scheduleManager: ScheduleManager
    lateinit var sessionManager: SessionManager
    private val permissionRequestCode = 123

    // Fragmento actual visible
    private var currentFragment: Fragment? = null
    var homeFragment: HomeFragment? = null
    var profileFragment: ProfileFragment? = null

    // Firebase Auth instance
    private lateinit var auth: FirebaseAuth

    // User role
    public var userRole: String? = null

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

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Inicializar SessionManager
        sessionManager = SessionManager(this)

        // Inicializar ScheduleManager
        scheduleManager = ScheduleManager(this)

        // Inicializar Text-to-Speech
        tts = TextToSpeech(this, this)

        // Verificar permisos
        checkAndRequestPermissions()

        // Configurar la navegación
        setupNavigation()

        // Verificar si ya hay sesión activa
        if (savedInstanceState == null) {
            checkAuthState()
        }

        updateFCMToken()

        scheduleNotificationCleanup()

        if (intent.getBooleanExtra("openHistoryTab", false)) {
            val historyFragment = HistoryFragment()
            loadFragment(historyFragment)
            binding.bottomNavigation.selectedItemId = R.id.nav_history
        }
    }

    private fun checkAuthState() {
        // Primero verificar si hay sesión guardada en preferencias
        if (sessionManager.isLoggedIn()) {
            // Existe sesión guardada, recuperar datos
            val userId = sessionManager.getUserId()
            userRole = sessionManager.getUserRole()

            Log.d(TAG, "Sesión recuperada. UserID: $userId, Role: $userRole")

            homeFragment = HomeFragment()
            loadFragment(homeFragment!!)
            binding.bottomNavigation.selectedItemId = R.id.nav_home

            // Verificar si también está autenticado en Firebase
            if (auth.currentUser == null || auth.currentUser?.uid != userId) {
                // No está autenticado en Firebase, pero tiene sesión local
                // Esta situación podría ocurrir si la sesión en Firebase expiró
                Log.w(TAG, "Sesión local activa pero no hay sesión en Firebase. Cerrando sesión.")
                Toast.makeText(this, "Tu sesión ha expirado. Por favor, inicia sesión nuevamente.",
                    Toast.LENGTH_LONG).show()
                sessionManager.logoutUser()

                val accountFragment = AccountFragment()
                loadFragment(accountFragment)
                binding.bottomNavigation.selectedItemId = R.id.nav_account
            }
        } else {
            // No hay sesión guardada, verificar Firebase
            val currentUser = auth.currentUser
            if (currentUser != null) {
                // Usuario autenticado en Firebase pero no tiene sesión local
                Log.d(TAG, "Usuario autenticado en Firebase: ${currentUser.email}")
                getUserRole(currentUser.uid)

                homeFragment = HomeFragment()
                loadFragment(homeFragment!!)
                binding.bottomNavigation.selectedItemId = R.id.nav_home
            } else {
                // No hay ninguna sesión, cargar vista principal
                Log.d(TAG, "No hay sesión activa")
                homeFragment = HomeFragment()
                loadFragment(homeFragment!!)
                binding.bottomNavigation.selectedItemId = R.id.nav_home
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
                    // Si ya está autenticado, ir a página de perfil en lugar de login
                    if (auth.currentUser != null || sessionManager.isLoggedIn()) {
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
                // Restrict navigation for employees
                if (userRole == "employee" && item.itemId != R.id.nav_home && item.itemId != R.id.nav_account) {
                    Toast.makeText(this, "Acceso restringido", Toast.LENGTH_SHORT).show()
                    return@setOnItemSelectedListener false
                }
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

    private fun scheduleNotificationCleanup() {
        try {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresDeviceIdle(true)
                .build()

            val cleanupWorkRequest = androidx.work.PeriodicWorkRequestBuilder<NotificationCleanupWorker>(
                7, java.util.concurrent.TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .build()

            androidx.work.WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(
                    NotificationCleanupWorker.WORK_NAME,
                    androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                    cleanupWorkRequest
                )
        } catch (e: Exception) {
            Log.e("MainActivity", "Error al programar limpieza: ${e.message}")
        }
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

    private fun updateFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            val userId = auth.currentUser?.uid ?: sessionManager.getUserId() ?: return@addOnCompleteListener

            // Update token in Firestore
            Firebase.firestore.collection("users")
                .document(userId)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d(TAG, "FCM token updated in Firestore")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error updating FCM token in Firestore: ${e.message}")
                }
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

    fun getUserRole(uid: String) {
        // Get role from Firestore
        Firebase.firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    userRole = document.getString("role")
                    Log.d(TAG, "User role from Firestore: $userRole")

                    // Save role in SessionManager
                    userRole?.let {
                        sessionManager.updateUserRole(it)
                    }

                    setupNavigation()
                } else {
                    Log.d(TAG, "No such user document")
                    userRole = "employee" // Default role
                    sessionManager.updateUserRole(userRole ?: "employee")
                    setupNavigation()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting user role from Firestore: ${e.message}")
                userRole = "employee" // Default on error
                sessionManager.updateUserRole(userRole ?: "employee")
                setupNavigation()
            }
    }
    // Método para crear sesión local (llamado desde AccountFragment)
    fun createUserSession(userId: String, email: String, role: String) {
        sessionManager.createLoginSession(userId, email, role)
        userRole = role
        setupNavigation()
        updateFCMToken()
    }

    // Método para cerrar sesión
    fun logoutUser() {
        // Cerrar sesión de Firebase
        auth.signOut()

        // Cerrar sesión local
        sessionManager.logoutUser()

        userRole = null

        Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()

        // Redirigir a página de inicio
        homeFragment = HomeFragment()
        loadFragment(homeFragment!!)
        binding.bottomNavigation.selectedItemId = R.id.nav_home
    }

    fun createEmployeeAccount(
        email: String,
        firstName: String,
        lastName: String,
        phone: String,
        birthDate: String
    ) {
        val password = generateRandomPassword()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "createEmployeeAccount:success")
                    val user = auth.currentUser
                    val adminUid = auth.currentUser?.uid

                    user?.uid?.let { employeeUid ->
                        // Create user for Firestore
                        val firestoreUser = FirestoreUser(
                            uid = employeeUid,
                            firstName = firstName,
                            lastName = lastName,
                            phone = phone,
                            birthDate = birthDate,
                            email = email,
                            role = "employee",
                            adminId = adminUid
                        )

                        // Save to Firestore
                        Firebase.firestore.collection("users")
                            .document(employeeUid)
                            .set(firestoreUser)
                            .addOnSuccessListener {
                                Log.d(TAG, "Employee data written to Firestore")
                                showEmployeeCredentials(email, password)
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error writing employee data to Firestore", e)
                                Toast.makeText(this, "Error al guardar los datos del empleado.", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Log.w(TAG, "createEmployeeAccount:failure", task.exception)
                    val errorCode = (task.exception as? FirebaseAuthException)?.errorCode
                    val errorMessage = when (errorCode) {
                        "ERROR_EMAIL_ALREADY_IN_USE" -> "Este correo electrónico ya está en uso."
                        "ERROR_INVALID_EMAIL" -> "El correo electrónico no es válido."
                        "ERROR_WEAK_PASSWORD" -> "La contraseña es demasiado débil."
                        else -> "Error al crear la cuenta del empleado: ${task.exception?.message}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Helper function to generate a random password
    private fun generateRandomPassword(length: Int = 12): String {
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (0 until length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    // Helper function to display employee credentials
    private fun showEmployeeCredentials(email: String, password: String) {
        AlertDialog.Builder(this)
            .setTitle("Credenciales del Empleado")
            .setMessage("Email: $email\nContraseña: $password\n\n¡Guarda estas credenciales de forma segura y comunícaselas al empleado!")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    companion object {
        val updateStatusAction = "com.example.notificacionesapp.UPDATE_STATUS"
        private const val TAG = "MainActivity"
    }
}