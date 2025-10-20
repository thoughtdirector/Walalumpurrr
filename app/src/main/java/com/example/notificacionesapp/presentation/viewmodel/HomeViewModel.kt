package com.example.notificacionesapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notificacionesapp.core.domain.Result
import com.example.notificacionesapp.domain.model.Schedule
import com.example.notificacionesapp.domain.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for HomeFragment
 * Manages home screen state and business logic
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    private val _schedule = MutableLiveData<Schedule>()
    val schedule: LiveData<Schedule> = _schedule

    private val _isScheduleActive = MutableLiveData<Boolean>()
    val isScheduleActive: LiveData<Boolean> = _isScheduleActive

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadSchedule()
    }

    /**
     * Load current schedule configuration
     */
    fun loadSchedule() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = scheduleRepository.getSchedule()) {
                is Result.Success -> {
                    _schedule.value = result.data
                    checkScheduleStatus()
                }
                is Result.Error -> {
                    _error.value = result.exception.message ?: "Error loading schedule"
                }
                is Result.Loading -> {
                    // Handle loading state if needed
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * Check if schedule is currently active
     */
    fun checkScheduleStatus() {
        viewModelScope.launch {
            when (val result = scheduleRepository.isScheduleActive()) {
                is Result.Success -> {
                    _isScheduleActive.value = result.data
                }
                is Result.Error -> {
                    _error.value = result.exception.message ?: "Error checking schedule status"
                }
                is Result.Loading -> {
                    // Handle loading state if needed
                }
            }
        }
    }

    /**
     * Enable or disable schedule
     */
    fun setScheduleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = scheduleRepository.setScheduleEnabled(enabled)) {
                is Result.Success -> {
                    loadSchedule() // Reload to get updated state
                }
                is Result.Error -> {
                    _error.value = result.exception.message ?: "Error updating schedule"
                }
                is Result.Loading -> {
                    // Handle loading state if needed
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * Update schedule time range
     */
    fun updateScheduleTime(
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = scheduleRepository.updateScheduleTime(
                startHour, startMinute, endHour, endMinute
            )) {
                is Result.Success -> {
                    loadSchedule() // Reload to get updated state
                }
                is Result.Error -> {
                    _error.value = result.exception.message ?: "Error updating schedule time"
                }
                is Result.Loading -> {
                    // Handle loading state if needed
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * Update enabled days
     */
    fun updateEnabledDays(enabledDays: Set<Int>) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = scheduleRepository.updateEnabledDays(enabledDays)) {
                is Result.Success -> {
                    loadSchedule() // Reload to get updated state
                }
                is Result.Error -> {
                    _error.value = result.exception.message ?: "Error updating enabled days"
                }
                is Result.Loading -> {
                    // Handle loading state if needed
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
}
