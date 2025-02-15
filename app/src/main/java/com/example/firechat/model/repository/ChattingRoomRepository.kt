package com.example.firechat.model.repository

import com.example.firechat.model.data.ChattingState
import com.example.firechat.model.data.Message
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue

class ChattingRoomRepository(private val db: FirebaseDatabase = FirebaseDatabase.getInstance()) {
    lateinit var reference: DatabaseReference
    lateinit var stateListener: ValueEventListener

    // 채팅 목록을 가져온 뒤, 현재 사용자와 원하는 상대방으로 구성된 채팅방 Key를 찾음
    fun getChattingRoomKey(
        uid: String,
        opponentUser: String,
        onResult: (String?) -> Unit
    ) {
        db.getReference("ChattingRoom")
            .orderByChild("users/$opponentUser/joinState").equalTo(true)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (data in snapshot.children) {
                        val userData = data.child("users").value.toString()
                        if (userData.contains(uid) && userData.contains(opponentUser)) {
                            onResult(data.key)
                            return
                        }
                    }
                    onResult(null)
                }

                override fun onCancelled(error: DatabaseError) {
                    onResult(null)
                }
            })
    }

    // 메세지 보내기 메소드
    // 사용자가 입력한 채팅을 Message 클래스 형식에 맞게 생성하여 서버에 저장함
    // Message의 형식은 UID, 현재 시간 정보, 메세지 내용, 읽은 상태로 구성됨
    // 읽은 상태는 현재 상대방이 채팅방 접속 상태에 따라 결정함
    // 예를들어, 상대가 채팅방에 접속한 상태라면 Message 인스턴스의 confirmed 값은 true로
    // 아닐경우는 false로 지정 후 DB에 저장
    fun sendMessage(
        chatRoomKey: String, message: Message,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.getReference("ChattingRoom")
            .child(chatRoomKey).child("messages")
            .push()
            .setValue(message)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    // 채팅방의 온라인 상태를 바꾸는 메소드
    // 이 메소드는 사용자의 채팅방 참여 상태를 관리하는 joinState
    // 메세지 전송시 읽은 상태 값을 관리 하기 위한 state(유저가 채팅방에 들어와 있는지)
    // joinState, state 두 상태 값을 DB에 저장함
    fun changeOnlineState(chatRoomKey: String, uid: String, state: ChattingState) {
        db.getReference("ChattingRoom")
            .child(chatRoomKey)
            .child("users")
            .child(uid)
            .setValue(state)
    }

    // 상대방이 현재 채팅방에 들어와있는 상태인지 상태값을 가져오는 메소드
    fun getOpponentUserOnlineState(chatRoomKey: String, opponentUid: String, onStateChanged: (Boolean) -> Unit) {
        reference = db.getReference("ChattingRoom").child(chatRoomKey).child("users")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val state = snapshot.child(opponentUid).getValue(ChattingState::class.java)
                onStateChanged(state?.onlineState == true)
            }

            override fun onCancelled(error: DatabaseError) {
            }
        }
        reference.addValueEventListener(listener)
        stateListener = listener
    }

    // 채팅방이 유효한지(참여한 유저가 존재하는지) 확인하는 메소드
    // 유효하지 않다면 DB에서 해당 채팅방을 삭제함
    fun chattingRoomAvailableCheck(roomKey: String) {
        db.getReference("ChattingRoom").child(roomKey)
            .child("users").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var total = 0
                    var check = 0

                    for (data in snapshot.children) {
                        val stateData = data.getValue<ChattingState>()
                        if (!stateData!!.joinState) {
                            check++
                        }
                        total++
                    }
                    // total 값은 채팅방에 존재하는 유저의 수
                    // check 값은 채팅방에서 나간 유저의 수
                    // 총 유저의 수와 나간 유저의 수가 같으면 DB에서 채팅방을 삭제함
                    if (total == check) {
                        db.getReference("ChattingRoom").child(roomKey).removeValue()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    fun removeListener() {
        reference.removeEventListener(stateListener)
    }
}