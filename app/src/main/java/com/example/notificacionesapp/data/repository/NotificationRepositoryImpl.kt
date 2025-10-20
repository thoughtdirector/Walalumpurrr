package com.example.notificacionesapp.data.repository

import android.content.Context
import com.example.notificacionesapp.core.domain.Result
import com.example.notificacionesapp.domain.model.Notification
import com.example.notificacionesapp.domain.model.NotificationType
import com.example.notificacionesapp.domain.repository.NotificationRepository
import com.example.notificacionesapp.domain.repository.NotificationStatistics
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of NotificationRepository using SharedPreferences
 * TODO: Migrate to Room database for better performance and data management
 */
@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val context: Context,
    private val gson: Gson
) : NotificationRepository {

    companion object {
        private const val PREFS_NAME = "notification_history"
        private const val HISTORY_KEY = "notifications"
        private const val MAX_HISTORY_SIZE = 1000
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun saveNotification(notification: Notification): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val notifications = getAllNotificationsFromStorage()
                val notificationWithId = notification.copy(id = UUID.randomUUID().toString())
                val updatedNotifications = (notifications + notificationWithId)
                    .sortedByDescending { it.timestamp }
                    .take(MAX_HISTORY_SIZE)

                saveNotificationsToStorage(updatedNotifications)
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    override suspend fun getAllNotifications(): Result<List<Notification>> {
        return withContext(Dispatchers.IO) {
            try {
                val notifications = getAllNotificationsFromStorage()
                Result.Success(notifications)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    override suspend fun getNotificationsByType(type: NotificationType): Result<List<Notification>> {
        return withContext(Dispatchers.IO) {
            try {
                val allNotifications = getAllNotificationsFromStorage()
                val filteredNotifications = allNotifications.filter { it.type == type }
                Result.Success(filteredNotifications)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    override suspend fun getNotificationsByDateRange(
        startDate: Date,
        endDate: Date
    ): Result<List<Notification>> {
        return withContext(Dispatchers.IO) {
            try {
                val allNotifications = getAllNotificationsFromStorage()
                val filteredNotifications = allNotifications.filter { notification ->
                    notification.timestamp.after(startDate) && notification.timestamp.before(endDate)
                }
                Result.Success(filteredNotifications)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    override suspend fun getNotificationsByPackage(packageName: String): Result<List<Notification>> {
        return withContext(Dispatchers.IO) {
            try {
                val allNotifications = getAllNotificationsFromStorage()
                val filteredNotifications = allNotifications.filter { it.packageName == packageName }
                Result.Success(filteredNotifications)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    override suspend fun searchNotifications(query: String): Result<List<Notification>> {
        return withContext(Dispatchers.IO) {
            try {
                val allNotifications = getAllNotificationsFromStorage()
                val filteredNotifications = allNotifications.filter { notification ->
                    notification.title.contains(query, ignoreCase = true) ||
                    notification.content.contains(query, ignoreCase = true) ||
                    notification.appName.contains(query, ignoreCase = true) ||
                    notification.sender.contains(query, ignoreCase = true)
                }
                Result.Success(filteredNotifications)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    override suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val notifications = getAllNotificationsFromStorage()
                val updatedNotifications = notifications.filter { it.id != notificationId }
                saveNotificationsToStorage(updatedNotifications)
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    override suspend fun clearAllNotifications(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                prefs.edit().putString(HISTORY_KEY, "[]").apply()
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    override suspend fun getNotificationStatistics(): Result<NotificationStatistics> {
        return withContext(Dispatchers.IO) {
            try {
                val notifications = getAllNotificationsFromStorage()
                
                val totalNotifications = notifications.size
                val notificationsByType = notifications.groupBy { it.type }
                    .mapValues { it.value.size }
                val notificationsByApp = notifications.groupBy { it.appName }
                    .mapValues { it.value.size }
                
                val amounts = notifications.mapNotNull { notification ->
                    notification.amount.replace(Regex("[^0-9.]"), "").toDoubleOrNull()
                }
                val totalAmount = amounts.sum()
                val averageAmount = if (amounts.isNotEmpty()) totalAmount / amounts.size else 0.0
                
                val lastNotificationDate = notifications.maxByOrNull { it.timestamp }?.timestamp

                val statistics = NotificationStatistics(
                    totalNotifications = totalNotifications,
                    notificationsByType = notificationsByType,
                    notificationsByApp = notificationsByApp,
                    totalAmount = totalAmount,
                    averageAmount = averageAmount,
                    lastNotificationDate = lastNotificationDate
                )

                Result.Success(statistics)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    override suspend fun exportNotificationsToCsv(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val notifications = getAllNotificationsFromStorage()
                val csvBuilder = StringBuilder()
                
                // CSV Header
                csvBuilder.appendLine("ID,App Name,Title,Content,Type,Amount,Sender,Timestamp")
                
                // CSV Data
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                notifications.forEach { notification ->
                    csvBuilder.appendLine(
                        "${notification.id}," +
                        "\"${notification.appName}\"," +
                        "\"${notification.title}\"," +
                        "\"${notification.content}\"," +
                        "${notification.type}," +
                        "\"${notification.amount}\"," +
                        "\"${notification.sender}\"," +
                        dateFormat.format(notification.timestamp)
                    )
                }
                
                Result.Success(csvBuilder.toString())
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    private fun getAllNotificationsFromStorage(): List<Notification> {
        val historyJson = prefs.getString(HISTORY_KEY, "[]") ?: "[]"
        return try {
            val type = object : TypeToken<List<Notification>>() {}.type
            gson.fromJson(historyJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveNotificationsToStorage(notifications: List<Notification>) {
        val json = gson.toJson(notifications)
        prefs.edit().putString(HISTORY_KEY, json).apply()
    }
}
