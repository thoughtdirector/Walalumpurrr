package com.example.notificacionesapp.core.di

import com.example.notificacionesapp.core.notification.NotificationProcessorRegistry
import com.example.notificacionesapp.core.notification.processors.DaviPlataNotificationProcessor
import com.example.notificacionesapp.core.notification.processors.NequiNotificationProcessor
import com.example.notificacionesapp.domain.repository.NotificationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for notification-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {

    @Provides
    @Singleton
    fun provideNotificationProcessorRegistry(
        notificationRepository: NotificationRepository,
        nequiProcessor: NequiNotificationProcessor,
        daviPlataProcessor: DaviPlataNotificationProcessor
    ): NotificationProcessorRegistry {
        val registry = NotificationProcessorRegistry(notificationRepository)
        
        registry.registerProcessor(nequiProcessor)
        registry.registerProcessor(daviPlataProcessor)
        
        return registry
    }

    @Provides
    @Singleton
    fun provideNequiNotificationProcessor(): NequiNotificationProcessor {
        return NequiNotificationProcessor()
    }

    @Provides
    @Singleton
    fun provideDaviPlataNotificationProcessor(): DaviPlataNotificationProcessor {
        return DaviPlataNotificationProcessor()
    }
}
