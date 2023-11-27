package com.example.firechat.view.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firechat.databinding.NewChatActivityBinding
import com.example.firechat.view.adapter.UserSearchRecyclerAdapter

class NewChatActivity : AppCompatActivity() {
    private lateinit var binding: NewChatActivityBinding
    private lateinit var backButton: ImageButton
    private lateinit var search: EditText
    private lateinit var userRecycler: RecyclerView
    private lateinit var uid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = NewChatActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initProperty()
        initView()

        backButton.setOnClickListener {
            backToHomeActivity()
        }

        // 사용자 검색창에서 입력에 따라 검색 결과를 표현하기 위한 리스너
        // 사용자가 입력한 후 현재 가입자 목록을 가진 리사이클러 뷰에서 검색
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun afterTextChanged(name: Editable?) {
                val adapter = userRecycler.adapter as UserSearchRecyclerAdapter
                adapter.searchName(name.toString())
            }
        })

        setRecycler()
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
    }

    // 메모리 누수 방지를 위해 Destroy시 콜백을 비활성화
    override fun onDestroy() {
        super.onDestroy()
        backPressedCallback.isEnabled = false
    }

    private fun backToHomeActivity() {
        startActivity(
            Intent(this, HomeActivity::class.java)
                .putExtra("uid", uid)
        )
        finish()
    }

    private fun initProperty() {
        uid = intent.getStringExtra("uid").toString()
    }

    private fun initView() {
        search = binding.userSearch
        backButton = binding.newChatBack
        userRecycler = binding.userRecycler
    }

    // 유저 리스트 리사이클러 뷰를 초기화하는 메소드
    private fun setRecycler() {
        userRecycler.layoutManager = LinearLayoutManager(this)
        val decoration = DividerItemDecoration(applicationContext, DividerItemDecoration.VERTICAL)
        userRecycler.addItemDecoration(decoration)
        userRecycler.adapter = UserSearchRecyclerAdapter(uid)
    }

    // 뒤로가기 버튼 클릭시 홈 화면으로 돌아감
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            backToHomeActivity()
            finish()
        }
    }
}