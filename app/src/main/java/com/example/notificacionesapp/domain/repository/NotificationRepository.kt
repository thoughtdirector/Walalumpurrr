package com.example.notificacionesapp.domain.repository

import com.example.notificacionesapp.core.domain.Result
import com.example.notificacionesapp.domain.model.Notification
import com.example.notificacionesapp.domain.model.NotificationType

/**
 * Repository interface for notification operations
 */
interface NotificationRepository {
    
    /**
     * Save a processed notification
     */
    suspend fun saveNotification(notification: Notification): Result<Unit>
    
    /**
     * Get all notifications
     */
    suspend fun getAllNotifications(): Result<List<Notification>>
    
    /**
     * Get notifications by type
     */
    suspend fun getNotificationsByType(type: NotificationType): Result<List<Notification>>
    
    /**
     * Get notifications by date range
     */
    suspend fun getNotificationsByDateRange(
        startDate: java.util.Date,
        endDate: java.util.Date
    ): Result<List<Notification>>
    
    /**
     * Get notifications by app package name
     */
    suspend fun getNotificationsByPackage(packageName: String): Result<List<Notification>>
    
    /**
     * Search notifications by content
     */
    suspend fun searchNotifications(query: String): Result<List<Notification>>
    
    /**
     * Delete notification by ID
     */
    suspend fun deleteNotification(notificationId: String): Result<Unit>
    
    /**
     * Clear all notifications
     */
    suspend fun clearAllNotifications(): Result<Unit>
    
    /**
     * Get notification statistics
     */
    suspend fun getNotificationStatistics(): Result<NotificationStatistics>
    
    /**
     * Export notifications to CSV
     */
    suspend fun exportNotificationsToCsv(): Result<String>
}

/**
 * Data class for notification statistics
 */
data class NotificationStatistics(
    val totalNotifications: Int,
    val notificationsByType: Map<NotificationType, Int>,
    val notificationsByApp: Map<String, Int>,
    val totalAmount: Double,
    val averageAmount: Double,
    val lastNotificationDate: java.util.Date?
)
