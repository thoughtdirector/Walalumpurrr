package com.example.notificacionesapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.SharedPreferences
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.junit.Assert.*
import java.util.Calendar

@RunWith(MockitoJUnitRunner::class)
class ScheduleManagerTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    @Mock
    private lateinit var mockAlarmManager: AlarmManager

    private lateinit var scheduleManager: ScheduleManager

    @Before
    fun setUp() {
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockSharedPreferences)
        `when`(mockContext.getSystemService(Context.ALARM_SERVICE)).thenReturn(mockAlarmManager)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor)
        `when`(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor)
        `when`(mockEditor.apply()).then { }

        scheduleManager = ScheduleManager(mockContext)
    }

    @Test
    fun `saveScheduleSettings should save schedule configuration correctly`() {
        // Given
        val isEnabled = true
        val startHour = 9
        val startMinute = 30
        val endHour = 17
        val endMinute = 45

        // When
        scheduleManager.saveScheduleSettings(isEnabled, startHour, startMinute, endHour, endMinute)

        // Then
        verify(mockEditor).putBoolean(ScheduleManager.KEY_SCHEDULE_ENABLED, isEnabled)
        verify(mockEditor).putInt(ScheduleManager.KEY_START_HOUR, startHour)
        verify(mockEditor).putInt(ScheduleManager.KEY_START_MINUTE, startMinute)
        verify(mockEditor).putInt(ScheduleManager.KEY_END_HOUR, endHour)
        verify(mockEditor).putInt(ScheduleManager.KEY_END_MINUTE, endMinute)
        verify(mockEditor).apply()
    }

    @Test
    fun `isScheduleEnabled should return correct schedule status`() {
        // Given
        `when`(mockSharedPreferences.getBoolean(ScheduleManager.KEY_SCHEDULE_ENABLED, false)).thenReturn(true)

        // When
        val isEnabled = scheduleManager.isScheduleEnabled()

        // Then
        assertTrue(isEnabled)
    }

    @Test
    fun `getStartHour should return correct start hour`() {
        // Given
        val startHour = 8
        `when`(mockSharedPreferences.getInt(ScheduleManager.KEY_START_HOUR, 9)).thenReturn(startHour)

        // When
        val result = scheduleManager.getStartHour()

        // Then
        assertEquals(startHour, result)
    }

    @Test
    fun `getStartMinute should return correct start minute`() {
        // Given
        val startMinute = 15
        `when`(mockSharedPreferences.getInt(ScheduleManager.KEY_START_MINUTE, 0)).thenReturn(startMinute)

        // When
        val result = scheduleManager.getStartMinute()

        // Then
        assertEquals(startMinute, result)
    }

    @Test
    fun `getEndHour should return correct end hour`() {
        // Given
        val endHour = 18
        `when`(mockSharedPreferences.getInt(ScheduleManager.KEY_END_HOUR, 17)).thenReturn(endHour)

        // When
        val result = scheduleManager.getEndHour()

        // Then
        assertEquals(endHour, result)
    }

    @Test
    fun `getEndMinute should return correct end minute`() {
        // Given
        val endMinute = 30
        `when`(mockSharedPreferences.getInt(ScheduleManager.KEY_END_MINUTE, 0)).thenReturn(endMinute)

        // When
        val result = scheduleManager.getEndMinute()

        // Then
        assertEquals(endMinute, result)
    }

    @Test
    fun `saveDaySettings should save day configuration correctly`() {
        // Given
        val monday = true
        val tuesday = false
        val wednesday = true
        val thursday = false
        val friday = true
        val saturday = false
        val sunday = true

        // When
        scheduleManager.saveDaySettings(monday, tuesday, wednesday, thursday, friday, saturday, sunday)

        // Then
        verify(mockEditor).putBoolean(ScheduleManager.KEY_MONDAY, monday)
        verify(mockEditor).putBoolean(ScheduleManager.KEY_TUESDAY, tuesday)
        verify(mockEditor).putBoolean(ScheduleManager.KEY_WEDNESDAY, wednesday)
        verify(mockEditor).putBoolean(ScheduleManager.KEY_THURSDAY, thursday)
        verify(mockEditor).putBoolean(ScheduleManager.KEY_FRIDAY, friday)
        verify(mockEditor).putBoolean(ScheduleManager.KEY_SATURDAY, saturday)
        verify(mockEditor).putBoolean(ScheduleManager.KEY_SUNDAY, sunday)
        verify(mockEditor).apply()
    }

    @Test
    fun `isDayEnabled should return correct day status`() {
        // Given
        val dayOfWeek = Calendar.MONDAY
        `when`(mockSharedPreferences.getBoolean(ScheduleManager.KEY_MONDAY, true)).thenReturn(true)

        // When
        val isEnabled = scheduleManager.isDayEnabled(dayOfWeek)

        // Then
        assertTrue(isEnabled)
    }

    @Test
    fun `isDayEnabled should return false for disabled day`() {
        // Given
        val dayOfWeek = Calendar.TUESDAY
        `when`(mockSharedPreferences.getBoolean(ScheduleManager.KEY_TUESDAY, true)).thenReturn(false)

        // When
        val isEnabled = scheduleManager.isDayEnabled(dayOfWeek)

        // Then
        assertFalse(isEnabled)
    }

    @Test
    fun `shouldServiceBeActive should return false when schedule is disabled`() {
        // Given
        `when`(mockSharedPreferences.getBoolean(ScheduleManager.KEY_SCHEDULE_ENABLED, false)).thenReturn(false)

        // When
        val shouldBeActive = scheduleManager.shouldServiceBeActive()

        // Then
        assertFalse(shouldBeActive)
    }

    @Test
    fun `shouldServiceBeActive should return false when current day is disabled`() {
        // Given
        `when`(mockSharedPreferences.getBoolean(ScheduleManager.KEY_SCHEDULE_ENABLED, false)).thenReturn(true)
        // Mock current day as Monday and Monday is disabled
        `when`(mockSharedPreferences.getBoolean(ScheduleManager.KEY_MONDAY, true)).thenReturn(false)

        // When
        val shouldBeActive = scheduleManager.shouldServiceBeActive()

        // Then
        assertFalse(shouldBeActive)
    }

    @Test
    fun `getNextScheduledEvent should return null when schedule is disabled`() {
        // Given
        `when`(mockSharedPreferences.getBoolean(ScheduleManager.KEY_SCHEDULE_ENABLED, false)).thenReturn(false)

        // When
        val nextEvent = scheduleManager.getNextScheduledEvent()

        // Then
        assertNull(nextEvent)
    }
}
