package com.example.notificacionesapp.firebase

import android.util.Log
import com.example.notificacionesapp.model.FirebaseNotification
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
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

    private val database = Firebase.database.reference
    private val client = OkHttpClient()

    companion object {
        private const val TAG = "AdminNotificationService"
        private const val FCM_API = "https://fcm.googleapis.com/fcm/send"
        // Nota: Esta clave server debería estar en un servidor seguro
        // y no en el código cliente por razones de seguridad
        private const val SERVER_KEY = "YOUR_FCM_SERVER_KEY"
    }

    fun notifyEmployees(adminId: String, notification: FirebaseNotification) {
        try {
            // Buscar todos los empleados que pertenezcan a este admin
            database.child("users")
                .orderByChild("adminId")
                .equalTo(adminId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val employeeTokens = mutableListOf<String>()

                        // Recopilar tokens FCM de los empleados
                        for (employeeSnapshot in snapshot.children) {
                            val fcmToken = employeeSnapshot.child("fcmToken").getValue(String::class.java)
                            fcmToken?.let {
                                employeeTokens.add(it)
                            }
                        }

                        // Enviar notificación a todos los tokens encontrados
                        if (employeeTokens.isNotEmpty()) {
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
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Error al buscar empleados: ${error.message}")
                    }
                })
        } catch (e: Exception) {
            Log.e(TAG, "Error en notifyEmployees: ${e.message}")
        }
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

    // Enviar notificación específica a un empleado por su ID
    fun notifyEmployee(employeeId: String, notification: FirebaseNotification) {
        try {
            // Buscar token FCM del empleado
            database.child("users").child(employeeId).child("fcmToken")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val fcmToken = snapshot.getValue(String::class.java)

                        if (fcmToken != null) {
                            sendFCMNotification(
                                tokens = listOf(fcmToken),
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
                            Log.w(TAG, "Token FCM no encontrado para el empleado $employeeId")
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Error al buscar token FCM: ${error.message}")
                    }
                })
        } catch (e: Exception) {
            Log.e(TAG, "Error en notifyEmployee: ${e.message}")
        }
    }

    // Enviar notificación de prueba para verificar configuración
    fun sendTestNotification(userId: String, onComplete: (Boolean) -> Unit) {
        try {
            // Buscar token FCM del usuario
            database.child("users").child(userId).child("fcmToken")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val fcmToken = snapshot.getValue(String::class.java)

                        if (fcmToken != null) {
                            // Enviar notificación de prueba
                            val testNotification = FirebaseNotification(
                                id = "test_${System.currentTimeMillis()}",
                                timestamp = System.currentTimeMillis(),
                                adminId = userId,
                                packageName = "test_notification",
                                appName = "Prueba de Notificación",
                                title = "Prueba de notificación",
                                content = "Esta es una notificación de prueba para verificar la configuración",
                                type = "TEST",
                                amount = "",
                                sender = "",
                                read = false
                            )

                            sendFCMNotification(
                                tokens = listOf(fcmToken),
                                title = testNotification.title,
                                message = testNotification.content,
                                data = mapOf(
                                    "type" to testNotification.type,
                                    "notificationId" to testNotification.id
                                )
                            )

                            onComplete(true)
                        } else {
                            Log.w(TAG, "Token FCM no encontrado para el usuario $userId")
                            onComplete(false)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e(TAG, "Error al buscar token FCM: ${error.message}")
                        onComplete(false)
                    }
                })
        } catch (e: Exception) {
            Log.e(TAG, "Error en sendTestNotification: ${e.message}")
            onComplete(false)
        }
    }
}