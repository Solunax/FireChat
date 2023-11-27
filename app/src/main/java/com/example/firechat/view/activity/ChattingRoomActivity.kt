package com.example.firechat.view.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firechat.model.data.ChattingRoom
import com.example.firechat.model.data.ChattingState
import com.example.firechat.model.data.Message
import com.example.firechat.model.data.User
import com.example.firechat.databinding.ChattingRoomActivityBinding
import com.example.firechat.view.adapter.ChattingRoomRecyclerAdapter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.TimeZone

class ChattingRoomActivity : AppCompatActivity() {
    private lateinit var binding: ChattingRoomActivityBinding
    private lateinit var topLayout: ConstraintLayout
    private lateinit var goBackButton: ImageButton
    private lateinit var sendMessageButton: ImageButton
    private lateinit var messageInput: EditText
    private lateinit var opponentName: TextView

    private lateinit var uid: String
    private lateinit var chatRoom: ChattingRoom
    private lateinit var opponentUser: User
    private lateinit var chatRoomKey: String
    private lateinit var inputMethodManager: InputMethodManager
    private var opponentUserOnlineState = false
    lateinit var messageRecyclerView: RecyclerView
    private val db = FirebaseDatabase.getInstance()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ChattingRoomActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initProperty()
        initView()
        setChattingRoom()

        // 뒤로가기 버튼을 누를시 홈 화면 Activity를 실행
        // 실행 후 현재 Activity 종료
        goBackButton.setOnClickListener {
            changeOnlineState(false)
        }

        // 메세지 전송 버튼을 누를시 현재 입력한 메세지를 서버에 저장
        sendMessageButton.setOnClickListener {
            sendMessage()
        }

