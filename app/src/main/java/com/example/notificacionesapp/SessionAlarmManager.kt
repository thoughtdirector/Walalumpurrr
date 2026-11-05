package com.example.notificacionesapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log

/**
 * Gestiona la alarma de expiración de sesión de NotiBank Lite.
 * Sigue el mismo patrón que [ScheduleManager] de la versión principal:
 * verifica canScheduleExactAlarms() y usa setExactAndAllowWhileIdle
 * para que funcione en segundo plano incluso con la app cerrada.
 */
class SessionAlarmManager(private val context: Context) {

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        const val PREFS_NAME = "lite_session_prefs"
        const val KEY_ACTIVATION_TIME = "activation_timestamp"
        const val ACTION_SESSION_EXPIRED = "com.example.notificacionesapp.SESSION_EXPIRED"
        const val REQUEST_CODE_SESSION_ALARM = 1001
        const val KEY_EXPIRED_NOTIFIED = "session_expired_notified"
        private const val TAG = "SessionAlarmManager"
    }

    /** Guarda el timestamp de activación. */
    fun saveActivationTime(timestamp: Long) {
        sharedPrefs.edit().putLong(KEY_ACTIVATION_TIME, timestamp).apply()
    }

    /** Lee el timestamp de activación guardado. */
    fun getActivationTime(): Long {
        return sharedPrefs.getLong(KEY_ACTIVATION_TIME, 0)
    }

    /** Borra el timestamp de activación. */
    fun clearActivationTime() {
        sharedPrefs.edit().remove(KEY_ACTIVATION_TIME).apply()
    }

    /**
     * Programa la alarma de expiración para activationTimestamp + SESSION_DURATION_MS.
     * Si ya existía una alarma previa, la cancela primero.
     */
    fun scheduleExpirationAlarm(activationTimestamp: Long, sessionDurationMs: Long) {
        cancelExpirationAlarm()

        val triggerAtMillis = activationTimestamp + sessionDurationMs

        val intent = Intent(context, SessionExpirationReceiver::class.java).apply {
            action = ACTION_SESSION_EXPIRED
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_SESSION_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Alarma de expiración programada (exact) para: $triggerAtMillis")
                } else {
                    // Fallback si no se concedió el permiso especial de alarmas exactas
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                    Log.w(TAG, "canScheduleExactAlarms=false, usando setAndAllowWhileIdle como fallback")
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Log.d(TAG, "Alarma de expiración programada (exact, API 23+) para: $triggerAtMillis")
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Log.d(TAG, "Alarma de expiración programada (exact, API <23) para: $triggerAtMillis")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error programando alarma de expiración: ${e.message}")
            // Fallback a alarma no exacta
            try {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Log.d(TAG, "Alarma de expiración programada (fallback set) para: $triggerAtMillis")
            } catch (e2: Exception) {
                Log.e(TAG, "Error fatal programando alarma de expiración: ${e2.message}")
            }
        }
    }

    /** Cancela la alarma de expiración (cuando el usuario apaga manualmente). */
    fun cancelExpirationAlarm() {
        val intent = Intent(context, SessionExpirationReceiver::class.java).apply {
            action = ACTION_SESSION_EXPIRED
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_SESSION_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Alarma de expiración cancelada")
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelando alarma de expiración: ${e.message}")
        }
    }

    /** Indica si el permiso de alarmas exactas está concedido (solo API 31+). */
    fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return alarmManager.canScheduleExactAlarms()
        }
        return true
    }
}
