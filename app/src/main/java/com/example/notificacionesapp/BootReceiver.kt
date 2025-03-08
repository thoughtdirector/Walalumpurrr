package com.example.notificacionesapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Dispositivo reiniciado, configurando alarmas")
            val scheduleManager = ScheduleManager(context)

            if (scheduleManager.isScheduleEnabled()) {
                scheduleManager.setScheduleAlarms()
            }
        }
    }
}