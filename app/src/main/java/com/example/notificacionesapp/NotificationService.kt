package com.example.notificacionesapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.notificacionesapp.core.domain.Result
import com.example.notificacionesapp.core.supabase.SupabaseManager
import com.example.notificacionesapp.domain.model.NotificationType
import com.example.notificacionesapp.domain.repository.NotificationRepository
import com.example.notificacionesapp.notification.NotificationProcessorRegistry
import com.example.notificacionesapp.util.AmountSettings
import com.example.notificacionesapp.util.NotificationHistoryManager
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotificationService : NotificationListenerService() {

    @Inject lateinit var supabaseManager: SupabaseManager
    @Inject lateinit var notificationRepository: NotificationRepository
    @Inject lateinit var sessionManager: SessionManager

    private var lastNotification = ""
    private val appSettings = ConcurrentHashMap<String, Boolean>()
    private val FOREGROUND_SERVICE_ID = 1001
    private val NOTIFICATION_CHANNEL_ID = "notification_service_channel"
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var notificationHistoryManager: NotificationHistoryManager
    private lateinit var processorRegistry: NotificationProcessorRegistry
    private lateinit var amountSettings: AmountSettings
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastSeenTimestamp: Long = System.currentTimeMillis()
    private var pollingJob: kotlinx.coroutines.Job? = null
    private var lastSavedContent: String = ""
    private var lastSavedTime: Long = 0L

    companion object {
        var isServiceActive = false
        const val ACTION_START_SERVICE = "com.example.notificacionesapp.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.notificacionesapp.STOP_SERVICE"
        const val ACTION_UPDATE_APP_SETTINGS = "com.example.notificacionesapp.UPDATE_APP_SETTINGS"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("NotificationService", "Servicio iniciando")

        notificationHistoryManager = NotificationHistoryManager(this)
        processorRegistry = NotificationProcessorRegistry(notificationHistoryManager)
        amountSettings = AmountSettings(this)

        setupSupabaseRealtime()
        startNotificationPolling()
        loadAppSettings()
    }

    private fun setupSupabaseRealtime() {
        supabaseManager.initRealtimeChannel { payload ->
            Log.d("NotificationService", "Notificación recibida vía Supabase: ${payload.message}")
            speakOut(payload.message)
        }
    }

    /**
     * Mecanismo de respaldo: consulta Supabase cada 5 segundos
     * para detectar nuevas notificaciones y reproducirlas con TTS.
     * Garantiza que el empleado escuche aunque falle el canal Realtime.
     */
    private fun startNotificationPolling() {
        pollingJob?.cancel()
        pollingJob = serviceScope.launch {
            var firstPoll = true
            while (isActive) {
                try {
                    delay(if (firstPoll) 3000L else 5000L)
                    if (!isServiceActive) continue

                    when (val result = notificationRepository.getAllNotifications()) {
                        is Result.Success -> {
                            val all = result.data.sortedBy { it.timestamp }
                            // Guardar timestamp base antes del filtro
                            val previousTimestamp = lastSeenTimestamp
                            // Siempre sincronizar al último timestamp conocido
                            if (all.isNotEmpty()) {
                                lastSeenTimestamp = all.last().timestamp.time
                            }
                            val newNotifications = all.filter { it.timestamp.time > previousTimestamp }
                            Log.d("TTS_DEBUG", "[PASO2] Total en Supabase: ${all.size}, nuevas desde $previousTimestamp: ${newNotifications.size}")

                            if (newNotifications.isNotEmpty()) {
                                val latest = newNotifications.last()
                                val message = "${latest.appName}: ${latest.content}"
                                Log.d("TTS_DEBUG", "[PASO3] Detectada nueva: $message")
                                speakOut(message)
                            } else {
                                Log.d("TTS_DEBUG", "[PASO2] Sin notificaciones nuevas")
                            }
                        }
                        is Result.Error -> {
                            Log.e("TTS_DEBUG", "[ERROR] Polling Supabase: ${result.exception.message}")
                        }
                        is Result.Loading -> {}
                    }
                    firstPoll = false
                } catch (e: Exception) {
                    Log.e("TTS_DEBUG", "Error en polling: ${e.message}")
                }
            }
        }
    }

    private fun isAppEnabled(packageName: String): Boolean {
        for ((app, enabled) in appSettings) {
            if (packageName.contains(app) && enabled) {
                return true
            }
        }
        return true
    }

    private fun loadAppSettings() {
        try {
            val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            appSettings.clear()
            appSettings["com.nequi.app"] = prefs.getBoolean("app_nequi", true)
            appSettings["com.nequi.o.android"] = prefs.getBoolean("app_nequi", true)
            appSettings["com.daviplata.app"] = prefs.getBoolean("app_daviplata", true)
            appSettings["com.bancolombia.app"] = prefs.getBoolean("app_bancolombia", true)
            appSettings["com.whatsapp"] = prefs.getBoolean("app_whatsapp", true)
            Log.d("NotificationService", "Configuración cargada: $appSettings")
        } catch (e: Exception) {
            Log.e("NotificationService", "Error al cargar configuración: ${e.message}")
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            if (!isServiceActive) return

            val packageName = sbn.packageName
            if (!isAppEnabled(packageName)) return

            val notification = sbn.notification
            val extras = notification.extras
            val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

            Log.d("NotificationService", "Notificación recibida de $packageName: $title - $text")

            val message = processorRegistry.processNotification(packageName, title, text)
            if (message != null) {
                Log.d("NotificationService", "Notificación procesada: $message")
                val metadata = processorRegistry.getLastProcessedMetadata()
                val amount = metadata["amount"]
                val sender = metadata["sender"]
                val appName = metadata["appName"] ?: packageName
                val type = metadata["type"] ?: "other"

                lastNotification = message

                // Dedup: evitar guardar la misma notificación en <30s
                val now = System.currentTimeMillis()
                if (message != lastSavedContent || (now - lastSavedTime) > 30_000) {
                    lastSavedContent = message
                    lastSavedTime = now

                    val adminId = sessionManager.getUserId()
                    val savedNotification = com.example.notificacionesapp.domain.model.Notification(
                        id = UUID.randomUUID().toString(),
                        packageName = packageName,
                        appName = appName,
                        title = title,
                        content = message,
                        type = mapProcessorType(type),
                        amount = amount ?: "",
                        sender = sender ?: "",
                        adminId = adminId,
                        timestamp = Date(),
                        isProcessed = true
                    )
                    serviceScope.launch {
                        try {
                            notificationRepository.saveNotification(savedNotification)
                            Log.d("NotificationService", "Guardado en Supabase: ${savedNotification.id}")
                        } catch (e: Exception) {
                            Log.e("NotificationService", "Error guardando en Supabase: ${e.message}", e)
                        }
                    }
                } else {
                    Log.d("NotificationService", "Notificación duplicada ignorada: $message")
                }

                supabaseManager.broadcastNotification(title, message, amount, sender)

                if (amountSettings.shouldReadAmount(amount)) {
                    val muted = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                        .getBoolean("mute_local_tts", false)
                    if (!muted) {
                        Log.d("TTS_DEBUG", "[ADMIN_DIRECTO] speakOut desde onNotificationPosted")
                        speakOut(message)
                        lastSeenTimestamp = System.currentTimeMillis()
                    } else {
                        Log.d("TTS_DEBUG", "[ADMIN_DIRECTO] Silenciado por mute_local_tts")
                    }
                } else {
                    Log.d("NotificationService", "Notificación no leída por límite de monto: $amount")
                }
            } else {
                Log.d("NotificationService", "Notificación no procesada (sin regex match): $packageName | $title | $text")
            }
        } catch (e: Exception) {
            Log.e("NotificationService", "Error al procesar notificación", e)
        }
    }

    private fun mapProcessorType(type: String): NotificationType {
        return when (type.uppercase()) {
            "NEQUI" -> NotificationType.TRANSFER_RECEIVED
            "DAVIPLATA" -> NotificationType.TRANSFER_RECEIVED
            "BANCOLOMBIA" -> NotificationType.TRANSFER_RECEIVED
            else -> NotificationType.OTHER
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_STICKY
        Log.d("TTS_DEBUG", "[PASO0] onStartCommand: $action")
        try {
            when (action) {
                ACTION_START_SERVICE -> {
                    isServiceActive = true
                    startForeground()
                    Log.d("NotificationService", "Servicio activado")
                    speakOut("Servicio de lectura activado")
                }
                ACTION_STOP_SERVICE -> {
                    isServiceActive = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    } else {
                        @Suppress("DEPRECATION")
                        stopForeground(true)
                    }
                    Log.d("NotificationService", "Servicio desactivado")
                }
                ACTION_UPDATE_APP_SETTINGS -> {
                    loadAppSettings()
                    Log.d("NotificationService", "Configuración de apps actualizada")
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationService", "Error en onStartCommand: ${e.message}")
        }
        return START_NOT_STICKY
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
            }
        }
    }

    private fun speakOut(text: String) {
        Log.d("TTS_DEBUG", "[PASO4] speakOut llamado: $text")
        if (!isServiceActive) {
            Log.d("TTS_DEBUG", "[PASO4] Abortado: isServiceActive=false")
            return
        }
        val intent = Intent("com.example.notificacionesapp.TTS_SPEAK")
        intent.putExtra("text", text)
        intent.setPackage(packageName)
        sendBroadcast(intent)
        Log.d("TTS_DEBUG", "[PASO5] Broadcast TTS_SPEAK enviado")
    }

    override fun onDestroy() {
        try {
            appSettings.clear()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }

            isServiceActive = false

            mainHandler.removeCallbacksAndMessages(null)
            supabaseManager.disconnect()

             pollingJob?.cancel()
            serviceScope.cancel()

            super.onDestroy()
        } catch (e: Exception) {
            Log.e("NotificationService", "Error en onDestroy: ${e.message}")
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }
}
