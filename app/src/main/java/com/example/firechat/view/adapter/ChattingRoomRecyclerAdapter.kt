package com.example.firechat.view.adapter

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.firechat.R
import com.example.firechat.model.data.Message
import com.example.firechat.databinding.MessageMyItemBinding
import com.example.firechat.databinding.MessageOpponentItemBinding
import com.example.firechat.model.data.ChattingRoomTimeData
import com.example.firechat.model.data.CurrentUserData
import com.example.firechat.view.activity.ChattingRoomActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import java.lang.StringBuilder

class ChattingRoomRecyclerAdapter(
    private val context: Context,
    var chattingRoomKey: String
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    val allMessage = ArrayList<Message>()
    val messageKeys = ArrayList<String>()
    private val db = FirebaseDatabase.getInstance()
    val recyclerView = (context as ChattingRoomActivity).messageRecyclerView

    init {
        setMessage()
    }

    // 인스턴스 생성시 수행되는 메소드로
    // 현재 채팅방에 저장된 메세지들을 가져오는 메소드임
    private fun setMessage() {
        getMessage()
    }

    // 채팅방의 고유 Key값에 맞는 채팅방에서 메세지들을 가져오는 메소드
    // 가져온 메세지는 message, messageKey 배열에 저장된다
    private fun getMessage() {
        db.getReference("ChattingRoom")
            .child(chattingRoomKey).child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allMessage.clear()
                    messageKeys.clear()

                    for (data in snapshot.children) {
                        allMessage.add(data.getValue<Message>()!!)
                        messageKeys.add(data.key!!)
                    }

                    notifyDataSetChanged()
                    recyclerView.scrollToPosition(allMessage.size - 1)
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    // 메세지를 누가 보냈느냐에 따라서 내용을 분리하는 메소드
    override fun getItemViewType(position: Int): Int {
        return if (allMessage[position].senderUid == CurrentUserData.uid) {
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
        if (allMessage[position].senderUid == CurrentUserData.uid) {
            (holder as MyMessageViewHolder).bind(position)
        } else {
            (holder as OpponentMessageViewHolder).bind(position)
        }
    }

    override fun getItemCount(): Int {
        return allMessage.size
    }

    inner class MyMessageViewHolder(binding: MessageMyItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val messageContent = binding.messageContent
        private val date = binding.messageDate
        private val readCheck = binding.messageRead

        // 현재 메세지의 내용, 전송 시간, 읽기 여부를 리사이클러 뷰 Item에 표시
        fun bind(position: Int) {
            val message = allMessage[position]
            val sendDate = message.sendingDate

            date.text = getDateText(sendDate)
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
                        db.getReference("ChattingRoom").child(chattingRoomKey)
                            .child("messages").child(messageKeys[position]).removeValue()
                    }
                    .setNegativeButton("아니요") { dialog, _ ->
                        dialog.dismiss()
                    }.show()

                return@setOnLongClickListener true
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

    inner class OpponentMessageViewHolder(binding: MessageOpponentItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val messageContent = binding.messageContent
        private val date = binding.messageDate
        private val readCheck = binding.messageRead

        // 현재 메세지의 내용, 전송 시간, 읽기 여부를 리사이클러 뷰 Item에 표시
        fun bind(position: Int) {
            val message = allMessage[position]
            val sendDate = message.sendingDate

            date.text = getDateText(sendDate)
            messageContent.text = message.content

            if (message.confirmed) {
                readCheck.visibility = View.GONE
            } else {
                readCheck.visibility = View.VISIBLE
            }

            setReadState(position)
        }

        // 사용자가 메세지를 읽었다는 State 값을 변경하는 메소드
        private fun setReadState(position: Int) {
            db.getReference("ChattingRoom")
                .child(chattingRoomKey).child("messages")
                .child(messageKeys[position]).child("confirmed")
                .setValue(true)
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
}