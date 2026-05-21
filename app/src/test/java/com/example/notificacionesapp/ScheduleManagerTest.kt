package com.example.notificacionesapp

import android.app.AlarmManager
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
        `when`(mockEditor.commit()).thenReturn(true)

        scheduleManager = ScheduleManager(mockContext)
    }

    @Test
    fun `saveScheduleSettings saves all fields`() {
        scheduleManager.saveScheduleSettings(true, 9, 30, 17, 45)

        verify(mockEditor).putBoolean(ScheduleManager.KEY_SCHEDULE_ENABLED, true)
        verify(mockEditor).putInt(ScheduleManager.KEY_START_HOUR, 9)
        verify(mockEditor).putInt(ScheduleManager.KEY_START_MINUTE, 30)
        verify(mockEditor).putInt(ScheduleManager.KEY_END_HOUR, 17)
        verify(mockEditor).putInt(ScheduleManager.KEY_END_MINUTE, 45)
        verify(mockEditor).commit()
    }

    @Test
    fun `isScheduleEnabled returns stored value`() {
        `when`(mockSharedPreferences.getBoolean(ScheduleManager.KEY_SCHEDULE_ENABLED, false)).thenReturn(true)
        assertTrue(scheduleManager.isScheduleEnabled())
    }

    @Test
    fun `isScheduleEnabled defaults to false`() {
        `when`(mockSharedPreferences.getBoolean(ScheduleManager.KEY_SCHEDULE_ENABLED, false)).thenReturn(false)
        assertFalse(scheduleManager.isScheduleEnabled())
    }

    @Test
    fun `getStartHour returns stored value`() {
        `when`(mockSharedPreferences.getInt(ScheduleManager.KEY_START_HOUR, 8)).thenReturn(10)
        assertEquals(10, scheduleManager.getStartHour())
    }

    @Test
    fun `getStartMinute returns stored value`() {
        `when`(mockSharedPreferences.getInt(ScheduleManager.KEY_START_MINUTE, 0)).thenReturn(15)
        assertEquals(15, scheduleManager.getStartMinute())
    }

    @Test
    fun `getEndHour returns stored value`() {
        `when`(mockSharedPreferences.getInt(ScheduleManager.KEY_END_HOUR, 18)).thenReturn(20)
        assertEquals(20, scheduleManager.getEndHour())
    }

    @Test
    fun `getEndMinute returns stored value`() {
        `when`(mockSharedPreferences.getInt(ScheduleManager.KEY_END_MINUTE, 0)).thenReturn(30)
        assertEquals(30, scheduleManager.getEndMinute())
    }

    @Test
    fun `isDayEnabled returns true for enabled day`() {
        `when`(mockSharedPreferences.getBoolean(ScheduleManager.KEY_MONDAY, true)).thenReturn(true)
        assertTrue(scheduleManager.isDayEnabled(Calendar.MONDAY))
    }

    @Test
    fun `isDayEnabled returns false for disabled day`() {
        `when`(mockSharedPreferences.getBoolean(ScheduleManager.KEY_SATURDAY, false)).thenReturn(false)
        assertFalse(scheduleManager.isDayEnabled(Calendar.SATURDAY))
    }

    @Test
    fun `isDayEnabled returns false for invalid day`() {
        assertFalse(scheduleManager.isDayEnabled(99))
    }

    @Test
    fun `shouldServiceBeActive returns false when schedule disabled`() {
        `when`(mockSharedPreferences.getBoolean(ScheduleManager.KEY_SCHEDULE_ENABLED, false)).thenReturn(false)
        assertFalse(scheduleManager.shouldServiceBeActive())
    }

    @Test
    fun `isNightSchedule returns true when end before start`() {
        `when`(mockSharedPreferences.getInt(ScheduleManager.KEY_START_HOUR, 8)).thenReturn(22)
        `when`(mockSharedPreferences.getInt(ScheduleManager.KEY_START_MINUTE, 0)).thenReturn(0)
        `when`(mockSharedPreferences.getInt(ScheduleManager.KEY_END_HOUR, 18)).thenReturn(6)
        `when`(mockSharedPreferences.getInt(ScheduleManager.KEY_END_MINUTE, 0)).thenReturn(0)
        assertTrue(scheduleManager.isNightSchedule())
    }

    @Test
    fun `isNightSchedule returns false for normal schedule`() {
        `when`(mockSharedPreferences.getInt(ScheduleManager.KEY_START_HOUR, 8)).thenReturn(8)
        `when`(mockSharedPreferences.getInt(ScheduleManager.KEY_START_MINUTE, 0)).thenReturn(0)
        `when`(mockSharedPreferences.getInt(ScheduleManager.KEY_END_HOUR, 18)).thenReturn(18)
        `when`(mockSharedPreferences.getInt(ScheduleManager.KEY_END_MINUTE, 0)).thenReturn(0)
        assertFalse(scheduleManager.isNightSchedule())
    }

    @Test
    fun `getNextScheduledEvent returns null when schedule disabled`() {
        `when`(mockSharedPreferences.getBoolean(ScheduleManager.KEY_SCHEDULE_ENABLED, false)).thenReturn(false)
        assertNull(scheduleManager.getNextScheduledEvent())
    }

    @Test
    fun `saveDaySettings saves all day flags`() {
        scheduleManager.saveDaySettings(true, false, true, false, true, false, true)

        verify(mockEditor).putBoolean(ScheduleManager.KEY_MONDAY, true)
        verify(mockEditor).putBoolean(ScheduleManager.KEY_TUESDAY, false)
        verify(mockEditor).putBoolean(ScheduleManager.KEY_WEDNESDAY, true)
        verify(mockEditor).putBoolean(ScheduleManager.KEY_THURSDAY, false)
        verify(mockEditor).putBoolean(ScheduleManager.KEY_FRIDAY, true)
        verify(mockEditor).putBoolean(ScheduleManager.KEY_SATURDAY, false)
        verify(mockEditor).putBoolean(ScheduleManager.KEY_SUNDAY, true)
        verify(mockEditor).commit()
    }
}
