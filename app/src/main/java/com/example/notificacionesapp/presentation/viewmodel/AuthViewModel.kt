package com.example.notificacionesapp.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notificacionesapp.core.domain.Result
import com.example.notificacionesapp.domain.model.User
import com.example.notificacionesapp.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for authentication operations
 * Manages user authentication state and operations
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _currentUser = MutableLiveData<FirebaseUser?>()
    val currentUser: LiveData<FirebaseUser?> = _currentUser

    private val _userData = MutableLiveData<User?>()
    val userData: LiveData<User?> = _userData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isLoggedIn = MutableLiveData<Boolean>()
    val isLoggedIn: LiveData<Boolean> = _isLoggedIn

    init {
        checkAuthState()
    }

    /**
     * Check current authentication state
     */
    fun checkAuthState() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val user = authRepository.getCurrentUser()
            _currentUser.value = user
            _isLoggedIn.value = user != null

            if (user != null) {
                loadUserData(user.uid)
            } else {
                _userData.value = null
            }

            _isLoading.value = false
        }
    }

    /**
     * Load user data from repository
     */
    private fun loadUserData(userId: String) {
        viewModelScope.launch {
            when (val result = authRepository.getUserById(userId)) {
                is Result.Success -> {
                    _userData.value = result.data
                }
                is Result.Error -> {
                    _error.value = result.exception.message ?: "Error loading user data"
                }
                is Result.Loading -> {
                    // Handle loading state if needed
                }
            }
        }
    }

    /**
     * Sign in with email and password
     */
    fun signInWithEmailAndPassword(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = authRepository.signInWithEmailAndPassword(email, password)) {
                is Result.Success -> {
                    _currentUser.value = result.data
                    _isLoggedIn.value = true
                    loadUserData(result.data.uid)
                }
                is Result.Error -> {
                    _error.value = result.exception.message ?: "Sign in failed"
                }
                is Result.Loading -> {
                    // Handle loading state if needed
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * Sign up with email and password
     */
    fun signUpWithEmailAndPassword(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phone: String,
        birthDate: String,
        role: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = authRepository.signUpWithEmailAndPassword(email, password)) {
                is Result.Success -> {
                    val user = result.data
                    _currentUser.value = user
                    _isLoggedIn.value = true

                    // Create user account with additional data
                    createUserAccount(user, firstName, lastName, phone, birthDate, role)
                }
                is Result.Error -> {
                    _error.value = result.exception.message ?: "Sign up failed"
                    _isLoading.value = false
                }
                is Result.Loading -> {
                    // Handle loading state if needed
                }
            }
        }
    }

    /**
     * Create user account with additional data
     */
    private fun createUserAccount(
        user: FirebaseUser,
        firstName: String,
        lastName: String,
        phone: String,
        birthDate: String,
        role: String
    ) {
        viewModelScope.launch {
            when (val result = authRepository.createUserAccount(
                user, firstName, lastName, phone, birthDate, role
            )) {
                is Result.Success -> {
                    _userData.value = result.data
                }
                is Result.Error -> {
                    _error.value = result.exception.message ?: "Error creating user account"
                }
                is Result.Loading -> {
                    // Handle loading state if needed
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * Sign in with Google
     */
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = authRepository.signInWithGoogle(idToken)) {
                is Result.Success -> {
                    _currentUser.value = result.data
                    _isLoggedIn.value = true
                    loadUserData(result.data.uid)
                }
                is Result.Error -> {
                    _error.value = result.exception.message ?: "Google sign in failed"
                }
                is Result.Loading -> {
                    // Handle loading state if needed
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * Sign out current user
     */
    fun signOut() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = authRepository.signOut()) {
                is Result.Success -> {
                    _currentUser.value = null
                    _userData.value = null
                    _isLoggedIn.value = false
                }
                is Result.Error -> {
                    _error.value = result.exception.message ?: "Sign out failed"
                }
                is Result.Loading -> {
                    // Handle loading state if needed
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * Reset password
     */
    fun resetPassword(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = authRepository.resetPassword(email)) {
                is Result.Success -> {
                    // Password reset email sent successfully
                }
                is Result.Error -> {
                    _error.value = result.exception.message ?: "Password reset failed"
                }
                is Result.Loading -> {
                    // Handle loading state if needed
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * Create employee account
     */
    fun createEmployeeAccount(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phone: String,
        birthDate: String,
        adminId: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = authRepository.signUpWithEmailAndPassword(email, password)) {
                is Result.Success -> {
                    val user = result.data
                    _currentUser.value = user

                    // Create employee account
                    createUserAccount(user, firstName, lastName, phone, birthDate, "employee")
                }
                is Result.Error -> {
                    _error.value = result.exception.message ?: "Employee account creation failed"
                    _isLoading.value = false
                }
                is Result.Loading -> {
                    // Handle loading state if needed
                }
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
}
