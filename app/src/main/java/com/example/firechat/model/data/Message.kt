package com.example.firechat.model.data

import java.io.Serializable

// 메세지 전달시 필요한 정보를 담은 Data Class
// confirmed 필드는 상대방이 메세지를 읽었는지 확인하는 용도
data class Message(
    var senderUid: String = "",
    var sendingDate: String = "",
    var content: String = "",
    var confirmed: Boolean = false
) : Serializable
