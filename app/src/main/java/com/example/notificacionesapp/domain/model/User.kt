package com.example.notificacionesapp.domain.model

/**
 * Domain model representing a User
 */
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

/**
 * User roles in the system
 */
enum class UserRole {
    ADMIN,
    EMPLOYEE
}

/**
 * Extension function to get full name
 */
fun User.getFullName(): String = "$firstName $lastName"

/**
 * Extension function to check if user is admin
 */
fun User.isAdmin(): Boolean = role == UserRole.ADMIN

/**
 * Extension function to check if user is employee
 */
fun User.isEmployee(): Boolean = role == UserRole.EMPLOYEE
