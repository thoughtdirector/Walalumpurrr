package com.example.notificacionesapp.core.notification

import com.example.notificacionesapp.domain.model.Notification
import com.example.notificacionesapp.domain.repository.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registry for managing notification processors
 */
@Singleton
class NotificationProcessorRegistry @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    
    private val processors = mutableListOf<NotificationProcessor>()
    
    /**
     * Register a notification processor
     */
    fun registerProcessor(processor: NotificationProcessor) {
        processors.add(processor)
    }
    
    /**
     * Process a notification using the appropriate processor
     */
    suspend fun processNotification(
        packageName: String,
        title: String,
        text: String
    ): ProcessedNotification? {
        val processor = processors.find { it.canProcess(packageName) }
        
        return processor?.processNotification(packageName, title, text)?.also { processedNotification ->
            // Save the processed notification
            val notification = Notification(
                packageName = packageName,
                appName = processor.getAppName(),
                title = title,
                content = processedNotification.message,
                type = processedNotification.type,
                amount = processedNotification.amount,
                sender = processedNotification.sender
            )
            
            notificationRepository.saveNotification(notification)
        }
    }
    
    /**
     * Get all registered processors
     */
    fun getProcessors(): List<NotificationProcessor> = processors.toList()
    
    /**
     * Get processor by package name
     */
    fun getProcessor(packageName: String): NotificationProcessor? {
        return processors.find { it.canProcess(packageName) }
    }
}
