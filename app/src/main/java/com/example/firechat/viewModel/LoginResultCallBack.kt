package com.example.firechat.viewModel

interface LoginResultCallBack {
    fun onLoginSuccess(result: String, uid: String)
    fun onLoginFailed(error: String)
}