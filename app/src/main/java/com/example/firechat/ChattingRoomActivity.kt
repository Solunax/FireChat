package com.example.firechat

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firechat.data.ChattingRoom
import com.example.firechat.data.Message
import com.example.firechat.data.User
import com.example.firechat.databinding.ChattingRoomActivityBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.TimeZone

class ChattingRoomActivity : AppCompatActivity() {
    private lateinit var binding : ChattingRoomActivityBinding
    private lateinit var goBackButton : ImageButton
    private lateinit var sendMessageButton : ImageButton
    private lateinit var messageInput : EditText
    private lateinit var opponentName : TextView

    private lateinit var db : DatabaseReference
    private lateinit var uid : String
    private lateinit var chatRoom : ChattingRoom
    private lateinit var opponentUser : User
    private lateinit var chatRoomKey : String
    lateinit var messageRecyclerView : RecyclerView

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ChattingRoomActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initProperty()
        initView()

        // 뒤로가기 버튼을 누를시 메인 화면 Activity를 실행
        // 실행 후 현재 Activity 종료
        goBackButton.setOnClickListener {
            backToHomeActivity()
        }

        // 메세지 전송 버튼을 누를시 현재 입력한 메세지를 서버에 저장
        sendMessageButton.setOnClickListener {
            sendMessage()
        }

        setChattingRoom()
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
    }

    // 메모리 누수 방지를 위해 Destroy시 콜백을 비활성화
    override fun onDestroy() {
        super.onDestroy()
        backPressedCallback.isEnabled = false
    }

    // 이전 Activity에서 넘겨준 값들을 현재 Activity에 기입
    // 기입된 데이터를 바탕으로 채팅방을 구성함
    // 정보는 채팅방 정보(사용자), 채팅방 고유 Key(ID), 상대방 UID를 포함함
    private fun initProperty() {
        uid = FirebaseAuth.getInstance().currentUser?.uid!!
        db = FirebaseDatabase.getInstance().reference
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
    }

    // 상대방의 이름을 Activity 상단에 고정
    // 누구와 대화를 하고있는지 사용자가 알 수 있게 함
    private fun initView() {
        goBackButton = binding.chattingRoomBack
        messageInput = binding.messageInput
        messageRecyclerView = binding.messageRecycler
        sendMessageButton = binding.sendMessage
        opponentName = binding.chattingRoomOpponentUserName

        opponentName.text = opponentUser.name
    }

    private fun backToHomeActivity() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    // 채팅방 초기화 메소드
    // 새로만들어진 채팅방이면 이전 Activity에서 Key값을 받지 못했기때문에
    // 서버에 저장된 Key값을 가져옴
    // 그게 아니라면 Key값을 바탕으로 리사이클러뷰(채팅 내역)을 구성함
    private fun setChattingRoom() {
        if(chatRoomKey.isBlank()){
            setChatRoomKey()
        } else {
            setRecycler()
        }
    }

    // 새로 만들어진 채팅방의 경우 실행되는 메소드
    // 채팅 목록을 가져온 뒤, 현재 사용자와 원하는 상대방으로 구성된 채팅방 Key를 찾음
    // 찾은 후 그 Key를 바탕으로 리사이클러 뷰(채팅방)을 구성함
    private fun setChatRoomKey() {
        FirebaseDatabase.getInstance().getReference("ChattingRoom")
            .orderByChild("users/${opponentUser.uid}").equalTo(true)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (data in snapshot.children) {
                        val userData = data.child("users").value.toString()
                        if(userData.contains(uid) && userData.contains(opponentUser.uid.toString())){
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
    // 읽은 상태는 기본값이 false로 지정되어 있기 때문에 별도로 수정하지 않음
    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendMessage() {
        if(messageInput.text.isNotEmpty()){
            val message = Message(uid, getTimeData(), messageInput.text.toString())

            FirebaseDatabase.getInstance().getReference("ChattingRoom")
                .child(chatRoomKey).child("messages")
                .push().setValue(message)
                .addOnSuccessListener {
                    messageInput.text.clear()
                }
        }
    }

    private fun setRecycler() {
        messageRecyclerView.layoutManager = LinearLayoutManager(this)
        messageRecyclerView.adapter = MessageRecyclerAdapter(this, chatRoomKey)
    }

    // Message 클래스 구성시 필요한 현재 시간 정보를 변환하는 메소드
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getTimeData() : String {
        val localDateTime = LocalDateTime.now()
        localDateTime.atZone(TimeZone.getDefault().toZoneId())
        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        return localDateTime.format(dateTimeFormatter).toString()
    }

    // 뒤로가기 버튼 클릭시 홈 화면으로 돌아감
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            backToHomeActivity()
            finish()
        }
    }
}