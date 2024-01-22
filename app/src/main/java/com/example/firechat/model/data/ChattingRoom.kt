package com.example.firechat.model.data

import java.io.Serializable

// 채팅룸 구성에 필요한 정보를 담은 Data Class
// users에 유저의 이름을 key, 유저의 채팅방 정보(참여 상태, 로그인 상태)를 Value로 HashMap에 저장
// messages에 메세지 고유 번호를 key, 메세지 정보(보낸 사람, 보낸 시간 정보, 내용, 읽은 상태)를
// value 로 HashMap에 저장
data class ChattingRoom(
    val users: Map<String, ChattingState>? = HashMap(),
    val messages: Map<String, Message>? = HashMap()
) : Serializable
