package com.example.notificacionesapp.domain.model

import org.junit.Test
import org.junit.Assert.*

class UserTest {

    private fun createUser(role: UserRole = UserRole.ADMIN) = User(
        id = "user123",
        email = "test@test.com",
        firstName = "Juan",
        lastName = "Pérez",
        phone = "3001234567",
        birthDate = "1990-01-15",
        role = role
    )

    @Test
    fun `isAdmin returns true for admin role`() {
        assertTrue(createUser(UserRole.ADMIN).isAdmin())
    }

    @Test
    fun `isAdmin returns false for employee role`() {
        assertFalse(createUser(UserRole.EMPLOYEE).isAdmin())
    }

    @Test
    fun `isEmployee returns true for employee role`() {
        assertTrue(createUser(UserRole.EMPLOYEE).isEmployee())
    }

    @Test
    fun `isEmployee returns false for admin role`() {
        assertFalse(createUser(UserRole.ADMIN).isEmployee())
    }

    @Test
    fun `UserRole has exactly two values`() {
        assertEquals(2, UserRole.values().size)
    }

    @Test
    fun `user defaults are correct`() {
        val user = createUser()
        assertFalse(user.isDisabled)
        assertNull(user.disabledReason)
        assertNull(user.replacedBy)
        assertFalse(user.isResetAccount)
        assertNull(user.originalEmail)
        assertNull(user.adminId)
    }

    @Test
    fun `user with all optional fields`() {
        val user = User(
            id = "u1",
            email = "e@e.com",
            firstName = "A",
            lastName = "B",
            phone = "123",
            birthDate = "2000-01-01",
            role = UserRole.EMPLOYEE,
            adminId = "admin1",
            isDisabled = true,
            disabledReason = "Inactivo",
            replacedBy = "u2",
            isResetAccount = true,
            originalEmail = "old@e.com"
        )
        assertTrue(user.isDisabled)
        assertEquals("Inactivo", user.disabledReason)
        assertEquals("u2", user.replacedBy)
        assertTrue(user.isResetAccount)
        assertEquals("old@e.com", user.originalEmail)
        assertEquals("admin1", user.adminId)
    }
}
