package com.example.notificacionesapp.notification.processors
import com.example.notificacionesapp.notification.NotificationProcessor

class DaviPlataNotificationProcessor : NotificationProcessor {

    override fun canProcess(packageName: String): Boolean {
        return packageName.contains("daviplata") || packageName.contains("daviplataapp")
    }

    override fun processNotification(title: String, text: String, packageName: String): String? {
        if (text.contains("Recibió") || text.contains("movimientos")) {
            return "DaviPlata: $text"
        }

        return null
    }

    override fun getMetadata(title: String, text: String, processedMessage: String): Map<String, String> {
        val metadata = mutableMapOf<String, String>()
        try {
            metadata["appName"] = "DaviPlata"
            metadata["type"] = "DAVIPLATA"
            metadata["title"] = title

            val amountRegex = "\\b(\\d+[.,]\\d+)\\b".toRegex()
            val amountMatch = amountRegex.find(text)
            if (amountMatch != null) {
                metadata["amount"] = amountMatch.value
            }
        } catch (e: Exception) {
        }

        return metadata
    }
}