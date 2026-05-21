package com.example.notificacionesapp.domain.repository

import com.example.notificacionesapp.core.domain.Result
import com.example.notificacionesapp.domain.model.Schedule

interface ScheduleRepository {
    suspend fun getSchedule(): Result<Schedule>
    suspend fun saveSchedule(schedule: Schedule): Result<Unit>
    suspend fun setScheduleEnabled(enabled: Boolean): Result<Unit>
    suspend fun updateScheduleTime(
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int
    ): Result<Unit>
    suspend fun updateEnabledDays(enabledDays: Set<Int>): Result<Unit>
    suspend fun isScheduleActive(): Result<Boolean>
    suspend fun getNextScheduledEvent(): Result<com.example.notificacionesapp.domain.model.ScheduledEvent?>
}
