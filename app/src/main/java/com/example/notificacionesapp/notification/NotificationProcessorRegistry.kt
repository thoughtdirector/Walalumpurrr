package com.example.notificacionesapp.notification

import android.util.Log
import com.example.notificacionesapp.notification.processors.DaviPlataNotificationProcessor
import com.example.notificacionesapp.notification.processors.NequiNotificationProcessor
import com.example.notificacionesapp.util.NotificationHistoryManager

class NotificationProcessorRegistry(private val historyManager: NotificationHistoryManager?) {
    private val processors = mutableListOf<NotificationProcessor>()

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
                        historyManager?.let {
                            val metadata = processor.getMetadata(title, text, message)

                            if (metadata.isNotEmpty()) {
                                it.saveNotification(
                                    packageName = packageName,
                                    appName = metadata["appName"] ?: "Desconocido",
                                    title = metadata["title"] ?: title,
                                    content = message,
                                    type = metadata["type"] ?: "OTRO",
                                    amount = metadata["amount"] ?: "",
                                    sender = metadata["sender"] ?: ""
                                )
                            }
                        }

                        return message
                    }
                } catch (e: Exception) {
                    Log.e("NotificationProcessor", "Error procesando notificación: ${e.message}")
                }
            }
        }

        return null
    }
}