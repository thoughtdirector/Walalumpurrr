package com.example.notificacionesapp.domain.model

import org.junit.Test
import org.junit.Assert.*
import java.util.Calendar

class ScheduleTest {

    @Test
    fun `default schedule has weekdays enabled`() {
        val schedule = Schedule()
        assertTrue(schedule.isDayEnabled(Calendar.MONDAY))
        assertTrue(schedule.isDayEnabled(Calendar.TUESDAY))
        assertTrue(schedule.isDayEnabled(Calendar.WEDNESDAY))
        assertTrue(schedule.isDayEnabled(Calendar.THURSDAY))
        assertTrue(schedule.isDayEnabled(Calendar.FRIDAY))
        assertFalse(schedule.isDayEnabled(Calendar.SATURDAY))
        assertFalse(schedule.isDayEnabled(Calendar.SUNDAY))
    }

    @Test
    fun `isDayEnabled returns false for day not in set`() {
        val schedule = Schedule(enabledDays = setOf(Calendar.MONDAY))
        assertTrue(schedule.isDayEnabled(Calendar.MONDAY))
        assertFalse(schedule.isDayEnabled(Calendar.TUESDAY))
    }

    @Test
    fun `isDayEnabled returns false for invalid day`() {
        val schedule = Schedule()
        assertFalse(schedule.isDayEnabled(99))
    }

    @Test
    fun `isCurrentlyActive returns false when disabled`() {
        val schedule = Schedule(isEnabled = false)
        assertFalse(schedule.isCurrentlyActive())
    }

    @Test
    fun `isCurrentlyActive returns false when day is not enabled`() {
        val schedule = Schedule(isEnabled = true, enabledDays = emptySet())
        assertFalse(schedule.isCurrentlyActive())
    }

    @Test
    fun `getNextScheduledEvent returns null when disabled`() {
        val schedule = Schedule(isEnabled = false)
        assertNull(schedule.getNextScheduledEvent())
    }

    @Test
    fun `getNextScheduledEvent returns null when no days enabled`() {
        val schedule = Schedule(isEnabled = true, enabledDays = emptySet())
        assertNull(schedule.getNextScheduledEvent())
    }

    @Test
    fun `getNextScheduledEvent returns event when days are enabled`() {
        val allDays = setOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
        )
        val schedule = Schedule(isEnabled = true, enabledDays = allDays)
        val event = schedule.getNextScheduledEvent()
        assertNotNull(event)
        assertTrue(event!!.type == ScheduledEventType.START || event.type == ScheduledEventType.STOP)
    }

    @Test
    fun `default schedule times are 9 to 17`() {
        val schedule = Schedule()
        assertEquals(9, schedule.startHour)
        assertEquals(0, schedule.startMinute)
        assertEquals(17, schedule.endHour)
        assertEquals(0, schedule.endMinute)
    }

    @Test
    fun `ScheduledEventType has START and STOP`() {
        assertEquals(2, ScheduledEventType.values().size)
        assertNotNull(ScheduledEventType.START)
        assertNotNull(ScheduledEventType.STOP)
    }
}
