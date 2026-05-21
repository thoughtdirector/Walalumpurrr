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

    fun loadAllNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = notificationRepository.getAllNotifications()) {
                is Result.Success -> {
                    _notifications.value = result.data
                    applyFilters()
                }
                is Result.Error -> _error.value = result.exception.message ?: "Error loading notifications"
                is Result.Loading -> {}
            }
            _isLoading.value = false
        }
    }

    fun loadStatistics() {
        viewModelScope.launch {
            when (val result = notificationRepository.getNotificationStatistics()) {
                is Result.Success -> _statistics.value = result.data
                is Result.Error -> _error.value = result.exception.message ?: "Error loading statistics"
                is Result.Loading -> {}
            }
        }
    }

    fun filterByType(type: NotificationType?) {
        _selectedType.value = type
        applyFilters()
    }

    fun searchNotifications(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    private fun applyFilters() {
        val allNotifications = _notifications.value ?: return
        var filtered = allNotifications

        _selectedType.value?.let { type ->
            filtered = filtered.filter { it.type == type }
        }

        val query = _searchQuery.value
        if (!query.isNullOrBlank()) {
            filtered = filtered.filter { notification ->
                notification.title.contains(query, ignoreCase = true) ||
                notification.content.contains(query, ignoreCase = true) ||
                notification.appName.contains(query, ignoreCase = true) ||
                notification.sender.contains(query, ignoreCase = true)
            }
        }

        _filteredNotifications.value = filtered
    }

    fun getNotificationsByDateRange(startDate: Date, endDate: Date) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = notificationRepository.getNotificationsByDateRange(startDate, endDate)) {
                is Result.Success -> _filteredNotifications.value = result.data
                is Result.Error -> _error.value = result.exception.message ?: "Error loading notifications by date range"
                is Result.Loading -> {}
            }
            _isLoading.value = false
        }
    }

    fun getNotificationsByPackage(packageName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = notificationRepository.getNotificationsByPackage(packageName)) {
                is Result.Success -> _filteredNotifications.value = result.data
                is Result.Error -> _error.value = result.exception.message ?: "Error loading notifications by package"
                is Result.Loading -> {}
            }
            _isLoading.value = false
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = notificationRepository.deleteNotification(notificationId)) {
                is Result.Success -> {
                    loadAllNotifications()
                    loadStatistics()
                }
                is Result.Error -> _error.value = result.exception.message ?: "Error deleting notification"
                is Result.Loading -> {}
            }
            _isLoading.value = false
        }
    }

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
                is Result.Error -> _error.value = result.exception.message ?: "Error clearing notifications"
                is Result.Loading -> {}
            }
            _isLoading.value = false
        }
    }

    fun exportNotificationsToCsv(): LiveData<Result<String>> {
        val result = MutableLiveData<Result<String>>()

        viewModelScope.launch {
            result.value = Result.Loading
            result.value = notificationRepository.exportNotificationsToCsv()
        }

        return result
    }

    fun clearError() {
        _error.value = null
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedType.value = null
        _filteredNotifications.value = _notifications.value
    }
}
