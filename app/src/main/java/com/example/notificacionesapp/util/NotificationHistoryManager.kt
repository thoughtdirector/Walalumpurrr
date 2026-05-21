package com.example.notificacionesapp.util

import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

class NotificationHistoryManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "notification_history"
        private const val HISTORY_KEY = "notifications"
        private const val MAX_HISTORY_SIZE = 100
    }

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

            historyArray.put(notification)

            if (historyArray.length() > MAX_HISTORY_SIZE) {
                val trimmedArray = JSONArray()
                for (i in (historyArray.length() - MAX_HISTORY_SIZE) until historyArray.length()) {
                    trimmedArray.put(historyArray.getJSONObject(i))
                }
                prefs.edit().putString(HISTORY_KEY, trimmedArray.toString()).apply()
            } else {
                prefs.edit().putString(HISTORY_KEY, historyArray.toString()).apply()
            }
        } catch (e: Exception) {
            Log.e("NotificationHistory", "Error al guardar notificación: ${e.message}")
        }
    }

    fun getNotifications(): List<Map<String, String>> {
        val result = mutableListOf<Map<String, String>>()

        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val historyJson = prefs.getString(HISTORY_KEY, "[]")
            val historyArray = JSONArray(historyJson)

            for (i in 0 until historyArray.length()) {
                val item = historyArray.getJSONObject(i)
                val notification = mutableMapOf<String, String>()
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

        return result.reversed()
    }

    fun getNotificationsByType(type: String): List<Map<String, String>> {
        return getNotifications().filter { it["type"] == type }
    }

    fun clearHistory() {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(HISTORY_KEY, "[]").apply()
        } catch (e: Exception) {
            Log.e("NotificationHistory", "Error al limpiar historial: ${e.message}")
        }
    }
}
