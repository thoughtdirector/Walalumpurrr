package com.example.notificacionesapp.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.notificacionesapp.SessionManager
import com.example.notificacionesapp.firebase.NotificationDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val sessionManager = SessionManager(context)
    private val notificationDatabase = NotificationDatabase()

    companion object {
        private const val TAG = "NotificationCleanup"
        const val WORK_NAME = "notification_cleanup_work"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Iniciando limpieza de notificaciones antiguas")

            val userId = sessionManager.getUserId()
            val role = sessionManager.getUserRole()

            if (userId != null && role == "admin") {
                // Limpiar notificaciones de más de 30 días
                val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000)
                notificationDatabase.cleanupOldNotifications(userId, thirtyDaysAgo)
                Log.d(TAG, "Limpieza completada exitosamente")
            } else {
                Log.d(TAG, "Usuario no es admin o no está logueado, no se realiza limpieza")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la limpieza de notificaciones: ${e.message}")
            Result.retry()
        }
    }
}