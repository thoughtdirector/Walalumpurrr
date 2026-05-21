package com.example.notificacionesapp.notification

import com.example.notificacionesapp.util.NotificationHistoryManager
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
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
    fun `processNotification returns message for nequi transfer`() {
        val result = registry.processNotification(
            packageName = "com.nequi",
            title = "Nequi",
            text = "JUAN PEREZ te envió 50.000, ¡lo mejor!"
        )
        assertNotNull(result)
        assertTrue(result!!.contains("JUAN PEREZ"))
        assertTrue(result.contains("50.000"))
    }

    @Test
    fun `processNotification returns message for daviplata`() {
        val result = registry.processNotification(
            packageName = "com.daviplata",
            title = "DaviPlata",
            text = "Recibió una transferencia de 25.000"
        )
        assertNotNull(result)
        assertTrue(result!!.contains("DaviPlata"))
    }

    @Test
    fun `processNotification returns null for unknown package`() {
        val result = registry.processNotification(
            packageName = "com.whatsapp",
            title = "WhatsApp",
            text = "New message"
        )
        assertNull(result)
    }

    @Test
    fun `processNotification returns null for empty package`() {
        val result = registry.processNotification(
            packageName = "",
            title = "Test",
            text = "Test message"
        )
        assertNull(result)
    }

    @Test
    fun `getLastProcessedMetadata returns metadata after processing`() {
        registry.processNotification(
            packageName = "com.nequi",
            title = "Nequi",
            text = "JUAN PEREZ te envió 50.000, ¡lo mejor!"
        )
        val metadata = registry.getLastProcessedMetadata()
        assertEquals("Nequi", metadata["appName"])
        assertEquals("NEQUI", metadata["type"])
    }

    @Test
    fun `getLastProcessedMetadata returns empty map before processing`() {
        val metadata = registry.getLastProcessedMetadata()
        assertTrue(metadata.isEmpty())
    }

    @Test
    fun `processNotification saves to history manager`() {
        registry.processNotification(
            packageName = "com.nequi",
            title = "Nequi",
            text = "JUAN PEREZ te envió 50.000, ¡lo mejor!"
        )

        verify(mockHistoryManager).saveNotification(
            packageName = org.mockito.ArgumentMatchers.eq("com.nequi"),
            appName = org.mockito.ArgumentMatchers.eq("Nequi"),
            title = org.mockito.ArgumentMatchers.eq("Transferencia recibida"),
            content = org.mockito.ArgumentMatchers.anyString(),
            type = org.mockito.ArgumentMatchers.eq("NEQUI"),
            amount = org.mockito.ArgumentMatchers.anyString(),
            sender = org.mockito.ArgumentMatchers.eq("JUAN PEREZ")
        )
    }

    @Test
    fun `processNotification works with null history manager`() {
        val registryNoHistory = NotificationProcessorRegistry(null)
        val result = registryNoHistory.processNotification(
            packageName = "com.nequi",
            title = "Nequi",
            text = "JUAN PEREZ te envió 50.000, ¡lo mejor!"
        )
        assertNotNull(result)
    }

    @Test
    fun `processNotification returns null when processor matches but text does not`() {
        val result = registry.processNotification(
            packageName = "com.nequi",
            title = "Nequi",
            text = "Actualización de seguridad"
        )
        assertNull(result)
    }
}
