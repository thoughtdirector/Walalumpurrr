package com.example.notificacionesapp.domain.model

import java.util.Date

/**
 * Domain model representing a processed notification
 */
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

/**
 * Types of notifications that can be processed
 */
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

/**
 * Extension function to get formatted amount
 */
fun Notification.getFormattedAmount(): String {
    return if (amount.isNotEmpty()) {
        "$${amount.replace(Regex("[^0-9]"), "")}"
    } else {
        ""
    }
}

/**
 * Extension function to check if notification has amount
 */
fun Notification.hasAmount(): Boolean = amount.isNotEmpty()

/**
 * Extension function to get display title
 */
fun Notification.getDisplayTitle(): String {
    return when (type) {
        NotificationType.PAYMENT_RECEIVED -> "Pago Recibido"
        NotificationType.PAYMENT_SENT -> "Pago Enviado"
        NotificationType.TRANSFER_RECEIVED -> "Transferencia Recibida"
        NotificationType.TRANSFER_SENT -> "Transferencia Enviada"
        NotificationType.BALANCE_UPDATE -> "Actualización de Saldo"
        NotificationType.PROMOTION -> "Promoción"
        NotificationType.SECURITY_ALERT -> "Alerta de Seguridad"
        NotificationType.OTHER -> title
    }
}
