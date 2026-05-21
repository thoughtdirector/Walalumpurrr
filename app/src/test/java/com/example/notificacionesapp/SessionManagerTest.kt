package com.example.notificacionesapp

import android.content.Context
import android.content.SharedPreferences
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.junit.Assert.*

@RunWith(MockitoJUnitRunner::class)
class SessionManagerTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        `when`(mockContext.getSharedPreferences(SessionManager.PREF_NAME, Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor)
        `when`(mockEditor.clear()).thenReturn(mockEditor)
        `when`(mockEditor.commit()).thenReturn(true)

        sessionManager = SessionManager(mockContext)
    }

    @Test
    fun `createLoginSession saves all user details`() {
        sessionManager.createLoginSession("user123", "test@test.com", "admin", "admin456")

        verify(mockEditor).putBoolean(SessionManager.KEY_IS_LOGGED_IN, true)
        verify(mockEditor).putString(SessionManager.KEY_USER_ID, "user123")
        verify(mockEditor).putString(SessionManager.KEY_USER_EMAIL, "test@test.com")
        verify(mockEditor).putString(SessionManager.KEY_USER_ROLE, "admin")
        verify(mockEditor).putString(SessionManager.KEY_ADMIN_ID, "admin456")
        verify(mockEditor).commit()
    }

    @Test
    fun `createLoginSession with null adminId`() {
        sessionManager.createLoginSession("user123", "test@test.com", "employee")

        verify(mockEditor).putString(SessionManager.KEY_ADMIN_ID, null)
        verify(mockEditor).commit()
    }

    @Test
    fun `isLoggedIn returns true when logged in`() {
        `when`(mockSharedPreferences.getBoolean(SessionManager.KEY_IS_LOGGED_IN, false)).thenReturn(true)
        assertTrue(sessionManager.isLoggedIn())
    }

    @Test
    fun `isLoggedIn returns false when not logged in`() {
        `when`(mockSharedPreferences.getBoolean(SessionManager.KEY_IS_LOGGED_IN, false)).thenReturn(false)
        assertFalse(sessionManager.isLoggedIn())
    }

    @Test
    fun `getUserDetails returns all stored fields`() {
        `when`(mockSharedPreferences.getString(SessionManager.KEY_USER_ID, null)).thenReturn("user123")
        `when`(mockSharedPreferences.getString(SessionManager.KEY_USER_EMAIL, null)).thenReturn("test@test.com")
        `when`(mockSharedPreferences.getString(SessionManager.KEY_USER_ROLE, null)).thenReturn("admin")
        `when`(mockSharedPreferences.getString(SessionManager.KEY_ADMIN_ID, null)).thenReturn("admin456")

        val details = sessionManager.getUserDetails()

        assertEquals("user123", details[SessionManager.KEY_USER_ID])
        assertEquals("test@test.com", details[SessionManager.KEY_USER_EMAIL])
        assertEquals("admin", details[SessionManager.KEY_USER_ROLE])
        assertEquals("admin456", details[SessionManager.KEY_ADMIN_ID])
    }

    @Test
    fun `getUserId returns stored user ID`() {
        `when`(mockSharedPreferences.getString(SessionManager.KEY_USER_ID, null)).thenReturn("user123")
        assertEquals("user123", sessionManager.getUserId())
    }

    @Test
    fun `getUserId returns null when not set`() {
        `when`(mockSharedPreferences.getString(SessionManager.KEY_USER_ID, null)).thenReturn(null)
        assertNull(sessionManager.getUserId())
    }

    @Test
    fun `getUserRole returns stored role`() {
        `when`(mockSharedPreferences.getString(SessionManager.KEY_USER_ROLE, null)).thenReturn("employee")
        assertEquals("employee", sessionManager.getUserRole())
    }

    @Test
    fun `getAdminId returns stored admin ID`() {
        `when`(mockSharedPreferences.getString(SessionManager.KEY_ADMIN_ID, null)).thenReturn("admin456")
        assertEquals("admin456", sessionManager.getAdminId())
    }

    @Test
    fun `updateUserRole saves new role`() {
        sessionManager.updateUserRole("admin")
        verify(mockEditor).putString(SessionManager.KEY_USER_ROLE, "admin")
        verify(mockEditor).commit()
    }

    @Test
    fun `logoutUser clears all data`() {
        sessionManager.logoutUser()
        verify(mockEditor).clear()
        verify(mockEditor).commit()
    }
}
