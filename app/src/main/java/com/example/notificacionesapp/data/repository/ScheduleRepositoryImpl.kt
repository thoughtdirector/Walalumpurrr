package com.example.notificacionesapp.data.repository

import android.content.Context
import com.example.notificacionesapp.core.domain.Result
import com.example.notificacionesapp.domain.model.Schedule
import com.example.notificacionesapp.domain.model.ScheduledEvent
import com.example.notificacionesapp.domain.model.getNextScheduledEvent
import com.example.notificacionesapp.domain.model.isCurrentlyActive
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepositoryImpl @Inject constructor(
    context: Context,
    private val gson: Gson
) : com.example.notificacionesapp.domain.repository.ScheduleRepository {

    companion object {
        private const val PREFS_NAME = "schedule_settings"
        private const val SCHEDULE_KEY = "schedule"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private suspend fun currentSchedule(): Schedule = withContext(Dispatchers.IO) {
        val json = prefs.getString(SCHEDULE_KEY, null)
        if (json != null) gson.fromJson(json, Schedule::class.java) else Schedule()
    }

    override suspend fun getSchedule(): Result<Schedule> = withContext(Dispatchers.IO) {
        try {
            Result.Success(currentSchedule())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun saveSchedule(schedule: Schedule): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            prefs.edit().putString(SCHEDULE_KEY, gson.toJson(schedule)).apply()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun setScheduleEnabled(enabled: Boolean): Result<Unit> = updateSchedule { it.copy(isEnabled = enabled) }

    override suspend fun updateScheduleTime(
        startHour: Int, startMinute: Int, endHour: Int, endMinute: Int
    ): Result<Unit> = updateSchedule {
        it.copy(startHour = startHour, startMinute = startMinute, endHour = endHour, endMinute = endMinute)
    }

    override suspend fun updateEnabledDays(enabledDays: Set<Int>): Result<Unit> = updateSchedule { it.copy(enabledDays = enabledDays) }

    override suspend fun isScheduleActive(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Result.Success(currentSchedule().isCurrentlyActive())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getNextScheduledEvent(): Result<ScheduledEvent?> = withContext(Dispatchers.IO) {
        try {
            Result.Success(currentSchedule().getNextScheduledEvent())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private suspend fun updateSchedule(transform: (Schedule) -> Schedule): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            saveSchedule(transform(currentSchedule()))
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
