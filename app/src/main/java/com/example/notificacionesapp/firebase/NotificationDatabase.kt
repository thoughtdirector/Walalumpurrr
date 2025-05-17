package com.example.notificacionesapp.firebase

import android.util.Log
import com.example.notificacionesapp.model.FirestoreNotification
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class NotificationDatabase {
    private val db = Firebase.firestore
    private val notificationsCollection = db.collection("notifications")

    companion object {
        private const val TAG = "NotificationDatabase"
    }

    // Obtener las últimas notificaciones para un admin (con paginación)
    fun getLatestNotifications(
        adminId: String,
        limit: Int = 20,
        callback: (List<FirestoreNotification>) -> Unit
    ) {
        notificationsCollection
            .whereEqualTo("adminId", adminId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .addOnSuccessListener { documents ->
                val notifications = mutableListOf<FirestoreNotification>()

                for (document in documents) {
                    val notification = document.toObject(FirestoreNotification::class.java)
                    notifications.add(notification)
                }

                callback(notifications)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al cargar notificaciones: ${e.message}")
                callback(emptyList())
            }
    }

    // Crear/guardar una nueva notificación
    fun saveNotification(
        notification: FirestoreNotification,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val notificationId = notification.id

        notificationsCollection.document(notificationId)
            .set(notification)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    // Marcar notificación como leída
    fun markAsRead(
        adminId: String,
        notificationId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        notificationsCollection.document(notificationId)
            .update("read", true)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    // Buscar notificaciones por tipo
    fun getNotificationsByType(
        adminId: String,
        type: String,
        callback: (List<FirestoreNotification>) -> Unit
    ) {
        notificationsCollection
            .whereEqualTo("adminId", adminId)
            .whereEqualTo("type", type)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val notifications = mutableListOf<FirestoreNotification>()

                for (document in documents) {
                    val notification = document.toObject(FirestoreNotification::class.java)
                    notifications.add(notification)
                }

                callback(notifications)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al cargar notificaciones por tipo: ${e.message}")
                callback(emptyList())
            }
    }

    // Eliminar notificaciones antiguas (retención de datos)
    fun cleanupOldNotifications(adminId: String, olderThanTimestamp: Long) {
        val timestampLimit = Timestamp(olderThanTimestamp / 1000, 0)

        notificationsCollection
            .whereEqualTo("adminId", adminId)
            .whereLessThan("timestamp", timestampLimit)
            .get()
            .addOnSuccessListener { documents ->
                val batch = db.batch()
                for (document in documents) {
                    batch.delete(document.reference)
                }

                if (documents.size() > 0) {
                    batch.commit()
                        .addOnSuccessListener {
                            Log.d(TAG, "Limpieza completada: ${documents.size()} notificaciones eliminadas")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error al ejecutar lote de eliminación: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al buscar notificaciones antiguas: ${e.message}")
            }
    }
}