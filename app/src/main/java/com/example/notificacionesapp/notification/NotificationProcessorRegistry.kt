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
import com.google.firebase.Timestamp
import java.util.UUID
import kotlinx.coroutines.*

class NotificationProcessorRegistry(
    private val context: Context,
    private val historyManager: NotificationHistoryManager?
) {
    private val processors = mutableListOf<NotificationProcessor>()
    private var lastMetadata: Map<String, String> = emptyMap()
    private val notificationSyncService = NotificationSyncService(context)

    // Usar un scope limitado para las operaciones asíncronas
    private val processingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        registerDefaultProcessors()
    }

    private fun registerDefaultProcessors() {
        try {
            val factory = NotificationProcessorFactory

            processors.add(factory.createProcessor(NotificationProcessorFactory.ProcessorType.NEQUI))
            processors.add(factory.createProcessor(NotificationProcessorFactory.ProcessorType.DAVIPLATA))

            Log.d(TAG, "Registered ${processors.size} notification processors")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering default processors: ${e.message}", e)
        }
    }

    fun addProcessor(processor: NotificationProcessor) {
        try {
            processors.add(processor)
            Log.d(TAG, "Added custom processor, total: ${processors.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding processor: ${e.message}", e)
        }
    }

    fun processNotification(packageName: String, title: String, text: String): String? {
        return try {
            Log.d(TAG, "Processing notification from package: $packageName")

            for (processor in processors) {
                if (processor.canProcess(packageName)) {
                    try {
                        val message = processor.processNotification(title, text, packageName)

                        if (message != null) {
                            // Save metadata for later access
                            lastMetadata = processor.getMetadata(title, text, message)

                            // Save to local history if needed (synchronously)
                            saveToLocalHistory(packageName, message)

                            // Save to Firestore (asynchronously)
                            saveToFirestore(packageName, title, message)

                            return message
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing notification with ${processor::class.simpleName}: ${e.message}", e)
                        // Continue to next processor instead of failing completely
                    }
                }
            }

            Log.d(TAG, "No processor found for package: $packageName")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error processing notification: ${e.message}", e)
            null
        }
    }

    private fun saveToLocalHistory(packageName: String, message: String) {
        try {
            historyManager?.let { history ->
                if (lastMetadata.isNotEmpty()) {
                    history.saveNotification(
                        packageName = packageName,
                        appName = lastMetadata["appName"] ?: "Unknown",
                        title = lastMetadata["title"] ?: "Notification",
                        content = message,
                        type = lastMetadata["type"] ?: "OTHER",
                        amount = lastMetadata["amount"] ?: "",
                        sender = lastMetadata["sender"] ?: ""
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to local history: ${e.message}", e)
        }
    }

    private fun saveToFirestore(packageName: String, title: String, message: String) {
        // Ejecutar en background thread para evitar bloquear el hilo principal
        processingScope.launch {
            try {
                val sessionManager = SessionManager(context)
                val userId = sessionManager.getUserId()
                val role = sessionManager.getUserRole() ?: "user"

                if (userId == null) {
                    Log.w(TAG, "Cannot save to Firestore: no user ID")
                    return@launch
                }

                // Determine adminId
                val adminId = if (role == "admin") {
                    userId
                } else {
                    sessionManager.getUserDetails()["adminId"]
                }

                if (adminId == null) {
                    Log.w(TAG, "Cannot save to Firestore: no admin ID")
                    return@launch
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
                    read = false,
                    timestamp = Timestamp.now()
                )

                // Save to Firestore with error handling
                val firestoreService = FirestoreService()
                firestoreService.saveNotification(
                    notification = notification,
                    onSuccess = {
                        Log.d(TAG, "Notification saved to Firestore successfully")

                        // Notify employees if user is admin (also in background)
                        if (role == "admin") {
                            try {
                                notifyEmployees(adminId, notification)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error notifying employees: ${e.message}", e)
                            }
                        }
                    },
                    onError = { e ->
                        Log.e(TAG, "Error saving to Firestore: ${e.message}", e)
                    }
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error in Firestore save operation: ${e.message}", e)
            }
        }
    }

    private fun notifyEmployees(adminId: String, notification: FirestoreNotification) {
        try {
            val firestoreService = FirestoreService()

            firestoreService.getEmployeesByAdminId(
                adminId = adminId,
                onSuccess = { employees ->
                    try {
                        // Get FCM tokens
                        val tokens = employees.mapNotNull { it.fcmToken }.filter { it.isNotEmpty() }

                        Log.d(TAG, "Found ${employees.size} employees, ${tokens.size} with valid tokens")

                        if (tokens.isNotEmpty()) {
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
                        } else {
                            Log.w(TAG, "No valid employee tokens found")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing employee tokens: ${e.message}", e)
                    }
                },
                onError = { e ->
                    Log.e(TAG, "Error getting employees: ${e.message}", e)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in notifyEmployees: ${e.message}", e)
        }
    }

    fun getLastProcessedMetadata(): Map<String, String> {
        return lastMetadata.toMap() // Return a copy to prevent modification
    }

    fun cleanup() {
        try {
            processingScope.cancel()
            processors.clear()
            Log.d(TAG, "NotificationProcessorRegistry cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "NotificationProcessorRegistry"
    }
}