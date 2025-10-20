package com.example.notificacionesapp.notification

import com.example.notificacionesapp.model.NotificationItem
import com.example.notificacionesapp.notification.processors.DaviPlataNotificationProcessor
import com.example.notificacionesapp.notification.processors.NequiNotificationProcessor
import com.example.notificacionesapp.util.NotificationHistoryManager
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.junit.Assert.*

@RunWith(MockitoJUnitRunner::class)
class NotificationProcessorRegistryTest {

    @Mock
    private lateinit var mockHistoryManager: NotificationHistoryManager

    private lateinit var registry: NotificationProcessorRegistry

    @Before
    fun setUp() {
        registry = NotificationProcessorRegistry(mockHistoryManager)
    }

    @Test
    fun `getProcessor should return NequiProcessor for Nequi notifications`() {
        // Given
        val notification = NotificationItem(
            id = "1",
            packageName = "com.nequi",
            title = "Nequi",
            text = "Recibiste $50.000",
            timestamp = System.currentTimeMillis()
        )

        // When
        val processor = registry.getProcessor(notification)

        // Then
        assertNotNull(processor)
        assertTrue(processor is NequiNotificationProcessor)
    }

    @Test
    fun `getProcessor should return DaviPlataProcessor for DaviPlata notifications`() {
        // Given
        val notification = NotificationItem(
            id = "1",
            packageName = "com.davivienda.daviplataapp",
            title = "DaviPlata",
            text = "Recibiste $25.000",
            timestamp = System.currentTimeMillis()
        )

        // When
        val processor = registry.getProcessor(notification)

        // Then
        assertNotNull(processor)
        assertTrue(processor is DaviPlataNotificationProcessor)
    }

    @Test
    fun `getProcessor should return null for unknown notifications`() {
        // Given
        val notification = NotificationItem(
            id = "1",
            packageName = "com.whatsapp",
            title = "WhatsApp",
            text = "New message",
            timestamp = System.currentTimeMillis()
        )

        // When
        val processor = registry.getProcessor(notification)

        // Then
        assertNull(processor)
    }

    @Test
    fun `getProcessor should return null for null notification`() {
        // When
        val processor = registry.getProcessor(null)

        // Then
        assertNull(processor)
    }

    @Test
    fun `getProcessor should handle case insensitive package names`() {
        // Given
        val notification = NotificationItem(
            id = "1",
            packageName = "COM.NEQUI",
            title = "Nequi",
            text = "Recibiste $50.000",
            timestamp = System.currentTimeMillis()
        )

        // When
        val processor = registry.getProcessor(notification)

        // Then
        assertNotNull(processor)
        assertTrue(processor is NequiNotificationProcessor)
    }

    @Test
    fun `getProcessor should handle partial package names`() {
        // Given
        val notification = NotificationItem(
            id = "1",
            packageName = "com.davivienda.daviplataapp.debug",
            title = "DaviPlata",
            text = "Recibiste $25.000",
            timestamp = System.currentTimeMillis()
        )

        // When
        val processor = registry.getProcessor(notification)

        // Then
        assertNotNull(processor)
        assertTrue(processor is DaviPlataNotificationProcessor)
    }

    @Test
    fun `getProcessor should return null for empty package name`() {
        // Given
        val notification = NotificationItem(
            id = "1",
            packageName = "",
            title = "Test",
            text = "Test message",
            timestamp = System.currentTimeMillis()
        )

        // When
        val processor = registry.getProcessor(notification)

        // Then
        assertNull(processor)
    }

    @Test
    fun `getProcessor should return null for null package name`() {
        // Given
        val notification = NotificationItem(
            id = "1",
            packageName = null,
            title = "Test",
            text = "Test message",
            timestamp = System.currentTimeMillis()
        )

        // When
        val processor = registry.getProcessor(notification)

        // Then
        assertNull(processor)
    }
}
