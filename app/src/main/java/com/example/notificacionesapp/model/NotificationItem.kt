package com.example.notificacionesapp.model

data class NotificationItem(
    val appName: String,
    val title: String,
    val content: String,
    val timestamp: Long,
    val sender: String? = null,
    val amount: String? = null
)