package com.example.notificacionesapp.firebase

import android.util.Log
import com.example.notificacionesapp.model.FirestoreNotification
import com.example.notificacionesapp.model.FirestoreUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.UUID

class FirestoreService {
    private val db: FirebaseFirestore = Firebase.firestore

    companion object {
        private const val TAG = "FirestoreService"
        const val USERS_COLLECTION = "users"
        const val NOTIFICATIONS_COLLECTION = "notifications"
    }

    // User operations methods
    fun saveUser(user: FirestoreUser, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        db.collection(USERS_COLLECTION).document(user.uid)
            .set(user)
            .addOnSuccessListener {
                Log.d(TAG, "User saved with ID: ${user.uid}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving user: ${e.message}")
                onError(e)
            }
    }

    fun getUserById(userId: String, onSuccess: (FirestoreUser?) -> Unit, onError: (Exception) -> Unit) {
        db.collection(USERS_COLLECTION).document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(FirestoreUser::class.java)
                    onSuccess(user)
                } else {
                    onSuccess(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting user: ${e.message}")
                onError(e)
            }
    }

    fun updateUserField(userId: String, field: String, value: Any, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        db.collection(USERS_COLLECTION).document(userId)
            .update(field, value)
            .addOnSuccessListener {
                Log.d(TAG, "User field updated: $field")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating user field: ${e.message}")
                onError(e)
            }
    }

    fun getEmployeesByAdminId(adminId: String, onSuccess: (List<FirestoreUser>) -> Unit, onError: (Exception) -> Unit) {
        db.collection(USERS_COLLECTION)
            .whereEqualTo("adminId", adminId)
            .get()
            .addOnSuccessListener { documents ->
                val employees = documents.toObjects(FirestoreUser::class.java)
                onSuccess(employees)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting employees: ${e.message}")
                onError(e)
            }
    }

    // Notification operations methods
    fun saveNotification(notification: FirestoreNotification, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        val notificationId = notification.id.ifEmpty { UUID.randomUUID().toString() }

        val notificationToSave = if (notification.id.isEmpty()) {
            notification.copy(id = notificationId)
        } else {
            notification
        }

        db.collection(NOTIFICATIONS_COLLECTION).document(notificationId)
            .set(notificationToSave)
            .addOnSuccessListener {
                Log.d(TAG, "Notification saved with ID: $notificationId")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving notification: ${e.message}")
                onError(e)
            }
    }

    fun getNotificationsByAdminId(
        adminId: String,
        limit: Long = 50,
        onSuccess: (List<FirestoreNotification>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection(NOTIFICATIONS_COLLECTION)
            .whereEqualTo("adminId", adminId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .addOnSuccessListener { documents ->
                val notifications = documents.toObjects(FirestoreNotification::class.java)
                onSuccess(notifications)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting notifications: ${e.message}")
                onError(e)
            }
    }

    fun getNotificationsByType(
        adminId: String,
        type: String,
        onSuccess: (List<FirestoreNotification>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection(NOTIFICATIONS_COLLECTION)
            .whereEqualTo("adminId", adminId)
            .whereEqualTo("type", type)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val notifications = documents.toObjects(FirestoreNotification::class.java)
                onSuccess(notifications)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting notifications by type: ${e.message}")
                onError(e)
            }
    }

    fun markNotificationAsRead(notificationId: String, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        db.collection(NOTIFICATIONS_COLLECTION).document(notificationId)
            .update("read", true)
            .addOnSuccessListener {
                Log.d(TAG, "Notification marked as read: $notificationId")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error marking notification as read: ${e.message}")
                onError(e)
            }
    }

    fun addNotificationsListener(
        adminId: String,
        onUpdate: (List<FirestoreNotification>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection(NOTIFICATIONS_COLLECTION)
            .whereEqualTo("adminId", adminId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for notifications: ${error.message}")
                    onError(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val notifications = snapshot.toObjects(FirestoreNotification::class.java)
                    onUpdate(notifications)
                }
            }
    }
}