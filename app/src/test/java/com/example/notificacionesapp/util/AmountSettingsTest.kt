package com.example.notificacionesapp.util

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
class AmountSettingsTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    private lateinit var amountSettings: AmountSettings

    @Before
    fun setUp() {
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.apply()).then { }

        amountSettings = AmountSettings(mockContext)
    }

    @Test
    fun `isAmountFilterEnabled should return correct filter status`() {
        // Given
        `when`(mockSharedPreferences.getBoolean("amount_filter_enabled", false)).thenReturn(true)

        // When
        val isEnabled = amountSettings.isAmountFilterEnabled()

        // Then
        assertTrue(isEnabled)
    }

    @Test
    fun `setAmountFilterEnabled should save filter status correctly`() {
        // Given
        val isEnabled = true

        // When
        amountSettings.setAmountFilterEnabled(isEnabled)

        // Then
        verify(mockEditor).putBoolean("amount_filter_enabled", isEnabled)
        verify(mockEditor).apply()
    }

    @Test
    fun `getMinAmount should return correct minimum amount`() {
        // Given
        val minAmount = "1000"
        `when`(mockSharedPreferences.getString("min_amount", "0")).thenReturn(minAmount)

        // When
        val result = amountSettings.getMinAmount()

        // Then
        assertEquals(minAmount, result)
    }

    @Test
    fun `setMinAmount should save minimum amount correctly`() {
        // Given
        val minAmount = "500"

        // When
        amountSettings.setMinAmount(minAmount)

        // Then
        verify(mockEditor).putString("min_amount", minAmount)
        verify(mockEditor).apply()
    }

    @Test
    fun `getMaxAmount should return correct maximum amount`() {
        // Given
        val maxAmount = "100000"
        `when`(mockSharedPreferences.getString("max_amount", "999999999")).thenReturn(maxAmount)

        // When
        val result = amountSettings.getMaxAmount()

        // Then
        assertEquals(maxAmount, result)
    }

    @Test
    fun `setMaxAmount should save maximum amount correctly`() {
        // Given
        val maxAmount = "50000"

        // When
        amountSettings.setMaxAmount(maxAmount)

        // Then
        verify(mockEditor).putString("max_amount", maxAmount)
        verify(mockEditor).apply()
    }

    @Test
    fun `isAmountInRange should return true when amount is within range`() {
        // Given
        val amount = "5000"
        `when`(mockSharedPreferences.getBoolean("amount_filter_enabled", false)).thenReturn(true)
        `when`(mockSharedPreferences.getString("min_amount", "0")).thenReturn("1000")
        `when`(mockSharedPreferences.getString("max_amount", "999999999")).thenReturn("10000")

        // When
        val isInRange = amountSettings.isAmountInRange(amount)

        // Then
        assertTrue(isInRange)
    }

    @Test
    fun `isAmountInRange should return false when amount is below minimum`() {
        // Given
        val amount = "500"
        `when`(mockSharedPreferences.getBoolean("amount_filter_enabled", false)).thenReturn(true)
        `when`(mockSharedPreferences.getString("min_amount", "0")).thenReturn("1000")
        `when`(mockSharedPreferences.getString("max_amount", "999999999")).thenReturn("10000")

        // When
        val isInRange = amountSettings.isAmountInRange(amount)

        // Then
        assertFalse(isInRange)
    }

    @Test
    fun `isAmountInRange should return false when amount is above maximum`() {
        // Given
        val amount = "15000"
        `when`(mockSharedPreferences.getBoolean("amount_filter_enabled", false)).thenReturn(true)
        `when`(mockSharedPreferences.getString("min_amount", "0")).thenReturn("1000")
        `when`(mockSharedPreferences.getString("max_amount", "999999999")).thenReturn("10000")

        // When
        val isInRange = amountSettings.isAmountInRange(amount)

        // Then
        assertFalse(isInRange)
    }

    @Test
    fun `isAmountInRange should return true when filter is disabled`() {
        // Given
        val amount = "500"
        `when`(mockSharedPreferences.getBoolean("amount_filter_enabled", false)).thenReturn(false)

        // When
        val isInRange = amountSettings.isAmountInRange(amount)

        // Then
        assertTrue(isInRange)
    }
}
