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
class NequiNotificationProcessorTest {

    @Mock
    private lateinit var mockHistoryManager: NotificationHistoryManager

    private lateinit var processor: NequiNotificationProcessor

    @Before
    fun setUp() {
        processor = NequiNotificationProcessor(mockHistoryManager)
    }

    @Test
    fun `canProcess should return true for Nequi notifications`() {
        // Given
        val notification = NotificationItem(
            id = "1",
            packageName = "com.nequi",
            title = "Nequi",
            text = "Recibiste $50.000",
            timestamp = System.currentTimeMillis()
        )

        // When
        val canProcess = processor.canProcess(notification)

        // Then
        assertTrue(canProcess)
    }

    @Test
    fun `canProcess should return false for non-Nequi notifications`() {
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
    fun `process should extract amount from Nequi notification`() {
        // Given
        val notification = NotificationItem(
            id = "1",
            packageName = "com.nequi",
            title = "Nequi",
            text = "Recibiste $50.000 de Juan Pérez",
            timestamp = System.currentTimeMillis()
        )

        // When
        val result = processor.process(notification)

        // Then
        assertNotNull(result)
        assertTrue(result.contains("50.000"))
        assertTrue(result.contains("Juan Pérez"))
    }

    @Test
    fun `process should handle different amount formats`() {
        // Given
        val notification = NotificationItem(
            id = "1",
            packageName = "com.nequi",
            title = "Nequi",
            text = "Recibiste $1.500.000 de María García",
            timestamp = System.currentTimeMillis()
        )

        // When
        val result = processor.process(notification)

        // Then
        assertNotNull(result)
        assertTrue(result.contains("1.500.000"))
        assertTrue(result.contains("María García"))
    }

    @Test
    fun `process should handle notifications without amounts`() {
        // Given
        val notification = NotificationItem(
            id = "1",
            packageName = "com.nequi",
            title = "Nequi",
            text = "Tu saldo ha sido actualizado",
            timestamp = System.currentTimeMillis()
        )

        // When
        val result = processor.process(notification)

        // Then
        assertNotNull(result)
        assertTrue(result.contains("Nequi"))
        assertTrue(result.contains("saldo"))
    }

    @Test
    fun `process should handle empty text`() {
        // Given
        val notification = NotificationItem(
            id = "1",
            packageName = "com.nequi",
            title = "Nequi",
            text = "",
            timestamp = System.currentTimeMillis()
        )

        // When
        val result = processor.process(notification)

        // Then
        assertNotNull(result)
        assertTrue(result.contains("Nequi"))
    }

    @Test
    fun `process should handle null text`() {
        // Given
        val notification = NotificationItem(
            id = "1",
            packageName = "com.nequi",
            title = "Nequi",
            text = null,
            timestamp = System.currentTimeMillis()
        )

        // When
        val result = processor.process(notification)

        // Then
        assertNotNull(result)
        assertTrue(result.contains("Nequi"))
    }
}
