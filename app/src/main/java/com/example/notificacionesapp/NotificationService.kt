package com.example.notificacionesapp

import android.app.Notification
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale
import java.util.regex.Pattern

class NotificationService : NotificationListenerService(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var lastNotification = ""
    // Patrón original para Nequi
    private val nequiPattern = Pattern.compile("([A-ZÁÉÍÓÚÑ\\s]+) te envió ([0-9,.]+), ¡lo mejor!")

    companion object {
        var isServiceActive = false
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("NotificationService", "Servicio intentando iniciar")

        // Configurar el volumen al máximo para asegurar que se escuche
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume / 2, 0)

        // Inicializar TTS con un pequeño retraso para asegurar que el servicio esté completamente listo
        Handler(Looper.getMainLooper()).postDelayed({
            tts = TextToSpeech(applicationContext, this)
            Log.d("NotificationService", "TTS inicializado con delay")
        }, 2000) // 2 segundos de delay
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            if (!isServiceActive) {
                Log.d("NotificationService", "Servicio inactivo, ignorando notificación")
                return
            }

            val packageName = sbn.packageName
            val notification = sbn.notification
            val extras = notification.extras

            val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

            Log.d("NotificationService", "Notificación recibida de $packageName: $title - $text")

            // Procesar notificaciones de Nequi
            if (packageName.contains("nequi") || packageName.contains("colombia.nequi")) {
                processNequiNotification("$title $text")
            }
            // Procesar notificaciones de WhatsApp
            else if (packageName.contains("whatsapp")) {
                processWhatsAppNotification(title, text)
            }
        } catch (e: Exception) {
            Log.e("NotificationService", "Error al procesar notificación", e)
        }
    }

    private fun processNequiNotification(content: String) {
        // Intentar con el patrón de Nequi
        var matcher = nequiPattern.matcher(content)
        if (matcher.find()) {
            val nombre = matcher.group(1)
            val monto = matcher.group(2)
            val mensaje = "$nombre te envió $monto pesos"

            if (mensaje != lastNotification) {
                lastNotification = mensaje
                speakOut(mensaje)
            }
            return
        }

        // Si no coincide con el patrón, intentar método alternativo
        if (content.contains("te envió")) {
            val parts = content.split("te envió")
            if (parts.size >= 2) {
                val nombre = parts[0].trim()
                val montoRaw = parts[1].replace("¡lo mejor!", "").trim()
                val mensaje = "$nombre te envió $montoRaw pesos"

                if (mensaje != lastNotification) {
                    lastNotification = mensaje
                    speakOut(mensaje)
                }
            }
        }
    }

    private fun processWhatsAppNotification(title: String, text: String) {
        // No leer notificaciones de grupos, solo mensajes directos
        if (!title.contains(":")) {  // Los mensajes de grupo suelen tener formato "Grupo: Remitente: Mensaje"
            val mensaje = "$title dice: $text"

            // Evitar repeticiones
            if (mensaje != lastNotification) {
                lastNotification = mensaje
                Log.d("NotificationService", "Mensaje de WhatsApp: $mensaje")
                speakOut(mensaje)
            }
        }
    }

    private fun speakOut(text: String) {
        if (!isServiceActive) {
            Log.d("NotificationService", "Servicio inactivo, no se leerá la notificación")
            return
        }

        Log.d("NotificationService", "Intentando decir: $text")
        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "notification_id")
        Log.d("NotificationService", "Resultado de TTS speak: $result")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("es", "ES"))

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("NotificationService", "El lenguaje español no está soportado")
            } else {
                Log.d("NotificationService", "TTS inicializado correctamente, intentando hablar")

                // Intentar hablar con un pequeño retraso
                Handler(Looper.getMainLooper()).postDelayed({
                    if (isServiceActive) {
                        speakOut("Servicio de lectura de notificaciones activado")
                        Log.d("NotificationService", "Comando de voz enviado")
                    }
                }, 1000)
            }
        } else {
            Log.e("NotificationService", "Error al inicializar TTS: código de estado = $status")
        }
    }

    override fun onDestroy() {
        if (tts != null) {
            tts?.stop()
            tts?.shutdown()
        }
        super.onDestroy()
    }
}