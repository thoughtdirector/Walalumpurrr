package com.example.notificacionesapp.domain.repository

import com.example.notificacionesapp.core.domain.Result
import com.example.notificacionesapp.domain.model.Schedule

/**
 * Repository interface for schedule operations
 */
interface ScheduleRepository {
    
    /**
     * Get current schedule configuration
     */
    suspend fun getSchedule(): Result<Schedule>
    
    /**
     * Save schedule configuration
     */
    suspend fun saveSchedule(schedule: Schedule): Result<Unit>
    
    /**
     * Enable or disable schedule
     */
    suspend fun setScheduleEnabled(enabled: Boolean): Result<Unit>
    
    /**
     * Update schedule time range
     */
    suspend fun updateScheduleTime(
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int
    ): Result<Unit>
    
    /**
     * Update enabled days
     */
    suspend fun updateEnabledDays(enabledDays: Set<Int>): Result<Unit>
    
    /**
     * Check if schedule is currently active
     */
    suspend fun isScheduleActive(): Result<Boolean>
    
    /**
     * Get next scheduled event
     */
    suspend fun getNextScheduledEvent(): Result<com.example.notificacionesapp.domain.model.ScheduledEvent?>
}
