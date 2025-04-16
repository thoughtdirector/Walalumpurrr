package com.example.notificacionesapp.notification

import android.util.Log
import com.example.notificacionesapp.notification.processors.DaviPlataNotificationProcessor
import com.example.notificacionesapp.notification.processors.NequiNotificationProcessor
import com.example.notificacionesapp.util.NotificationHistoryManager

class NotificationProcessorRegistry(private val historyManager: NotificationHistoryManager?) {
    private val processors = mutableListOf<NotificationProcessor>()
    private var lastMetadata: Map<String, String> = emptyMap()

    init {
        registerDefaultProcessors()
    }

    private fun registerDefaultProcessors() {
        val factory = NotificationProcessorFactory

        processors.add(factory.createProcessor(NotificationProcessorFactory.ProcessorType.NEQUI))
        processors.add(factory.createProcessor(NotificationProcessorFactory.ProcessorType.DAVIPLATA))
    }


    fun addProcessor(processor: NotificationProcessor) {
        processors.add(processor)
    }


    fun processNotification(packageName: String, title: String, text: String): String? {
        for (processor in processors) {
            if (processor.canProcess(packageName)) {
                try {
                    val message = processor.processNotification(title, text, packageName)

                    if (message != null) {
                        // Guardar los metadatos para acceso posterior
                        lastMetadata = processor.getMetadata(title, text, message)

                        historyManager?.let {
                            if (lastMetadata.isNotEmpty()) {
                                it.saveNotification(
                                    packageName = packageName,
                                    appName = lastMetadata["appName"] ?: "Desconocido",
                                    title = lastMetadata["title"] ?: title,
                                    content = message,
                                    type = lastMetadata["type"] ?: "OTRO",
                                    amount = lastMetadata["amount"] ?: "",
                                    sender = lastMetadata["sender"] ?: ""
                                )
                            }
                        }

                        return message
                    }
                } catch (e: Exception) {
                    Log.e("NotificationProcessor", "Error procesando notificación: ${e.message}")
                    lastMetadata = emptyMap()
                }
            }
        }

        return null
    }

    fun getLastProcessedMetadata(): Map<String, String> {
        return lastMetadata
    }

}