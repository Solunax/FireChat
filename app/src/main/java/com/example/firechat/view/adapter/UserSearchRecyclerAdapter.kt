package com.example.firechat.view.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.firechat.R
import com.example.firechat.databinding.NewChatUserSearchResultItemBinding
import com.example.firechat.model.data.ChattingRoom
import com.example.firechat.model.data.ChattingState
import com.example.firechat.model.data.CurrentUserData
import com.example.firechat.model.data.User
import com.example.firechat.view.activity.ChattingRoomActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue

class UserSearchRecyclerAdapter :
    RecyclerView.Adapter<UserSearchRecyclerAdapter.ViewHolder>() {
    private var userList = ArrayList<User>()
    private var allUserList = ArrayList<User>()
    private val db = FirebaseDatabase.getInstance()
    private lateinit var context: Context

    // 리사이클러 뷰 초기화시 수행되는 메소드
    init {
        setAllUser()
    }

    // 현재 DB에 존재하는 모든 사용자의 정보를 가져오는 메소드
    // 자기 자신은 제외하고 유저 목록 배열에 추가
    private fun setAllUser() {
        db.getReference("User")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allUserList.clear()
                    userList.clear()

                    for (data in snapshot.children) {
                        val item = data.getValue<User>()

                        if (item?.uid.equals(CurrentUserData.uid)) {
                            continue
                        }

                        allUserList.add(item!!)
                    }

                    userList.addAll(allUserList)
                    notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    // Activity에서 사용자 검색시 사용되는 메소드
    // 초기 상태 즉, 아무것도 입력되지 않은 상태일 때 모든 유저의 목록을 표시
    // 사용자가 글자 입력 시, 사용자 정보 배열에서 해당 글자가 포함돼 있는지 확인 후
    // 해당 글자가 포함된 경우로 필터링하여 결과를 표시함
    fun searchName(name: String) {
        userList.clear()
        if (name == "") {
            userList.addAll(allUserList)
        } else {
            val matchList = allUserList.filter { it.name!!.contains(name) }
            userList.addAll(matchList)
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        context = parent.context
        val view =
            LayoutInflater.from(context)
                .inflate(R.layout.new_chat_user_search_result_item, parent, false)
        return ViewHolder(NewChatUserSearchResultItemBinding.bind(view))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.name.text = userList[position].name
        holder.email.text = userList[position].email

        // 검색창에서 나오는 사용자를 클릭시, 새로운 채팅방을 생성함
        holder.background.setOnClickListener {
            createChattingRoom(position)
        }
    }

    // 채팅방을 새롭게 생성하는 메소드
    // 사용자의 정보와 상대방의 정보를 바탕으로 채팅방을 구성함
    // 채팅방 구성시 만약 이미 생성된 채팅방이 존재하면
    // 채팅방을 생성하지 않고 기존 채팅방으로 이동함
    private fun createChattingRoom(position: Int) {
        val opponent = userList[position]
        val chatRoom = ChattingRoom(
            mapOf(
                Pair(CurrentUserData.uid!!, ChattingState(joinState = true, onlineState = true)),
                Pair(opponent.uid!!, ChattingState(joinState = true, onlineState = false))
            ), null
        )

        db.getReference("ChattingRoom").orderByChild("users/${CurrentUserData.uid!!}/joinState")
            .equalTo(true)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var validationCheck = false

                    for (data in snapshot.children) {
                        val userData = data.child("users").value.toString()

                        if (userData.contains(CurrentUserData.uid!!) && userData.contains(opponent.uid)) {
                            validationCheck = true
                        }
                    }

                    if (validationCheck) {
                        AlertDialog.Builder(context)
                            .setTitle("채팅방 생성 알림")
                            .setMessage("이미 해당 사용자와 대화한 채팅방이 있습니다. 이동하시겠습니까?")
                            .setPositiveButton("네") { dialog, _ ->
                                // View Model의 로그아웃 메소드 호출 후 로그인 화면으로 돌아감
                                moveToChattingRoom(opponent)
                            }
                            .setNegativeButton("아니요") { dialog, _ ->
                                dialog.dismiss()
                            }.show()
                    } else {
                        db.getReference("ChattingRoom").push()
                            .setValue(chatRoom).addOnSuccessListener {
                                moveToChattingRoom(opponent)
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    // 채팅방 Activity를 시작하는 메소드
    // 채팅방 구성에 필요한 정보들을 담은 Intent로 Activity를 시작함
    fun moveToChattingRoom(opponent: User) {
        val intent = Intent(context, ChattingRoomActivity::class.java)
        intent.putExtra("opponent", opponent)
        intent.putExtra("chatRoomKey", "")

        context.startActivity(intent)
        (context as AppCompatActivity).finish()
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    inner class ViewHolder(binding: NewChatUserSearchResultItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        var background = binding.background
        var name = binding.searchName
        var email = binding.searEmail
    }
}