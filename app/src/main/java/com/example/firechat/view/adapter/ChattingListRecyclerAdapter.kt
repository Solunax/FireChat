package com.example.firechat.view.adapter

import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.firechat.R
import com.example.firechat.databinding.ChattingListRecyclerItemBinding
import com.example.firechat.model.data.ChattingRoom
import com.example.firechat.model.data.ChattingRoomTimeData
import com.example.firechat.model.data.ChattingState
import com.example.firechat.model.data.CurrentUserData
import com.example.firechat.model.data.Message
import com.example.firechat.model.data.User
import com.example.firechat.view.activity.ChattingRoomActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import java.time.LocalDateTime
import java.time.Month
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.TimeZone

class ChattingListRecyclerAdapter :
    RecyclerView.Adapter<ChattingListRecyclerAdapter.ViewHolder>() {
    val chattingRooms = ArrayList<ChattingRoom>()
    val chattingRoomKeys = ArrayList<String>()
    private val db = FirebaseDatabase.getInstance()
    private lateinit var context: Context

    // 리사이클러 뷰 초기화시 수행되는 메소드
    init {
        setChattingRooms()
    }

    // 초기 채팅방 데이터를 설정하는 메소드
    // 현재 사용자가 참여하고 있는 모든 채팅방의 정보를 가져옴
    // 현재 존재하는 채팅방에서 현재 사용자의 uid를 기준으로 joinState(참여 상태) 가 true인 값만 반환
    private fun setChattingRooms() {
        db.getReference("ChattingRoom")
            .orderByChild("users/${CurrentUserData.uid}/joinState").equalTo(true)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    chattingRooms.clear()

                    for (data in snapshot.children) {
                        chattingRooms.add(data.getValue<ChattingRoom>()!!)
                        chattingRoomKeys.add(data.key!!)
                    }

                    notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.chatting_list_recycler_item, parent, false)
        return ViewHolder(ChattingListRecyclerItemBinding.bind(view))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pos = holder.adapterPosition
        val userKeys = chattingRooms[pos].users!!.keys
        val opponentKey = userKeys.first { it != CurrentUserData.uid }
        lateinit var opponentUser: User

        // 현재 채팅방 정보중 상대방의 UID를 바탕으로 이름 정보를 가져옴
        db.getReference("User").orderByChild("uid")
            .equalTo(opponentKey)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (data in snapshot.children) {
                        opponentUser = data.getValue<User>()!!
                        holder.opponentUserName.text = opponentUser.name
                    }

                    // 현재 채팅방의 메세지가 하나 이상 있으면
                    // 마지막 메세지 내용과 전송된 시간 그리고 읽지 않은 메세지의 갯수를
                    // 채팅방 Item에 표시함
                    if (chattingRooms[pos].messages!!.isNotEmpty()) {
                        val lastMessage = getLastMessage(chattingRooms[pos])
                        holder.lastChat.text = lastMessage.content
                        holder.lastSendTime.text =
                            getLastMessageTimeString(lastMessage.sendingDate)
                        val unReadCount = getUnreadCount(CurrentUserData.uid!!, chattingRooms[pos])

                        // 읽지 않은 메세지가 없으면 view의 카운트를 안보이게 설정함
                        // 읽지 않은 메세지가 있다면 view의 카운트를 보이게 하고, 안읽은 메세지의 갯수로 변경
                        if (unReadCount > 0) {
                            holder.unreadCount.visibility = View.VISIBLE
                            holder.unreadCount.text = unReadCount.toString()
                        } else {
                            holder.unreadCount.visibility = View.INVISIBLE
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        // 사용자가 채팅방을 클릭시 사용되는 리스너
        // 채팅방의 정보를 담은 Intent로 채팅방 Activity를 시작함
        // 그후 Home Activity는 종료함
        holder.chattingRoomBackground.setOnClickListener {
            val intent = Intent(context, ChattingRoomActivity::class.java)
            intent.putExtra("chatRoom", chattingRooms[position])
            intent.putExtra("opponent", opponentUser)
            intent.putExtra("chatRoomKey", chattingRoomKeys[position])

            context.startActivity(intent)
            (context as AppCompatActivity).finish()
        }

        holder.chattingRoomBackground.setOnLongClickListener {
            AlertDialog.Builder(context)
                .setTitle("채팅방 나가기")
                .setMessage("채팅방에서 나가시겠습니까?")
                .setPositiveButton("확인") { dialog, _ ->
                    db.getReference("ChattingRoom").child(chattingRoomKeys[position])
                        .child("users").child("${CurrentUserData.uid}")
                        .child("joinState").setValue(false)

                    chattingRoomAvailableCheck(chattingRoomKeys[position])
                }
                .setNegativeButton("취소") { dialog, _ ->
                    dialog.dismiss()
                }.show()

            return@setOnLongClickListener true
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int {
        return chattingRooms.size
    }

    inner class ViewHolder(binding: ChattingListRecyclerItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val opponentUserName = binding.opponentUserName
        val lastChat = binding.lastChat
        val unreadCount = binding.unreadCount
        val lastSendTime = binding.lastSendTime
        val chattingRoomBackground = binding.chattingRoomBackground
    }

    // 해당 채팅방에서 마지막으로 전송된 메세지를 확인하여 반환하는 메소드
    private fun getLastMessage(chattingRoomData: ChattingRoom): Message {
        return chattingRoomData.messages!!.values.sortedBy { it.sendingDate }.last()
    }

    // 현재 사용자가 해당 채팅방에서 읽지 않은 메세지의 갯수를 확인하여 반환하는 메소드
    // filter 메소드를 사용하여 원하는 결과값(현재 사용자가 보낸 메세지가 아니면서 확인 안한 메세지)만 필터링함
    private fun getUnreadCount(uid: String, chattingRoomData: ChattingRoom): Int {
        return chattingRoomData.messages!!
            .filter { !it.value.confirmed && it.value.senderUid != uid }.size
    }

    // 채팅방이 유효한지(참여한 유저가 존재하는지) 확인하는 메소드
    // 유효하지 않다면 DB에서 해당 채팅방을 삭제함
    private fun chattingRoomAvailableCheck(roomKey: String) {
        db.getReference("ChattingRoom").child(roomKey)
            .child("users").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var total = 0
                    var check = 0

                    for (data in snapshot.children) {
                        val stateData = data.getValue<ChattingState>()
                        if (!stateData!!.onlineState) {
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

    // 마지막으로 전송된 메세지의 시간을 확인하여 채팅목록에 표시하기 적절한 형태로 문자열을 수정하는 메소드
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getLastMessageTimeString(lastTimeString: String): String {           //마지막 메시지가 전송된 시각 구하기
        val currentTime = LocalDateTime.now().atZone(TimeZone.getDefault().toZoneId()) //현재 시각
        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        val nowTimeData = ChattingRoomTimeData(currentTime.format(dateTimeFormatter))
        val lastTimeData = ChattingRoomTimeData(lastTimeString)
        val nowCalendar = Calendar.getInstance().apply { time = nowTimeData.dateTime }
        val lastCalendar = Calendar.getInstance().apply { time = lastTimeData.dateTime }


        //현 시각과 마지막 메시지 시각과의 차이. 월,일,시,분
        val diffValue = nowTimeData.dateTime.time - lastTimeData.dateTime.time
        val minuteAgo = diffValue / (60 * 1000)
        val hourAgo = diffValue / (60 * 60 * 1000)
        val dayAgo = diffValue / (24 * 60 * 60 * 1000)

        val yearAgo = nowCalendar.get(Calendar.YEAR) - lastCalendar.get(Calendar.YEAR)
        var monthAgo = yearAgo * 12 + nowCalendar.get(Calendar.MONTH) - lastCalendar.get(Calendar.MONTH)

        if (nowCalendar.get(Calendar.DAY_OF_MONTH) < lastCalendar.get(Calendar.DAY_OF_MONTH)) {
            monthAgo--
        }

        // 1년 이상
        if (yearAgo > 0 && monthAgo >= 12) {
            return yearAgo.toString() + "년 전"
        }

        // 1개월 이상
        if (monthAgo > 0 && dayAgo >= 30) {
            return monthAgo.toString() + "개월 전"
        }

        // 1일 이상
        if (dayAgo > 0) {
            return if (dayAgo == 1L) {
                "어제"
            } else {
                dayAgo.toString() + "일 전"
            }
        }

        // 1시간 이상
        if (hourAgo > 0) {
            return hourAgo.toString() + "시간 전"
        }

        // 1분 이상
        return if (minuteAgo > 0) {
            minuteAgo.toString() + "분 전"
        } else {
            "방금"
        }
    }
}