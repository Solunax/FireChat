package com.example.firechat

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import com.example.firechat.databinding.LoginActivityBinding
import com.example.firechat.viewModel.AuthViewModel

class LoginActivity : AppCompatActivity() {
    private lateinit var binding : LoginActivityBinding
    private lateinit var inputEmail : EditText
    private lateinit var inputPw : EditText
    private lateinit var loginButton : Button
    private lateinit var register : TextView
    private lateinit var sharePreference : SharedPreferences
    private val viewModel : AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initProperty()
        initView()

        loginButton.setOnClickListener {
            attemptLogin()
        }

        register.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    // Firebase auth를 로그인 사용해야 하기 때문에 인스턴스를 불러옴
    // sharedPreference로 마지막으로 성공한 로그인 유저의 ID와 PW를 자동으로 입력
    private fun initProperty() {
        sharePreference = getSharedPreferences("loginData", MODE_PRIVATE)

        // ViewModel에서 로그인 시도시 결과에 따라 Event를 발생시킴
        // success면 홈화면으로, 그 외에는 오류 메세지를 사용자에게 표시
        viewModel.event.observe(this) { event ->
            event.getContentIfNotHandled()?.let { code ->
                when (code) {
                    "login success" -> updateUI()
                    "login id/pw mismatch" -> Toast.makeText(this, "아이디 혹은 비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                    "login network error" -> Toast.makeText(this, "네트워크 상태를 확인해 주세요", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun initView() {
        inputEmail = binding.email
        inputPw = binding.pw
        loginButton = binding.buttonLogin
        register = binding.textRegister

        inputEmail.setText(sharePreference.getString("email", ""))
        inputPw.setText(sharePreference.getString("pw", ""))
    }

    // 로그인 시도 메소드
    // ViewMdoel의 로그인 시도 메소드를 실행
    private fun attemptLogin() {
        val email = inputEmail.text.toString().trim()
        val pw = inputPw.text.toString().trim()

        if(infoValidationCheck(email, pw))
            viewModel.tryLogin(email, pw)
    }

    // 로그인 성공시 화면 이동 메소드
    private fun updateUI() {
        val editPreference = sharePreference.edit()

        editPreference.putString("email", inputEmail.text.toString().trim())
        editPreference.putString("pw", inputPw.text.toString().trim())
        editPreference.apply()

        // UID를 인텐트에 추가하여 넘겨줌
        startActivity(
            Intent(this, HomeActivity::class.java)
            .putExtra("uid", viewModel.currentUserUID)
        )
        finish()
    }

    // 로그인 정보를 제대로 입력했는지 확인하기 위한 유효성 검사용 함수
    // 조건을 충족하지 못할시 Toast Message를 사용자에게 표시함
    private fun infoValidationCheck(email : String, pw : String) : Boolean {
        if (email.isBlank()) {
            inputEmail.requestFocus()
            Toast.makeText(this, "아이디가 입력되지 않았습니다.", Toast.LENGTH_SHORT).show()
            return false
        }

        if(pw.isBlank()){
            inputPw.requestFocus()
            Toast.makeText(this, "비밀번호가 입력되지 않았습니다.", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }
}