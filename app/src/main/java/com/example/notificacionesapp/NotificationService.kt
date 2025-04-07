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
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

class NotificationService : NotificationListenerService() {

    private var tts: TextToSpeech? = null
    private var lastNotification = ""
    private val nequiPattern = Pattern.compile("([A-ZÁÉÍÓÚÑ\\s]+) te envió ([0-9,.]+), ¡lo mejor!")
    private val appSettings = ConcurrentHashMap<String, Boolean>()
    private val FOREGROUND_SERVICE_ID = 1001
    private val NOTIFICATION_CHANNEL_ID = "notification_service_channel"
    private var ttsInitialized = false

    companion object {
        var isServiceActive = false
        const val ACTION_START_SERVICE = "com.example.notificacionesapp.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.notificacionesapp.STOP_SERVICE"
        const val ACTION_UPDATE_APP_SETTINGS = "com.example.notificacionesapp.UPDATE_APP_SETTINGS"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("NotificationService", "Servicio iniciando")

        initializeTTS()
        loadAppSettings()
    }

    private fun initializeTTS() {
        // Configurar el volumen al máximo para asegurar que se escuche
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume / 2, 0)

        // Inicializar TTS de manera segura
        try {
            tts = TextToSpeech(applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale("es", "ES"))

                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("NotificationService", "El lenguaje español no está soportado")
                        ttsInitialized = false
                    } else {
                        Log.d("NotificationService", "TTS inicializado correctamente")
                        ttsInitialized = true

                        // Asegurar que esto ocurra en el hilo principal
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (isServiceActive) {
                                speakOut("Servicio de lectura de notificaciones activado")
                            }
                        }, 1000)
                    }
                } else {
                    Log.e("NotificationService", "Error al inicializar TTS: código = $status")
                    ttsInitialized = false
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationService", "Excepción al inicializar TTS: ${e.message}")
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
                    Log.d("NotificationService", "Servicio activado explícitamente")

                    // Solo hablar si TTS está inicializado
                    if (ttsInitialized) {
                        speakOut("Servicio de lectura activado")
                    }
                }
                ACTION_STOP_SERVICE -> {
                    isServiceActive = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    } else {
                        @Suppress("DEPRECATION")
                        stopForeground(true)
                    }
                    Log.d("NotificationService", "Servicio desactivado explícitamente")

                    // Solo hablar si TTS está inicializado
                    if (ttsInitialized) {
                        speakOut("Servicio de lectura desactivado")
                    }
                }
                ACTION_UPDATE_APP_SETTINGS -> {
                    loadAppSettings()
                    Log.d("NotificationService", "Configuración de apps actualizada")
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationService", "Error en onStartCommand: ${e.message}")
            e.printStackTrace()
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
                .build()

            startForeground(FOREGROUND_SERVICE_ID, notification)
        } catch (e: Exception) {
            Log.e("NotificationService", "Error en startForeground: ${e.message}")
            e.printStackTrace()
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
                }

                val manager = getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(serviceChannel)
            } catch (e: Exception) {
                Log.e("NotificationService", "Error al crear canal de notificación: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun loadAppSettings() {
        try {
            val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            appSettings.clear()

            // Valores predeterminados para apps bancarias comunes
            appSettings["com.nequi.app"] = prefs.getBoolean("app_nequi", true)
            appSettings["com.nequi.o.android"] = prefs.getBoolean("app_nequi", true)
            appSettings["com.daviplata.app"] = prefs.getBoolean("app_daviplata", true)
            appSettings["com.bancolombia.app"] = prefs.getBoolean("app_bancolombia", true)
            appSettings["com.whatsapp"] = prefs.getBoolean("app_whatsapp", true)

            Log.d("NotificationService", "Configuración cargada: $appSettings")
        } catch (e: Exception) {
            Log.e("NotificationService", "Error al cargar configuración: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            if (!isServiceActive || !ttsInitialized) {
                Log.d("NotificationService", "Servicio inactivo o TTS no inicializado, ignorando notificación")
                return
            }

            val packageName = sbn.packageName

            // Verificar si esta app está habilitada
            if (!isAppEnabled(packageName)) {
                Log.d("NotificationService", "App $packageName deshabilitada, ignorando notificación")
                return
            }

            val notification = sbn.notification
            val extras = notification.extras

            val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

            Log.d("NotificationService", "Notificación recibida de $packageName: $title - $text")

            // Procesar notificaciones según el paquete
            when {
                packageName.contains("nequi") || packageName.contains("colombia.nequi") -> {
                    processNequiNotification("$title $text")
                }
                packageName.contains("daviplata") || packageName.contains("daviplataapp")-> {
                    processDaviPlataNotification(title, text)
                }
                packageName.contains("bancolombia") || packageName.contains("bancolombia.mibancolombia") -> {
                    processBancolombiaNotification(title, text)
                }
                packageName.contains("whatsapp") -> {
                    processWhatsAppNotification(title, text)
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationService", "Error al procesar notificación", e)
        }
    }

    private fun isAppEnabled(packageName: String): Boolean {
        // Buscar coincidencias parciales en las claves de appSettings
        for ((app, enabled) in appSettings) {
            if (packageName.contains(app) && enabled) {
                return true
            }
        }

        // Si no está en la lista de configuraciones, se permite por defecto
        return true
    }

    private fun processNequiNotification(content: String) {
        try {
            // Intentar con el patrón de Nequi
            var matcher = nequiPattern.matcher(content)
            if (matcher.find()) {
                val nombre = matcher.group(1)
                val monto = matcher.group(2)
                val mensaje = "$nombre te envió $monto pesos por nequi"
                speakOut(mensaje)
                return
            }

            // Si no coincide con el patrón, intentar metodo alternativo
            if (content.contains("te envió")) {
                val parts = content.split("te envió")
                if (parts.size >= 2) {
                    val nombre = parts[0].trim()
                    val montoRaw = parts[1].replace("¡lo mejor!", "").trim()
                    val mensaje = "$nombre te envió $montoRaw pesos por nequi"

                    if (mensaje != lastNotification) {
                        lastNotification = mensaje
                        speakOut(mensaje)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationService", "Error al procesar notificación de Nequi: ${e.message}")
        }
    }

    private fun processDaviPlataNotification(title: String, text: String) {
        try {
            if (text.contains("Recibió") || text.contains("movimientos")) {
                val mensaje = "DaviPlata: $text"

                if (mensaje != lastNotification) {
                    lastNotification = mensaje
                    speakOut(mensaje)
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationService", "Error al procesar notificación de DaviPlata: ${e.message}")
        }
    }

    private fun processBancolombiaNotification(title: String, text: String) {
        try {
            if (text.contains("recibiste") || text.contains("transferencia") ||
                text.contains("consignación") || title.contains("Transferencia")) {
                val mensaje = "Bancolombia: $text"

                if (mensaje != lastNotification) {
                    lastNotification = mensaje
                    speakOut(mensaje)
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationService", "Error al procesar notificación de Bancolombia: ${e.message}")
        }
    }

    private fun processWhatsAppNotification(title: String, text: String) {
        try {
            // No leer notificaciones de grupos, solo mensajes directos
            if (!title.contains(":")) {  // Los mensajes de grupo suelen tener formato "Grupo: Remitente: Mensaje"
                val mensaje = "$title dice: $text"

                // Evitar repeticiones
                if (mensaje != lastNotification) {
                    lastNotification = mensaje
                    Log.d("NotificationService", "Mensaje de WhatsApp: $mensaje")
                    speakOut(mensaje)
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationService", "Error al procesar notificación de WhatsApp: ${e.message}")
        }
    }

    private fun speakOut(text: String) {
        if (!isServiceActive || !ttsInitialized) {
            Log.d("NotificationService", "Servicio inactivo o TTS no inicializado, no se leerá la notificación")
            return
        }

        try {
            Log.d("NotificationService", "Intentando decir: $text")
            // Usar Handler para asegurar que esto se ejecute en el hilo principal
            Handler(Looper.getMainLooper()).post {
                try {
                    val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "notification_id")
                    Log.d("NotificationService", "Resultado de TTS speak: $result")
                } catch (e: Exception) {
                    Log.e("NotificationService", "Error al hablar: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationService", "Error en speakOut: ${e.message}")
        }
    }

    override fun onDestroy() {
        try {
            if (tts != null) {
                tts?.stop()
                tts?.shutdown()
                ttsInitialized = false
            }
            super.onDestroy()
        } catch (e: Exception) {
            Log.e("NotificationService", "Error en onDestroy: ${e.message}")
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }
}