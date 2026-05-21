package com.example.notificacionesapp.data.repository

import com.example.notificacionesapp.domain.model.Notification
import com.example.notificacionesapp.domain.model.NotificationType
import org.junit.Test
import org.junit.Assert.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationDtoMappingTest {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    @Test
    fun `toDomainNotification maps all fields`() {
        val dto = NotificationDto(
            id = "n1",
            packageName = "com.nequi",
            appName = "Nequi",
            title = "Transfer",
            content = "Got 50k",
            notificationType = "transfer_received",
            amount = "50000",
            sender = "JUAN",
            adminId = "admin1",
            timestampIso = "2024-01-15T10:30:00",
            isProcessed = true
        )
        val notification = dto.toDomainNotification()

        assertEquals("n1", notification.id)
        assertEquals("com.nequi", notification.packageName)
        assertEquals("Nequi", notification.appName)
        assertEquals("Transfer", notification.title)
        assertEquals("Got 50k", notification.content)
        assertEquals(NotificationType.TRANSFER_RECEIVED, notification.type)
        assertEquals("50000", notification.amount)
        assertEquals("JUAN", notification.sender)
        assertEquals("admin1", notification.adminId)
        assertTrue(notification.isProcessed)
    }

    @Test
    fun `toDomainNotification defaults to OTHER for unknown type`() {
        val dto = NotificationDto(
            id = "n1", packageName = "p", appName = "a",
            title = "t", content = "c", notificationType = "unknown",
            timestampIso = "2024-01-15T10:30:00"
        )
        assertEquals(NotificationType.OTHER, dto.toDomainNotification().type)
    }

    @Test
    fun `toDomainNotification handles uppercase type`() {
        val dto = NotificationDto(
            id = "n1", packageName = "p", appName = "a",
            title = "t", content = "c", notificationType = "PAYMENT_RECEIVED",
            timestampIso = "2024-01-15T10:30:00"
        )
        assertEquals(NotificationType.PAYMENT_RECEIVED, dto.toDomainNotification().type)
    }

    @Test
    fun `toDomainNotification handles invalid timestamp gracefully`() {
        val dto = NotificationDto(
            id = "n1", packageName = "p", appName = "a",
            title = "t", content = "c", notificationType = "other",
            timestampIso = "invalid-date"
        )
        val notification = dto.toDomainNotification()
        assertNotNull(notification.timestamp)
    }

    @Test
    fun `toDto maps domain to dto correctly`() {
        val now = Date()
        val notification = Notification(
            id = "n1",
            packageName = "com.nequi",
            appName = "Nequi",
            title = "Transfer",
            content = "Content",
            type = NotificationType.TRANSFER_RECEIVED,
            amount = "50000",
            sender = "JUAN",
            adminId = "admin1",
            timestamp = now,
            isProcessed = true
        )
        val dto = notification.toDto()

        assertEquals("n1", dto.id)
        assertEquals("com.nequi", dto.packageName)
        assertEquals("Nequi", dto.appName)
        assertEquals("Transfer", dto.title)
        assertEquals("Content", dto.content)
        assertEquals("transfer_received", dto.notificationType)
        assertEquals("50000", dto.amount)
        assertEquals("JUAN", dto.sender)
        assertEquals("admin1", dto.adminId)
        assertEquals(dateFormat.format(now), dto.timestampIso)
        assertTrue(dto.isProcessed)
    }

    @Test
    fun `roundtrip domain to dto and back preserves data`() {
        val original = Notification(
            id = "n1",
            packageName = "com.nequi",
            appName = "Nequi",
            title = "Transfer",
            content = "Content",
            type = NotificationType.PAYMENT_SENT,
            amount = "10000",
            sender = "MARIA"
        )
        val roundtripped = original.toDto().toDomainNotification()

        assertEquals(original.id, roundtripped.id)
        assertEquals(original.packageName, roundtripped.packageName)
        assertEquals(original.type, roundtripped.type)
        assertEquals(original.amount, roundtripped.amount)
        assertEquals(original.sender, roundtripped.sender)
    }

    @Test
    fun `NotificationDto defaults are correct`() {
        val dto = NotificationDto(
            id = "n1", packageName = "p", appName = "a",
            title = "t", content = "c", notificationType = "other",
            timestampIso = "2024-01-01T00:00:00"
        )
        assertEquals("", dto.amount)
        assertEquals("", dto.sender)
        assertNull(dto.adminId)
        assertTrue(dto.isProcessed)
    }
}
