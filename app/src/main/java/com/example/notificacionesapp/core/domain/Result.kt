package com.example.notificacionesapp.core.domain

/**
 * A generic class that holds a value or an error.
 * Used for handling success and error states in a type-safe way.
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

/**
 * Extension function to check if Result is successful
 */
fun <T> Result<T>.isSuccess(): Boolean = this is Result.Success

/**
 * Extension function to check if Result is error
 */
fun <T> Result<T>.isError(): Boolean = this is Result.Error

/**
 * Extension function to check if Result is loading
 */
fun <T> Result<T>.isLoading(): Boolean = this is Result.Loading

/**
 * Extension function to get data from Result
 */
fun <T> Result<T>.getDataOrNull(): T? = when (this) {
    is Result.Success -> data
    else -> null
}

/**
 * Extension function to get error from Result
 */
fun <T> Result<T>.getErrorOrNull(): Throwable? = when (this) {
    is Result.Error -> exception
    else -> null
}
