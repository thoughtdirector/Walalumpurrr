package com.example.notificacionesapp.notification

import android.content.Context
import android.util.Log
import com.example.notificacionesapp.SessionManager
import com.example.notificacionesapp.firebase.AdminNotificationService
import com.example.notificacionesapp.firebase.NotificationSyncService
import com.example.notificacionesapp.notification.processors.DaviPlataNotificationProcessor
import com.example.notificacionesapp.notification.processors.NequiNotificationProcessor
import com.example.notificacionesapp.util.NotificationHistoryManager
import com.example.notificacionesapp.model.FirestoreNotification
import com.example.notificacionesapp.firebase.FirestoreService
import java.util.UUID

class NotificationProcessorRegistry(
    private val context: Context,
    private val historyManager: NotificationHistoryManager?
) {
    private val processors = mutableListOf<NotificationProcessor>()
    private var lastMetadata: Map<String, String> = emptyMap()
    private val notificationSyncService = NotificationSyncService(context)

    init {
        registerDefaultProcessors()
    }

    private fun registerDefaultProcessors() {
        val factory = NotificationProcessorFactory

        processors.add(factory.createProcessor(NotificationProcessorFactory.ProcessorType.NEQUI))
        processors.add(factory.createProcessor(NotificationProcessorFactory.ProcessorType.DAVIPLATA))
    }

    fun addProcessor(processor: NotificationProcessor) {
        processors.add(processor)
    }

    // Modify the processNotification method
    fun processNotification(packageName: String, title: String, text: String): String? {
        Log.d("NotificationProcessor", "Processing notification from package: $packageName")

        for (processor in processors) {
            if (processor.canProcess(packageName)) {
                try {
                    val message = processor.processNotification(title, text, packageName)

                    if (message != null) {
                        // Save metadata for later access
                        lastMetadata = processor.getMetadata(title, text, message)

                        // Save to local history if needed
                        historyManager?.let {
                            if (lastMetadata.isNotEmpty()) {
                                it.saveNotification(
                                    packageName = packageName,
                                    appName = lastMetadata["appName"] ?: "Unknown",
                                    title = lastMetadata["title"] ?: title,
                                    content = message,
                                    type = lastMetadata["type"] ?: "OTHER",
                                    amount = lastMetadata["amount"] ?: "",
                                    sender = lastMetadata["sender"] ?: ""
                                )
                            }
                        }

                        // Get user ID and role
                        val sessionManager = SessionManager(context)
                        val userId = sessionManager.getUserId() ?: return message
                        val role = sessionManager.getUserRole() ?: "user"

                        // Determine adminId
                        val adminId = if (role == "admin") {
                            userId
                        } else {
                            sessionManager.getUserDetails()["adminId"] ?: return message
                        }

                        // Create notification for Firestore
                        val notification = FirestoreNotification(
                            id = UUID.randomUUID().toString(),
                            adminId = adminId,
                            packageName = packageName,
                            appName = lastMetadata["appName"] ?: "Unknown",
                            title = lastMetadata["title"] ?: title,
                            content = message,
                            type = lastMetadata["type"] ?: "OTHER",
                            amount = lastMetadata["amount"] ?: "",
                            sender = lastMetadata["sender"] ?: "",
                            read = false
                        )

                        // Save to Firestore
                        val firestoreService = FirestoreService()
                        firestoreService.saveNotification(
                            notification = notification,
                            onSuccess = {
                                Log.d("NotificationProcessor", "Notification saved to Firestore")

                                // Notify employees if user is admin
                                if (role == "admin") {
                                    notifyEmployees(adminId, notification)
                                }
                            },
                            onError = { e ->
                                Log.e("NotificationProcessor", "Error saving to Firestore: ${e.message}")
                            }
                        )

                        return message
                    }
                } catch (e: Exception) {
                    Log.e("NotificationProcessor", "Error processing notification", e)
                }
            }
        }

        return null
    }

    // Add this method to notify employees
    private fun notifyEmployees(adminId: String, notification: FirestoreNotification) {
        val firestoreService = FirestoreService()

        firestoreService.getEmployeesByAdminId(
            adminId = adminId,
            onSuccess = { employees ->
                // Get FCM tokens
                val tokens = employees.mapNotNull { it.fcmToken }.filter { it.isNotEmpty() }

                if (tokens.isNotEmpty()) {
                    // Call your FCM notification service
                    val adminNotificationService = AdminNotificationService()
                    adminNotificationService.sendFCMNotifications(
                        tokens = tokens,
                        title = notification.title,
                        message = notification.content,
                        data = mapOf(
                            "type" to notification.type,
                            "notificationId" to notification.id,
                            "appName" to notification.appName,
                            "amount" to notification.amount,
                            "sender" to notification.sender
                        )
                    )
                }
            },
            onError = { e ->
                Log.e("NotificationProcessor", "Error getting employees: ${e.message}")
            }
        )
    }
    fun getLastProcessedMetadata(): Map<String, String> {
        return lastMetadata
    }
}