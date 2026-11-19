package com.example.notificacionesapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.notificacionesapp.domain.model.NotificationType
import com.example.notificacionesapp.notification.NotificationProcessorRegistry
import com.example.notificacionesapp.util.AmountSettings
import com.example.notificacionesapp.util.NotificationHistoryManager
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * Servicio de notificaciones para NotiBank Lite.
 *
 * Versión ligera SIN Supabase: lee notificaciones entrantes de otras apps
 * mediante NotificationListenerService nativo de Android, las procesa
 * localmente con regex y las anuncia por TTS. No guarda historial en la nube
 * ni depende de conexión a internet para funcionar.
 */
class NotificationServiceLite : NotificationListenerService() {

    @Inject lateinit var sessionManager: SessionManager

    private val appSettings = ConcurrentHashMap<String, Boolean>()
    private val FOREGROUND_SERVICE_ID = 1002
    private val NOTIFICATION_CHANNEL_ID = "notification_service_lite_channel"

    companion object {
        var isServiceActive = false
        const val ACTION_START_SERVICE = "com.example.notibanklite.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.notibanklite.STOP_SERVICE"
        const val ACTION_UPDATE_APP_SETTINGS = "com.example.notibanklite.UPDATE_APP_SETTINGS"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("NotificationServiceLite", "Servicio Lite iniciando")

        // Inicializar procesadores y ajustes locales
        NotificationProcessorRegistry(NotificationHistoryManager(this))
        AmountSettings(this)
        loadAppSettings()
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
            Log.d("NotificationServiceLite", "Configuración Lite cargada")
        } catch (e: Exception) {
            Log.e("NotificationServiceLite", "Error al cargar configuración: ${e.message}")
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

            Log.d("NotificationServiceLite", "Notificación recibida de $packageName: $title - $text")

            val message = NotificationProcessorRegistry(
                NotificationHistoryManager(this)
            ).processNotification(packageName, title, text)

            if (message != null) {
                Log.d("NotificationServiceLite", "Notificación procesada: $message")
                speakOut(message)
            } else {
                Log.d("NotificationServiceLite", "Notificación no procesada: $packageName | $title | $text")
            }
        } catch (e: Exception) {
            Log.e("NotificationServiceLite", "Error al procesar notificación", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_STICKY
        Log.d("TTS_DEBUG", "[LITE] onStartCommand: $action")
        try {
            when (action) {
                ACTION_START_SERVICE -> {
                    isServiceActive = true
                    startForeground()
                    Log.d("NotificationServiceLite", "Servicio Lite activado")
                }
                ACTION_STOP_SERVICE -> {
                    isServiceActive = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    } else {
                        @Suppress("DEPRECATION")
                        stopForeground(true)
                    }
                    stopSelf()
                    Log.d("NotificationServiceLite", "Servicio Lite desactivado y detenido")
                }
                ACTION_UPDATE_APP_SETTINGS -> {
                    loadAppSettings()
                    Log.d("NotificationServiceLite", "Configuración Lite actualizada")
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationServiceLite", "Error en onStartCommand: ${e.message}")
        }
        return START_NOT_STICKY
    }

    private fun startForeground() {
        try {
            createNotificationChannel()
            val notificationIntent = Intent(this, MainActivityLite::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("NotiBank Lite - Activo")
                .setContentText("Leyendo notificaciones en segundo plano")
                .setSmallIcon(R.drawable.notification_icon)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
            startForeground(FOREGROUND_SERVICE_ID, notification)
        } catch (e: Exception) {
            Log.e("NotificationServiceLite", "Error en startForeground: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val serviceChannel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Servicio NotiBank Lite",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Canal para el servicio de lectura de notificaciones Lite"
                    setShowBadge(false)
                }
                val manager = getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(serviceChannel)
            } catch (e: Exception) {
                Log.e("NotificationServiceLite", "Error al crear canal: ${e.message}")
            }
        }
    }

    private fun speakOut(text: String) {
        Log.d("NotificationServiceLite", "TTS: $text")
        val intent = Intent(this, TtsService::class.java)
        intent.putExtra("text", text)
        startService(intent)
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
            super.onDestroy()
        } catch (e: Exception) {
            Log.e("NotificationServiceLite", "Error en onDestroy: ${e.message}")
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }
}
