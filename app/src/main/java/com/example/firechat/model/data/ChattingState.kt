package com.example.firechat.model.data

import java.io.Serializable

// 채팅방 내의 사용자 상태에 대한 클래스
// joinState 필드는 참여 여부(채팅방을 나갔는지 확인하기 위한 용도)를 확인하기 위한 사용됨
// onlineState 필드는 현재 유저가 해당을 보고있는지 를 확인하기 위한 용도로 사용됨
data class ChattingState(
    val joinState: Boolean = false,
    val onlineState: Boolean = false
) : Serializable
