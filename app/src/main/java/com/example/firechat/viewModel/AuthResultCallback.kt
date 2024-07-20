package com.example.firechat.viewModel

interface AuthResultCallback {
    fun onSuccess(result: AuthSuccessResult)
    fun onFailure(error: AuthError)
}

data class AuthSuccessResult(
    val message: String,
    val uid: String? = null
)

data class AuthError(
    val code: String,
    val message: String
)