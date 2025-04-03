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
                    // Activar servicio directamente
                    val serviceIntent = Intent(context, NotificationService::class.java)
                    serviceIntent.action = NotificationService.ACTION_START_SERVICE

                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }

                        Log.d("ScheduleReceiver", "Servicio iniciado por horario")

                        // Notificar a la UI
                        val updateIntent = Intent(MainActivity.updateStatusAction)
                        updateIntent.setPackage(context.packageName)
                        updateIntent.putExtra("service_state", true)
                        updateIntent.putExtra("schedule_activated", true)
                        context.sendBroadcast(updateIntent)
                    } catch (e: Exception) {
                        Log.e("ScheduleReceiver", "Error al iniciar servicio: ${e.message}")
                    }
                }

                // Programar la próxima alarma
                rescheduleAlarm(context, action)
            }

            ScheduleManager.ACTION_STOP_SERVICE -> {
                if (scheduleManager.isScheduleEnabled()) {
                    // Detener servicio directamente
                    val serviceIntent = Intent(context, NotificationService::class.java)
                    serviceIntent.action = NotificationService.ACTION_STOP_SERVICE

                    try {
                        context.startService(serviceIntent)
                        Log.d("ScheduleReceiver", "Servicio detenido por horario")

                        // Notificar a la UI
                        val updateIntent = Intent(MainActivity.updateStatusAction)
                        updateIntent.setPackage(context.packageName)
                        updateIntent.putExtra("service_state", false)
                        updateIntent.putExtra("schedule_activated", true)
                        context.sendBroadcast(updateIntent)
                    } catch (e: Exception) {
                        Log.e("ScheduleReceiver", "Error al detener servicio: ${e.message}")
                    }
                }

                // Programar la próxima alarma
                rescheduleAlarm(context, action)
            }

            Intent.ACTION_BOOT_COMPLETED -> {
                // Manejar el arranque del dispositivo
                if (scheduleManager.isScheduleEnabled()) {
                    Log.d("ScheduleReceiver", "Reinicio del dispositivo, restaurando alarmas")
                    scheduleManager.setScheduleAlarms()

                    // Verificar si el servicio debería estar activo ahora
                    val shouldBeActive = scheduleManager.shouldServiceBeActive()

                    // Iniciar o detener el servicio según corresponda
                    val serviceIntent = Intent(context, NotificationService::class.java)
                    serviceIntent.action = if (shouldBeActive) {
                        NotificationService.ACTION_START_SERVICE
                    } else {
                        NotificationService.ACTION_STOP_SERVICE
                    }

                    try {
                        if (shouldBeActive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                        Log.d("BootReceiver", "Servicio iniciado después de reinicio")
                    } catch (e: Exception) {
                        Log.e("BootReceiver", "Error al iniciar servicio después de reinicio: ${e.message}")
                    }
                }
            }
        }
    }

    private fun rescheduleAlarm(context: Context, action: String) {
        val scheduleManager = ScheduleManager(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Crear un nuevo intent para la próxima alarma
        val newIntent = Intent(context, ScheduleReceiver::class.java)
        newIntent.action = action

        val requestCode = if (action == ScheduleManager.ACTION_START_SERVICE) 1 else 2

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            newIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calcular la hora para el próximo día habilitado
        val targetHour = if (action == ScheduleManager.ACTION_START_SERVICE) {
            scheduleManager.getStartHour()
        } else {
            scheduleManager.getEndHour()
        }

        val targetMinute = if (action == ScheduleManager.ACTION_START_SERVICE) {
            scheduleManager.getStartMinute()
        } else {
            scheduleManager.getEndMinute()
        }

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, targetHour)
        calendar.set(Calendar.MINUTE, targetMinute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Si la hora ya pasó hoy, programar para mañana
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Buscar el próximo día habilitado
        var daysChecked = 0
        var foundEnabledDay = false

        while (daysChecked < 7 && !foundEnabledDay) {
            val dayToCheck = calendar.get(Calendar.DAY_OF_WEEK)

            if (scheduleManager.isDayEnabled(dayToCheck)) {
                foundEnabledDay = true
            } else {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                daysChecked++
            }
        }

        if (!foundEnabledDay) {
            Log.w("ScheduleReceiver", "No se encontraron días habilitados para programar alarma")
            return
        }

        // Configurar la alarma para el próximo día habilitado
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
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