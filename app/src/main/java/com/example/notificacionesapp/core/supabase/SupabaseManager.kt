package com.example.notificacionesapp.core.supabase

import android.util.Log
import com.example.notificacionesapp.SessionManager
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class NotificationPayload(
    val title: String,
    val message: String,
    val amount: String? = null,
    val sender: String? = null,
    val adminId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Singleton
class SupabaseManager @Inject constructor(
    private val client: SupabaseClient,
    private val sessionManager: SessionManager
) {

    private val TAG = "SupabaseManager"

    private val exceptionHandler = CoroutineExceptionHandler { _, e ->
        Log.e(TAG, "Error en Supabase Realtime: ${e.message}", e)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)

    private var notificationChannel: io.github.jan.supabase.realtime.RealtimeChannel? = null

    private var reconnectJob: Job? = null

    fun initRealtimeChannel(
        onNotificationReceived: (NotificationPayload) -> Unit = {}
    ) {
        // Realtime vía WebSocket deshabilitado temporalmente por crash en Ktor.
        // El polling en NotificationService (cada 5s) cumple la misma función.
        // Para reactivar: descomentar el bloque de abajo.
        Log.d(TAG, "Realtime por WebSocket deshabilitado. Usando polling como alternativa.")
        /*
        val userId = sessionManager.getUserId()
        val role = sessionManager.getUserRole()
        val adminId = if (role == "admin") userId else sessionManager.getAdminId()

        if (adminId.isNullOrBlank()) {
            Log.e(TAG, "adminId es null")
            return
        }

        val secureChannelName = "notifications-$adminId"

        scope.launch {
            try {
                disconnect()
                notificationChannel = client.channel(secureChannelName) { isPrivate = true }
                notificationChannel
                    ?.broadcastFlow<NotificationPayload>(event = "new_notification")
                    ?.onEach { payload -> onNotificationReceived(payload) }
                    ?.catch { e -> Log.e(TAG, "Error en flujo Realtime: ${e.message}", e) }
                    ?.launchIn(scope)
                notificationChannel?.subscribe()
                Log.d(TAG, "Canal conectado: $secureChannelName")
            } catch (e: Exception) {
                Log.e(TAG, "Error inicializando canal: ${e.message}")
            }
        }
        */
    }

    fun broadcastNotification(
        title: String,
        message: String,
        amount: String? = null,
        sender: String? = null
    ) {
        // Deshabilitado junto con Realtime. La notificación se entrega vía polling.
        Log.d(TAG, "Broadcast deshabilitado. Entrega vía polling en Supabase.")
        /*
        val role = sessionManager.getUserRole()
        if (role != "admin") { Log.w(TAG, "Solo admin puede retransmitir"); return }
        val adminId = sessionManager.getUserId() ?: return
        scope.launch {
            try {
                if (notificationChannel == null) initRealtimeChannel()
                val payload = NotificationPayload(title, message, amount, sender, adminId = adminId)
                val jsonMessage = Json.encodeToJsonElement(payload).jsonObject
                notificationChannel?.broadcast(event = "new_notification", message = jsonMessage)
                Log.d(TAG, "Notificación enviada")
            } catch (e: Exception) {
                Log.e(TAG, "Error enviando notificación: ${e.message}")
            }
        }
        */
    }

    private fun reconnect(
        onNotificationReceived: (NotificationPayload) -> Unit = {}
    ) {
        reconnectJob?.cancel()

        reconnectJob = scope.launch {
            try {
                Log.w(TAG, "Reconectando en 5 segundos...")
                delay(5000)
                disconnect()
                initRealtimeChannel(onNotificationReceived)
            } catch (e: Exception) {
                Log.e(TAG, "Error reconectando: ${e.message}")
            }
        }
    }

    fun disconnect() {
        scope.launch {
            try {
                notificationChannel?.unsubscribe()
                notificationChannel = null
                Log.d(TAG, "Canal desconectado")
            } catch (e: Exception) {
                Log.e(TAG, "Error desconectando: ${e.message}")
            }
        }
    }

    fun destroy() {
        reconnectJob?.cancel()
        disconnect()
        scope.cancel()
    }
}
