package com.example.firechat

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firechat.databinding.NewChatActivityBinding
import com.google.firebase.database.FirebaseDatabase

class NewChatActivity : AppCompatActivity() {
    private lateinit var binding : NewChatActivityBinding
    private lateinit var backButton : ImageButton
    private lateinit var search : EditText
    private lateinit var userRecycler : RecyclerView
    private lateinit var db : FirebaseDatabase
    private var lastBackPressedTime = 0L

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
    }

    // 메모리 누수 방지를 위해 Destroy시 콜백을 비활성화
    override fun onDestroy() {
        super.onDestroy()
        backPressedCallback.isEnabled = false
    }

    private fun backToHomeActivity() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun initProperty() {
        db = FirebaseDatabase.getInstance()
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
        userRecycler.adapter = UserSearchRecyclerAdapter(this)
    }

    // 뒤로가기 버튼을 두번 클릭시 앱을 종료하는 기능을 수행하는 콜백
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if(System.currentTimeMillis() > lastBackPressedTime + 2000) {
                lastBackPressedTime = System.currentTimeMillis()
                Toast.makeText(this@NewChatActivity, "뒤로가기 버튼을 한번 더 누르면 앱이 종료됩니다.", Toast.LENGTH_SHORT).show()
            } else {
                backToHomeActivity()
                finish()
            }
        }
    }
}