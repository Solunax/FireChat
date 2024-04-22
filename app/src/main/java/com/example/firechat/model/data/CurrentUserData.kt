package com.example.firechat.model.data

// Application 전역에서 사용할 현재 로그인한 사용자 데이터를 가진 데이터 클래스
class CurrentUserData {
    companion object {
        var userName: String? = null
        var email: String? = null
        var uid: String? = null
    }
}
