package com.example.firechat.model.data

import java.io.Serializable

// 회원 정보를 담은 Data Class
data class User(
    val name: String? = "",
    val email: String? = "",
    val uid: String? = ""
) : Serializable