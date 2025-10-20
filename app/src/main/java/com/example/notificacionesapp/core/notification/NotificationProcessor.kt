package com.example.notificacionesapp.core.notification

import com.example.notificacionesapp.domain.model.Notification
import com.example.notificacionesapp.domain.model.NotificationType

/**
 * Interface for processing different types of notifications
 */
interface NotificationProcessor {
    
    /**
     * Check if this processor can handle the given package name
     */
    fun canProcess(packageName: String): Boolean
    
    /**
     * Process the notification and return a formatted message
     */
    fun processNotification(
        packageName: String,
        title: String,
        text: String
    ): ProcessedNotification?
    
    /**
     * Get the app name for this processor
     */
    fun getAppName(): String
    
    /**
     * Get the supported package names
     */
    fun getSupportedPackages(): List<String>
}

/**
 * Data class representing a processed notification
 */
data class ProcessedNotification(
    val message: String,
    val type: NotificationType,
    val amount: String = "",
    val sender: String = "",
    val metadata: Map<String, String> = emptyMap()
)
