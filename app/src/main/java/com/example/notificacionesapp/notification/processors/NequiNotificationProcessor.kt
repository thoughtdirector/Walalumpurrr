package com.example.notificacionesapp.notification.processors

import com.example.notificacionesapp.notification.NotificationProcessor
import java.util.regex.Pattern

class NequiNotificationProcessor : NotificationProcessor {
    private val nequiPattern = Pattern.compile("([A-ZÁÉÍÓÚÑ\\s]+) te envió ([0-9,.]+), ¡lo mejor!")

    override fun canProcess(packageName: String): Boolean {
        return packageName.contains("nequi") || packageName.contains("colombia.nequi")
    }

    override fun processNotification(title: String, text: String, packageName: String): String? {
        val content = "$title $text"

        val matcher = nequiPattern.matcher(content)
        if (matcher.find()) {
            val nombre = matcher.group(1)
            val monto = matcher.group(2)
            return "$nombre te envió $monto pesos por nequi"
        }
        return null
    }

    override fun getMetadata(title: String, text: String, processedMessage: String): Map<String, String> {
        val metadata = mutableMapOf<String, String>()

        try {
            if (processedMessage.contains(" te envió ") && processedMessage.contains(" pesos por nequi")) {
                val parts = processedMessage.split(" te envió ")
                if (parts.size == 2) {
                    val sender = parts[0].trim()
                    val amountPart = parts[1].replace(" pesos por nequi", "").trim()

                    metadata["appName"] = "Nequi"
                    metadata["type"] = "NEQUI"
                    metadata["title"] = "Transferencia recibida"
                    metadata["sender"] = sender
                    metadata["amount"] = amountPart
                }
            }
        } catch (e: Exception) {
        }

        return metadata
    }
}