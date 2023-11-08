package com.example.firechat.viewModel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.firechat.repository.AuthRepository

class AuthViewModel : ViewModel(), LoginResultCallBack, RegisterResultCallback {
    private val repository = AuthRepository()
    private lateinit var _currentUserUID : String
    val currentUser get() = _currentUserUID

    private var _event = MutableLiveData<Event<String>>()
    val event get() = _event


    fun tryLogin(id : String, pw : String){
        repository.tryLogin(id, pw, this@AuthViewModel)
    }

    fun attemptRegister(name : String, email : String, pw : String) {
        repository.attemptRegister(name, email, pw, this@AuthViewModel)
    }


    // 로그인 성공시 uid 값을 갱신
    override fun onLoginSuccess(result: String, uid: String) {
        _currentUserUID = uid
        _event.value = Event(result)
    }

    // 로그인 실패시 실패 메시지 반환
    override fun onLoginFailed(error: String) {
        _event.value = Event(error)
    }

    // 회원가입 결과 반환
    override fun returnRegisterResult(result: String) {
        Log.d("view model register", result)
        _event.value = Event(result)
    }
}