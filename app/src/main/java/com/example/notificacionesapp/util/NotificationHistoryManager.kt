package com.example.notificacionesapp.util

import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/**
 * Gestor del historial de notificaciones.
 * Se encarga de guardar y recuperar el historial de notificaciones procesadas.
 */
class NotificationHistoryManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "notification_history"
        private const val HISTORY_KEY = "notifications"
        private const val MAX_HISTORY_SIZE = 100
    }

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
            Log.e("NotificationHistory", "Error al guardar notificación: ${e.message}")
        }
    }

    /**
     * Obtiene todo el historial de notificaciones.
     */
    fun getNotifications(): List<Map<String, String>> {
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
            Log.e("NotificationHistory", "Error al obtener historial: ${e.message}")
        }

        return result.reversed() // Devolver en orden cronológico inverso (más recientes primero)
    }

    /**
     * Obtiene las notificaciones filtradas por tipo.
     */
    fun getNotificationsByType(type: String): List<Map<String, String>> {
        return getNotifications().filter { it["type"] == type }
    }

    /**
     * Limpia todo el historial de notificaciones.
     */
    fun clearHistory() {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(HISTORY_KEY, "[]").apply()
        } catch (e: Exception) {
            Log.e("NotificationHistory", "Error al limpiar historial: ${e.message}")
        }
    }
}