package com.example.notificacionesapp.domain.model

import org.junit.Test
import org.junit.Assert.*
import java.util.Date

class NotificationTest {

    @Test
    fun `notification defaults are correct`() {
        val notification = Notification(
            packageName = "com.nequi",
            appName = "Nequi",
            title = "Transfer",
            content = "Got money",
            type = NotificationType.TRANSFER_RECEIVED
        )
        assertEquals("", notification.id)
        assertEquals("", notification.amount)
        assertEquals("", notification.sender)
        assertNull(notification.adminId)
        assertTrue(notification.isProcessed)
    }

    @Test
    fun `notification with all fields`() {
        val now = Date()
        val notification = Notification(
            id = "n1",
            packageName = "com.nequi",
            appName = "Nequi",
            title = "Transferencia",
            content = "JUAN te envió 50.000",
            type = NotificationType.TRANSFER_RECEIVED,
            amount = "50000",
            sender = "JUAN",
            adminId = "admin1",
            timestamp = now,
            isProcessed = false
        )
        assertEquals("n1", notification.id)
        assertEquals("50000", notification.amount)
        assertEquals("JUAN", notification.sender)
        assertEquals("admin1", notification.adminId)
        assertEquals(now, notification.timestamp)
        assertFalse(notification.isProcessed)
    }

    @Test
    fun `NotificationType has all expected values`() {
        val types = NotificationType.values()
        assertEquals(8, types.size)
        assertNotNull(NotificationType.PAYMENT_RECEIVED)
        assertNotNull(NotificationType.PAYMENT_SENT)
        assertNotNull(NotificationType.TRANSFER_RECEIVED)
        assertNotNull(NotificationType.TRANSFER_SENT)
        assertNotNull(NotificationType.BALANCE_UPDATE)
        assertNotNull(NotificationType.PROMOTION)
        assertNotNull(NotificationType.SECURITY_ALERT)
        assertNotNull(NotificationType.OTHER)
    }

    @Test
    fun `notification copy preserves all fields`() {
        val original = Notification(
            id = "n1",
            packageName = "com.nequi",
            appName = "Nequi",
            title = "Test",
            content = "Content",
            type = NotificationType.OTHER,
            amount = "1000",
            sender = "Sender"
        )
        val copy = original.copy(amount = "2000")
        assertEquals("2000", copy.amount)
        assertEquals(original.id, copy.id)
        assertEquals(original.sender, copy.sender)
    }
}
