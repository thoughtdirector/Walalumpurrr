package com.example.notificacionesapp.domain.model

data class User(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val birthDate: String,
    val role: UserRole,
    val adminId: String? = null,
    val isDisabled: Boolean = false,
    val disabledReason: String? = null,
    val replacedBy: String? = null,
    val isResetAccount: Boolean = false,
    val originalEmail: String? = null
)

enum class UserRole {
    ADMIN,
    EMPLOYEE
}

fun User.isAdmin(): Boolean = role == UserRole.ADMIN

fun User.isEmployee(): Boolean = role == UserRole.EMPLOYEE