        onBackPressedDispatcher.addCallback(this, backPressedCallback)
    }

    // 소프트 키보드가 활성화된 상태에서 다른곳 터치시 소프트 키보드를 비활성화함
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (currentFocus != null) {
            inputMethodManager.hideSoftInputFromWindow(this.currentFocus?.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }

    // 메모리 누수 방지를 위해 Destroy시 콜백을 비활성화
    // 채팅방에서 완전히 나갈시(Destroy) 채팅방 접속 상태 변경
    override fun onDestroy() {
        super.onDestroy()
        backPressedCallback.isEnabled = false
        changeOnlineState(false)
    }

    // 이전 Activity에서 넘겨준 값들을 현재 Activity에 기입
    // 기입된 데이터를 바탕으로 채팅방을 구성함
    // 정보는 채팅방 정보(사용자), 채팅방 고유 Key(ID), 상대방 UID를 포함함
    // 소프트키보드가 열린 상태로 다른곳을 터치하면 소프트 키보드를 닫는 기능을 위해
    // InputMethodManager 사용
    private fun initProperty() {
        uid = intent.getStringExtra("uid").toString()
        chatRoom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("chatRoom", ChattingRoom::class.java)!!
        } else {
            intent.getSerializableExtra("chatRoom") as ChattingRoom
        }
        chatRoomKey = intent.getStringExtra("chatRoomKey")!!
        opponentUser = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("opponent", User::class.java)!!
        } else {
            intent.getSerializableExtra("opponent") as User
        }

        inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    // 상대방의 이름을 Activity 상단에 고정
    // 누구와 대화를 하고있는지 사용자가 알 수 있게 함
    private fun initView() {
        goBackButton = binding.chattingRoomBack
        messageInput = binding.messageInput
        messageRecyclerView = binding.messageRecycler
        sendMessageButton = binding.sendMessage
        opponentName = binding.chattingRoomOpponentUserName
        topLayout = binding.constraintTop

        opponentName.text = opponentUser.name
    }

    // 채팅방 초기화 메소드
    // 새로만들어진 채팅방이면 이전 Activity에서 Key값을 받지 못했기때문에
    // 서버에 저장된 Key값을 가져옴
    // 그게 아니라면 Key값을 바탕으로 리사이클러뷰(채팅 내역)을 구성함
    private fun setChattingRoom() {
        if (chatRoomKey.isBlank()) {
            setChatRoomKey()
        } else {
            setRecycler()
        }
    }

    // 새로 만들어진 채팅방의 경우 실행되는 메소드
    // 채팅 목록을 가져온 뒤, 현재 사용자와 원하는 상대방으로 구성된 채팅방 Key를 찾음
    // 찾은 후 그 Key를 바탕으로 리사이클러 뷰(채팅방)을 구성함
    private fun setChatRoomKey() {
        db.getReference("ChattingRoom")
            .orderByChild("users/${opponentUser.uid}/joinState").equalTo(true)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (data in snapshot.children) {
                        val userData = data.child("users").value.toString()
                        if (userData.contains(uid) && userData.contains(opponentUser.uid.toString())) {
                            chatRoomKey = data.key!!
                            setRecycler()
                            break
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    // 메세지 보내기 메소드
    // 사용자가 입력한 채팅을 Message 클래스 형식에 맞게 생성하여 서버에 저장함
    // Message의 형식은 UID, 현재 시간 정보, 메세지 내용, 읽은 상태로 구성됨
    // 읽은 상태는 현재 상대방이 채팅방 접속 상태에 따라 결정함
    // 예를들어, 상대가 이미 채팅방에 접속한 상태라면 안읽었다는 표시를 할 필요가 없음 따라서 Message 클래스에 onlineState를 true로
    // 아닐경우는 false로 보냄
    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendMessage() {
        if (messageInput.text.isNotEmpty()) {
            val message =
                Message(uid, getTimeData(), messageInput.text.toString(), opponentUserOnlineState)

            db.getReference("ChattingRoom")
                .child(chatRoomKey).child("messages")
                .push().setValue(message)
                .addOnSuccessListener {
                    messageInput.text.clear()
                }
        }
    }

    private fun setRecycler() {
        messageRecyclerView.layoutManager = LinearLayoutManager(this)
        messageRecyclerView.adapter = ChattingRoomRecyclerAdapter(this, chatRoomKey, uid)
        getOpponentOnlineState()
        changeOnlineState(true)

        // 소프트 키보드 사용시 리사이클러 뷰의 마지막 항목을 표시하는 기능을 수행
        messageRecyclerView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            messageRecyclerView.post {
                messageRecyclerView.adapter?.itemCount?.takeIf { it > 0 }?.let {
                    messageRecyclerView.smoothScrollToPosition(it - 1)
                }
            }
        }
    }

    // 상대방이 현재 채팅방에 접속해있는지(채팅방 activity를 보는 상태인지) 상태 값을 메소드
    private fun getOpponentOnlineState() {
        db.getReference("ChattingRoom")
            .child(chatRoomKey).child("users")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (data in snapshot.children) {
                        if (data.key == opponentUser.uid)
                            opponentUserOnlineState = data.getValue<ChattingState>()!!.onlineState
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    // Message 클래스 구성시 필요한 현재 시간 정보를 변환하는 메소드
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getTimeData(): String {
        val localDateTime = LocalDateTime.now()
        localDateTime.atZone(TimeZone.getDefault().toZoneId())
        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        return localDateTime.format(dateTimeFormatter).toString()
    }

    // 뒤로가기 버튼 클릭시 홈 화면으로 돌아감
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            changeOnlineState(false)
        }
    }

    // 현재 채팅방을 나갈시 현재 채팅방의 온라인 상태를 바꾸는 메소드
    // 이 메소드는 메세지 전송시 읽은 상태 값을 설정하기 위해 DB 값을 수정함
    private fun changeOnlineState(state: Boolean) {
        db.getReference("ChattingRoom")
            .child(chatRoomKey).child("users")
            .child(uid).setValue(ChattingState(true, state)).addOnSuccessListener {
                if (!state)
                    backToHomeActivity()
            }
    }

    // Home Activity로 돌아가는 메소드
    private fun backToHomeActivity() {
        startActivity(
            Intent(this, HomeActivity::class.java)
                .putExtra("uid", uid)
        )
        finish()
    }
}