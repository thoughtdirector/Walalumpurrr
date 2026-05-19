package com.example.notificacionesapp

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = sharedPreferences.edit()

    companion object {
        const val PREF_NAME = "NotificacionesAppSession"
        const val KEY_IS_LOGGED_IN = "isLoggedIn"
        const val KEY_USER_ID = "userId"
        const val KEY_USER_EMAIL = "userEmail"
        const val KEY_USER_ROLE = "userRole"
        const val KEY_ADMIN_ID = "adminId"
        private const val TAG = "SessionManager"
    }

    fun createLoginSession(userId: String, email: String, role: String, adminId: String? = null) {
        try {
            editor.apply {
                putBoolean(KEY_IS_LOGGED_IN, true)
                putString(KEY_USER_ID, userId)
                putString(KEY_USER_EMAIL, email)
                putString(KEY_USER_ROLE, role)
                putString(KEY_ADMIN_ID, adminId)
                commit()
            }
            Log.d(TAG, "Sesión creada para usuario: $email")
        } catch (e: Exception) {
            Log.e(TAG, "Error al crear sesión: ${e.message}")
        }
    }

    fun updateUserRole(role: String) {
        try {
            editor.putString(KEY_USER_ROLE, role)
            editor.commit()
            Log.d(TAG, "Rol de usuario actualizado a: $role")
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar rol de usuario: ${e.message}")
        }
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getUserDetails(): HashMap<String, String?> {
        val user = HashMap<String, String?>()
        user[KEY_USER_ID] = sharedPreferences.getString(KEY_USER_ID, null)
        user[KEY_USER_EMAIL] = sharedPreferences.getString(KEY_USER_EMAIL, null)
        user[KEY_USER_ROLE] = sharedPreferences.getString(KEY_USER_ROLE, null)
        user[KEY_ADMIN_ID] = sharedPreferences.getString(KEY_ADMIN_ID, null)
        return user
    }

    fun getUserId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }

    fun getUserRole(): String? {
        return sharedPreferences.getString(KEY_USER_ROLE, null)
    }

    fun getAdminId(): String? {
        return sharedPreferences.getString(KEY_ADMIN_ID, null)
    }

    fun logoutUser() {
        try {
            editor.apply {
                clear()
                commit()
            }
            Log.d(TAG, "Sesión cerrada correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al cerrar sesión: ${e.message}")
        }
    }
}
