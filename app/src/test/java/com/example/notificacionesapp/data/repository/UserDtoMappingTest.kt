package com.example.notificacionesapp.data.repository

import com.example.notificacionesapp.domain.model.UserRole
import org.junit.Test
import org.junit.Assert.*

class UserDtoMappingTest {

    @Test
    fun `toDomainUser maps all fields correctly`() {
        val dto = UserDto(
            id = "u1",
            email = "test@test.com",
            firstName = "Juan",
            lastName = "Pérez",
            phone = "300123",
            birthDate = "1990-01-15",
            role = "admin",
            adminId = "a1",
            isDisabled = true,
            disabledReason = "Inactivo",
            replacedBy = "u2",
            isResetAccount = true,
            originalEmail = "old@test.com"
        )
        val user = dto.toDomainUser()

        assertEquals("u1", user.id)
        assertEquals("test@test.com", user.email)
        assertEquals("Juan", user.firstName)
        assertEquals("Pérez", user.lastName)
        assertEquals("300123", user.phone)
        assertEquals("1990-01-15", user.birthDate)
        assertEquals(UserRole.ADMIN, user.role)
        assertEquals("a1", user.adminId)
        assertTrue(user.isDisabled)
        assertEquals("Inactivo", user.disabledReason)
        assertEquals("u2", user.replacedBy)
        assertTrue(user.isResetAccount)
        assertEquals("old@test.com", user.originalEmail)
    }

    @Test
    fun `toDomainUser maps employee role`() {
        val dto = UserDto(id = "u1", email = "e@e.com", role = "employee")
        assertEquals(UserRole.EMPLOYEE, dto.toDomainUser().role)
    }

    @Test
    fun `toDomainUser maps uppercase role`() {
        val dto = UserDto(id = "u1", email = "e@e.com", role = "ADMIN")
        assertEquals(UserRole.ADMIN, dto.toDomainUser().role)
    }

    @Test
    fun `toDomainUser defaults to EMPLOYEE for unknown role`() {
        val dto = UserDto(id = "u1", email = "e@e.com", role = "unknown")
        assertEquals(UserRole.EMPLOYEE, dto.toDomainUser().role)
    }

    @Test
    fun `toDomainUser defaults to EMPLOYEE for empty role`() {
        val dto = UserDto(id = "u1", email = "e@e.com", role = "")
        assertEquals(UserRole.EMPLOYEE, dto.toDomainUser().role)
    }

    @Test
    fun `toDomainUser handles null optional fields`() {
        val dto = UserDto(id = "u1", email = "e@e.com")
        val user = dto.toDomainUser()
        assertNull(user.adminId)
        assertNull(user.disabledReason)
        assertNull(user.replacedBy)
        assertNull(user.originalEmail)
        assertFalse(user.isDisabled)
        assertFalse(user.isResetAccount)
    }

    @Test
    fun `UserDto defaults are correct`() {
        val dto = UserDto(id = "u1", email = "e@e.com")
        assertEquals("", dto.firstName)
        assertEquals("", dto.lastName)
        assertEquals("", dto.phone)
        assertEquals("", dto.birthDate)
        assertEquals("employee", dto.role)
        assertFalse(dto.isDisabled)
    }
}
