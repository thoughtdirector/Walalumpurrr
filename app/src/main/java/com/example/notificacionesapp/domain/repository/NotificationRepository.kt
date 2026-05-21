package com.example.notificacionesapp.domain.repository

import com.example.notificacionesapp.core.domain.Result
import com.example.notificacionesapp.domain.model.Notification
import com.example.notificacionesapp.domain.model.NotificationType

interface NotificationRepository {
    suspend fun saveNotification(notification: Notification): Result<Unit>
    suspend fun getAllNotifications(): Result<List<Notification>>
    suspend fun getNotificationsSince(timestampMs: Long): Result<List<Notification>>
    suspend fun getNotificationsByType(type: NotificationType): Result<List<Notification>>
    suspend fun getNotificationsByDateRange(
        startDate: java.util.Date,
        endDate: java.util.Date
    ): Result<List<Notification>>
    suspend fun getNotificationsByPackage(packageName: String): Result<List<Notification>>
    suspend fun searchNotifications(query: String): Result<List<Notification>>
    suspend fun deleteNotification(notificationId: String): Result<Unit>
    suspend fun clearAllNotifications(): Result<Unit>
    suspend fun getNotificationStatistics(): Result<NotificationStatistics>
    suspend fun exportNotificationsToCsv(): Result<String>
}

data class NotificationStatistics(
    val totalNotifications: Int,
    val notificationsByType: Map<NotificationType, Int>,
    val notificationsByApp: Map<String, Int>,
    val totalAmount: Double,
    val averageAmount: Double,
    val lastNotificationDate: java.util.Date?
)
