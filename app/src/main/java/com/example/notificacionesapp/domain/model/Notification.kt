package com.example.notificacionesapp.domain.model

import java.util.Date

data class Notification(
    val id: String = "",
    val packageName: String,
    val appName: String,
    val title: String,
    val content: String,
    val type: NotificationType,
    val amount: String = "",
    val sender: String = "",
    val adminId: String? = null,
    val timestamp: Date = Date(),
    val isProcessed: Boolean = true
)

enum class NotificationType {
    PAYMENT_RECEIVED,
    PAYMENT_SENT,
    TRANSFER_RECEIVED,
    TRANSFER_SENT,
    BALANCE_UPDATE,
    PROMOTION,
    SECURITY_ALERT,
    OTHER
}
