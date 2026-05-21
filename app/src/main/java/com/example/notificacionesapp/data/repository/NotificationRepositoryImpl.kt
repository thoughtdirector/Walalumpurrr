package com.example.notificacionesapp.data.repository

import com.example.notificacionesapp.core.domain.Result
import com.example.notificacionesapp.domain.model.Notification
import com.example.notificacionesapp.domain.model.NotificationType
import com.example.notificacionesapp.domain.repository.NotificationRepository
import com.example.notificacionesapp.domain.repository.NotificationStatistics
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class NotificationDto(
    val id: String,
    @SerialName("packagename") val packageName: String,
    @SerialName("appname") val appName: String,
    val title: String,
    val content: String,
    @SerialName("type") val notificationType: String,
    val amount: String = "",
    val sender: String = "",
    @SerialName("admin_id") val adminId: String? = null,
    @SerialName("timestamp") val timestampIso: String,
    @SerialName("isprocessed") val isProcessed: Boolean = true
)

fun NotificationDto.toDomainNotification(): Notification {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    return Notification(
        id = id,
        packageName = packageName,
        appName = appName,
        title = title,
        content = content,
        type = try { NotificationType.valueOf(notificationType.uppercase()) } catch (_: Exception) { NotificationType.OTHER },
        amount = amount,
        sender = sender,
        adminId = adminId,
        timestamp = try { dateFormat.parse(timestampIso) ?: Date() } catch (_: Exception) { Date() },
        isProcessed = isProcessed
    )
}

fun Notification.toDto(): NotificationDto {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    return NotificationDto(
        id = id,
        packageName = packageName,
        appName = appName,
        title = title,
        content = content,
        notificationType = type.name.lowercase(),
        amount = amount,
        sender = sender,
        adminId = adminId,
        timestampIso = dateFormat.format(timestamp),
        isProcessed = isProcessed
    )
}

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : NotificationRepository {

    companion object {
        private const val MAX_HISTORY_SIZE = 1000L
    }

    override suspend fun saveNotification(notification: Notification): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val notificationWithId = notification.copy(id = UUID.randomUUID().toString())
                supabaseClient.from("relayed_notifications").insert(notificationWithId.toDto())
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    override suspend fun getNotificationsSince(timestampMs: Long): Result<List<Notification>> {
        return withContext(Dispatchers.IO) {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val sinceIso = dateFormat.format(Date(timestampMs))
                val dtos: List<NotificationDto> = supabaseClient.from("relayed_notifications")
                    .select {
                        filter { gt("timestamp", sinceIso) }
                        order("timestamp", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                        limit(10)
                    }
                    .decodeList()
                Result.Success(dtos.map { it.toDomainNotification() })
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    override suspend fun getAllNotifications(): Result<List<Notification>> {
        return withContext(Dispatchers.IO) {
            try {
                val dtos: List<NotificationDto> = supabaseClient.from("relayed_notifications")
                    .select {
                        order("timestamp", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                        limit(MAX_HISTORY_SIZE)
                    }
                    .decodeList()
                Result.Success(dtos.map { it.toDomainNotification() })
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    override suspend fun getNotificationsByType(type: NotificationType): Result<List<Notification>> {
        return withContext(Dispatchers.IO) {
            try {
                val dtos: List<NotificationDto> = supabaseClient.from("relayed_notifications")
                    .select { filter { eq("type", type.name.lowercase()) } }
                    .decodeList()
                Result.Success(dtos.map { it.toDomainNotification() })
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
                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val startIso = dateFormat.format(startDate)
                val endIso = dateFormat.format(endDate)

                val dtos: List<NotificationDto> = supabaseClient.from("relayed_notifications")
                    .select {
                        filter {
                            gte("timestamp", startIso)
                            lt("timestamp", endIso)
                        }
                    }
                    .decodeList()
                Result.Success(dtos.map { it.toDomainNotification() })
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    override suspend fun getNotificationsByPackage(packageName: String): Result<List<Notification>> {
        return withContext(Dispatchers.IO) {
            try {
                val dtos: List<NotificationDto> = supabaseClient.from("relayed_notifications")
                    .select { filter { eq("packagename", packageName) } }
                    .decodeList()
                Result.Success(dtos.map { it.toDomainNotification() })
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    override suspend fun searchNotifications(query: String): Result<List<Notification>> {
        return withContext(Dispatchers.IO) {
            try {
                val dtos: List<NotificationDto> = supabaseClient.from("relayed_notifications")
                    .select {
                        filter {
                            or {
                                ilike("title", "%$query%")
                                ilike("content", "%$query%")
                                ilike("appname", "%$query%")
                                ilike("sender", "%$query%")
                            }
                        }
                    }
                    .decodeList()
                Result.Success(dtos.map { it.toDomainNotification() })
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    override suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                supabaseClient.from("relayed_notifications").delete {
                    filter { eq("id", notificationId) }
                }
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    override suspend fun clearAllNotifications(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                supabaseClient.from("relayed_notifications").delete { filter { neq("id", "none") } }
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    override suspend fun getNotificationStatistics(): Result<NotificationStatistics> {
        return withContext(Dispatchers.IO) {
            try {
                val dtos: List<NotificationDto> = supabaseClient.from("relayed_notifications")
                    .select()
                    .decodeList()

                val notifications = dtos.map { it.toDomainNotification() }
                val totalNotifications = notifications.size
                val notificationsByType = notifications.groupBy { it.type }
                    .mapValues { it.value.size }
                val notificationsByApp = notifications.groupBy { it.appName }
                    .mapValues { it.value.size }

                val amounts = notifications.mapNotNull { n ->
                    n.amount.replace(Regex("[^0-9.]"), "").toDoubleOrNull()
                }
                val totalAmount = amounts.sum()
                val averageAmount = if (amounts.isNotEmpty()) totalAmount / amounts.size else 0.0
                val lastNotificationDate = notifications.maxByOrNull { it.timestamp }?.timestamp

                Result.Success(NotificationStatistics(
                    totalNotifications = totalNotifications,
                    notificationsByType = notificationsByType,
                    notificationsByApp = notificationsByApp,
                    totalAmount = totalAmount,
                    averageAmount = averageAmount,
                    lastNotificationDate = lastNotificationDate
                ))
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    override suspend fun exportNotificationsToCsv(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val dtos: List<NotificationDto> = supabaseClient.from("relayed_notifications")
                    .select()
                    .decodeList()

                val notifications = dtos.map { it.toDomainNotification() }
                val csvBuilder = StringBuilder()
                csvBuilder.appendLine("ID,App Name,Title,Content,Type,Amount,Sender,Timestamp")

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
}
