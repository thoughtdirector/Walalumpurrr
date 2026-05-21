package com.example.notificacionesapp.domain.model

import java.util.Calendar

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

fun Schedule.isDayEnabled(dayOfWeek: Int): Boolean {
    return enabledDays.contains(dayOfWeek)
}

fun Schedule.isCurrentlyActive(): Boolean {
    if (!isEnabled) return false

    val now = Calendar.getInstance()
    val currentDay = now.get(Calendar.DAY_OF_WEEK)
    val currentHour = now.get(Calendar.HOUR_OF_DAY)
    val currentMinute = now.get(Calendar.MINUTE)

    if (!isDayEnabled(currentDay)) return false

    val currentTimeInMinutes = currentHour * 60 + currentMinute
    val startTimeInMinutes = startHour * 60 + startMinute
    val endTimeInMinutes = endHour * 60 + endMinute

    return currentTimeInMinutes in startTimeInMinutes..endTimeInMinutes
}

fun Schedule.getNextScheduledEvent(): ScheduledEvent? {
    if (!isEnabled) return null

    val now = Calendar.getInstance()
    val currentDay = now.get(Calendar.DAY_OF_WEEK)
    val currentHour = now.get(Calendar.HOUR_OF_DAY)

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

data class ScheduledEvent(
    val time: java.util.Date,
    val type: ScheduledEventType
)

enum class ScheduledEventType {
    START,
    STOP
}
