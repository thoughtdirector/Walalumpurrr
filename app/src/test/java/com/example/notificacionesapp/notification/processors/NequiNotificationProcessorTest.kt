package com.example.notificacionesapp.notification.processors

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class NequiNotificationProcessorTest {

    private lateinit var processor: NequiNotificationProcessor

    @Before
    fun setUp() {
        processor = NequiNotificationProcessor()
    }

    @Test
    fun `canProcess returns true for nequi package`() {
        assertTrue(processor.canProcess("com.nequi"))
    }

    @Test
    fun `canProcess returns true for colombia nequi package`() {
        assertTrue(processor.canProcess("com.colombia.nequi"))
    }

    @Test
    fun `canProcess returns false for unrelated package`() {
        assertFalse(processor.canProcess("com.whatsapp"))
    }

    @Test
    fun `canProcess returns false for empty package`() {
        assertFalse(processor.canProcess(""))
    }

    @Test
    fun `processNotification extracts sender and amount for normal transfer`() {
        val result = processor.processNotification(
            title = "Nequi",
            text = "JUAN PEREZ te envió 50.000, ¡lo mejor!",
            packageName = "com.nequi"
        )
        assertNotNull(result)
        assertTrue(result!!.contains("JUAN PEREZ"))
        assertTrue(result.contains("50.000"))
        assertTrue(result.contains("pesos por nequi"))
    }

    @Test
    fun `processNotification extracts QR transfer`() {
        val result = processor.processNotification(
            title = "Nequi",
            text = "MARIA LOPEZ te envió \$10 a través de QR a tu nequi, ¡lo mejor!",
            packageName = "com.nequi"
        )
        assertNotNull(result)
        assertTrue(result!!.contains("MARIA LOPEZ"))
        assertTrue(result.contains("10"))
        assertTrue(result.contains("por QR a tu Nequi"))
    }

    @Test
    fun `processNotification returns null for non-matching text`() {
        val result = processor.processNotification(
            title = "Nequi",
            text = "Tu saldo ha sido actualizado",
            packageName = "com.nequi"
        )
        assertNull(result)
    }

    @Test
    fun `processNotification returns null for empty text`() {
        val result = processor.processNotification(
            title = "Nequi",
            text = "",
            packageName = "com.nequi"
        )
        assertNull(result)
    }

    @Test
    fun `getMetadata extracts sender and amount`() {
        val processedMessage = "JUAN PEREZ te envió 50.000 pesos por nequi"
        val metadata = processor.getMetadata("Nequi", "original text", processedMessage)

        assertEquals("Nequi", metadata["appName"])
        assertEquals("NEQUI", metadata["type"])
        assertEquals("Transferencia recibida", metadata["title"])
        assertEquals("JUAN PEREZ", metadata["sender"])
        assertNotNull(metadata["amount"])
    }

    @Test
    fun `getMetadata returns empty map for non-transfer message`() {
        val metadata = processor.getMetadata("Nequi", "text", "saldo actualizado")
        assertTrue(metadata.isEmpty())
    }

    @Test
    fun `processNotification handles large amounts`() {
        val result = processor.processNotification(
            title = "Nequi",
            text = "CARLOS GARCIA te envió 1.500.000, ¡lo mejor!",
            packageName = "com.nequi"
        )
        assertNotNull(result)
        assertTrue(result!!.contains("1.500.000"))
    }
}
