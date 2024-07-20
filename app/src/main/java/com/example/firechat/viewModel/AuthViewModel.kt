package com.example.firechat.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.firechat.model.repository.AuthRepository

class AuthViewModel : ViewModel(), AuthResultCallback {
    private val repository = AuthRepository()
    private lateinit var _currentUserUID: String
    val currentUserUID get() = _currentUserUID

    // 로그인, 회원가입시 이벤트를 다루기 위한 LiveData
    private var _event = MutableLiveData<Event<String>>()
    val event get() = _event

    // 로그인 메소드
    fun tryLogin(id: String, pw: String) {
        repository.tryLogin(id, pw, this)
    }

    // 회원가입 메소드
    fun attemptRegister(name: String, email: String, pw: String) {
        repository.attemptRegister(name, email, pw, this)
    }

    // 로그아웃 메소드
    fun logout() {
        repository.logout()
    }

    // 로그인, 회원가입 성공시 uid 값을 갱신 및 이벤트 발생
    override fun onSuccess(result: AuthSuccessResult) {
        if (result.uid != null) {
            _currentUserUID = result.uid
        }
        _event.value = Event(result.message)
    }

    // 로그인, 회원가입 실패시 실패 메시지를 담은 이벤트 발생
    override fun onFailure(error: AuthError) {
        _event.value = Event(error.message)
    }
}