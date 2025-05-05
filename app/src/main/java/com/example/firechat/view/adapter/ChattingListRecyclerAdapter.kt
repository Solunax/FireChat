package com.example.firechat.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.firechat.R
import com.example.firechat.databinding.ChattingListRecyclerItemBinding
import com.example.firechat.model.data.ChattingListData
import com.example.firechat.model.data.ChattingRoom
import com.example.firechat.model.data.ChattingRoomTimeData
import com.example.firechat.model.data.CurrentUserData
import com.example.firechat.model.data.User
import java.time.Duration
import java.time.LocalDateTime

private const val ONE_MINUTE_MILLIS = 60000L
private const val ONE_HOUR_MILLIS = 3600000L
private const val ONE_DAY_MILLIS = 86400000L

// ListAdapter를 사용하면 AsyncDiffer(스레드 처리)를 더 편하게 사용할 수 있음
// submitList로 데이터를 갱신, currentList로 현재 데이터를 참조할 수 있음
class ChattingListRecyclerAdapter(
    private val chattingRoomSelected: (Pair<User, String>) -> Unit,
    private val chattingRoomLongClick: (String) -> Unit
) : ListAdapter<ChattingListData, ChattingListRecyclerAdapter.ViewHolder>(DataComparator) {

    // 데이터 셋을 받아 차이를 계산
    // areItemsTheSame은 두 객체가 동일객체인지 확인
    // areContentsTheSame은 두 아이템이 동일한 데이터를 가지는지 확인함
    companion object DataComparator : DiffUtil.ItemCallback<ChattingListData>() {
        override fun areItemsTheSame(
            oldItem: ChattingListData,
            newItem: ChattingListData
        ): Boolean {
            return oldItem.chattingRoomKey == newItem.chattingRoomKey
        }

        override fun areContentsTheSame(
            oldItem: ChattingListData,
            newItem: ChattingListData
        ): Boolean {
            return oldItem == newItem
        }
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
        val (chattingRoomKey, opponentUser, chattingRoomData) = getItem(position)
        holder.opponentUserName.text = opponentUser.name

        // 현재 채팅방의 메세지가 하나 이상 있으면
        // 마지막 메세지 내용과 전송된 시간 그리고 읽지 않은 메세지의 갯수를
        // 채팅방 Item에 표시함
        if (chattingRoomData.messages!!.isNotEmpty()) {
            val lastMessage = chattingRoomData.messages.values.maxBy { it.sendingDate }
            holder.lastChat.text = lastMessage.content
            holder.lastSendTime.text =
                getLastMessageTimeString(lastMessage.sendingDate)

            // 읽지 않은 메세지가 없으면 view의 카운트를 안보이게 설정함
            // 읽지 않은 메세지가 있다면 view의 카운트를 보이게 하고, 안읽은 메세지의 갯수로 변경
            val unReadCount = getUnreadCount(CurrentUserData.uid!!, chattingRoomData)
            if (unReadCount > 0) {
                holder.unreadCount.visibility = View.VISIBLE
                holder.unreadCount.text = unReadCount.toString()
            } else {
                holder.unreadCount.visibility = View.INVISIBLE
            }
        }

        holder.chattingRoomBackground.setOnClickListener {
            chattingRoomSelected(Pair(opponentUser, chattingRoomKey))
        }

        holder.chattingRoomBackground.setOnLongClickListener {
            chattingRoomLongClick(chattingRoomKey)

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

    // 현재 사용자가 해당 채팅방에서 읽지 않은 메세지의 갯수를 확인하여 반환하는 메소드
    // filter 메소드를 사용하여 원하는 결과값(현재 사용자가 보낸 메세지가 아니면서 확인 안한 메세지)만 필터링함
    private fun getUnreadCount(uid: String, chattingRoomData: ChattingRoom): Int {
        return chattingRoomData.messages?.filter { !it.value.confirmed && it.value.senderUid != uid }?.size
            ?: 0
    }

    // 마지막으로 전송된 메세지의 시간을 확인하여 채팅목록에 표시하기 적절한 형태로 문자열을 수정하는 메소드
    private fun getLastMessageTimeString(lastTimeString: String): String {
        //마지막 메세지가 전송된 시각 구하기
        val lastTimeData = ChattingRoomTimeData(lastTimeString)
        val currentTime = LocalDateTime.now()
        val lastTime = lastTimeData.dateTime

        //현 시각과 마지막 메세지 시각과의 차이. 월,일,시,분
        val duration = Duration.between(lastTime, currentTime)
        val diffValue = duration.toMillis()

        return when {
            diffValue < ONE_MINUTE_MILLIS -> "방금 전"
            diffValue < ONE_HOUR_MILLIS -> "${duration.toMinutes()}분 전"
            diffValue < ONE_DAY_MILLIS -> "${duration.toHours()}시간 전"
            duration.toDays() < 7 -> "${duration.toDays()}일 전"
            duration.toDays() < 30 -> "${duration.toDays() / 7}주 전"
            duration.toDays() < 365 -> "${duration.toDays() / 30}개월 전"
            else -> "${duration.toDays() / 365}년 전"
        }
    }
}