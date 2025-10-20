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
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor)
        `when`(mockEditor.apply()).then { }
        `when`(mockEditor.clear()).thenReturn(mockEditor)
        `when`(mockEditor.commit()).thenReturn(true)

        sessionManager = SessionManager(mockContext)
    }

    @Test
    fun `createLoginSession should save user details correctly`() {
        // Given
        val userId = "test_user_123"
        val email = "test@example.com"
        val role = "admin"

        // When
        sessionManager.createLoginSession(userId, email, role)

        // Then
        verify(mockEditor).putString(SessionManager.KEY_USER_ID, userId)
        verify(mockEditor).putString(SessionManager.KEY_USER_EMAIL, email)
        verify(mockEditor).putString(SessionManager.KEY_USER_ROLE, role)
        verify(mockEditor).putBoolean(SessionManager.KEY_IS_LOGGED_IN, true)
        verify(mockEditor).apply()
    }

    @Test
    fun `getUserDetails should return correct user information`() {
        // Given
        val userId = "test_user_123"
        val email = "test@example.com"
        val role = "employee"
        
        `when`(mockSharedPreferences.getString(SessionManager.KEY_USER_ID, null)).thenReturn(userId)
        `when`(mockSharedPreferences.getString(SessionManager.KEY_USER_EMAIL, null)).thenReturn(email)
        `when`(mockSharedPreferences.getString(SessionManager.KEY_USER_ROLE, null)).thenReturn(role)
        `when`(mockSharedPreferences.getBoolean(SessionManager.KEY_IS_LOGGED_IN, false)).thenReturn(true)

        // When
        val userDetails = sessionManager.getUserDetails()

        // Then
        assertEquals(userId, userDetails[SessionManager.KEY_USER_ID])
        assertEquals(email, userDetails[SessionManager.KEY_USER_EMAIL])
        assertEquals(role, userDetails[SessionManager.KEY_USER_ROLE])
        assertTrue(userDetails[SessionManager.KEY_IS_LOGGED_IN]?.toBoolean() ?: false)
    }

    @Test
    fun `isLoggedIn should return true when user is logged in`() {
        // Given
        `when`(mockSharedPreferences.getBoolean(SessionManager.KEY_IS_LOGGED_IN, false)).thenReturn(true)

        // When
        val isLoggedIn = sessionManager.isLoggedIn()

        // Then
        assertTrue(isLoggedIn)
    }

    @Test
    fun `isLoggedIn should return false when user is not logged in`() {
        // Given
        `when`(mockSharedPreferences.getBoolean(SessionManager.KEY_IS_LOGGED_IN, false)).thenReturn(false)

        // When
        val isLoggedIn = sessionManager.isLoggedIn()

        // Then
        assertFalse(isLoggedIn)
    }

    @Test
    fun `logoutUser should clear all user data`() {
        // When
        sessionManager.logoutUser()

        // Then
        verify(mockEditor).clear()
        verify(mockEditor).commit()
    }

    @Test
    fun `getUserId should return correct user ID`() {
        // Given
        val userId = "test_user_456"
        `when`(mockSharedPreferences.getString(SessionManager.KEY_USER_ID, null)).thenReturn(userId)

        // When
        val result = sessionManager.getUserId()

        // Then
        assertEquals(userId, result)
    }

    @Test
    fun `getUserEmail should return correct email`() {
        // Given
        val email = "user@test.com"
        `when`(mockSharedPreferences.getString(SessionManager.KEY_USER_EMAIL, null)).thenReturn(email)

        // When
        val result = sessionManager.getUserEmail()

        // Then
        assertEquals(email, result)
    }

    @Test
    fun `getUserRole should return correct role`() {
        // Given
        val role = "admin"
        `when`(mockSharedPreferences.getString(SessionManager.KEY_USER_ROLE, null)).thenReturn(role)

        // When
        val result = sessionManager.getUserRole()

        // Then
        assertEquals(role, result)
    }
}
