package com.example.notificacionesapp.notification

import com.example.notificacionesapp.notification.processors.DaviPlataNotificationProcessor
import com.example.notificacionesapp.notification.processors.NequiNotificationProcessor

object NotificationProcessorFactory {

    enum class ProcessorType {
        NEQUI,
        DAVIPLATA,
        CUSTOM
    }

    fun createProcessor(type: ProcessorType): NotificationProcessor {
        return when (type) {
            ProcessorType.NEQUI -> NequiNotificationProcessor()
            ProcessorType.DAVIPLATA -> DaviPlataNotificationProcessor()
            ProcessorType.CUSTOM -> throw IllegalArgumentException("Los procesadores personalizados deben implementarse directamente")
        }
    }

    fun createCustomProcessor(
        packageMatcher: (String) -> Boolean,
        processor: (String, String, String) -> String?,
        metadataExtractor: ((String, String, String) -> Map<String, String>)? = null
    ): NotificationProcessor {

        return object : NotificationProcessor {
            override fun canProcess(packageName: String): Boolean {
                return packageMatcher(packageName)
            }

            override fun processNotification(title: String, text: String, packageName: String): String? {
                return processor(title, text, packageName)
            }

            override fun getMetadata(title: String, text: String, processedMessage: String): Map<String, String> {
                return metadataExtractor?.invoke(title, text, processedMessage) ?: emptyMap()
            }
        }
    }
}