package com.example.firechat.model.data

import java.io.Serializable

// 회원 정보를 담은 Data Class
// uid는 FireBase에서 자동으로 생성된 고유한 uid 값을 사용함
data class User(
    val name: String? = "",
    val email: String? = "",
    val uid: String? = ""
) : Serializable