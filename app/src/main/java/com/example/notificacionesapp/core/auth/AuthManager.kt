package com.example.notificacionesapp.core.auth

import android.content.Context
import com.example.notificacionesapp.core.domain.Result
import com.example.notificacionesapp.domain.model.User
import com.example.notificacionesapp.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class for authentication operations
 * Provides a clean interface for authentication-related functionality
 */
@Singleton
class AuthManager @Inject constructor(
    private val authRepository: AuthRepository,
    private val context: Context
) {
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    init {
        checkAuthState()
    }
    
    /**
     * Check current authentication state
     */
    suspend fun checkAuthState() {
        _authState.value = AuthState.Loading
        
        val firebaseUser = authRepository.getCurrentUser()
        if (firebaseUser != null) {
            when (val result = authRepository.getUserById(firebaseUser.uid)) {
                is Result.Success -> {
                    _currentUser.value = result.data
                    _authState.value = AuthState.Authenticated(result.data)
                }
                is Result.Error -> {
                    _authState.value = AuthState.Error(result.exception.message ?: "Error loading user data")
                }
                is Result.Loading -> {
                    _authState.value = AuthState.Loading
                }
            }
        } else {
            _currentUser.value = null
            _authState.value = AuthState.Unauthenticated
        }
    }
    
    /**
     * Sign in with email and password
     */
    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<Unit> {
        _authState.value = AuthState.Loading
        
        return when (val result = authRepository.signInWithEmailAndPassword(email, password)) {
            is Result.Success -> {
                val user = result.data
                when (val userResult = authRepository.getUserById(user.uid)) {
                    is Result.Success -> {
                        _currentUser.value = userResult.data
                        _authState.value = AuthState.Authenticated(userResult.data)
                        Result.Success(Unit)
                    }
                    is Result.Error -> {
                        _authState.value = AuthState.Error(userResult.exception.message ?: "Error loading user data")
                        Result.Error(userResult.exception)
                    }
                    is Result.Loading -> {
                        _authState.value = AuthState.Loading
                        Result.Loading
                    }
                }
            }
            is Result.Error -> {
                _authState.value = AuthState.Error(result.exception.message ?: "Sign in failed")
                result
            }
            is Result.Loading -> {
                _authState.value = AuthState.Loading
                result
            }
        }
    }
    
    /**
     * Sign up with email and password
     */
    suspend fun signUpWithEmailAndPassword(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phone: String,
        birthDate: String,
        role: String
    ): Result<Unit> {
        _authState.value = AuthState.Loading
        
        return when (val result = authRepository.signUpWithEmailAndPassword(email, password)) {
            is Result.Success -> {
                val user = result.data
                when (val createResult = authRepository.createUserAccount(
                    user, firstName, lastName, phone, birthDate, role
                )) {
                    is Result.Success -> {
                        _currentUser.value = createResult.data
                        _authState.value = AuthState.Authenticated(createResult.data)
                        Result.Success(Unit)
                    }
                    is Result.Error -> {
                        _authState.value = AuthState.Error(createResult.exception.message ?: "Error creating user account")
                        Result.Error(createResult.exception)
                    }
                    is Result.Loading -> {
                        _authState.value = AuthState.Loading
                        Result.Loading
                    }
                }
            }
            is Result.Error -> {
                _authState.value = AuthState.Error(result.exception.message ?: "Sign up failed")
                result
            }
            is Result.Loading -> {
                _authState.value = AuthState.Loading
                result
            }
        }
    }
    
    /**
     * Sign out current user
     */
    suspend fun signOut(): Result<Unit> {
        _authState.value = AuthState.Loading
        
        return when (val result = authRepository.signOut()) {
            is Result.Success -> {
                _currentUser.value = null
                _authState.value = AuthState.Unauthenticated
                Result.Success(Unit)
            }
            is Result.Error -> {
                _authState.value = AuthState.Error(result.exception.message ?: "Sign out failed")
                result
            }
            is Result.Loading -> {
                _authState.value = AuthState.Loading
                result
            }
        }
    }
    
    /**
     * Create employee account
     */
    suspend fun createEmployeeAccount(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phone: String,
        birthDate: String,
        adminId: String
    ): Result<Unit> {
        return signUpWithEmailAndPassword(
            email, password, firstName, lastName, phone, birthDate, "employee"
        )
    }
    
    /**
     * Reset password
     */
    suspend fun resetPassword(email: String): Result<Unit> {
        return authRepository.resetPassword(email)
    }
    
    /**
     * Check if current user is admin
     */
    fun isCurrentUserAdmin(): Boolean {
        return _currentUser.value?.isAdmin() ?: false
    }
    
    /**
     * Check if current user is employee
     */
    fun isCurrentUserEmployee(): Boolean {
        return _currentUser.value?.isEmployee() ?: false
    }
}

/**
 * Sealed class representing authentication states
 */
sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}
