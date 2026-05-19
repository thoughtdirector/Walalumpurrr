package com.example.notificacionesapp.core.auth

import com.example.notificacionesapp.core.domain.AuthUserInfo
import com.example.notificacionesapp.core.domain.Result
import com.example.notificacionesapp.domain.model.User
import com.example.notificacionesapp.domain.model.isAdmin
import com.example.notificacionesapp.domain.model.isEmployee
import com.example.notificacionesapp.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    private val authRepository: AuthRepository
) {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    suspend fun checkAuthState() {
        _authState.value = AuthState.Loading

        val authUser = authRepository.getCurrentUser()
        if (authUser != null) {
            when (val result = authRepository.getUserById(authUser.id)) {
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

    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<Unit> {
        _authState.value = AuthState.Loading

        return when (val result = authRepository.signInWithEmailAndPassword(email, password)) {
            is Result.Success -> {
                val user = result.data
                when (val userResult = authRepository.getUserById(user.id)) {
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

    suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        _authState.value = AuthState.Loading

        return when (val result = authRepository.signInWithGoogle(idToken)) {
            is Result.Success -> {
                val user = result.data
                when (val userResult = authRepository.getUserById(user.id)) {
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
                _authState.value = AuthState.Error(result.exception.message ?: "Google sign in failed")
                result
            }
            is Result.Loading -> {
                _authState.value = AuthState.Loading
                result
            }
        }
    }

    suspend fun signUpWithEmailAndPassword(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phone: String,
        birthDate: String,
        role: String,
        adminId: String? = null
    ): Result<Unit> {
        _authState.value = AuthState.Loading

        return when (val result = authRepository.signUpWithEmailAndPassword(email, password)) {
            is Result.Success -> {
                val user = result.data
                when (val createResult = authRepository.createUserAccount(
                    user, firstName, lastName, phone, birthDate, role, adminId
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

    suspend fun createEmployeeAccount(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phone: String,
        birthDate: String,
        adminId: String?
    ): Result<Unit> {
        return signUpWithEmailAndPassword(
            email, password, firstName, lastName, phone, birthDate, "employee", adminId
        )
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return authRepository.resetPassword(email)
    }

    fun isCurrentUserAdmin(): Boolean {
        return _currentUser.value?.isAdmin() ?: false
    }

    fun isCurrentUserEmployee(): Boolean {
        return _currentUser.value?.isEmployee() ?: false
    }
}

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}
