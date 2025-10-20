package com.example.notificacionesapp.domain.model

import java.util.Calendar

/**
 * Domain model representing a notification schedule
 */
data class Schedule(
    val isEnabled: Boolean = false,
    val startHour: Int = 9,
    val startMinute: Int = 0,
    val endHour: Int = 17,
    val endMinute: Int = 0,
    val enabledDays: Set<Int> = setOf(
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY
    )
)

/**
 * Extension function to check if a specific day is enabled
 */
fun Schedule.isDayEnabled(dayOfWeek: Int): Boolean {
    return enabledDays.contains(dayOfWeek)
}

/**
 * Extension function to get formatted time range
 */
fun Schedule.getFormattedTimeRange(): String {
    val startTime = String.format("%02d:%02d", startHour, startMinute)
    val endTime = String.format("%02d:%02d", endHour, endMinute)
    return "$startTime - $endTime"
}

/**
 * Extension function to check if current time is within schedule
 */
fun Schedule.isCurrentlyActive(): Boolean {
    if (!isEnabled) return false
    
    val now = Calendar.getInstance()
    val currentDay = now.get(Calendar.DAY_OF_WEEK)
    val currentHour = now.get(Calendar.HOUR_OF_DAY)
    val currentMinute = now.get(Calendar.MINUTE)
    
    // Check if current day is enabled
    if (!isDayEnabled(currentDay)) return false
    
    // Check if current time is within range
    val currentTimeInMinutes = currentHour * 60 + currentMinute
    val startTimeInMinutes = startHour * 60 + startMinute
    val endTimeInMinutes = endHour * 60 + endMinute
    
    return currentTimeInMinutes in startTimeInMinutes..endTimeInMinutes
}

/**
 * Extension function to get next scheduled event
 */
fun Schedule.getNextScheduledEvent(): ScheduledEvent? {
    if (!isEnabled) return null
    
    val now = Calendar.getInstance()
    val currentDay = now.get(Calendar.DAY_OF_WEEK)
    val currentHour = now.get(Calendar.HOUR_OF_DAY)
    val currentMinute = now.get(Calendar.MINUTE)
    
    // Find next enabled day
    for (i in 0..6) {
        val dayToCheck = (currentDay + i - 1) % 7 + 1
        if (isDayEnabled(dayToCheck)) {
            val eventTime = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, i)
                set(Calendar.HOUR_OF_DAY, if (i == 0 && currentHour >= startHour) endHour else startHour)
                set(Calendar.MINUTE, if (i == 0 && currentHour >= startHour) endMinute else startMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            return ScheduledEvent(
                time = eventTime.time,
                type = if (i == 0 && currentHour >= startHour) ScheduledEventType.STOP else ScheduledEventType.START
            )
        }
    }
    
    return null
}

/**
 * Represents a scheduled event (start or stop)
 */
data class ScheduledEvent(
    val time: java.util.Date,
    val type: ScheduledEventType
)

/**
 * Types of scheduled events
 */
enum class ScheduledEventType {
    START,
    STOP
}
