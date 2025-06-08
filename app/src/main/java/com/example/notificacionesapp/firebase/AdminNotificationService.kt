package com.example.notificacionesapp.firebase

import android.util.Log
import com.example.notificacionesapp.model.FirestoreNotification
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class AdminNotificationService {

    private val db = Firebase.firestore
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "AdminNotificationService"
        private const val FCM_API = "https://fcm.googleapis.com/fcm/send"
        // Nota: Esta clave debe ser configurada correctamente en producción
        private const val SERVER_KEY = "wTx5g4qcH2a6EVjKhOAfqZiEd5MiS2kYqqpskiO8lr8"
    }

    fun notifyEmployees(adminId: String, notification: FirestoreNotification) {
        try {
            Log.d(TAG, "Starting to notify employees for admin: $adminId")

            // Find employees from Firestore with better error handling
            db.collection("users")
                .whereEqualTo("adminId", adminId)
                .get()
                .addOnSuccessListener { documents ->
                    try {
                        val employeeTokens = mutableListOf<String>()

                        // Collect FCM tokens directly from user documents
                        for (document in documents) {
                            val fcmToken = document.getString("fcmToken")
                            Log.d(TAG, "Found employee: ${document.id}, has token: ${!fcmToken.isNullOrEmpty()}")

                            if (!fcmToken.isNullOrEmpty() && fcmToken.length > 10) {
                                employeeTokens.add(fcmToken)
                            }
                        }

                        Log.d(TAG, "Found ${documents.size()} employees, with ${employeeTokens.size} valid tokens")

                        // Send notification to all found tokens
                        if (employeeTokens.isNotEmpty()) {
                            // Check if we have a valid server key
                            if (SERVER_KEY == "wTx5g4qcH2a6EVjKhOAfqZiEd5MiS2kYqqpskiO8lr8" || SERVER_KEY.isEmpty()) {
                                Log.w(TAG, "FCM Server Key not configured properly. Skipping FCM notification.")
                                // En lugar de fallar completamente, podríamos usar notificaciones locales
                                // o simplemente loggear que no se pueden enviar notificaciones push
                                return@addOnSuccessListener
                            }

                            sendFCMNotification(
                                tokens = employeeTokens,
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
                            Log.w(TAG, "No valid employee tokens found to send notifications")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing employee tokens: ${e.message}", e)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error finding employees: ${e.message}", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in notifyEmployees: ${e.message}", e)
        }
    }

    fun sendFCMNotifications(
        tokens: List<String>,
        title: String,
        message: String,
        data: Map<String, String> = emptyMap()
    ) {
        if (tokens.isEmpty()) {
            Log.w(TAG, "No tokens provided for FCM notification")
            return
        }

        sendFCMNotification(tokens, title, message, data)
    }

    private fun sendFCMNotification(
        tokens: List<String>,
        title: String,
        message: String,
        data: Map<String, String> = emptyMap()
    ) {
        try {
            // Validate inputs
            if (tokens.isEmpty()) {
                Log.w(TAG, "No tokens to send notification to")
                return
            }

            if (SERVER_KEY == "wTx5g4qcH2a6EVjKhOAfqZiEd5MiS2kYqqpskiO8lr8" || SERVER_KEY.isEmpty()) {
                Log.w(TAG, "FCM Server Key not configured. Cannot send notifications.")
                return
            }

            // Split tokens into batches of 1000 (FCM limit)
            val batchSize = 1000
            val tokenBatches = tokens.chunked(batchSize)

            for (tokenBatch in tokenBatches) {
                sendBatchNotification(tokenBatch, title, message, data)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error preparing FCM notification: ${e.message}", e)
        }
    }

    private fun sendBatchNotification(
        tokens: List<String>,
        title: String,
        message: String,
        data: Map<String, String>
    ) {
        try {
            val json = JSONObject().apply {
                put("registration_ids", tokens)

                // Notificación visible
                put("notification", JSONObject().apply {
                    put("title", title)
                    put("body", message)
                    put("sound", "default")
                    put("priority", "high")
                })

                // Datos adicionales
                val dataJson = JSONObject()
                for ((key, value) in data) {
                    dataJson.put(key, value)
                }
                put("data", dataJson)

                // Configuraciones adicionales
                put("priority", "high")
                put("content_available", true)
            }

            val body = json.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(FCM_API)
                .post(body)
                .addHeader("Authorization", "key=$SERVER_KEY")
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Error al enviar notificación FCM: ${e.message}", e)
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val responseBody = response.body?.string()
                        if (response.isSuccessful) {
                            Log.d(TAG, "FCM notification sent successfully to ${tokens.size} tokens")
                            Log.v(TAG, "FCM Response: $responseBody")
                        } else {
                            Log.e(TAG, "FCM notification failed with code: ${response.code}")
                            Log.e(TAG, "FCM Error response: $responseBody")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing FCM response: ${e.message}", e)
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error en sendBatchNotification: ${e.message}", e)
        }
    }
}