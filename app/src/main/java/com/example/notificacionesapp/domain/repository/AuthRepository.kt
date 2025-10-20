package com.example.notificacionesapp.domain.repository

import com.example.notificacionesapp.core.domain.Result
import com.example.notificacionesapp.domain.model.User
import com.google.firebase.auth.FirebaseUser

/**
 * Repository interface for authentication operations
 */
interface AuthRepository {
    
    /**
     * Get current authenticated user
     */
    suspend fun getCurrentUser(): FirebaseUser?
    
    /**
     * Sign in with email and password
     */
    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<FirebaseUser>
    
    /**
     * Sign up with email and password
     */
    suspend fun signUpWithEmailAndPassword(email: String, password: String): Result<FirebaseUser>
    
    /**
     * Sign in with Google
     */
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser>
    
    /**
     * Sign out current user
     */
    suspend fun signOut(): Result<Unit>
    
    /**
     * Create user account with additional data
     */
    suspend fun createUserAccount(
        user: FirebaseUser,
        firstName: String,
        lastName: String,
        phone: String,
        birthDate: String,
        role: String
    ): Result<User>
    
    /**
     * Get user data by ID
     */
    suspend fun getUserById(userId: String): Result<User>
    
    /**
     * Update user data
     */
    suspend fun updateUser(user: User): Result<User>
    
    /**
     * Delete user account
     */
    suspend fun deleteUser(userId: String): Result<Unit>
    
    /**
     * Reset password
     */
    suspend fun resetPassword(email: String): Result<Unit>
    
    /**
     * Check if user exists
     */
    suspend fun userExists(email: String): Result<Boolean>
}
