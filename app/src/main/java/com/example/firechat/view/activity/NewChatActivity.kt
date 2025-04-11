package com.example.firechat.view.activity

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firechat.databinding.NewChatActivityBinding
import com.example.firechat.util.handleError
import com.example.firechat.view.adapter.NewChatRecyclerAdapter
import com.example.firechat.viewModel.NewChatViewModel

class NewChatActivity : AppCompatActivity() {
    private lateinit var binding: NewChatActivityBinding
    private val viewModel: NewChatViewModel by viewModels()
    private lateinit var backButton: ImageButton
    private lateinit var search: EditText
    private lateinit var userRecycler: RecyclerView
    private lateinit var userSearchAdapter: NewChatRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = NewChatActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initView()
        initObserver()
        initListener()
        setRecycler()

        viewModel.getAllUsers()
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
    }

    // 메모리 누수 방지를 위해 Destroy시 콜백을 비활성화
    override fun onDestroy() {
        super.onDestroy()
        backPressedCallback.isEnabled = false
        userRecycler.adapter = null
    }

    private fun initView() {
        search = binding.userSearch
        backButton = binding.newChatBack
        userRecycler = binding.userRecycler
    }

    private fun initListener() {
        // 사용자 검색창에서 입력에 따라 검색 결과를 표현하기 위한 리스너
        // 사용자가 이름을 입력한 후 현재 가입자 목록을 가진 리사이클러 뷰에서 이름을 검색함
        // 검색 결과중 사용자가 입력한 글자가 포함되는 사람의 목록을 표시
        search.addTextChangedListener { name ->
            viewModel.searchUser(name.toString())
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun initObserver() {
        viewModel.filterUserList.observe(this) { users ->
            userSearchAdapter.submitList(users)
        }
    }

    // 유저 리스트 리사이클러 뷰를 초기화하는 메소드
    private fun setRecycler() {
        userRecycler.layoutManager = LinearLayoutManager(this)
        userSearchAdapter = NewChatRecyclerAdapter { opponentUser ->
            viewModel.createChattingRoom(opponentUser) { result ->
                if (result.first.isNotEmpty()) {
                    var userChoice = true
                    if (result.second) {
                        AlertDialog.Builder(this)
                            .setTitle("채팅방 생성 알림")
                            .setMessage("이미 해당 사용자와 대화한 채팅방이 있습니다. 이동하시겠습니까?")
                            .setPositiveButton("네") { dialog, _ ->
                                dialog.dismiss()
                            }
                            .setNegativeButton("아니요") { dialog, _ ->
                                userChoice = false
                                dialog.dismiss()
                            }.show()
                    }

                    if (userChoice) {
                        val intent = Intent(this, ChattingRoomActivity::class.java)
                        intent.putExtra("opponent", opponentUser)
                        intent.putExtra("chatRoomKey", result.first)

                        startActivity(intent)
                        finish()
                    }
                } else {
                    handleError(
                        this,
                        null,
                        "ChattingRoom Create Error",
                        "채팅방 생성 실패",
                        "채팅방 생성에 실패했습니다."
                    )
                }
            }
        }
        val decoration = DividerItemDecoration(applicationContext, DividerItemDecoration.VERTICAL)
        userRecycler.addItemDecoration(decoration)
        userRecycler.adapter = userSearchAdapter
    }

    // 뒤로가기 버튼 클릭시 홈 화면으로 돌아감
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()
        }
    }
}