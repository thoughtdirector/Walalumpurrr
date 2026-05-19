package com.example.notificacionesapp.domain.repository

import com.example.notificacionesapp.core.domain.AuthUserInfo
import com.example.notificacionesapp.core.domain.Result
import com.example.notificacionesapp.domain.model.User

interface AuthRepository {

    suspend fun getCurrentUser(): AuthUserInfo?

    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<AuthUserInfo>

    suspend fun signUpWithEmailAndPassword(email: String, password: String): Result<AuthUserInfo>

    suspend fun signInWithGoogle(idToken: String): Result<AuthUserInfo>

    suspend fun signOut(): Result<Unit>

    suspend fun createUserAccount(
        user: AuthUserInfo,
        firstName: String,
        lastName: String,
        phone: String,
        birthDate: String,
        role: String,
        adminId: String? = null
    ): Result<User>

    suspend fun getUserById(userId: String): Result<User>

    suspend fun updateUser(user: User): Result<User>

    suspend fun deleteUser(userId: String): Result<Unit>

    suspend fun resetPassword(email: String): Result<Unit>

    suspend fun userExists(email: String): Result<Boolean>

    suspend fun getUsersByAdminId(adminId: String): Result<List<User>>
}
