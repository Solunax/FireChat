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
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
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
    // 현재 채팅방에 저장된 메세지들을 가져오는 메소드
    // 기존 ValueEventListener에서 ChildEventListener로 변경
    // 기존 코드에선 DB에 이벤트가 발생할 때 마다 모든 값을 새로 가져왔지만,
    // ChildEventListener는 모든 값이 아닌 [추가, 변경, 삭제, 이동]된 값만 가져오기 때문에 더 효율적이라 판단함
    private fun setMessage() {
        db.getReference("ChattingRoom")
            .child(chattingRoomKey).child("messages")
            .addChildEventListener(object : ChildEventListener {
                // 채팅방에 입장시 최초 1회는 모든 값을 가져와 messageKey, allMessage 배열을 채우고
                // 최초 설정 이후 새로 추가된 값만 가져옴
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    messageKeys.add(snapshot.key!!)
                    allMessage.add(snapshot.getValue<Message>()!!)

                    notifyItemInserted(allMessage.lastIndex)

                    // 사용자가 가장 최근에 온 메세지를 확인할 수 있게 스크롤
                    recyclerView.scrollToPosition(allMessage.size - 1)
                }

                // 메세지 읽음 상태 변경시 호출됨
                // 최초에 읽지 않은 상태에서 읽음 상태로 변경됐을 경우 호출
                // 이 때 사용자의 메세지 ArrayList에 저장된 값은 읽지 않은 상태의 메세지
                // 올바른 메세지를 변경하기 위해서 수정된 snapshot의 key와 일치하는 key ArrayList의 인덱스를 찾음
                // 그 후 해당 인덱스 번호의 메세지 정보의 confirmed 값을 수정
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    val index = messageKeys.indexOf(snapshot.key)
                    allMessage[index].confirmed = true

                    notifyItemChanged(index)
                }

                // 유저가 메세지를 삭제할 때 호출됨
                // 가져온 Key, Messsage를 바탕으로 기존 배열에 존재하던 Key, Message 인스턴스를 삭제
                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val data = snapshot.getValue<Message>()!!
                    val index = allMessage.indexOf(data)
                    allMessage.remove(data)
                    messageKeys.remove(snapshot.key)

                    notifyItemRemoved(index)
                    // 사용자가 가장 최근에 온 메세지를 확인할 수 있게 스크롤
                    recyclerView.scrollToPosition(allMessage.size - 1)
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
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