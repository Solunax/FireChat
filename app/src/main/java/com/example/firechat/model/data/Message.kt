package com.example.firechat.model.data

import java.io.Serializable

// 메세지 전달시 필요한 정보를 담은 Data Class
data class Message(
    var senderUid: String = "",
    var sendingDate: String = "",
    var content: String = "",
    var confirmed: Boolean = false
) : Serializable
