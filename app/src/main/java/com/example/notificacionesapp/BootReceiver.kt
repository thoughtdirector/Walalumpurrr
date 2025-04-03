package com.example.notificacionesapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Dispositivo reiniciado, configurando alarmas")
            val scheduleManager = ScheduleManager(context)

            if (scheduleManager.isScheduleEnabled()) {
                // Programar las alarmas después del reinicio
                scheduleManager.setScheduleAlarms()

                // Verificar si el servicio debería estar activo
                val shouldBeActive = scheduleManager.shouldServiceBeActive()
                NotificationService.isServiceActive = shouldBeActive

                // Iniciar el servicio si corresponde
                if (shouldBeActive) {
                    val serviceIntent = Intent(context, NotificationService::class.java).apply {
                        action = NotificationService.ACTION_START_SERVICE
                    }

                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
}