package com.example.firechat.model.repository

import com.example.firechat.model.data.ChattingRoom
import com.example.firechat.model.data.ChattingState
import com.example.firechat.model.data.CurrentUserData
import com.example.firechat.model.data.Message
import com.example.firechat.model.data.User
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue

class ChattingListRepository(private val db: FirebaseDatabase = FirebaseDatabase.getInstance()) {
    private lateinit var chattingRoomListener: ChildEventListener
    private val chattingRoomData = LinkedHashMap<String, ChattingRoom>()

    fun getChattingRoomsData(callback: (List<Pair<String, ChattingRoom>>) -> Unit) {
        val reference = db.getReference("ChattingRoom")

        chattingRoomListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val key = snapshot.key ?: return
                val chattingRoom = snapshot.getValue(ChattingRoom::class.java) ?: return
                chattingRoomData[key] = chattingRoom
                callback(sortData())
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val key = snapshot.key ?: return
                val chattingRoom = snapshot.getValue(ChattingRoom::class.java) ?: return
                chattingRoomData[key] = chattingRoom
                callback(sortData())
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val key = snapshot.key ?: return
                chattingRoomData.remove(key)
                callback(sortData())
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onCancelled(error: DatabaseError) {
            }
        }

        reference.orderByChild("users/${CurrentUserData.uid}/joinState")
            .equalTo(true)
            .addChildEventListener(chattingRoomListener)
    }

    fun getOpponentUserData(opponentKey: String, callback: (User) -> Unit) {
        db.getReference("User").orderByChild("uid")
            .equalTo(opponentKey)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        val user = child.getValue(User::class.java)
                        callback(user!!)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    // 채팅방이 유효한지(참여한 유저가 존재하는지) 확인하는 메소드
    // 유효하지 않다면 DB에서 해당 채팅방을 삭제함
    fun quitChattingRoom(chattingRoomKey: String) {
        db.getReference("ChattingRoom").child(chattingRoomKey)
            .child("users").child("${CurrentUserData.uid}")
            .child("joinState").setValue(false).addOnSuccessListener {
                chattingRoomAvailableCheck(chattingRoomKey)
            }
    }

    private fun chattingRoomAvailableCheck(chattingRoomKey: String) {
        db.getReference("ChattingRoom").child(chattingRoomKey)
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
                        db.getReference("ChattingRoom").child(chattingRoomKey).removeValue()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    private fun sortData(): List<Pair<String, ChattingRoom>> {
        return chattingRoomData.toList().sortedWith(
            nullsLast(compareByDescending { getLastMessage(it.second)?.sendingDate })
        )
    }

    private fun getLastMessage(chattingRoomData: ChattingRoom): Message? {
        return chattingRoomData.messages?.values?.maxByOrNull { it.sendingDate }
    }

    fun removeListener() {
        val reference = db.getReference("ChattingRoom")
        reference.removeEventListener(chattingRoomListener)
    }
}