package com.example.notificacionesapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

class ScheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d("ScheduleReceiver", "Alarma recibida: $action")

        val scheduleManager = ScheduleManager(context)

        when (action) {
            ScheduleManager.ACTION_START_SERVICE -> {
                if (scheduleManager.isScheduleEnabled() &&
                    scheduleManager.isDayEnabled(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))) {
                    NotificationService.isServiceActive = true
                    Log.d("ScheduleReceiver", "Servicio activado por horario")

                    // Enviar broadcast para notificar a la UI con datos adicionales
                    val updateIntent = Intent(MainActivity.updateStatusAction).apply {
                        setPackage(context.packageName)
                        putExtra("service_state", true) // Indicar que el servicio está activo
                        putExtra("schedule_activated", true) // Activado por horario
                    }
                    context.sendBroadcast(updateIntent)
                }

                // Programar la próxima alarma para mañana o el próximo día habilitado
                rescheduleAlarm(context, action)
            }
            ScheduleManager.ACTION_STOP_SERVICE -> {
                if (scheduleManager.isScheduleEnabled()) {
                    NotificationService.isServiceActive = false
                    Log.d("ScheduleReceiver", "Servicio desactivado por horario")

                    // Enviar broadcast para notificar a la UI con datos adicionales
                    val updateIntent = Intent(MainActivity.updateStatusAction).apply {
                        setPackage(context.packageName)
                        putExtra("service_state", false) // Indicar que el servicio está inactivo
                        putExtra("schedule_activated", true) // Desactivado por horario
                    }
                    context.sendBroadcast(updateIntent)
                }

                // Programar la próxima alarma para mañana o el próximo día habilitado
                rescheduleAlarm(context, action)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                // Manejar el arranque del dispositivo
                if (scheduleManager.isScheduleEnabled()) {
                    Log.d("ScheduleReceiver", "Reinicio del dispositivo, restaurando alarmas")
                    scheduleManager.setScheduleAlarms()

                    // Verificar si el servicio debería estar activo ahora
                    val shouldBeActive = scheduleManager.shouldServiceBeActive()
                    NotificationService.isServiceActive = shouldBeActive

                    // Notificar a la UI si está abierta
                    val updateIntent = Intent(MainActivity.updateStatusAction).apply {
                        setPackage(context.packageName)
                        putExtra("service_state", shouldBeActive)
                        putExtra("boot_completed", true)
                    }
                    context.sendBroadcast(updateIntent)
                }
            }
        }
    }

    private fun rescheduleAlarm(context: Context, action: String) {
        val scheduleManager = ScheduleManager(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Crear un nuevo intent para la próxima alarma
        val newIntent = Intent(context, ScheduleReceiver::class.java).apply {
            this.action = action
        }

        val requestCode = if (action == ScheduleManager.ACTION_START_SERVICE) 1 else 2

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            newIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calcular la hora para mañana o el próximo día habilitado
        val calendar = Calendar.getInstance()

        if (action == ScheduleManager.ACTION_START_SERVICE) {
            calendar.set(Calendar.HOUR_OF_DAY, scheduleManager.getStartHour())
            calendar.set(Calendar.MINUTE, scheduleManager.getStartMinute())
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, scheduleManager.getEndHour())
            calendar.set(Calendar.MINUTE, scheduleManager.getEndMinute())
        }
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Si la hora ya pasó hoy, programar para mañana
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Buscar el próximo día habilitado (puede que no sea mañana)
        var dayToAdd = 0
        var daysChecked = 0
        var foundEnabledDay = false

        while (daysChecked < 7 && !foundEnabledDay) {
            val checkDay = (calendar.get(Calendar.DAY_OF_WEEK) + dayToAdd) % 7
            val dayToCheck = if (checkDay == 0) 7 else checkDay // Convertir 0 a 7 (domingo)

            if (scheduleManager.isDayEnabled(dayToCheck)) {
                if (dayToAdd > 0) {
                    calendar.add(Calendar.DAY_OF_YEAR, dayToAdd)
                }
                foundEnabledDay = true
            } else {
                dayToAdd++
                daysChecked++
            }
        }

        if (!foundEnabledDay) {
            Log.w("ScheduleReceiver", "No se encontraron días habilitados para programar alarma")
            return
        }

        // Configurar la alarma para el próximo día habilitado - usar método compatible con todas las versiones
        try {
            // Intentar usar alarmas exactas si está disponible
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    // Fallback a alarma no exacta
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }

            Log.d("ScheduleReceiver", "Próxima alarma programada para ${calendar.time}")
        } catch (e: Exception) {
            Log.e("ScheduleReceiver", "Error al programar alarma: ${e.message}")
            // Fallback a alarma no exacta
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }
}