package com.example.notificacionesapp.model

data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = "employee", // "admin" o "employee"
    val adminId: String = "",      // Solo para empleados, ID del admin asociado
    val employees: List<String> = emptyList(), // Solo para admins, lista de IDs de empleados
    val fcmToken: String = ""     // Token para notificaciones push
)