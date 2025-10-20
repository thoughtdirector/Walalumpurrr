package com.example.notificacionesapp.data.repository

import android.content.Context
import com.example.notificacionesapp.core.domain.Result
import com.example.notificacionesapp.domain.model.Schedule
import com.example.notificacionesapp.domain.model.ScheduledEvent
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ScheduleRepository using SharedPreferences
 */
@Singleton
class ScheduleRepositoryImpl @Inject constructor(
    private val context: Context,
    private val gson: Gson
) : com.example.notificacionesapp.domain.repository.ScheduleRepository {

    companion object {
        private const val PREFS_NAME = "schedule_settings"
        private const val SCHEDULE_KEY = "schedule"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun getSchedule(): Result<Schedule> {
        return withContext(Dispatchers.IO) {
            try {
                val scheduleJson = prefs.getString(SCHEDULE_KEY, null)
                val schedule = if (scheduleJson != null) {
                    gson.fromJson(scheduleJson, Schedule::class.java)
                } else {
                    Schedule() // Default schedule
                }
                Result.Success(schedule)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    override suspend fun saveSchedule(schedule: Schedule): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val scheduleJson = gson.toJson(schedule)
                prefs.edit().putString(SCHEDULE_KEY, scheduleJson).apply()
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    override suspend fun setScheduleEnabled(enabled: Boolean): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val currentSchedule = getSchedule().getDataOrNull() ?: Schedule()
                val updatedSchedule = currentSchedule.copy(isEnabled = enabled)
                saveSchedule(updatedSchedule)
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    override suspend fun updateScheduleTime(
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val currentSchedule = getSchedule().getDataOrNull() ?: Schedule()
                val updatedSchedule = currentSchedule.copy(
                    startHour = startHour,
                    startMinute = startMinute,
                    endHour = endHour,
                    endMinute = endMinute
                )
                saveSchedule(updatedSchedule)
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    override suspend fun updateEnabledDays(enabledDays: Set<Int>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val currentSchedule = getSchedule().getDataOrNull() ?: Schedule()
                val updatedSchedule = currentSchedule.copy(enabledDays = enabledDays)
                saveSchedule(updatedSchedule)
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    override suspend fun isScheduleActive(): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val schedule = getSchedule().getDataOrNull() ?: Schedule()
                val isActive = schedule.isCurrentlyActive()
                Result.Success(isActive)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    override suspend fun getNextScheduledEvent(): Result<ScheduledEvent?> {
        return withContext(Dispatchers.IO) {
            try {
                val schedule = getSchedule().getDataOrNull() ?: Schedule()
                val nextEvent = schedule.getNextScheduledEvent()
                Result.Success(nextEvent)
            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }
}
