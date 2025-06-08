package com.example.notificacionesapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.notificacionesapp.notification.NotificationProcessorRegistry
import com.example.notificacionesapp.util.AmountSettings
import com.example.notificacionesapp.util.NotificationHistoryManager
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class NotificationService : NotificationListenerService() {

    private var tts: TextToSpeech? = null
    private var lastNotification = ""
    private val appSettings = ConcurrentHashMap<String, Boolean>()
    private val FOREGROUND_SERVICE_ID = 1001
    private val NOTIFICATION_CHANNEL_ID = "notification_service_channel"
    private var ttsInitialized = false
    private lateinit var notificationHistoryManager: NotificationHistoryManager
    private var processorRegistry: NotificationProcessorRegistry? = null
    private lateinit var amountSettings: AmountSettings
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        var isServiceActive = false
        const val ACTION_START_SERVICE = "com.example.notificacionesapp.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.notificacionesapp.STOP_SERVICE"
        const val ACTION_UPDATE_APP_SETTINGS = "com.example.notificacionesapp.UPDATE_APP_SETTINGS"
        private const val TAG = "NotificationService"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        try {
            // Initialize components with error handling
            notificationHistoryManager = NotificationHistoryManager(this)
            processorRegistry = NotificationProcessorRegistry(applicationContext, notificationHistoryManager)
            amountSettings = AmountSettings(this)

            initializeTTS()
            loadAppSettings()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
        }
    }

    private fun initializeTTS() {
        try {
            // Configure audio to reasonable level
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

            // Only adjust if volume is too low
            if (currentVolume < maxVolume / 3) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume / 2, 0)
            }

            // Initialize TTS safely
            tts = TextToSpeech(applicationContext) { status ->
                mainHandler.post {
                    handleTTSInitialization(status)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception initializing TTS: ${e.message}", e)
            ttsInitialized = false
        }
    }

    private fun handleTTSInitialization(status: Int) {
        try {
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("es", "ES"))

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Spanish language not supported")
                    ttsInitialized = false
                } else {
                    Log.d(TAG, "TTS initialized successfully")
                    ttsInitialized = true

                    // Test TTS after initialization with delay
                    mainHandler.postDelayed({
                        if (isServiceActive && ttsInitialized) {
                            speakOut("Servicio de lectura de notificaciones activado")
                        }
                    }, 1000)
                }
            } else {
                Log.e(TAG, "TTS initialization failed with status: $status")
                ttsInitialized = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling TTS initialization: ${e.message}", e)
            ttsInitialized = false
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_STICKY

        try {
            when (action) {
                ACTION_START_SERVICE -> {
                    isServiceActive = true
                    startForeground()
                    Log.d(TAG, "Service activated explicitly")

                    // Only speak if TTS is ready
                    if (ttsInitialized) {
                        mainHandler.postDelayed({
                            speakOut("Servicio de lectura activado")
                        }, 500)
                    }
                }
                ACTION_STOP_SERVICE -> {
                    isServiceActive = false
                    stopForegroundService()
                    Log.d(TAG, "Service deactivated explicitly")

                    // Only speak if TTS is ready
                    if (ttsInitialized) {
                        speakOut("Servicio de lectura desactivado")
                    }
                }
                ACTION_UPDATE_APP_SETTINGS -> {
                    loadAppSettings()
                    Log.d(TAG, "App settings updated")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStartCommand: ${e.message}", e)
        }

        return START_STICKY
    }

    private fun startForeground() {
        try {
            createNotificationChannel()

            val notificationIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Servicio de Notificaciones Activo")
                .setContentText("Leyendo notificaciones en segundo plano")
                .setSmallIcon(R.drawable.notification_icon)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build()

            startForeground(FOREGROUND_SERVICE_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error in startForeground: ${e.message}", e)
        }
    }

    private fun stopForegroundService() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping foreground service: ${e.message}", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val serviceChannel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Servicio de Notificaciones",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Canal para el servicio de lectura de notificaciones"
                    setShowBadge(false)
                    enableVibration(false)
                    setSound(null, null)
                }

                val manager = getSystemService(NotificationManager::class.java)
                manager?.createNotificationChannel(serviceChannel)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating notification channel: ${e.message}", e)
            }
        }
    }

    private fun loadAppSettings() {
        try {
            val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            appSettings.clear()

            // Default values for common banking apps
            appSettings["com.nequi.app"] = prefs.getBoolean("app_nequi", true)
            appSettings["com.nequi.o.android"] = prefs.getBoolean("app_nequi", true)
            appSettings["com.daviplata.app"] = prefs.getBoolean("app_daviplata", true)
            appSettings["com.bancolombia.app"] = prefs.getBoolean("app_bancolombia", true)
            appSettings["com.whatsapp"] = prefs.getBoolean("app_whatsapp", true)

            Log.d(TAG, "Settings loaded: $appSettings")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading app settings: ${e.message}", e)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!isServiceActive) {
            Log.v(TAG, "Service inactive, ignoring notification")
            return
        }

        try {
            val packageName = sbn.packageName

            if (!isAppEnabled(packageName)) {
                Log.v(TAG, "App $packageName disabled, ignoring notification")
                return
            }

            val notification = sbn.notification
            val extras = notification.extras

            val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

            // Ignore empty notifications
            if (title.isEmpty() && text.isEmpty()) {
                Log.v(TAG, "Empty notification, ignoring")
                return
            }

            Log.d(TAG, "Processing notification from $packageName: $title - $text")

            processNotificationSafely(packageName, title, text)

        } catch (e: Exception) {
            Log.e(TAG, "Error processing notification", e)
        }
    }

    private fun processNotificationSafely(packageName: String, title: String, text: String) {
        try {
            val message = processorRegistry?.processNotification(packageName, title, text)

            if (message != null) {
                // Extract amount from metadata
                val metadata = processorRegistry?.getLastProcessedMetadata() ?: emptyMap()
                val amount = metadata["amount"]

                // Save to history regardless of amount limit
                lastNotification = message

                // Check if should be read according to amount limit
                if (amountSettings.shouldReadAmount(amount)) {
                    if (ttsInitialized) {
                        speakOut(message)
                    } else {
                        Log.w(TAG, "TTS not initialized, cannot speak notification")
                    }
                } else {
                    Log.d(TAG, "Notification not read due to amount limit: $amount")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in processNotificationSafely: ${e.message}", e)
        }
    }

    private fun isAppEnabled(packageName: String): Boolean {
        try {
            // Look for partial matches in appSettings keys
            for ((app, enabled) in appSettings) {
                if (packageName.contains(app, ignoreCase = true) && enabled) {
                    return true
                }
            }

            // If not in configuration list, allow by default
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking app enabled status: ${e.message}", e)
            return true // Default to enabled on error
        }
    }

    private fun speakOut(text: String) {
        if (!isServiceActive || !ttsInitialized) {
            Log.v(TAG, "Cannot speak: service inactive or TTS not ready")
            return
        }

        try {
            Log.d(TAG, "Speaking: $text")
            mainHandler.post {
                try {
                    val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "notification_${System.currentTimeMillis()}")
                    Log.v(TAG, "TTS speak result: $result")
                } catch (e: Exception) {
                    Log.e(TAG, "Error in TTS speak: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in speakOut: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "Service being destroyed")

        try {
            // Clean up TTS
            tts?.let {
                it.stop()
                it.shutdown()
                ttsInitialized = false
            }
            tts = null

            // Clean up processor registry
            processorRegistry?.cleanup()
            processorRegistry = null

            // Clear settings
            appSettings.clear()

        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy: ${e.message}", e)
        }

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "NotificationListenerService connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "NotificationListenerService disconnected")
    }
}