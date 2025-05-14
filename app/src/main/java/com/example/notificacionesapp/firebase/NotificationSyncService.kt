package com.example.notificacionesapp.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.notificacionesapp.MainActivity
import com.example.notificacionesapp.R
import com.example.notificacionesapp.SessionManager
import com.example.notificacionesapp.model.FirestoreNotification
import com.example.notificacionesapp.model.NotificationItem
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.UUID

class NotificationSyncService(private val context: Context) {

    private val db = Firebase.firestore
    private val sessionManager = SessionManager(context)
    private var listenerRegistration: ListenerRegistration? = null
    private val notificationDatabase = NotificationDatabase()
    private val adminNotificationService = AdminNotificationService()

    companion object {
        private const val TAG = "NotificationSyncService"
        private const val NOTIFICATION_CHANNEL_ID = "sync_notification_channel"
    }

    // Guardar una notificación en Firestore
    fun saveNotification(
        packageName: String,
        appName: String,
        title: String,
        content: String,
        type: String,
        amount: String = "",
        sender: String = ""
    ) {
        try {
            val userId = sessionManager.getUserId()
            if (userId == null) {
                Log.e(TAG, "Failed to save notification: No user ID found in session")
                return
            }

            val role = sessionManager.getUserRole() ?: "user"
            Log.d(TAG, "Saving notification as role: $role, userId: $userId")

            // Determine the adminId
            val adminId = if (role == "admin") {
                userId // Current user is admin
            } else {
                // Get adminId from preferences
                val adminIdFromPrefs = sessionManager.getUserDetails()["adminId"]
                if (adminIdFromPrefs == null) {
                    Log.e(TAG, "Failed to save notification: Employee has no adminId")
                    return
                }
                adminIdFromPrefs
            }

            Log.d(TAG, "Using adminId: $adminId for notification")

            // Create notification object
            val notificationId = UUID.randomUUID().toString()
            val notification = FirestoreNotification(
                id = notificationId,
                adminId = adminId,
                packageName = packageName,
                appName = appName,
                title = title,
                content = content,
                type = type,
                amount = amount,
                sender = sender,
                read = false,
                timestamp = Timestamp.now()
            )

            // Save using NotificationDatabase
            notificationDatabase.saveNotification(
                notification = notification,
                onSuccess = {
                    Log.d(TAG, "Notification saved successfully to Firestore with id: ${notification.id}")

                    // Notify employees if administrator
                    if (role == "admin") {
                        Log.d(TAG, "Attempting to notify employees as admin")
                        adminNotificationService.notifyEmployees(adminId, notification)
                    } else {
                        Log.d(TAG, "Skipping employee notification as user is not admin")
                    }

                    // Clean up old notifications (more than 30 days)
                    val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000)
                    notificationDatabase.cleanupOldNotifications(adminId, thirtyDaysAgo)
                },
                onError = { e ->
                    Log.e(TAG, "Error saving notification to Firestore: ${e.message}")
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in saveNotification: ${e.message}", e)
        }
    }

    // Empezar a escuchar actualizaciones para un admin o empleado
    fun startListeningForNotifications(onNotificationReceived: (List<NotificationItem>) -> Unit) {
        try {
            val userId = sessionManager.getUserId() ?: return
            val role = sessionManager.getUserRole() ?: "user"

            // Determinar el adminId para suscribirse
            val adminId = if (role == "admin") {
                userId // El usuario actual es admin
            } else {
                // Buscar el adminId del empleado desde preferencias
                sessionManager.getUserDetails()["adminId"] ?: return
            }

            // Obtener notificaciones usando NotificationDatabase
            notificationDatabase.getLatestNotifications(adminId) { firestoreNotifications ->
                val notifications = mutableListOf<NotificationItem>()
                var hasNewNotifications = false
                val lastSyncTime = sessionManager.getLastNotificationSyncTime()

                for (fbNotification in firestoreNotifications) {
                    // Verificar si es una notificación nueva
                    val notificationTimestamp = fbNotification.timestamp?.toDate()?.time ?: 0
                    if (notificationTimestamp > lastSyncTime && !fbNotification.read) {
                        hasNewNotifications = true
                    }

                    val notificationItem = NotificationItem(
                        appName = fbNotification.appName,
                        title = fbNotification.title,
                        content = fbNotification.content,
                        timestamp = notificationTimestamp,
                        sender = fbNotification.sender,
                        amount = fbNotification.amount,
                        id = fbNotification.id,
                        isRead = fbNotification.read
                    )
                    notifications.add(notificationItem)
                }

                // Actualizar el tiempo de última sincronización
                if (notifications.isNotEmpty()) {
                    sessionManager.saveLastNotificationSyncTime(System.currentTimeMillis())
                }

                // Notificar al callback
                onNotificationReceived(notifications)

                // Si hay notificaciones nuevas y el usuario es un empleado, mostrar notificación local
                if (hasNewNotifications && sessionManager.getUserRole() == "employee" && notifications.isNotEmpty()) {
                    showNewNotificationsAlert(notifications.first())
                }
            }

            // También configurar un SnapshotListener para actualizaciones en tiempo real
            setupRealtimeListener(adminId, onNotificationReceived)

        } catch (e: Exception) {
            Log.e(TAG, "Error en startListeningForNotifications: ${e.message}")
        }
    }

    // Detener la escucha de notificaciones
    fun stopListeningForNotifications() {
        try {
            listenerRegistration?.remove()
            listenerRegistration = null
            Log.d(TAG, "Listener de notificaciones detenido")
        } catch (e: Exception) {
            Log.e(TAG, "Error en stopListeningForNotifications: ${e.message}")
        }
    }

    // Marcar una notificación como leída
    fun markNotificationAsRead(notificationId: String) {
        try {
            val userId = sessionManager.getUserId() ?: return
            val role = sessionManager.getUserRole() ?: "user"

            // Determinar el adminId
            val adminId = if (role == "admin") {
                userId
            } else {
                sessionManager.getUserDetails()["adminId"] ?: return
            }

            // Marcar como leída usando NotificationDatabase
            notificationDatabase.markAsRead(
                adminId = adminId,
                notificationId = notificationId,
                onSuccess = {
                    Log.d(TAG, "Notificación marcada como leída")
                },
                onError = { e ->
                    Log.e(TAG, "Error al marcar notificación como leída: ${e.message}")
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error en markNotificationAsRead: ${e.message}")
        }
    }

    // Mostrar una notificación local al usuario
    private fun showNewNotificationsAlert(latestNotification: NotificationItem) {
        try {
            // Usar NotificationManager para mostrar una notificación local
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Crear canal para Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Notificaciones sincronizadas",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                notificationManager.createNotificationChannel(channel)
            }

            // Crear intent para abrir la app
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra("openHistoryTab", true)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Crear notificación
            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(latestNotification.appName)
                .setContentText(latestNotification.content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            // Mostrar notificación
            notificationManager.notify(1001, notification)
            Log.d(TAG, "Notificación local mostrada al usuario")
        } catch (e: Exception) {
            Log.e(TAG, "Error al mostrar notificación local: ${e.message}")
        }
    }

    // Configurar listener en tiempo real para actualizaciones
    private fun setupRealtimeListener(
        adminId: String,
        onNotificationReceived: (List<NotificationItem>) -> Unit
    ) {
        try {
            // Detener listener previo si existe
            stopListeningForNotifications()

            // Crear nuevo listener
            listenerRegistration = db.collection("notifications")
                .whereEqualTo("adminId", adminId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error en listener de notificaciones: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val notifications = mutableListOf<NotificationItem>()
                        var hasNewNotifications = false
                        val lastSyncTime = sessionManager.getLastNotificationSyncTime()

                        for (document in snapshot.documents) {
                            val firestoreNotification = document.toObject(FirestoreNotification::class.java)
                            firestoreNotification?.let {
                                // Verificar si es una notificación nueva
                                val notificationTimestamp = it.timestamp?.toDate()?.time ?: 0
                                if (notificationTimestamp > lastSyncTime && !it.read) {
                                    hasNewNotifications = true
                                }

                                val notificationItem = NotificationItem(
                                    appName = it.appName,
                                    title = it.title,
                                    content = it.content,
                                    timestamp = notificationTimestamp,
                                    sender = it.sender,
                                    amount = it.amount,
                                    id = it.id,
                                    isRead = it.read
                                )
                                notifications.add(notificationItem)
                            }
                        }

                        // Ordenar por timestamp (más recientes primero)
                        notifications.sortByDescending { it.timestamp }

                        // Actualizar el tiempo de última sincronización si hay nuevas notificaciones
                        if (hasNewNotifications) {
                            sessionManager.saveLastNotificationSyncTime(System.currentTimeMillis())
                        }

                        // Notificar al callback
                        onNotificationReceived(notifications)

                        // Si hay notificaciones nuevas y el usuario es un empleado, mostrar notificación local
                        if (hasNewNotifications && sessionManager.getUserRole() == "employee" && notifications.isNotEmpty()) {
                            showNewNotificationsAlert(notifications.first())
                        }
                    }
                }

            Log.d(TAG, "Listener en tiempo real configurado para notificaciones")
        } catch (e: Exception) {
            Log.e(TAG, "Error al configurar listener en tiempo real: ${e.message}")
        }
    }

    // Obtener notificaciones filtradas por tipo
    fun getNotificationsByType(type: String, onNotificationsReceived: (List<NotificationItem>) -> Unit) {
        try {
            val userId = sessionManager.getUserId() ?: return
            val role = sessionManager.getUserRole() ?: "user"

            // Determinar el adminId
            val adminId = if (role == "admin") {
                userId
            } else {
                sessionManager.getUserDetails()["adminId"] ?: return
            }

            // Obtener notificaciones del tipo específico
            notificationDatabase.getNotificationsByType(adminId, type) { firestoreNotifications ->
                val notifications = firestoreNotifications.map { fbNotification ->
                    NotificationItem(
                        appName = fbNotification.appName,
                        title = fbNotification.title,
                        content = fbNotification.content,
                        timestamp = fbNotification.timestamp?.toDate()?.time ?: 0,
                        sender = fbNotification.sender,
                        amount = fbNotification.amount,
                        id = fbNotification.id,
                        isRead = fbNotification.read
                    )
                }

                onNotificationsReceived(notifications)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en getNotificationsByType: ${e.message}")
            onNotificationsReceived(emptyList())
        }
    }

    // Marcar todas las notificaciones como leídas
    fun markAllNotificationsAsRead(onComplete: () -> Unit) {
        try {
            val userId = sessionManager.getUserId() ?: return
            val role = sessionManager.getUserRole() ?: "user"

            // Determinar el adminId
            val adminId = if (role == "admin") {
                userId
            } else {
                sessionManager.getUserDetails()["adminId"] ?: return
            }

            // Obtener todas las notificaciones no leídas
            db.collection("notifications")
                .whereEqualTo("adminId", adminId)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener { documents ->
                    val batch = db.batch()

                    // Preparar actualizaciones en lote
                    for (document in documents) {
                        batch.update(document.reference, "read", true)
                    }

                    // Aplicar actualizaciones en lote si hay documentos
                    if (!documents.isEmpty) {
                        batch.commit()
                            .addOnSuccessListener {
                                Log.d(TAG, "Todas las notificaciones marcadas como leídas")
                                onComplete()
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error al marcar notificaciones como leídas: ${e.message}")
                                onComplete()
                            }
                    } else {
                        // No hay notificaciones para actualizar
                        onComplete()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al buscar notificaciones no leídas: ${e.message}")
                    onComplete()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error en markAllNotificationsAsRead: ${e.message}")
            onComplete()
        }
    }

    // Eliminar una notificación específica
    fun deleteNotification(notificationId: String, onComplete: (Boolean) -> Unit) {
        try {
            val userId = sessionManager.getUserId() ?: return onComplete(false)
            val role = sessionManager.getUserRole() ?: "user"

            // Solo los administradores pueden eliminar notificaciones
            if (role != "admin") {
                Log.w(TAG, "Solo los administradores pueden eliminar notificaciones")
                onComplete(false)
                return
            }

            // Eliminar la notificación
            db.collection("notifications").document(notificationId)
                .delete()
                .addOnSuccessListener {
                    Log.d(TAG, "Notificación eliminada correctamente")
                    onComplete(true)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al eliminar notificación: ${e.message}")
                    onComplete(false)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error en deleteNotification: ${e.message}")
            onComplete(false)
        }
    }

    // Enviar una notificación manual a todos los empleados
    fun sendManualNotificationToEmployees(
        title: String,
        content: String,
        onComplete: (Boolean) -> Unit
    ) {
        try {
            val userId = sessionManager.getUserId() ?: return onComplete(false)
            val role = sessionManager.getUserRole() ?: "user"

            // Solo los administradores pueden enviar notificaciones manuales
            if (role != "admin") {
                Log.w(TAG, "Solo los administradores pueden enviar notificaciones manuales")
                onComplete(false)
                return
            }

            // Crear notificación manual
            val notificationId = UUID.randomUUID().toString()
            val notification = FirestoreNotification(
                id = notificationId,
                adminId = userId,
                packageName = "manual_notification",
                appName = "Notificación Manual",
                title = title,
                content = content,
                type = "MANUAL",
                amount = "",
                sender = "",
                read = false,
                timestamp = Timestamp.now()
            )

            // Guardar la notificación
            notificationDatabase.saveNotification(
                notification = notification,
                onSuccess = {
                    Log.d(TAG, "Notificación manual guardada correctamente")
                    // Notificar a empleados
                    adminNotificationService.notifyEmployees(userId, notification)
                    onComplete(true)
                },
                onError = { e ->
                    Log.e(TAG, "Error al guardar notificación manual: ${e.message}")
                    onComplete(false)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error en sendManualNotificationToEmployees: ${e.message}")
            onComplete(false)
        }
    }
}