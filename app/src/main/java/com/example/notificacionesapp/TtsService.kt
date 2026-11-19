package com.example.notificacionesapp

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Servicio dedicado de TTS para NotiBank Lite.
 *
 * Vive independientemente de MainActivityLite para que las notificaciones
 * reales se lean en voz alta incluso cuando la actividad no está visible.
 */
class TtsService : Service(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech

    override fun onCreate() {
        super.onCreate()
        Log.d("TtsService", "TtsService onCreate")
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(java.util.Locale("es", "ES"))
            tts.setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            Log.d("TtsService", "TTS listo (es-ES)")
        } else {
            Log.e("TtsService", "TTS no disponible")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val text = intent?.getStringExtra("text") ?: return START_NOT_STICKY
        Log.d("TtsService", "Hablando: $text")
        if (::tts.isInitialized) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "notification_tts")
        } else {
            Log.e("TtsService", "TTS no inicializado, no se puede hablar")
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        try {
            tts.stop()
            tts.shutdown()
        } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
