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

class AdminNotificationService {

    private val db = Firebase.firestore
    private val client = OkHttpClient()

    companion object {
        private const val TAG = "AdminNotificationService"
        private const val FCM_API = "https://fcm.googleapis.com/fcm/send"
        private const val SERVER_KEY = "BA5WW_PfmB2wSX5AFtNr4SE_7W6Q6rcJlsVn2rC2_6Q6NDDgTsTbgpuCEecp0V7ghtx6TdUGTER1wt1Dzv81yjY"
    }

    fun notifyEmployees(adminId: String, notification: FirestoreNotification) {
        try {
            Log.d(TAG, "Starting to notify employees for admin: $adminId")

            // Find employees from Firestore
            db.collection("users")
                .whereEqualTo("adminId", adminId)
                .get()
                .addOnSuccessListener { documents ->
                    val employeeTokens = mutableListOf<String>()

                    // Collect FCM tokens directly from user documents
                    for (document in documents) {
                        val fcmToken = document.getString("fcmToken")
                        Log.d(TAG, "Found employee: ${document.id}, token: ${fcmToken?.take(10)}...")

                        if (!fcmToken.isNullOrEmpty()) {
                            employeeTokens.add(fcmToken)
                        }
                    }

                    Log.d(TAG, "Found ${documents.size()} employees, with ${employeeTokens.size} valid tokens")

                    // Send notification to all found tokens
                    if (employeeTokens.isNotEmpty()) {
                        // Check if we have a valid server key
                        if (SERVER_KEY == "YOUR_FCM_SERVER_KEY") {
                            Log.e(TAG, "Cannot send FCM notifications: SERVER_KEY not configured")
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
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error finding employees: ${e.message}")
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
        sendFCMNotification(tokens, title, message, data)
    }

    private fun sendFCMNotification(
        tokens: List<String>,
        title: String,
        message: String,
        data: Map<String, String> = emptyMap()
    ) {
        try {
            val json = JSONObject().apply {
                put("registration_ids", tokens)

                // Notificación visible
                put("notification", JSONObject().apply {
                    put("title", title)
                    put("body", message)
                    put("sound", "default")
                })

                // Datos adicionales
                val dataJson = JSONObject()
                for ((key, value) in data) {
                    dataJson.put(key, value)
                }
                put("data", dataJson)
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
                    Log.e(TAG, "Error al enviar notificación FCM: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()
                    Log.d(TAG, "Respuesta FCM: $responseBody")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error en sendFCMNotification: ${e.message}")
        }
    }
}