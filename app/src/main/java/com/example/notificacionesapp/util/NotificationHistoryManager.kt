// app/src/main/java/com/example/notificacionesapp/util/NotificationHistoryManager.kt (actualizado)
package com.example.notificacionesapp.util

import android.content.Context
import android.util.Log
import com.example.notificacionesapp.firebase.FirebaseManager
import com.example.notificacionesapp.model.NotificationItem
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gestor del historial de notificaciones.
 * Se encarga de guardar y recuperar el historial de notificaciones procesadas.
 * Ahora funciona con almacenamiento local y Firebase.
 */
class NotificationHistoryManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "notification_history"
        private const val HISTORY_KEY = "notifications"
        private const val MAX_HISTORY_SIZE = 100
    }

    private val firebaseManager = FirebaseManager()

    /**
     * Guarda una notificación en el historial.
     */
    fun saveNotification(
        packageName: String,
        appName: String,
        title: String,
        content: String,
        type: String,
        amount: String = "",
        sender: String = ""
    ) {
        try {
            // Guardar localmente para compatibilidad
            saveLocalNotification(packageName, appName, title, content, type, amount, sender)

            // Si hay usuario autenticado, guardar en Firebase
            if (firebaseManager.currentUser != null) {
                val notificationItem = NotificationItem(
                    appName = appName,
                    title = title,
                    content = content,
                    timestamp = System.currentTimeMillis(),
                    sender = sender.ifEmpty { null },
                    amount = amount.ifEmpty { null },
                )

                firebaseManager.saveNotification(notificationItem)
            }
        } catch (e: Exception) {
            Log.e("NotificationHistory", "Error al guardar notificación: ${e.message}")
        }
    }

    private fun saveLocalNotification(
        packageName: String,
        appName: String,
        title: String,
        content: String,
        type: String,
        amount: String = "",
        sender: String = ""
    ) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val historyJson = prefs.getString(HISTORY_KEY, "[]")
            val historyArray = JSONArray(historyJson)

            // Crear nuevo objeto de notificación
            val notification = JSONObject().apply {
                put("packageName", packageName)
                put("appName", appName)
                put("title", title)
                put("content", content)
                put("type", type)
                put("amount", amount)
                put("sender", sender)
                put("timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            }

            // Añadir al historial
            historyArray.put(notification)

            // Mantener el tamaño máximo del historial
            if (historyArray.length() > MAX_HISTORY_SIZE) {
                val trimmedArray = JSONArray()
                for (i in (historyArray.length() - MAX_HISTORY_SIZE) until historyArray.length()) {
                    trimmedArray.put(historyArray.getJSONObject(i))
                }

                // Guardar el historial recortado
                prefs.edit().putString(HISTORY_KEY, trimmedArray.toString()).apply()
            } else {
                // Guardar el historial completo
                prefs.edit().putString(HISTORY_KEY, historyArray.toString()).apply()
            }
        } catch (e: Exception) {
            Log.e("NotificationHistory", "Error al guardar notificación local: ${e.message}")
        }
    }

    /**
     * Obtiene todo el historial de notificaciones.
     * Ahora intenta obtenerlas de Firebase primero, y si falla usa el almacenamiento local.
     */
    fun getNotifications(callback: (List<Map<String, String>>) -> Unit) {
        if (firebaseManager.currentUser != null) {
            // Obtener desde Firebase
            firebaseManager.getNotificationHistory(object : FirebaseManager.NotificationsListener {
                override fun onNotificationsLoaded(notifications: List<NotificationItem>) {
                    // Convertir NotificationItem a Map<String, String>
                    val result = notifications.map { item ->
                        mapOf(
                            "appName" to item.appName,
                            "title" to item.title,
                            "content" to item.content,
                            "timestamp" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                .format(Date(item.timestamp)),
                            "amount" to (item.amount ?: ""),
                            "sender" to (item.sender ?: "")
                        )
                    }
                    callback(result)
                }

                override fun onError(message: String) {
                    Log.e("NotificationHistory", "Error Firebase: $message")
                    // Fallar con elegancia a local
                    callback(getLocalNotifications())
                }
            })
        } else {
            // Sin autenticación, usar almacenamiento local
            callback(getLocalNotifications())
        }
    }

    /**
     * Obtiene el historial local de notificaciones.
     */
    private fun getLocalNotifications(): List<Map<String, String>> {
        val result = mutableListOf<Map<String, String>>()

        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val historyJson = prefs.getString(HISTORY_KEY, "[]")
            val historyArray = JSONArray(historyJson)

            for (i in 0 until historyArray.length()) {
                val item = historyArray.getJSONObject(i)
                val notification = mutableMapOf<String, String>()

                // Extraer todos los campos del JSONObject
                val keys = item.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    notification[key] = item.getString(key)
                }

                result.add(notification)
            }
        } catch (e: Exception) {
            Log.e("NotificationHistory", "Error al obtener historial local: ${e.message}")
        }

        return result.reversed() // Devolver en orden cronológico inverso (más recientes primero)
    }

    /**
     * Obtiene las notificaciones filtradas por tipo.
     */
    fun getNotificationsByType(type: String, callback: (List<Map<String, String>>) -> Unit) {
        if (firebaseManager.currentUser != null) {
            // Obtener desde Firebase
            firebaseManager.getNotificationsByType(type, object : FirebaseManager.NotificationsListener {
                override fun onNotificationsLoaded(notifications: List<NotificationItem>) {
                    // Convertir NotificationItem a Map<String, String>
                    val result = notifications.map { item ->
                        mapOf(
                            "appName" to item.appName,
                            "title" to item.title,
                            "content" to item.content,
                            "timestamp" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                .format(Date(item.timestamp)),
                            "amount" to (item.amount ?: ""),
                            "sender" to (item.sender ?: "")
                        )
                    }
                    callback(result)
                }

                override fun onError(message: String) {
                    Log.e("NotificationHistory", "Error Firebase filtrado: $message")
                    // Fallar con elegancia a local
                    callback(getLocalNotificationsByType(type))
                }
            })
        } else {
            // Sin autenticación, usar almacenamiento local
            callback(getLocalNotificationsByType(type))
        }
    }

    /**
     * Obtiene las notificaciones locales filtradas por tipo.
     */
    private fun getLocalNotificationsByType(type: String): List<Map<String, String>> {
        return getLocalNotifications().filter { it["type"] == type }
    }

    /**
     * Limpia todo el historial de notificaciones.
     */
    fun clearHistory(callback: (Boolean) -> Unit) {
        try {
            // Limpiar almacenamiento local
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(HISTORY_KEY, "[]").apply()

            // Si hay usuario autenticado, limpiar Firebase
            if (firebaseManager.currentUser != null) {
                firebaseManager.clearNotificationHistory(object : FirebaseManager.OperationListener {
                    override fun onSuccess() {
                        callback(true)
                    }

                    override fun onError(message: String) {
                        Log.e("NotificationHistory", "Error al limpiar Firebase: $message")
                        callback(false)
                    }
                })
            } else {
                callback(true)
            }
        } catch (e: Exception) {
            Log.e("NotificationHistory", "Error al limpiar historial: ${e.message}")
            callback(false)
        }
    }
}