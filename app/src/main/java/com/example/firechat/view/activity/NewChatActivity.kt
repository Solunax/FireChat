package com.example.firechat.view.activity

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
import com.example.firechat.model.data.CurrentUserData
import com.example.firechat.view.adapter.UserSearchRecyclerAdapter

class NewChatActivity : AppCompatActivity() {
    private lateinit var binding: NewChatActivityBinding
    private lateinit var backButton: ImageButton
    private lateinit var search: EditText
    private lateinit var userRecycler: RecyclerView
    private lateinit var userSearchAdapter: UserSearchRecyclerAdapter
    private lateinit var uid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = NewChatActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initProperty()
        initView()
        initListener()
        setRecycler()

        onBackPressedDispatcher.addCallback(this, backPressedCallback)
    }

    // 메모리 누수 방지를 위해 Destroy시 콜백을 비활성화
    override fun onDestroy() {
        super.onDestroy()
        backPressedCallback.isEnabled = false
        userSearchAdapter.detachDatabaseListener()
    }

    // 현재 사용자 정보를 가지고 있는 Singleton 객체에서 uid를 가져옴
    private fun initProperty() {
        uid = CurrentUserData.uid!!
    }

    private fun initView() {
        search = binding.userSearch
        backButton = binding.newChatBack
        userRecycler = binding.userRecycler
    }

    private fun initListener() {
        backButton.setOnClickListener {
            finish()
        }

        // 사용자 검색창에서 입력에 따라 검색 결과를 표현하기 위한 리스너
        // 사용자가 이름을 입력한 후 현재 가입자 목록을 가진 리사이클러 뷰에서 이름을 검색함
        // 검색 결과중 사용자가 입력한 글자가 포함되는 사람의 목록을 표시
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
    }

    // 유저 리스트 리사이클러 뷰를 초기화하는 메소드
    private fun setRecycler() {
        userRecycler.layoutManager = LinearLayoutManager(this)
        userSearchAdapter = UserSearchRecyclerAdapter()
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