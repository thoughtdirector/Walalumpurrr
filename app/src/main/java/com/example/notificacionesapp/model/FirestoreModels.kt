package com.example.notificacionesapp.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class FirestoreUser(
    @DocumentId val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val birthDate: String = "",
    val role: String = "user",
    val adminId: String? = null,
    val fcmToken: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class FirestoreNotification(
    @DocumentId val id: String = "",
    val adminId: String = "",
    val packageName: String = "",
    val appName: String = "",
    val title: String = "",
    val content: String = "",
    val type: String = "",
    val amount: String = "",
    val sender: String = "",
    val read: Boolean = false,
    @ServerTimestamp val timestamp: Timestamp? = null
)