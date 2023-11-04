package com.example.firechat.data

import java.io.Serializable

// 회원 정보를 담는 Data Class
data class User(
    val name : String? = "",
    val email : String? = "",
    val uid : String? = ""
) : Serializable