package com.example.notificacionesapp.core.notification.processors

import com.example.notificacionesapp.core.notification.NotificationProcessor
import com.example.notificacionesapp.core.notification.ProcessedNotification
import com.example.notificacionesapp.domain.model.NotificationType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Processor for Nequi notifications
 */
@Singleton
class NequiNotificationProcessor @Inject constructor() : NotificationProcessor {
    
    companion object {
        private const val PACKAGE_NAME = "com.nequi"
        private const val APP_NAME = "Nequi"
    }
    
    override fun canProcess(packageName: String): Boolean {
        return packageName == PACKAGE_NAME
    }
    
    override fun processNotification(
        packageName: String,
        title: String,
        text: String
    ): ProcessedNotification? {
        return when {
            isPaymentReceived(title, text) -> processPaymentReceived(title, text)
            isPaymentSent(title, text) -> processPaymentSent(title, text)
            isTransferReceived(title, text) -> processTransferReceived(title, text)
            isTransferSent(title, text) -> processTransferSent(title, text)
            isBalanceUpdate(title, text) -> processBalanceUpdate(title, text)
            isPromotion(title, text) -> processPromotion(title, text)
            else -> null
        }
    }
    
    override fun getAppName(): String = APP_NAME
    
    override fun getSupportedPackages(): List<String> = listOf(PACKAGE_NAME)
    
    private fun isPaymentReceived(title: String, text: String): Boolean {
        return title.contains("Recibiste", ignoreCase = true) ||
               text.contains("recibiste", ignoreCase = true) ||
               text.contains("te llegó", ignoreCase = true)
    }
    
    private fun isPaymentSent(title: String, text: String): Boolean {
        return title.contains("Enviaste", ignoreCase = true) ||
               text.contains("enviaste", ignoreCase = true) ||
               text.contains("pagaste", ignoreCase = true)
    }
    
    private fun isTransferReceived(title: String, text: String): Boolean {
        return title.contains("Transferencia", ignoreCase = true) &&
               (text.contains("recibiste", ignoreCase = true) || text.contains("te llegó", ignoreCase = true))
    }
    
    private fun isTransferSent(title: String, text: String): Boolean {
        return title.contains("Transferencia", ignoreCase = true) &&
               text.contains("enviaste", ignoreCase = true)
    }
    
    private fun isBalanceUpdate(title: String, text: String): Boolean {
        return title.contains("Saldo", ignoreCase = true) ||
               text.contains("tu saldo", ignoreCase = true)
    }
    
    private fun isPromotion(title: String, text: String): Boolean {
        return title.contains("Promoción", ignoreCase = true) ||
               title.contains("Oferta", ignoreCase = true) ||
               text.contains("promoción", ignoreCase = true) ||
               text.contains("oferta", ignoreCase = true)
    }
    
    private fun processPaymentReceived(title: String, text: String): ProcessedNotification {
        val amount = extractAmount(text)
        val sender = extractSender(text)
        
        return ProcessedNotification(
            message = "Recibiste un pago de $amount${if (sender.isNotEmpty()) " de $sender" else ""} en Nequi",
            type = NotificationType.PAYMENT_RECEIVED,
            amount = amount,
            sender = sender
        )
    }
    
    private fun processPaymentSent(title: String, text: String): ProcessedNotification {
        val amount = extractAmount(text)
        val recipient = extractRecipient(text)
        
        return ProcessedNotification(
            message = "Enviaste un pago de $amount${if (recipient.isNotEmpty()) " a $recipient" else ""} desde Nequi",
            type = NotificationType.PAYMENT_SENT,
            amount = amount,
            sender = recipient
        )
    }
    
    private fun processTransferReceived(title: String, text: String): ProcessedNotification {
        val amount = extractAmount(text)
        val sender = extractSender(text)
        
        return ProcessedNotification(
            message = "Recibiste una transferencia de $amount${if (sender.isNotEmpty()) " de $sender" else ""} en Nequi",
            type = NotificationType.TRANSFER_RECEIVED,
            amount = amount,
            sender = sender
        )
    }
    
    private fun processTransferSent(title: String, text: String): ProcessedNotification {
        val amount = extractAmount(text)
        val recipient = extractRecipient(text)
        
        return ProcessedNotification(
            message = "Enviaste una transferencia de $amount${if (recipient.isNotEmpty()) " a $recipient" else ""} desde Nequi",
            type = NotificationType.TRANSFER_SENT,
            amount = amount,
            sender = recipient
        )
    }
    
    private fun processBalanceUpdate(title: String, text: String): ProcessedNotification {
        val amount = extractAmount(text)
        
        return ProcessedNotification(
            message = "Tu saldo en Nequi es de $amount",
            type = NotificationType.BALANCE_UPDATE,
            amount = amount
        )
    }
    
    private fun processPromotion(title: String, text: String): ProcessedNotification {
        return ProcessedNotification(
            message = "Nueva promoción en Nequi: $title",
            type = NotificationType.PROMOTION
        )
    }
    
    private fun extractAmount(text: String): String {
        val amountRegex = Regex("\\$?([0-9,]+(?:\\.[0-9]{2})?)")
        val match = amountRegex.find(text)
        return match?.groupValues?.get(1) ?: ""
    }
    
    private fun extractSender(text: String): String {
        // Try to extract sender name from common patterns
        val patterns = listOf(
            Regex("de\\s+([A-Za-z\\s]+)"),
            Regex("desde\\s+([A-Za-z\\s]+)"),
            Regex("([A-Za-z\\s]+)\\s+te\\s+envió")
        )
        
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }
        
        return ""
    }
    
    private fun extractRecipient(text: String): String {
        // Try to extract recipient name from common patterns
        val patterns = listOf(
            Regex("a\\s+([A-Za-z\\s]+)"),
            Regex("para\\s+([A-Za-z\\s]+)"),
            Regex("([A-Za-z\\s]+)\\s+recibió")
        )
        
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }
        
        return ""
    }
}
