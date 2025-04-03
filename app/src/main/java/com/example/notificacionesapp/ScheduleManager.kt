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

    /**
     * Obtiene cuándo será el próximo evento programado (inicio o fin)
     */
    fun getNextScheduledEvent(): Calendar? {
        if (!isScheduleEnabled()) return null

        val startEvent = getNextScheduledTime(true)
        val endEvent = getNextScheduledTime(false)

        return if (startEvent == null && endEvent == null) {
            null
        } else if (startEvent == null) {
            endEvent
        } else if (endEvent == null) {
            startEvent
        } else {
            if (startEvent.timeInMillis < endEvent.timeInMillis) startEvent else endEvent
        }
    }

    /**
     * Calcula la próxima hora programada para inicio o fin
     */
    private fun getNextScheduledTime(isStart: Boolean): Calendar? {
        val hour = if (isStart) getStartHour() else getEndHour()
        val minute = if (isStart) getStartMinute() else getEndMinute()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Si la hora ya pasó hoy, añadir un día
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Buscar el próximo día habilitado
        var daysChecked = 0
        var foundEnabledDay = false

        while (daysChecked < 7 && !foundEnabledDay) {
            val dayToCheck = calendar.get(Calendar.DAY_OF_WEEK)

            if (isDayEnabled(dayToCheck)) {
                foundEnabledDay = true
            } else {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                daysChecked++
            }
        }

        return if (foundEnabledDay) calendar else null
    }

    /**
     * Verifica si el horario programado es nocturno (cuando hora fin < hora inicio)
     */
    fun isNightSchedule(): Boolean {
        val startHour = getStartHour()
        val startMinute = getStartMinute()
        val endHour = getEndHour()
        val endMinute = getEndMinute()

        val startTime = startHour * 60 + startMinute
        val endTime = endHour * 60 + endMinute

        return endTime < startTime
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

        // Cancelar alarmas existentes antes de programar nuevas
        cancelScheduleAlarms()

        if (isEnabled) {
            setScheduleAlarms()

            // Verificar el estado actual inmediatamente
            val shouldBeActive = shouldServiceBeActive()
            updateServiceState(shouldBeActive)
        }
    }

    // Configurar alarmas de inicio y fin
    fun setScheduleAlarms() {
        if (!isScheduleEnabled()) return

        val startCalendar = getNextScheduledTime(true)
        val endCalendar = getNextScheduledTime(false)

        if (startCalendar == null || endCalendar == null) {
            Log.d("ScheduleManager", "No se pudieron configurar alarmas: ningún día habilitado")
            return
        }

        // Crear intent explícito para iniciar el servicio
        val startServiceIntent = Intent(context, NotificationService::class.java).apply {
            action = NotificationService.ACTION_START_SERVICE
        }

        // Crear intent explícito para detener el servicio
        val stopServiceIntent = Intent(context, NotificationService::class.java).apply {
            action = NotificationService.ACTION_STOP_SERVICE
        }

        // Crear intents para el receptor de broadcasts que activará los servicios
        val startAlarmIntent = Intent(context, ScheduleReceiver::class.java).apply {
            action = ACTION_START_SERVICE
        }

        val endAlarmIntent = Intent(context, ScheduleReceiver::class.java).apply {
            action = ACTION_STOP_SERVICE
        }

        // PendingIntents para las alarmas
        val startPendingIntent = PendingIntent.getBroadcast(
            context, 1, startAlarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val endPendingIntent = PendingIntent.getBroadcast(
            context, 2, endAlarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // Configurar alarmas usando el método más compatible
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        startCalendar.timeInMillis,
                        startPendingIntent
                    )

                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        endCalendar.timeInMillis,
                        endPendingIntent
                    )
                } else {
                    // Fallback para Android 12+
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        startCalendar.timeInMillis,
                        startPendingIntent
                    )

                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        endCalendar.timeInMillis,
                        endPendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Para Android 6.0+
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    startCalendar.timeInMillis,
                    startPendingIntent
                )

                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    endCalendar.timeInMillis,
                    endPendingIntent
                )
            } else {
                // Para versiones anteriores
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    startCalendar.timeInMillis,
                    startPendingIntent
                )

                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    endCalendar.timeInMillis,
                    endPendingIntent
                )
            }

            Log.d("ScheduleManager", "Alarmas programadas: inicio ${startCalendar.time}, fin ${endCalendar.time}")

            // Verificar el estado actual e iniciar/detener servicio inmediatamente
            val shouldBeActive = shouldServiceBeActive()
            updateServiceState(shouldBeActive)

        } catch (e: Exception) {
            Log.e("ScheduleManager", "Error al programar alarmas exactas: ${e.message}")

            // Intentar con método menos exacto como respaldo
            try {
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

                Log.d("ScheduleManager", "Alarmas programadas con método alternativo")
            } catch (e2: Exception) {
                Log.e("ScheduleManager", "Error fatal al programar alarmas: ${e2.message}")
            }
        }
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

    // Actualizar estado del servicio
    private fun updateServiceState(activate: Boolean) {
        NotificationService.isServiceActive = activate

        // Iniciar/detener el servicio explícitamente
        val serviceIntent = Intent(context, NotificationService::class.java).apply {
            action = if (activate) {
                NotificationService.ACTION_START_SERVICE
            } else {
                NotificationService.ACTION_STOP_SERVICE
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e("ScheduleManager", "Error al cambiar estado del servicio: ${e.message}")
        }

        // Notificar a la UI
        val updateIntent = Intent(MainActivity.updateStatusAction).apply {
            setPackage(context.packageName)
            putExtra("service_state", activate)
            putExtra("schedule_activated", true)
        }
        context.sendBroadcast(updateIntent)
    }

    // Comprobar si el servicio debería estar activo según la hora actual
    fun shouldServiceBeActive(): Boolean {
        if (!isScheduleEnabled()) return true

        val startHour = getStartHour()
        val startMinute = getStartMinute()
        val endHour = getEndHour()
        val endMinute = getEndMinute()

        val currentTime = Calendar.getInstance()
        val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
        val currentMinute = currentTime.get(Calendar.MINUTE)

        val currentTimeValue = currentHour * 60 + currentMinute
        val startTimeValue = startHour * 60 + startMinute
        val endTimeValue = endHour * 60 + endMinute

        // Verificar si el día actual está habilitado
        val currentDay = currentTime.get(Calendar.DAY_OF_WEEK)
        if (!isDayEnabled(currentDay)) {
            return false
        }

        // Determinar si estamos en el período activo
        return if (startTimeValue <= endTimeValue) {
            // Caso normal: inicio < fin (mismo día)
            currentTimeValue in startTimeValue..endTimeValue
        } else {
            // Caso especial: fin < inicio (por ejemplo, 22:00 a 06:00)
            currentTimeValue >= startTimeValue || currentTimeValue <= endTimeValue
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