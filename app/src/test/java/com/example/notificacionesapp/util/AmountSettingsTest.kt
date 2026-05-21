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
        `when`(mockContext.getSharedPreferences("amount_settings", Context.MODE_PRIVATE))
            .thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor)
        `when`(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor)
        `when`(mockEditor.commit()).thenReturn(true)

        amountSettings = AmountSettings(mockContext)
    }

    @Test
    fun `isAmountLimitEnabled returns value from prefs`() {
        `when`(mockSharedPreferences.getBoolean("amount_limit_enabled", false)).thenReturn(true)
        assertTrue(amountSettings.isAmountLimitEnabled())
    }

    @Test
    fun `isAmountLimitEnabled defaults to false`() {
        `when`(mockSharedPreferences.getBoolean("amount_limit_enabled", false)).thenReturn(false)
        assertFalse(amountSettings.isAmountLimitEnabled())
    }

    @Test
    fun `setAmountLimitEnabled saves to prefs`() {
        amountSettings.setAmountLimitEnabled(true)
        verify(mockEditor).putBoolean("amount_limit_enabled", true)
        verify(mockEditor).commit()
    }

    @Test
    fun `getAmountThreshold returns stored value`() {
        `when`(mockSharedPreferences.getInt("amount_threshold", 100000)).thenReturn(50000)
        assertEquals(50000, amountSettings.getAmountThreshold())
    }

    @Test
    fun `getAmountThreshold defaults to 100000`() {
        `when`(mockSharedPreferences.getInt("amount_threshold", 100000)).thenReturn(100000)
        assertEquals(100000, amountSettings.getAmountThreshold())
    }

    @Test
    fun `setAmountThreshold saves to prefs`() {
        amountSettings.setAmountThreshold(75000)
        verify(mockEditor).putInt("amount_threshold", 75000)
        verify(mockEditor).commit()
    }

    @Test
    fun `shouldReadAmount returns true when limit disabled`() {
        `when`(mockSharedPreferences.getBoolean("amount_limit_enabled", false)).thenReturn(false)
        assertTrue(amountSettings.shouldReadAmount("500000"))
    }

    @Test
    fun `shouldReadAmount returns true when amount is null`() {
        `when`(mockSharedPreferences.getBoolean("amount_limit_enabled", false)).thenReturn(true)
        assertTrue(amountSettings.shouldReadAmount(null))
    }

    @Test
    fun `shouldReadAmount returns true when amount is blank`() {
        `when`(mockSharedPreferences.getBoolean("amount_limit_enabled", false)).thenReturn(true)
        assertTrue(amountSettings.shouldReadAmount(""))
    }

    @Test
    fun `shouldReadAmount returns true when amount is at or below threshold`() {
        `when`(mockSharedPreferences.getBoolean("amount_limit_enabled", false)).thenReturn(true)
        `when`(mockSharedPreferences.getInt("amount_threshold", 100000)).thenReturn(100000)
        assertTrue(amountSettings.shouldReadAmount("50000"))
    }

    @Test
    fun `shouldReadAmount returns true when amount equals threshold`() {
        `when`(mockSharedPreferences.getBoolean("amount_limit_enabled", false)).thenReturn(true)
        `when`(mockSharedPreferences.getInt("amount_threshold", 100000)).thenReturn(100000)
        assertTrue(amountSettings.shouldReadAmount("100000"))
    }

    @Test
    fun `shouldReadAmount returns false when amount exceeds threshold`() {
        `when`(mockSharedPreferences.getBoolean("amount_limit_enabled", false)).thenReturn(true)
        `when`(mockSharedPreferences.getInt("amount_threshold", 100000)).thenReturn(100000)
        assertFalse(amountSettings.shouldReadAmount("150000"))
    }

    @Test
    fun `shouldReadAmount strips non-numeric chars before comparing`() {
        `when`(mockSharedPreferences.getBoolean("amount_limit_enabled", false)).thenReturn(true)
        `when`(mockSharedPreferences.getInt("amount_threshold", 100000)).thenReturn(100000)
        assertTrue(amountSettings.shouldReadAmount("$50.000"))
    }

    @Test
    fun `shouldReadAmount returns true for unparseable amount`() {
        `when`(mockSharedPreferences.getBoolean("amount_limit_enabled", false)).thenReturn(true)
        assertTrue(amountSettings.shouldReadAmount("abc"))
    }
}
