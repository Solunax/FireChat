package com.example.firechat.viewModel

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firechat.model.repository.AuthRepository
import com.example.firechat.model.repository.ProfileRepository
import kotlinx.coroutines.launch

class ViewModel : ViewModel(), AuthResultCallback {
    private val authRepository = AuthRepository()
    private val profileRepository = ProfileRepository()
    
    private lateinit var _currentUserUID: String
    val currentUserUID get() = _currentUserUID

    // 로그인, 회원가입시 이벤트를 다루기 위한 LiveData
    private var _event = MutableLiveData<Event<String>>()
    val event get() = _event

    // 프로필 이미지 Uri LiveData
    private val _profileImageUri = MutableLiveData<Uri?>()
    val profileImageUri get() = _profileImageUri

    // 로그인 메소드
    fun tryLogin(id: String, pw: String) {
        authRepository.tryLogin(id, pw, this)
    }

    // 회원가입 메소드
    fun attemptRegister(name: String, email: String, pw: String) {
        authRepository.attemptRegister(name, email, pw, this)
    }

    // 로그아웃 메소드
    fun logout() {
        authRepository.logout()
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

    // 프로필 이미지 업로드 메소드
    fun uploadProfileImage(uri: Uri) {
        viewModelScope.launch {
            val success = profileRepository.uploadProfileImage(uri)

            if (success) {
                _event.postValue(Event("프로필 이미지 업로드를 성공했습니다."))
                getProfileImage()
            } else {
                _event.postValue(Event("프로필 이미지 업로드를 실패했습니다."))
            }
        }
    }

    // 프로필 이미지 로드 메소드
    fun getProfileImage() {
        viewModelScope.launch {
            val uri = profileRepository.getProfileImage()

            if (uri != null) {
                _profileImageUri.postValue(uri)
            } else {
                _event.postValue(Event("프로필 이미지를 불러오는데 실패했습니다."))
            }
        }
    }
}