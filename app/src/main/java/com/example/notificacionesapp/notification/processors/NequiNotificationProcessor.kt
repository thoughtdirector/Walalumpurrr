package com.example.notificacionesapp.notification.processors

import com.example.notificacionesapp.notification.NotificationProcessor
import java.util.regex.Pattern

class NequiNotificationProcessor : NotificationProcessor {
    // Captura transferencias normales y por QR:
    // "NOMBRE te envió 50.000, ¡lo mejor!"
    // "NOMBRE te envió $10 a través de QR a tu nequi, ¡lo mejor!"
    private val nequiPattern = Pattern.compile(
        "([A-ZÁÉÍÓÚÑ\\s]+) te envió \\$?([0-9,.]+)(?: a través de QR)?(?: a tu nequi)?,?\\s*¡lo mejor!",
        Pattern.CASE_INSENSITIVE
    )
    // Bre-B: "Te enviaron plata por Bre-B. Te enviaron $50."
    private val brebPattern = Pattern.compile(
        "Te enviaron plata por Bre-B\\.?\\s*Te enviaron \\$?([0-9,.]+)",
        Pattern.CASE_INSENSITIVE
    )

    override fun canProcess(packageName: String): Boolean {
        return packageName.contains("nequi") || packageName.contains("colombia.nequi")
    }

    override fun processNotification(title: String, text: String, packageName: String): String? {
        val content = "$title $text"

        // Bre-B primero
        val brebMatcher = brebPattern.matcher(content)
        if (brebMatcher.find()) {
            val monto = brebMatcher.group(1)
            return "te enviaron $monto pesos por breve a tu nequi"
        }

        // Nequi normal (regex original)
        val matcher = nequiPattern.matcher(content)
        if (matcher.find()) {
            val monto = matcher.group(2)
            return "Recibiste $monto pesos por nequi"
        }

        return null
    }

    override fun getMetadata(title: String, text: String, processedMessage: String): Map<String, String> {
        val metadata = mutableMapOf<String, String>()
        try {
            metadata["appName"] = "Nequi"

            if (processedMessage.contains(" por breve a tu nequi")) {
                metadata["type"] = "BRE_B"
                metadata["title"] = "Transferencia Bre-B"
                val amount = processedMessage.replace(Regex("(te enviaron | pesos por breve a tu nequi|\\$)"), "").trim()
                metadata["sender"] = "Bre-B"
                metadata["amount"] = amount
            } else if (processedMessage.contains(" te envió ")) {
                val parts = processedMessage.split(" te envió ")
                if (parts.size == 2) {
                    val sender = parts[0].trim()
                    val rest = parts[1].trim()
                    val amount = rest.replace(Regex("( pesos por nequi| por QR a tu Nequi|\\$|,)"), "").trim()

                    metadata["appName"] = "Nequi"
                    metadata["type"] = "NEQUI"
                    metadata["title"] = "Transferencia recibida"
                    metadata["sender"] = sender
                    metadata["amount"] = amount
                }
            }
        } catch (e: Exception) {
        }
        return metadata
    }
}
