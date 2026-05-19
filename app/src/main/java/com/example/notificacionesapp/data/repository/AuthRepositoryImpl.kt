package com.example.notificacionesapp.data.repository

import com.example.notificacionesapp.core.domain.AuthUserInfo
import com.example.notificacionesapp.core.domain.Result
import com.example.notificacionesapp.domain.model.User
import com.example.notificacionesapp.domain.model.UserRole
import com.example.notificacionesapp.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.providers.builtin.IDToken
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    @SerialName("firstname") val firstName: String = "",
    @SerialName("lastname") val lastName: String = "",
    val phone: String = "",
    @SerialName("birthdate") val birthDate: String = "",
    val role: String = "employee",
    @SerialName("adminid") val adminId: String? = null,
    @SerialName("isdisabled") val isDisabled: Boolean = false,
    @SerialName("disabledreason") val disabledReason: String? = null,
    @SerialName("replacedby") val replacedBy: String? = null,
    @SerialName("isresetaccount") val isResetAccount: Boolean = false,
    @SerialName("originalemail") val originalEmail: String? = null
)

fun UserDto.toDomainUser(): User = User(
    id = id,
    email = email,
    firstName = firstName,
    lastName = lastName,
    phone = phone,
    birthDate = birthDate,
    role = try { UserRole.valueOf(role.uppercase()) } catch (_: Exception) { UserRole.EMPLOYEE },
    adminId = adminId,
    isDisabled = isDisabled,
    disabledReason = disabledReason,
    replacedBy = replacedBy,
    isResetAccount = isResetAccount,
    originalEmail = originalEmail
)

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val sessionManager: com.example.notificacionesapp.SessionManager
) : AuthRepository {

    override suspend fun getCurrentUser(): AuthUserInfo? {
        return try {
            val user = supabaseClient.auth.currentUserOrNull()
            user?.let { AuthUserInfo(id = it.id, email = it.email) }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun signInWithEmailAndPassword(email: String, password: String): Result<AuthUserInfo> {
        return try {
            supabaseClient.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val user = supabaseClient.auth.currentUserOrNull()
            if (user != null) {
                Result.Success(AuthUserInfo(id = user.id, email = user.email))
            } else {
                Result.Error(Exception("Sign in failed: user is null"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun signUpWithEmailAndPassword(email: String, password: String): Result<AuthUserInfo> {
        return try {
            supabaseClient.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            val user = supabaseClient.auth.currentUserOrNull()
            if (user != null) {
                Result.Success(AuthUserInfo(id = user.id, email = user.email))
            } else {
                Result.Success(AuthUserInfo(id = "", email = email)) // Sign-up may need email confirmation
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<AuthUserInfo> {
        return try {
            supabaseClient.auth.signInWith(IDToken) {
                this.idToken = idToken
                this.provider = Google
            }
            val user = supabaseClient.auth.currentUserOrNull()
            if (user != null) {
                Result.Success(AuthUserInfo(id = user.id, email = user.email))
            } else {
                Result.Error(Exception("Google sign in failed: user is null"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            supabaseClient.auth.signOut()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun createUserAccount(
        user: AuthUserInfo,
        firstName: String,
        lastName: String,
        phone: String,
        birthDate: String,
        role: String,
        adminId: String?
    ): Result<User> {
        return try {
            val userDto = UserDto(
                id = user.id,
                email = user.email ?: "",
                firstName = firstName,
                lastName = lastName,
                phone = phone,
                birthDate = birthDate,
                role = role,
                adminId = adminId
            )
            supabaseClient.from("users").upsert(userDto)
            Result.Success(userDto.toDomainUser())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getUserById(userId: String): Result<User> {
        return try {
            val users: List<UserDto> = supabaseClient.from("users")
                .select { filter { eq("id", userId) } }
                .decodeList()
            if (users.isNotEmpty()) {
                Result.Success(users.first().toDomainUser())
            } else {
                Result.Error(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateUser(user: User): Result<User> {
        return try {
            val userDto = UserDto(
                id = user.id,
                email = user.email,
                firstName = user.firstName,
                lastName = user.lastName,
                phone = user.phone,
                birthDate = user.birthDate,
                role = user.role.name.lowercase(),
                adminId = user.adminId,
                isDisabled = user.isDisabled,
                disabledReason = user.disabledReason,
                replacedBy = user.replacedBy,
                isResetAccount = user.isResetAccount,
                originalEmail = user.originalEmail
            )
            supabaseClient.from("users").update(userDto) { filter { eq("id", user.id) } }
            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            supabaseClient.from("users").delete { filter { eq("id", userId) } }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            supabaseClient.auth.resetPasswordForEmail(email)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun userExists(email: String): Result<Boolean> {
        return try {
            val users: List<UserDto> = supabaseClient.from("users")
                .select { filter { eq("email", email) } }
                .decodeList()
            Result.Success(users.isNotEmpty())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getUsersByAdminId(adminId: String): Result<List<User>> {
        return try {
            val users: List<UserDto> = supabaseClient.from("users")
                .select { filter { eq("adminid", adminId) } }
                .decodeList()
            Result.Success(users.map { it.toDomainUser() })
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
