package com.example.notificacionesapp.notification.processors

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class DaviPlataNotificationProcessorTest {

    private lateinit var processor: DaviPlataNotificationProcessor

    @Before
    fun setUp() {
        processor = DaviPlataNotificationProcessor()
    }

    @Test
    fun `canProcess returns true for daviplata package`() {
        assertTrue(processor.canProcess("com.davivienda.daviplataapp"))
    }

    @Test
    fun `canProcess returns true for package containing daviplata`() {
        assertTrue(processor.canProcess("com.daviplata.debug"))
    }

    @Test
    fun `canProcess returns false for unrelated package`() {
        assertFalse(processor.canProcess("com.whatsapp"))
    }

    @Test
    fun `processNotification returns message for Recibio text`() {
        val result = processor.processNotification(
            title = "DaviPlata",
            text = "Recibió una transferencia de 25.000",
            packageName = "com.daviplata"
        )
        assertNotNull(result)
        assertTrue(result!!.startsWith("DaviPlata: "))
        assertTrue(result.contains("Recibió"))
    }

    @Test
    fun `processNotification returns message for movimientos text`() {
        val result = processor.processNotification(
            title = "DaviPlata",
            text = "Tienes nuevos movimientos en tu cuenta",
            packageName = "com.daviplata"
        )
        assertNotNull(result)
        assertTrue(result!!.contains("movimientos"))
    }

    @Test
    fun `processNotification returns null for non-matching text`() {
        val result = processor.processNotification(
            title = "DaviPlata",
            text = "Promoción especial para ti",
            packageName = "com.daviplata"
        )
        assertNull(result)
    }

    @Test
    fun `processNotification returns null for empty text`() {
        val result = processor.processNotification(
            title = "DaviPlata",
            text = "",
            packageName = "com.daviplata"
        )
        assertNull(result)
    }

    @Test
    fun `getMetadata extracts app name and type`() {
        val metadata = processor.getMetadata("DaviPlata", "Recibió 25.000", "DaviPlata: Recibió 25.000")
        assertEquals("DaviPlata", metadata["appName"])
        assertEquals("DAVIPLATA", metadata["type"])
        assertEquals("DaviPlata", metadata["title"])
    }

    @Test
    fun `getMetadata extracts amount with decimals`() {
        val metadata = processor.getMetadata("DaviPlata", "Recibió 25,500 pesos", "DaviPlata: Recibió 25,500")
        assertEquals("25,500", metadata["amount"])
    }

    @Test
    fun `getMetadata handles text without amount`() {
        val metadata = processor.getMetadata("DaviPlata", "movimientos en tu cuenta", "DaviPlata: movimientos")
        assertEquals("DaviPlata", metadata["appName"])
        assertNull(metadata["amount"])
    }
}
