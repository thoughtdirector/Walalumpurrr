package com.example.notificacionesapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import java.util.Calendar

class ScheduleManager(private val context: Context) {

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("app_schedule", Context.MODE_PRIVATE)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        const val KEY_SCHEDULE_ENABLED = "schedule_enabled"
        const val KEY_START_HOUR = "start_hour"
        const val KEY_START_MINUTE = "start_minute"
        const val KEY_END_HOUR = "end_hour"
        const val KEY_END_MINUTE = "end_minute"
        const val ACTION_START_SERVICE = "com.example.notificacionesapp.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.notificacionesapp.STOP_SERVICE"

        // Días de la semana
        const val KEY_MONDAY = "monday_enabled"
        const val KEY_TUESDAY = "tuesday_enabled"
        const val KEY_WEDNESDAY = "wednesday_enabled"
        const val KEY_THURSDAY = "thursday_enabled"
        const val KEY_FRIDAY = "friday_enabled"
        const val KEY_SATURDAY = "saturday_enabled"
        const val KEY_SUNDAY = "sunday_enabled"
    }

    // Guardar configuración de horario
    fun saveScheduleSettings(
        isEnabled: Boolean,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int
    ) {
        sharedPrefs.edit().apply {
            putBoolean(KEY_SCHEDULE_ENABLED, isEnabled)
            putInt(KEY_START_HOUR, startHour)
            putInt(KEY_START_MINUTE, startMinute)
            putInt(KEY_END_HOUR, endHour)
            putInt(KEY_END_MINUTE, endMinute)
        }.apply()

        if (isEnabled) {
            setScheduleAlarms()
        } else {
            cancelScheduleAlarms()
        }
    }

    // Configurar alarmas de inicio y fin
    fun setScheduleAlarms() {
        if (!isScheduleEnabled()) return

        val startHour = sharedPrefs.getInt(KEY_START_HOUR, 8)
        val startMinute = sharedPrefs.getInt(KEY_START_MINUTE, 0)
        val endHour = sharedPrefs.getInt(KEY_END_HOUR, 18)
        val endMinute = sharedPrefs.getInt(KEY_END_MINUTE, 0)

        // Configurar alarma de inicio
        val startCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, startMinute)
            set(Calendar.SECOND, 0)

            // Si la hora ya pasó, programar para mañana
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // Configurar alarma de fin
        val endCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, endHour)
            set(Calendar.MINUTE, endMinute)
            set(Calendar.SECOND, 0)

            // Si la hora ya pasó, programar para mañana
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // Crear intents para las alarmas
        val startIntent = Intent(context, ScheduleReceiver::class.java).apply {
            action = ACTION_START_SERVICE
        }

        val endIntent = Intent(context, ScheduleReceiver::class.java).apply {
            action = ACTION_STOP_SERVICE
        }

        // PendingIntents
        val startPendingIntent = PendingIntent.getBroadcast(
            context, 1, startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val endPendingIntent = PendingIntent.getBroadcast(
            context, 2, endIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // Configurar alarmas con manejo de excepciones
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        startCalendar.timeInMillis,
                        startPendingIntent
                    )

                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        endCalendar.timeInMillis,
                        endPendingIntent
                    )
                } else {
                    // Fallback a alarmas no exactas
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        startCalendar.timeInMillis,
                        startPendingIntent
                    )

                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        endCalendar.timeInMillis,
                        endPendingIntent
                    )
                }
            } else {
                // Para versiones anteriores
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    startCalendar.timeInMillis,
                    startPendingIntent
                )

                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    endCalendar.timeInMillis,
                    endPendingIntent
                )
            }

            Log.d("ScheduleManager", "Alarmas programadas: inicio ${startHour}:${startMinute}, fin ${endHour}:${endMinute}")
        } catch (e: Exception) {
            Log.e("ScheduleManager", "Error al programar alarmas: ${e.message}")
        }

        // Verificar el estado actual inmediatamente
        checkCurrentStatus()
    }

    // Cancelar alarmas programadas
    fun cancelScheduleAlarms() {
        val startIntent = Intent(context, ScheduleReceiver::class.java).apply {
            action = ACTION_START_SERVICE
        }

        val endIntent = Intent(context, ScheduleReceiver::class.java).apply {
            action = ACTION_STOP_SERVICE
        }

        val startPendingIntent = PendingIntent.getBroadcast(
            context, 1, startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val endPendingIntent = PendingIntent.getBroadcast(
            context, 2, endIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(startPendingIntent)
        alarmManager.cancel(endPendingIntent)

        Log.d("ScheduleManager", "Alarmas canceladas")
    }

    // Verificar el estado actual y aplicarlo
    // Verificar el estado actual y aplicarlo
    private fun checkCurrentStatus() {
        val shouldBeActive = shouldServiceBeActive()
        Log.d("ScheduleManager", "Verificación de estado: debería estar activo = $shouldBeActive")
        NotificationService.isServiceActive = shouldBeActive

        // Enviar broadcast para notificar a la UI usando Intent explícito
        val updateIntent = Intent(MainActivity.updateStatusAction)
        updateIntent.setPackage(context.packageName) // Hace que sea explícito
        context.sendBroadcast(updateIntent)
    }

    // Comprobar si el servicio debería estar activo según la hora actual
    fun shouldServiceBeActive(): Boolean {
        if (!isScheduleEnabled()) return true

        val startHour = sharedPrefs.getInt(KEY_START_HOUR, 8)
        val startMinute = sharedPrefs.getInt(KEY_START_MINUTE, 0)
        val endHour = sharedPrefs.getInt(KEY_END_HOUR, 18)
        val endMinute = sharedPrefs.getInt(KEY_END_MINUTE, 0)

        val currentTime = Calendar.getInstance()
        val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
        val currentMinute = currentTime.get(Calendar.MINUTE)

        val currentTimeValue = currentHour * 60 + currentMinute
        val startTimeValue = startHour * 60 + startMinute
        val endTimeValue = endHour * 60 + endMinute

        // Verificar si el día actual está habilitado
        val currentDay = currentTime.get(Calendar.DAY_OF_WEEK)
        val isDayEnabled = when (currentDay) {
            Calendar.MONDAY -> sharedPrefs.getBoolean(KEY_MONDAY, true)
            Calendar.TUESDAY -> sharedPrefs.getBoolean(KEY_TUESDAY, true)
            Calendar.WEDNESDAY -> sharedPrefs.getBoolean(KEY_WEDNESDAY, true)
            Calendar.THURSDAY -> sharedPrefs.getBoolean(KEY_THURSDAY, true)
            Calendar.FRIDAY -> sharedPrefs.getBoolean(KEY_FRIDAY, true)
            Calendar.SATURDAY -> sharedPrefs.getBoolean(KEY_SATURDAY, false)
            Calendar.SUNDAY -> sharedPrefs.getBoolean(KEY_SUNDAY, false)
            else -> false
        }

        return if (isDayEnabled) {
            if (startTimeValue <= endTimeValue) {
                // Caso normal: inicio < fin (mismo día)
                currentTimeValue in startTimeValue..endTimeValue
            } else {
                // Caso especial: fin < inicio (por ejemplo, 22:00 a 06:00)
                currentTimeValue >= startTimeValue || currentTimeValue <= endTimeValue
            }
        } else {
            false // Si el día está deshabilitado, el servicio no debería estar activo
        }
    }

    // Obtener configuración de horario
    fun getStartHour() = sharedPrefs.getInt(KEY_START_HOUR, 8)
    fun getStartMinute() = sharedPrefs.getInt(KEY_START_MINUTE, 0)
    fun getEndHour() = sharedPrefs.getInt(KEY_END_HOUR, 18)
    fun getEndMinute() = sharedPrefs.getInt(KEY_END_MINUTE, 0)
    fun isScheduleEnabled() = sharedPrefs.getBoolean(KEY_SCHEDULE_ENABLED, false)

    // Verificar si un día específico está habilitado
    fun isDayEnabled(day: Int): Boolean {
        return when (day) {
            Calendar.MONDAY -> sharedPrefs.getBoolean(KEY_MONDAY, true)
            Calendar.TUESDAY -> sharedPrefs.getBoolean(KEY_TUESDAY, true)
            Calendar.WEDNESDAY -> sharedPrefs.getBoolean(KEY_WEDNESDAY, true)
            Calendar.THURSDAY -> sharedPrefs.getBoolean(KEY_THURSDAY, true)
            Calendar.FRIDAY -> sharedPrefs.getBoolean(KEY_FRIDAY, true)
            Calendar.SATURDAY -> sharedPrefs.getBoolean(KEY_SATURDAY, false)
            Calendar.SUNDAY -> sharedPrefs.getBoolean(KEY_SUNDAY, false)
            else -> false
        }
    }

    // Guardar configuración de días
    fun saveDaySettings(
        monday: Boolean,
        tuesday: Boolean,
        wednesday: Boolean,
        thursday: Boolean,
        friday: Boolean,
        saturday: Boolean,
        sunday: Boolean
    ) {
        sharedPrefs.edit().apply {
            putBoolean(KEY_MONDAY, monday)
            putBoolean(KEY_TUESDAY, tuesday)
            putBoolean(KEY_WEDNESDAY, wednesday)
            putBoolean(KEY_THURSDAY, thursday)
            putBoolean(KEY_FRIDAY, friday)
            putBoolean(KEY_SATURDAY, saturday)
            putBoolean(KEY_SUNDAY, sunday)
        }.apply()

        if (isScheduleEnabled()) {
            cancelScheduleAlarms()
            setScheduleAlarms()
        }
    }
}