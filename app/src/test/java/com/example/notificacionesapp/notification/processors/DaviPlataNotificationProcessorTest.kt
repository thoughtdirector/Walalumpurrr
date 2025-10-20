package com.example.notificacionesapp.notification.processors

import com.example.notificacionesapp.model.NotificationItem
import com.example.notificacionesapp.util.NotificationHistoryManager
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.junit.Assert.*

@RunWith(MockitoJUnitRunner::class)
class DaviPlataNotificationProcessorTest {

    @Mock
    private lateinit var mockHistoryManager: NotificationHistoryManager

    private lateinit var processor: DaviPlataNotificationProcessor

    @Before
    fun setUp() {
        processor = DaviPlataNotificationProcessor(mockHistoryManager)
    }

    @Test
    fun `canProcess should return true for DaviPlata notifications`() {
        // Given
        val notification = NotificationItem(
            id = "1",
            packageName = "com.davivienda.daviplataapp",
            title = "DaviPlata",
            text = "Recibiste $25.000",
            timestamp = System.currentTimeMillis()
        )

        // When
        val canProcess = processor.canProcess(notification)

        // Then
        assertTrue(canProcess)
    }

    @Test
    fun `canProcess should return false for non-DaviPlata notifications`() {
        // Given
        val notification = NotificationItem(
            id = "1",
            packageName = "com.whatsapp",
            title = "WhatsApp",
            text = "New message",
            timestamp = System.currentTimeMillis()
        )

        // When
        val canProcess = processor.canProcess(notification)

        // Then
        assertFalse(canProcess)
    }

    @Test
    fun `process should extract amount from DaviPlata notification`() {
        // Given
        val notification = NotificationItem(
            id = "1",
            packageName = "com.davivienda.daviplataapp",
            title = "DaviPlata",
            text = "Recibiste $25.000 de Carlos López",
            timestamp = System.currentTimeMillis()
        )

        // When
        val result = processor.process(notification)

        // Then
        assertNotNull(result)
        assertTrue(result.contains("25.000"))
        assertTrue(result.contains("Carlos López"))
    }

    @Test
    fun `process should handle different amount formats`() {
        // Given
        val notification = NotificationItem(
            id = "1",
            packageName = "com.davivienda.daviplataapp",
            title = "DaviPlata",
            text = "Recibiste $2.000.000 de Ana Rodríguez",
            timestamp = System.currentTimeMillis()
        )

        // When
        val result = processor.process(notification)

        // Then
        assertNotNull(result)
        assertTrue(result.contains("2.000.000"))
        assertTrue(result.contains("Ana Rodríguez"))
    }

    @Test
    fun `process should handle notifications without amounts`() {
        // Given
        val notification = NotificationItem(
            id = "1",
            packageName = "com.davivienda.daviplataapp",
            title = "DaviPlata",
            text = "Tu cuenta ha sido actualizada",
            timestamp = System.currentTimeMillis()
        )

        // When
        val result = processor.process(notification)

        // Then
        assertNotNull(result)
        assertTrue(result.contains("DaviPlata"))
        assertTrue(result.contains("cuenta"))
    }

    @Test
    fun `process should handle empty text`() {
        // Given
        val notification = NotificationItem(
            id = "1",
            packageName = "com.davivienda.daviplataapp",
            title = "DaviPlata",
            text = "",
            timestamp = System.currentTimeMillis()
        )

        // When
        val result = processor.process(notification)

        // Then
        assertNotNull(result)
        assertTrue(result.contains("DaviPlata"))
    }

    @Test
    fun `process should handle null text`() {
        // Given
        val notification = NotificationItem(
            id = "1",
            packageName = "com.davivienda.daviplataapp",
            title = "DaviPlata",
            text = null,
            timestamp = System.currentTimeMillis()
        )

        // When
        val result = processor.process(notification)

        // Then
        assertNotNull(result)
        assertTrue(result.contains("DaviPlata"))
    }
}
