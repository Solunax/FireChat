package com.example.firechat.model.data

import java.io.Serializable

// 채팅룸 구성에 필요한 정보를 담은 Data Class
data class ChattingRoom(
    val users: Map<String, ChattingState>? = HashMap(),
    val messages: Map<String, Message>? = HashMap()
) : Serializable
