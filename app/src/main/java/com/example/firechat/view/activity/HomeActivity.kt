package com.example.firechat.view.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firechat.databinding.HomeActivityBinding
import com.example.firechat.model.data.CurrentUserData
import com.example.firechat.service.TaskRemoveService
import com.example.firechat.view.adapter.ChattingListRecyclerAdapter
import com.example.firechat.viewModel.AuthViewModel

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: HomeActivityBinding
    private lateinit var newChatButton: Button
    private lateinit var logoutButton: Button
    private lateinit var chattingRoomRecycler: RecyclerView
    private lateinit var uid: String
    private var lastBackPressedTime = 0L
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = HomeActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initProperty()
        initView()
        initListener()
        setRecycler()

        // 뒤로가기 버튼 클릭 콜백 연결
        onBackPressedDispatcher.addCallback(this, backPressedCallback)

        // 앱 강제종료시 Firebase 로그아웃을 위한 서비스 시작
        startService(Intent(this, TaskRemoveService::class.java))
    }

    // 메모리 누수 방지를 위해 Destory시 콜백을 비활성화
    override fun onDestroy() {
        super.onDestroy()
        backPressedCallback.isEnabled = false
    }

    // 현재 사용자 정보를 가지고 있는 Singleton 객체에서 uid를 가져옴
    private fun initProperty() {
        uid = CurrentUserData.uid!!
    }

    private fun initView() {
        newChatButton = binding.buttonNewChat
        logoutButton = binding.buttonLogout
        chattingRoomRecycler = binding.chattingRoomRecycler
    }

    private fun initListener() {
        logoutButton.setOnClickListener {
            logout()
        }

        // 새로운 채팅 생성 버튼
        // 클릭시 채팅방 생성 Activity 시작
        newChatButton.setOnClickListener {
            startActivity(Intent(this, NewChatActivity::class.java))
        }
    }

    // 리사이클러 뷰를 초기화하는 메소드
    // decoration을 사용해 Item에 구분선을 추가함
    private fun setRecycler() {
        chattingRoomRecycler.layoutManager = LinearLayoutManager(this)
        val decoration = DividerItemDecoration(applicationContext, DividerItemDecoration.VERTICAL)
        chattingRoomRecycler.addItemDecoration(decoration)

        chattingRoomRecycler.adapter = ChattingListRecyclerAdapter()
    }

    // 로그아웃 메소드
    private fun logout() {
        // 로그아웃 경고창
        AlertDialog.Builder(this)
            .setTitle("로그아웃")
            .setMessage("로그아웃 하시겠습니까?")
            .setPositiveButton("확인") { dialog, _ ->
                // View Model의 로그아웃 메소드 호출 후 로그인 화면으로 돌아감
                viewModel.logout()
                startActivity(Intent(this, LoginActivity::class.java))
                dialog.dismiss()
                finish()
            }
            .setNegativeButton("취소") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    // 뒤로가기 버튼을 두번 클릭시 앱을 종료하는 기능을 수행하는 콜백
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (System.currentTimeMillis() > lastBackPressedTime + 2000) {
                lastBackPressedTime = System.currentTimeMillis()
                Toast.makeText(this@HomeActivity, "뒤로가기 버튼을 한번 더 누르면 앱이 종료됩니다.", Toast.LENGTH_SHORT)
                    .show()
            } else {
                finish()
            }
        }
    }
}