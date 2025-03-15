package com.example.firechat.view.adapter

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.firechat.R
import com.example.firechat.model.data.Message
import com.example.firechat.databinding.MessageMyItemBinding
import com.example.firechat.databinding.MessageOpponentItemBinding
import com.example.firechat.model.data.ChattingRoomTimeData
import com.example.firechat.model.data.CurrentUserData
import java.lang.StringBuilder

class ChattingRoomRecyclerAdapter(
    private val context: Context,
    private val deleteMessage: (String) -> Unit,
    private val changeReadState: (String) -> Unit
) : ListAdapter<Pair<String, Message>, RecyclerView.ViewHolder>(DataComparator) {

    // 데이터 셋을 받아 차이를 계산
    // areItemsTheSame은 두 객체가 동일객체인지 확인
    // areContentsTheSame은 두 아이템이 동일한 데이터를 가지는지 확인함
    // 비교 기준은 메세지의 고유한 Key값과 해당 메세지의 Contents(보낸 사람 UID, 보낸 시각, 내용, 확인 여부)
    companion object DataComparator : DiffUtil.ItemCallback<Pair<String, Message>>() {
        override fun areItemsTheSame(
            oldItem: Pair<String, Message>,
            newItem: Pair<String, Message>
        ): Boolean {
            return oldItem.first == newItem.first
        }

        override fun areContentsTheSame(
            oldItem: Pair<String, Message>,
            newItem: Pair<String, Message>
        ): Boolean {
            return oldItem.second == newItem.second
        }
    }

    // 메세지를 누가 보냈느냐에 따라서 내용을 분리하는 메소드
    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).second.senderUid == CurrentUserData.uid) {
            1
        } else {
            0
        }
    }

    // 위 메소드로 구분된 viewType이 ViewHolder 생성시 사용됨
    // 내가 보낸 메세지면 MyMessage, 다른 사람이 보낸 메세지면 OpponentMessage
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            1 -> {
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.message_my_item, parent, false)

                MyMessageViewHolder(MessageMyItemBinding.bind(view))
            }

            else -> {
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.message_opponent_item, parent, false)

                OpponentMessageViewHolder(MessageOpponentItemBinding.bind(view))
            }
        }
    }

    // 메세지를 전송한 사람이 누구인지에 따라서 My, Opponent 분리
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val (key, message) = getItem(position)
        if (holder is MyMessageViewHolder) {
            holder.bind(key, message)
        } else if (holder is OpponentMessageViewHolder) {
            holder.bind(key, message)
        }
    }

    inner class MyMessageViewHolder(binding: MessageMyItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val messageContent = binding.messageContent
        private val date = binding.messageDate
        private val readCheck = binding.messageRead

        // 현재 메세지의 내용, 전송 시간, 읽기 여부를 리사이클러 뷰 Item에 표시
        fun bind(messageKey: String, message: Message) {
            date.text = getDateText(message.sendingDate)
            messageContent.text = message.content

            if (message.confirmed) {
                readCheck.visibility = View.GONE
            } else {
                readCheck.visibility = View.VISIBLE
            }

            // 자신이 보낸 메세지를 길게 터치하면 alert dialog를 생성하고
            // 사용자가 승인시 해당 메세지를 삭제함(사용자, 상대방)
            // 단, 상대방이 보낸 메세지를 삭제하는것은 불가
            messageContent.setOnLongClickListener {
                AlertDialog.Builder(context)
                    .setTitle("메세지 삭제")
                    .setMessage("해당 메세지를 삭제하시겠습니까?")
                    .setPositiveButton("네") { _, _ ->
                        deleteMessage(messageKey)
                    }
                    .setNegativeButton("아니요") { dialog, _ ->
                        dialog.dismiss()
                    }.show()

                return@setOnLongClickListener true
            }
        }
    }

    inner class OpponentMessageViewHolder(binding: MessageOpponentItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val messageContent = binding.messageContent
        private val date = binding.messageDate
        private val readCheck = binding.messageRead

        // 현재 메세지의 내용, 전송 시간, 읽기 여부를 리사이클러 뷰 Item에 표시
        fun bind(messageKey: String, message: Message) {
            date.text = getDateText(message.sendingDate)
            messageContent.text = message.content

            if (message.confirmed) {
                readCheck.visibility = View.GONE
            } else {
                readCheck.visibility = View.VISIBLE
            }

            setReadState(messageKey, message)
        }

        // 사용자가 메세지를 읽었다는 State 값을 변경하는 메소드
        private fun setReadState(messageKey: String, message: Message) {
            changeReadState(messageKey)
            message.confirmed = true
            readCheck.visibility = View.GONE
        }
    }

    // 메세지가 전송된 시간을 YYYY-MM-DD HH:MM 형식 문자열로 만들어주는 메소드
    private fun getDateText(sendTime: String): String {
        val timeData = ChattingRoomTimeData(sendTime)
        val timeText = StringBuilder()
        val dateFormat = "%04d-%02d-%02d"
        val timeFormat = "%02d:%02d"

        timeText.append("${dateFormat.format(timeData.year, timeData.month, timeData.date)}\n")

        if (timeData.hour == 24) {
            timeText.append("오전 ${timeFormat.format(0, timeData.minute)}")
        } else if (timeData.hour == 12) {
            timeText.append("오후 ${timeFormat.format(12, timeData.minute)}")
        } else if (timeData.hour > 12) {
            timeText.append("오후 ${timeFormat.format(timeData.hour - 12, timeData.minute)}")
        } else {
            timeText.append("오전 ${timeFormat.format(timeData.hour, timeData.minute)}")
        }

        return timeText.toString()
    }
}