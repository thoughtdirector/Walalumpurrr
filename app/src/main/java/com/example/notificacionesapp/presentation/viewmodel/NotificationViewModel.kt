package com.example.notificacionesapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notificacionesapp.core.domain.Result
import com.example.notificacionesapp.domain.model.Notification
import com.example.notificacionesapp.domain.model.NotificationType
import com.example.notificacionesapp.domain.repository.NotificationRepository
import com.example.notificacionesapp.domain.repository.NotificationStatistics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel for notification operations
 * Manages notification data and operations
 */
@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _notifications = MutableLiveData<List<Notification>>()
    val notifications: LiveData<List<Notification>> = _notifications

    private val _filteredNotifications = MutableLiveData<List<Notification>>()
    val filteredNotifications: LiveData<List<Notification>> = _filteredNotifications

    private val _statistics = MutableLiveData<NotificationStatistics?>()
    val statistics: LiveData<NotificationStatistics?> = _statistics

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _searchQuery = MutableLiveData<String>()
    val searchQuery: LiveData<String> = _searchQuery

    private val _selectedType = MutableLiveData<NotificationType?>()
    val selectedType: LiveData<NotificationType?> = _selectedType

    init {
        loadAllNotifications()
        loadStatistics()
    }

    /**
     * Load all notifications
     */
    fun loadAllNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = notificationRepository.getAllNotifications()) {
                is Result.Success -> {
                    _notifications.value = result.data
                    applyFilters()
                }
                is Result.Error -> {
                    _error.value = result.exception.message ?: "Error loading notifications"
                }
                is Result.Loading -> {
                    // Handle loading state if needed
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * Load notification statistics
     */
    fun loadStatistics() {
        viewModelScope.launch {
            when (val result = notificationRepository.getNotificationStatistics()) {
                is Result.Success -> {
                    _statistics.value = result.data
                }
                is Result.Error -> {
                    _error.value = result.exception.message ?: "Error loading statistics"
                }
                is Result.Loading -> {
                    // Handle loading state if needed
                }
            }
        }
    }

    /**
     * Filter notifications by type
     */
    fun filterByType(type: NotificationType?) {
        _selectedType.value = type
        applyFilters()
    }

    /**
     * Search notifications
     */
    fun searchNotifications(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    /**
     * Apply current filters to notifications
     */
    private fun applyFilters() {
        val allNotifications = _notifications.value ?: return
        var filtered = allNotifications

        // Filter by type
        val selectedType = _selectedType.value
        if (selectedType != null) {
            filtered = filtered.filter { it.type == selectedType }
        }

        // Filter by search query
        val searchQuery = _searchQuery.value
        if (!searchQuery.isNullOrBlank()) {
            filtered = filtered.filter { notification ->
                notification.title.contains(searchQuery, ignoreCase = true) ||
                notification.content.contains(searchQuery, ignoreCase = true) ||
                notification.appName.contains(searchQuery, ignoreCase = true) ||
                notification.sender.contains(searchQuery, ignoreCase = true)
            }
        }

        _filteredNotifications.value = filtered
    }

    /**
     * Get notifications by date range
     */
    fun getNotificationsByDateRange(startDate: Date, endDate: Date) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = notificationRepository.getNotificationsByDateRange(startDate, endDate)) {
                is Result.Success -> {
                    _filteredNotifications.value = result.data
                }
                is Result.Error -> {
                    _error.value = result.exception.message ?: "Error loading notifications by date range"
                }
                is Result.Loading -> {
                    // Handle loading state if needed
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * Get notifications by package name
     */
    fun getNotificationsByPackage(packageName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = notificationRepository.getNotificationsByPackage(packageName)) {
                is Result.Success -> {
                    _filteredNotifications.value = result.data
                }
                is Result.Error -> {
                    _error.value = result.exception.message ?: "Error loading notifications by package"
                }
                is Result.Loading -> {
                    // Handle loading state if needed
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * Delete notification
     */
    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = notificationRepository.deleteNotification(notificationId)) {
                is Result.Success -> {
                    loadAllNotifications() // Reload to get updated list
                    loadStatistics() // Reload statistics
                }
                is Result.Error -> {
                    _error.value = result.exception.message ?: "Error deleting notification"
                }
                is Result.Loading -> {
                    // Handle loading state if needed
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * Clear all notifications
     */
    fun clearAllNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = notificationRepository.clearAllNotifications()) {
                is Result.Success -> {
                    _notifications.value = emptyList()
                    _filteredNotifications.value = emptyList()
                    _statistics.value = null
                }
                is Result.Error -> {
                    _error.value = result.exception.message ?: "Error clearing notifications"
                }
                is Result.Loading -> {
                    // Handle loading state if needed
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * Export notifications to CSV
     */
    fun exportNotificationsToCsv(): LiveData<Result<String>> {
        val result = MutableLiveData<Result<String>>()
        
        viewModelScope.launch {
            result.value = Result.Loading
            
            when (val exportResult = notificationRepository.exportNotificationsToCsv()) {
                is Result.Success -> {
                    result.value = exportResult
                }
                is Result.Error -> {
                    result.value = exportResult
                }
                is Result.Loading -> {
                    result.value = exportResult
                }
            }
        }
        
        return result
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Clear search and filters
     */
    fun clearFilters() {
        _searchQuery.value = ""
        _selectedType.value = null
        _filteredNotifications.value = _notifications.value
    }
}
