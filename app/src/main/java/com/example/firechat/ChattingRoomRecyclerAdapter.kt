package com.example.firechat

import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.firechat.data.ChattingRoom
import com.example.firechat.data.Message
import com.example.firechat.data.User
import com.example.firechat.databinding.ChattingRoomRecyclerItemBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.TimeZone

class ChattingRoomRecyclerAdapter : RecyclerView.Adapter<ChattingRoomRecyclerAdapter.ChattingRoomViewHolder>() {
    val chattingRooms = ArrayList<ChattingRoom>()
    val chattingRoomKeys = ArrayList<String>()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid.toString()

    // 리사이클러 뷰 초기화시 수행되는 메소드
    init {
        setChattingRooms()
    }

    class ChattingRoomViewHolder(private val context : Context, private val binding : ChattingRoomRecyclerItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(uid : String, chattingRoomData : ChattingRoom, key : String){
            val userKeys = chattingRoomData.users!!.keys
            val opponentKey = userKeys.first { it != uid }
            lateinit var opponentUser : User

            // 현재 채팅방 정보중 상대방의 UID를 바탕으로 이름 정보를 가져옴
            FirebaseDatabase.getInstance().getReference("User").orderByChild("uid")
                .equalTo(opponentKey)
                .addListenerForSingleValueEvent( object : ValueEventListener {
                    @RequiresApi(Build.VERSION_CODES.O)
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for(data in snapshot.children){
                            opponentUser = data.getValue<User>()!!
                            binding.opponentUserName.text = opponentUser.name
                        }

                        // 현재 채팅방의 메세지가 하나 이상 있으면
                        // 마지막 메세지 내용과 전송된 시간 그리고 읽지 않은 메세지의 갯수를
                        // 채팅방 Item에 표시함
                        if(chattingRoomData.messages!!.isNotEmpty()){
                            val lastMessage = getLastMessage(chattingRoomData)
                            binding.lastChat.text = lastMessage.content
                            binding.lastSendTime.text = getLastMessageTimeString(lastMessage.sendingDate)
                            val unReadCount = getUnreadCount(uid, chattingRoomData)

                            if(unReadCount > 0){
                                binding.unreadCount.visibility = View.VISIBLE
                                binding.unreadCount.text = unReadCount.toString()
                            } else {
                                binding.unreadCount.visibility = View.INVISIBLE
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })

            // 사용자가 채팅방을 클릭시 사용되는 리스너
            // 채팅방의 정보를 담은 Intent로 채팅방 Activity를 시작함
            // 그후 현재 Home Activity는 종료
            binding.chattingRoomBackground.setOnClickListener {
                val intent = Intent(context, ChattingRoomActivity::class.java)
                intent.putExtra("chatRoom", chattingRoomData)
                intent.putExtra("opponent", opponentUser)
                intent.putExtra("chatRoomKey", key)

                context.startActivity(intent)
                (context as AppCompatActivity).finish()
            }
        }

        // 해당 채팅방에서 마지막으로 전송된 메세지를 확인하여 반환하는 메소드
        private fun getLastMessage(chattingRoomData: ChattingRoom): Message {
            return chattingRoomData.messages!!.values.sortedBy { it.sendingDate }.last()
        }

        // 현재 사용자가 해당 채팅방에서 읽지 않은 메세지의 갯수를 확인하여 반환하는 메소드
        private fun getUnreadCount(uid: String, chattingRoomData: ChattingRoom): Int {
            return chattingRoomData.messages!!
                .filter { !it.value.confirmed && it.value.senderUid != uid }.size
        }

        // 마지막으로 전송된 메세지의 시간을 확인하여 채팅방에 표시하기 적절한 형태로 수정하는 메소드
        @RequiresApi(Build.VERSION_CODES.O)
        private fun getLastMessageTimeString(lastTimeString: String): String {           //마지막 메시지가 전송된 시각 구하기
            val currentTime = LocalDateTime.now().atZone(TimeZone.getDefault().toZoneId()) //현재 시각
            val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

            val messageMonth = lastTimeString.substring(4, 6).toInt()                   //마지막 메시지 시각 월,일,시,분
            val messageDate = lastTimeString.substring(6, 8).toInt()
            val messageHour = lastTimeString.substring(8, 10).toInt()
            val messageMinute = lastTimeString.substring(10, 12).toInt()

            val formattedCurrentTimeString = currentTime.format(dateTimeFormatter)     //현 시각 월,일,시,분
            val currentMonth = formattedCurrentTimeString.substring(4, 6).toInt()
            val currentDate = formattedCurrentTimeString.substring(6, 8).toInt()
            val currentHour = formattedCurrentTimeString.substring(8, 10).toInt()
            val currentMinute = formattedCurrentTimeString.substring(10, 12).toInt()

            val monthAgo = currentMonth - messageMonth                           //현 시각과 마지막 메시지 시각과의 차이. 월,일,시,분
            val dayAgo = currentDate - messageDate
            val hourAgo = currentHour - messageHour
            val minuteAgo = currentMinute - messageMinute

            //1개월 이상 차이 나는 경우
            if (monthAgo > 0) {
                return monthAgo.toString() + "개월 전"
            } else {
                //1일 이상 차이 나는 경우
                return if (dayAgo > 0) {
                    if (dayAgo == 1){
                        "어제"
                    } else {
                        dayAgo.toString() + "일 전"
                    }
                } else {
                    //1시간 이상 차이 나는 경우
                    if (hourAgo > 0){
                        hourAgo.toString() + "시간 전"
                    } else {
                        //1분 이상 차이 나는 경우
                        if (minuteAgo > 0) {
                            minuteAgo.toString() + "분 전"
                        } else {
                            "방금"
                        }
                    }
                }
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChattingRoomViewHolder {
        val binding = ChattingRoomRecyclerItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChattingRoomViewHolder(parent.context, binding)
    }

    override fun onBindViewHolder(holder: ChattingRoomViewHolder, position: Int) {
        holder.bind(uid, chattingRooms[position], chattingRoomKeys[position])
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int {
        return chattingRooms.size
    }

    // 초기 채팅방을 설정하는 메소드
    // 현재 사용자가 참여하고 있는 모든 채팅방의 정보를 가져옴
    private fun setChattingRooms() {
        FirebaseDatabase.getInstance().getReference("ChattingRoom")
            .orderByChild("users/$uid").equalTo(true)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    chattingRooms.clear()

                    for (data in snapshot.children){
                        chattingRooms.add(data.getValue<ChattingRoom>()!!)
                        chattingRoomKeys.add(data.key!!)
                    }

                    notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }
}