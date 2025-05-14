package com.example.notificacionesapp.firebase

import android.util.Log
import com.example.notificacionesapp.model.FirebaseNotification
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener

class NotificationDatabase {
    private val database = FirebaseDatabase.getInstance()
    private val notificationsRef = database.getReference("notifications")

    companion object {
        private const val TAG = "NotificationDatabase"
    }

    // Obtener las últimas notificaciones para un admin (con paginación)
    fun getLatestNotifications(
        adminId: String,
        limit: Int = 20,
        callback: (List<FirebaseNotification>) -> Unit
    ) {
        val query = notificationsRef.child(adminId)
            .orderByChild("timestamp")
            .limitToLast(limit)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notifications = mutableListOf<FirebaseNotification>()

                for (notificationSnapshot in snapshot.children) {
                    val notification = notificationSnapshot.getValue(FirebaseNotification::class.java)
                    notification?.let {
                        notifications.add(it)
                    }
                }

                // Orden descendente (más recientes primero)
                notifications.sortByDescending { it.timestamp }

                callback(notifications)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al cargar notificaciones: ${error.message}")
                callback(emptyList())
            }
        })
    }

    // Crear/guardar una nueva notificación
    fun saveNotification(
        notification: FirebaseNotification,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val notificationRef = notificationsRef.child(notification.adminId).child(notification.id)

        notificationRef.setValue(notification)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    // Marcar notificación como leída
    fun markAsRead(
        adminId: String,
        notificationId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val updates = hashMapOf<String, Any>(
            "read" to true
        )

        notificationsRef.child(adminId).child(notificationId)
            .updateChildren(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    // Buscar notificaciones por tipo
    fun getNotificationsByType(
        adminId: String,
        type: String,
        callback: (List<FirebaseNotification>) -> Unit
    ) {
        val query = notificationsRef.child(adminId)
            .orderByChild("type")
            .equalTo(type)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notifications = mutableListOf<FirebaseNotification>()

                for (notificationSnapshot in snapshot.children) {
                    val notification = notificationSnapshot.getValue(FirebaseNotification::class.java)
                    notification?.let {
                        notifications.add(it)
                    }
                }

                // Orden descendente (más recientes primero)
                notifications.sortByDescending { it.timestamp }

                callback(notifications)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al cargar notificaciones por tipo: ${error.message}")
                callback(emptyList())
            }
        })
    }

    // Eliminar notificaciones antiguas (retención de datos)
    fun cleanupOldNotifications(adminId: String, olderThanTimestamp: Long) {
        val query = notificationsRef.child(adminId)
            .orderByChild("timestamp")
            .endAt(olderThanTimestamp.toDouble())

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (notificationSnapshot in snapshot.children) {
                    notificationSnapshot.ref.removeValue()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al limpiar notificaciones antiguas: ${error.message}")
            }
        })
    }
}