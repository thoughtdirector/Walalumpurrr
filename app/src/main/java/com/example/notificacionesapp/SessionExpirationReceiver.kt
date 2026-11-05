package com.example.notificacionesapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.notificacionesapp.MainActivityLite

/**
 * Receiver que se dispara cuando expira la sesión de NotiBank Lite,
 * incluso si la aplicación está en segundo plano o cerrada.
 *
 * Sigue el mismo patrón que [ScheduleReceiver] de la versión principal:
 * - Recibe el broadcast de la alarma.
 * - Detiene el servicio de notificaciones mediante startForegroundService + ACTION_STOP_SERVICE.
 * - Muestra una notificación push visible (confiable en segundo plano).
 * - Intenta anunciar por TTS como refuerzo de voz.
 */
class SessionExpirationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("SessionExpirationReceiver", "Alarma recibida: $action")

        when (action) {
            ACTION_SESSION_EXPIRED -> {
                // 1. Detener el servicio de notificaciones (igual que ScheduleReceiver hace para iniciar/detener)
                val serviceIntent = Intent(context, NotificationService::class.java)
                serviceIntent.action = NotificationService.ACTION_STOP_SERVICE
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                    Log.d("SessionExpirationReceiver", "Servicio de notificaciones detenido por expiración")
                } catch (e: Exception) {
                    Log.e("SessionExpirationReceiver", "Error al detener servicio: ${e.message}")
                }

                // 2. Mostrar notificación push visible (confiable en segundo plano)
                try {
                    showExpirationNotification(context)
                } catch (e: Exception) {
                    Log.e("SessionExpirationReceiver", "No se pudo mostrar notificación de expiración: ${e.message}")
                }

                // 3. Marcar que expiró para anunciar al usuario al volver a la app
                val prefs: SharedPreferences = context.getSharedPreferences(SessionAlarmManager.PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putBoolean(SessionAlarmManager.KEY_EXPIRED_NOTIFIED, true).apply()
            }
        }
    }

    /**
     * Publica una notificación push avisando que la sesión expiró.
     * No requiere iniciar servicios ni depender del motor de TTS.
     */
    private fun showExpirationNotification(context: Context) {
        val channelId = "session_expiration"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Expiración de sesión",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Aviso cuando expira la sesión de NotiBank Lite"
                enableLights(true)
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivityLite::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle("NotiBank Lite")
            .setContentText(context.getString(R.string.lite_service_disconnected))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .build()

        manager.notify(2002, notification)
    }

    companion object {
        const val ACTION_SESSION_EXPIRED = SessionAlarmManager.ACTION_SESSION_EXPIRED
    }
}
