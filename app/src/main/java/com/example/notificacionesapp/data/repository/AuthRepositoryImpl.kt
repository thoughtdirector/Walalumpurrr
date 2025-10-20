package com.example.notificacionesapp.data.repository

import com.example.notificacionesapp.core.domain.Result
import com.example.notificacionesapp.domain.model.User
import com.example.notificacionesapp.domain.model.UserRole
import com.example.notificacionesapp.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AuthRepository using Firebase
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseDatabase: FirebaseDatabase
) : AuthRepository {

    override suspend fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    override suspend fun signInWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                Result.Success(user)
            } else {
                Result.Error(Exception("Sign in failed: User is null"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun signUpWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                Result.Success(user)
            } else {
                Result.Error(Exception("Sign up failed: User is null"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user
            if (user != null) {
                Result.Success(user)
            } else {
                Result.Error(Exception("Google sign in failed: User is null"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun createUserAccount(
        user: FirebaseUser,
        firstName: String,
        lastName: String,
        phone: String,
        birthDate: String,
        role: String
    ): Result<User> {
        return try {
            val userData = hashMapOf(
                "firstName" to firstName,
                "lastName" to lastName,
                "phone" to phone,
                "birthDate" to birthDate,
                "role" to role,
                "email" to user.email
            )

            firebaseDatabase.reference
                .child("users")
                .child(user.uid)
                .setValue(userData)
                .await()

            val domainUser = User(
                id = user.uid,
                email = user.email ?: "",
                firstName = firstName,
                lastName = lastName,
                phone = phone,
                birthDate = birthDate,
                role = UserRole.valueOf(role.uppercase())
            )

            Result.Success(domainUser)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getUserById(userId: String): Result<User> {
        return try {
            val snapshot = firebaseDatabase.reference
                .child("users")
                .child(userId)
                .get()
                .await()

            if (snapshot.exists()) {
                val userData = snapshot.value as? Map<String, Any>
                if (userData != null) {
                    val user = User(
                        id = userId,
                        email = userData["email"] as? String ?: "",
                        firstName = userData["firstName"] as? String ?: "",
                        lastName = userData["lastName"] as? String ?: "",
                        phone = userData["phone"] as? String ?: "",
                        birthDate = userData["birthDate"] as? String ?: "",
                        role = UserRole.valueOf((userData["role"] as? String ?: "EMPLOYEE").uppercase()),
                        adminId = userData["adminId"] as? String,
                        isDisabled = userData["isDisabled"] as? Boolean ?: false,
                        disabledReason = userData["disabledReason"] as? String,
                        replacedBy = userData["replacedBy"] as? String,
                        isResetAccount = userData["isResetAccount"] as? Boolean ?: false,
                        originalEmail = userData["originalEmail"] as? String
                    )
                    Result.Success(user)
                } else {
                    Result.Error(Exception("User data is null"))
                }
            } else {
                Result.Error(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun updateUser(user: User): Result<User> {
        return try {
            val userData = hashMapOf(
                "firstName" to user.firstName,
                "lastName" to user.lastName,
                "phone" to user.phone,
                "birthDate" to user.birthDate,
                "role" to user.role.name.lowercase(),
                "email" to user.email,
                "isDisabled" to user.isDisabled,
                "disabledReason" to user.disabledReason,
                "replacedBy" to user.replacedBy,
                "isResetAccount" to user.isResetAccount,
                "originalEmail" to user.originalEmail
            )

            firebaseDatabase.reference
                .child("users")
                .child(user.id)
                .updateChildren(userData)
                .await()

            Result.Success(user)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            firebaseDatabase.reference
                .child("users")
                .child(userId)
                .removeValue()
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun userExists(email: String): Result<Boolean> {
        return try {
            val snapshot = firebaseDatabase.reference
                .child("users")
                .orderByChild("email")
                .equalTo(email)
                .get()
                .await()

            Result.Success(snapshot.exists())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
