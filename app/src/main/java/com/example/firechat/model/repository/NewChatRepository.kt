package com.example.firechat.model.repository

import com.example.firechat.model.data.ChattingRoom
import com.example.firechat.model.data.ChattingState
import com.example.firechat.model.data.CurrentUserData
import com.example.firechat.model.data.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class NewChatRepository(private val db: FirebaseDatabase = FirebaseDatabase.getInstance()) {
    fun getAllUserList(callback: (List<User>) -> Unit) {
        db.getReference("User")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val users = snapshot.children.mapNotNull { it.getValue(User::class.java) }
                    callback(users)
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    fun createChattingRoom(opponentUser: User, result: (Pair<String, Boolean>) -> Unit) {
        db.getReference("ChattingRoom")
            .orderByChild("users/${CurrentUserData.uid!!}/joinState")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var roomKey: String? = null
                    for (data in snapshot.children) {
                        val userData = data.child("users").value.toString()

                        if (userData.contains(CurrentUserData.uid!!) && userData.contains(
                                opponentUser.uid!!
                            )
                        ) {
                            roomKey = data.key
                            break
                        }
                    }

                    if (roomKey != null) {
                        result(Pair(roomKey, true))
                    } else {
                        val chatRoom = ChattingRoom(
                            mapOf(
                                Pair(
                                    CurrentUserData.uid!!,
                                    ChattingState(joinState = true, onlineState = true)
                                ),
                                Pair(
                                    opponentUser.uid!!,
                                    ChattingState(joinState = true, onlineState = false)
                                )
                            ), null
                        )

                        val ref = db.getReference("ChattingRoom").push()
                        ref.setValue(chatRoom).addOnSuccessListener {
                            result(Pair(ref.key!!, false))
                        }.addOnFailureListener {
                            result(Pair("", false))
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }
}