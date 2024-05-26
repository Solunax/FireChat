package com.example.firechat.view.adapter

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
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
import com.example.firechat.view.activity.HomeActivity
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import java.util.Calendar
import java.util.concurrent.TimeUnit

// ListAdapter를 사용하면 AsyncDiffer(스레드 처리)를 더 편하게 사용할 수 있음
// submitList로 데이터를 갱신, currentList로 현재 데이터를 참조할 수 있음
class ChattingListRecyclerAdapter(private val context: Context) :
    ListAdapter<Pair<String, ChattingRoom>, ChattingListRecyclerAdapter.ViewHolder>(DataComparator) {
    private val db = FirebaseDatabase.getInstance()
    private val dataList = HashMap<String, ChattingRoom>()
    private var sortedList = emptyList<Pair<String, ChattingRoom>>()
    private val recyclerView = (context as HomeActivity).chattingRoomRecycler

    // 데이터 셋을 받아 차이를 계산
    // areItemsTheSame은 두 객체가 동일객체인지 확인
    // areContentsTheSame은 두 아이템이 동일한 데이터를 가지는지 확인함
    companion object DataComparator : DiffUtil.ItemCallback<Pair<String, ChattingRoom>>() {
        override fun areItemsTheSame(
            oldItem: Pair<String, ChattingRoom>,
            newItem: Pair<String, ChattingRoom>
        ): Boolean {
            return oldItem == newItem
        }

        // 채팅방 Key값 비교
        override fun areContentsTheSame(
            oldItem: Pair<String, ChattingRoom>,
            newItem: Pair<String, ChattingRoom>
        ): Boolean {
            return oldItem.first == newItem.first
        }
    }

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
            .addChildEventListener(object : ChildEventListener {
                // Adapter 생성시 채팅방의 정보를 가져와 HashMap에 저장
                // 최초 설정 이후 새로 추가된 정보만 가져옴
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    dataList[snapshot.key!!] = snapshot.getValue<ChattingRoom>()!!
                    sortData()
                }

                // 채팅방 정보가 갱신되었을 경우 호출함
                // snapshot의 Key를 바탕으로 기존에 저장된 정보를 새로 받은 정보로 갱신함
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    val data = snapshot.getValue<ChattingRoom>()!!
                    dataList[snapshot.key!!] = data
                    sortData()
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    // 서버로부터 받아온 데이터를 정렬하는 메소드
    // 정렬 기준 : 가장 마지막에 받은 메세지 시간기준으로 내림차순(최근순)
    // 채팅방만 생성되고 메세지를 받은적이 없다면 맨 뒤로 위치시킴
    fun sortData() {
        submitList(null)
        sortedList = dataList.toList()
            .sortedWith(nullsLast(compareByDescending { getLastMessage(it.second)?.sendingDate }))
        submitList(sortedList)

        // adapter의 list 정렬 후 가장 위로 스크롤함
        Handler(Looper.getMainLooper()).postDelayed({
            recyclerView.scrollToPosition(0)
        }, 200)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.chatting_list_recycler_item, parent, false)
        return ViewHolder(ChattingListRecyclerItemBinding.bind(view))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chattingRoomKey = currentList[position].first
        val chattingRoomData = currentList[position].second
        val userKeys = chattingRoomData.users!!.keys
        val opponentKey = userKeys.first { it != CurrentUserData.uid }
        var initializeCheck = false
        lateinit var opponentUser: User

        // 현재 채팅방 정보중 상대방의 UID를 바탕으로 이름 정보를 가져옴
        db.getReference("User").orderByChild("uid")
            .equalTo(opponentKey)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (data in snapshot.children) {
                        opponentUser = data.getValue<User>()!!
                        holder.opponentUserName.text = opponentUser.name
                    }
                    initializeCheck = true
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })

        // 현재 채팅방의 메세지가 하나 이상 있으면
        // 마지막 메세지 내용과 전송된 시간 그리고 읽지 않은 메세지의 갯수를
        // 채팅방 Item에 표시함
        @RequiresApi(Build.VERSION_CODES.O)
        if (chattingRoomData.messages!!.isNotEmpty()) {
            val lastMessage = getLastMessage(chattingRoomData)
            holder.lastChat.text = lastMessage?.content
            holder.lastSendTime.text =
                getLastMessageTimeString(lastMessage!!.sendingDate)
            val unReadCount = getUnreadCount(CurrentUserData.uid!!, chattingRoomData)

            // 읽지 않은 메세지가 없으면 view의 카운트를 안보이게 설정함
            // 읽지 않은 메세지가 있다면 view의 카운트를 보이게 하고, 안읽은 메세지의 갯수로 변경
            if (unReadCount > 0) {
                holder.unreadCount.visibility = View.VISIBLE
                holder.unreadCount.text = unReadCount.toString()
            } else {
                holder.unreadCount.visibility = View.INVISIBLE
            }
        }

        // 사용자가 채팅방을 클릭시 사용되는 리스너
        // 채팅방의 정보를 담은 Intent로 채팅방 Activity를 시작함
        holder.chattingRoomBackground.setOnClickListener {
            val intent = Intent(context, ChattingRoomActivity::class.java)
            if (initializeCheck) {
                intent.putExtra("opponent", opponentUser)
                intent.putExtra("chatRoomKey", chattingRoomKey)

                context.startActivity(intent)
            }
        }

        holder.chattingRoomBackground.setOnLongClickListener {
            AlertDialog.Builder(context)
                .setTitle("채팅방 나가기")
                .setMessage("채팅방에서 나가시겠습니까?")
                .setPositiveButton("확인") { _, _ ->
                    db.getReference("ChattingRoom").child(chattingRoomKey)
                        .child("users").child("${CurrentUserData.uid}")
                        .child("joinState").setValue(false)
                    chattingRoomAvailableCheck(chattingRoomKey)

                    dataList.remove(chattingRoomKey)
                    sortData()
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
        return currentList.size
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
    private fun getLastMessage(chattingRoomData: ChattingRoom): Message? {
        return chattingRoomData.messages!!.values.maxByOrNull { it.sendingDate }
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

    // 마지막으로 전송된 메세지의 시간을 확인하여 채팅목록에 표시하기 적절한 형태로 문자열을 수정하는 메소드
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getLastMessageTimeString(lastTimeString: String): String {
        //마지막 메세지가 전송된 시각 구하기
        val lastTimeData = ChattingRoomTimeData(lastTimeString)
        val currentTime = Calendar.getInstance().timeInMillis
        val lastTime = Calendar.getInstance().apply { time = lastTimeData.dateTime }.timeInMillis

        //현 시각과 마지막 메세지 시각과의 차이. 월,일,시,분
        val diffValue = currentTime - lastTime

        return when {
            diffValue < 60000 -> {
                "방금 전"
            }

            diffValue < 3600000 -> {
                TimeUnit.MILLISECONDS.toMinutes(diffValue).toString() + "분 전"
            }

            diffValue < 86400000 -> {
                TimeUnit.MILLISECONDS.toHours(diffValue).toString() + "시간 전"
            }

            diffValue < 604800000 -> {
                TimeUnit.MILLISECONDS.toDays(diffValue).toString() + "일 전"
            }

            diffValue < 2419200000 -> {
                TimeUnit.MILLISECONDS.toDays(diffValue / 7).toString() + "주 전"
            }

            diffValue < 31556952000 -> {
                val days = TimeUnit.MILLISECONDS.toDays(diffValue / 30)
                if (days == 0L) {
                    "1 개월 전"
                } else {
                    TimeUnit.MILLISECONDS.toDays(diffValue / 30).toString() + "개월 전"
                }
            }

            else -> {
                TimeUnit.MILLISECONDS.toDays(diffValue / 365).toString() + "년 전"
            }
        }
    }
}