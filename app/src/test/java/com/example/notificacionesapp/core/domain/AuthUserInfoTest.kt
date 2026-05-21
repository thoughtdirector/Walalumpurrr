package com.example.notificacionesapp.core.domain

import org.junit.Test
import org.junit.Assert.*

class AuthUserInfoTest {

    @Test
    fun `AuthUserInfo stores id and email`() {
        val info = AuthUserInfo(id = "user123", email = "test@test.com")
        assertEquals("user123", info.id)
        assertEquals("test@test.com", info.email)
    }

    @Test
    fun `AuthUserInfo allows null email`() {
        val info = AuthUserInfo(id = "user123", email = null)
        assertNull(info.email)
    }

    @Test
    fun `AuthUserInfo equality works`() {
        val a = AuthUserInfo(id = "u1", email = "e@e.com")
        val b = AuthUserInfo(id = "u1", email = "e@e.com")
        assertEquals(a, b)
    }

    @Test
    fun `AuthUserInfo inequality on different id`() {
        val a = AuthUserInfo(id = "u1", email = "e@e.com")
        val b = AuthUserInfo(id = "u2", email = "e@e.com")
        assertNotEquals(a, b)
    }
}
