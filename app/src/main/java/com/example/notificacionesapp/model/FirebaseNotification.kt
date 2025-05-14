package com.example.notificacionesapp.model

data class FirebaseNotification(
    val id: String = "",
    val timestamp: Long = 0,
    val adminId: String = "",
    val packageName: String = "",
    val appName: String = "",
    val title: String = "",
    val content: String = "",
    val type: String = "",
    val amount: String = "",
    val sender: String = "",
    val read: Boolean = false
) {
    constructor() : this("", 0, "", "", "", "", "", "", "", "", false)
}