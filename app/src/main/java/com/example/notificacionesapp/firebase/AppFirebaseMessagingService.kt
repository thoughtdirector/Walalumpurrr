package com.example.notificacionesapp.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.notificacionesapp.MainActivity
import com.example.notificacionesapp.R
import com.example.notificacionesapp.SessionManager
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AppFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "notifications_channel"
        private const val SYNC_CHANNEL_ID = "sync_notifications_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")
        Log.d(TAG, "Message ID: ${remoteMessage.messageId}")

        // Check if message contains a data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }

        // Check if message contains a notification payload
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Message Notification Body: ${notification.body}")
            val title = notification.title ?: "Nueva notificación"
            val body = notification.body ?: ""

            // Show notification with data if available
            showNotification(title, body, remoteMessage.data)
        }

        // If no notification payload, but has data, show notification from data
        if (remoteMessage.notification == null && remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"] ?: "Nueva notificación"
            val message = remoteMessage.data["message"] ?: remoteMessage.data["body"] ?: ""
            showNotification(title, message, remoteMessage.data)
        }
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val title = data["title"] ?: "Nueva notificación"
        val message = data["message"] ?: data["body"] ?: ""
        val notificationType = data["type"] ?: ""
        val targetRole = data["targetRole"] ?: "all"

        Log.d(TAG, "Handling data message - Type: $notificationType, Target: $targetRole")

        // Check if user should receive this notification
        val sessionManager = SessionManager(this)
        val userRole = sessionManager.getUserRole()

        if (targetRole == "all" || targetRole == userRole) {
            showNotification(title, message, data)
        } else {
            Log.d(TAG, "Notification filtered out - User role: $userRole, Target: $targetRole")
        }
    }

    private fun showNotification(title: String, messageBody: String, data: Map<String, String> = emptyMap()) {
        try {
            // Create intent with extra data
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

                // Add notification data as extras
                data.forEach { (key, value) ->
                    putExtra(key, value)
                }

                // Special handling for different notification types
                when (data["type"]) {
                    "NEQUI", "DAVIPLATA", "BANCOLOMBIA" -> {
                        putExtra("openHistoryTab", true)
                    }
                    "MANUAL" -> {
                        putExtra("openHomeTab", true)
                    }
                }
            }

            val pendingIntent = PendingIntent.getActivity(
                this,
                System.currentTimeMillis().toInt(), // Unique request code
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
            )

            // Choose channel based on notification type
            val channelId = if (data["type"] in listOf("NEQUI", "DAVIPLATA", "BANCOLOMBIA")) {
                CHANNEL_ID
            } else {
                SYNC_CHANNEL_ID
            }

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)

            // Add expandable text if message is long
            if (messageBody.length > 50) {
                notificationBuilder.setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(messageBody)
                        .setBigContentTitle(title)
                )
            }

            // Add additional info for financial notifications
            if (data["amount"]?.isNotEmpty() == true) {
                val inboxStyle = NotificationCompat.InboxStyle()
                    .setBigContentTitle(title)
                data["sender"]?.let { sender ->
                    if (sender.isNotEmpty()) {
                        inboxStyle.addLine("De: $sender")
                    }
                }
                inboxStyle.addLine("Monto: ${data["amount"]}")
                notificationBuilder.setStyle(inboxStyle)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Use unique ID for each notification
            val notificationId = data["notificationId"]?.hashCode() ?: System.currentTimeMillis().toInt()
            notificationManager.notify(notificationId, notificationBuilder.build())

            Log.d(TAG, "Notification displayed with ID: $notificationId")

        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification: ${e.message}", e)
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed FCM token: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
        val sessionManager = SessionManager(this)
        if (sessionManager.isLoggedIn()) {
            val userId = sessionManager.getUserId()
            userId?.let {
                // Update FCM token in Firestore
                Firebase.firestore.collection("users")
                    .document(it)
                    .update("fcmToken", token)
                    .addOnSuccessListener {
                        Log.d(TAG, "FCM token updated in Firestore")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error updating FCM token in Firestore: ${e.message}")
                    }
            }
        } else {
            Log.d(TAG, "User not logged in, token will be updated when user logs in")
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                // Main notifications channel
                val mainChannel = NotificationChannel(
                    CHANNEL_ID,
                    "Notificaciones Bancarias",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notificaciones de transferencias y movimientos bancarios"
                    enableVibration(true)
                    setShowBadge(true)
                }

                // Sync notifications channel
                val syncChannel = NotificationChannel(
                    SYNC_CHANNEL_ID,
                    "Notificaciones Sincronizadas",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notificaciones sincronizadas entre dispositivos"
                    enableVibration(false)
                    setShowBadge(false)
                }

                notificationManager.createNotificationChannel(mainChannel)
                notificationManager.createNotificationChannel(syncChannel)

                Log.d(TAG, "Notification channels created")

            } catch (e: Exception) {
                Log.e(TAG, "Error creating notification channels: ${e.message}", e)
            }
        }
    }

    // Method to handle background message processing
    private fun handleBackgroundMessage(data: Map<String, String>) {
        // This method can be used for processing data when app is in background
        // without showing notification (if needed)
        Log.d(TAG, "Handling background message: $data")

        // Example: Update local database, sync data, etc.
        // Implementation depends on your specific needs
    }
}