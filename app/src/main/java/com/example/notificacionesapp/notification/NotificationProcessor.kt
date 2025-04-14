package com.example.notificacionesapp.notification

interface NotificationProcessor {

    fun canProcess(packageName: String): Boolean
    fun processNotification(title: String, text: String, packageName: String): String?
    fun getMetadata(title: String, text: String, processedMessage: String): Map<String, String> {
        return emptyMap()
    }
}