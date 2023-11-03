package com.example.firechat

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.firechat.databinding.LoginActivityBinding
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class LoginActivity : AppCompatActivity() {
    private lateinit var binding : LoginActivityBinding
    private lateinit var inputEmail : EditText
    private lateinit var inputPw : EditText
    private lateinit var loginButton : Button
    private lateinit var register : TextView
    private lateinit var auth : FirebaseAuth
    private lateinit var sharePreference : SharedPreferences

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

    // 필드 초기화 메소드
    // Firebase auth를 로그인 사용해야 하기 때문에 인스턴스를 불러옴
    // sharedPreference로 마지막으로 성공한 로그인 유저의 ID와 PW를 자동으로 입력
    private fun initProperty() {
        auth = FirebaseAuth.getInstance()
        sharePreference = getSharedPreferences("loginData", MODE_PRIVATE)
    }

    // View 초기화 메소드
    private fun initView() {
        inputEmail = binding.email
        inputPw = binding.pw
        loginButton = binding.buttonLogin
        register = binding.textRegister

        inputEmail.setText(sharePreference.getString("email", ""))
        inputPw.setText(sharePreference.getString("pw", ""))
    }

    private fun attemptLogin() {
        val email = inputEmail.text.toString().trim()
        val pw = inputPw.text.toString().trim()

        if(infoValidationCheck(email, pw)){
            auth.signInWithEmailAndPassword(email, pw).addOnCompleteListener { task ->
                // 로그인 시도 성공 여부에 따라 분기
                if(task.isSuccessful){
                    // 성공시 메인 화면으로 넘어가는 메소드 실행
                    updateUI(auth.currentUser)
                } else {
                    val sb = StringBuilder("로그인에 실패했습니다.")

                    // 로그인 실패시 발생한 문제에 따라서 사용자에게 메시지를 보여줌
                    Log.d("TASK", "${task.exception?.message}")
                    when(task.exception){
                        is FirebaseNetworkException -> sb.append("\n네트워크 연결에 실패했습니다.")
                        else -> sb.append("\n아이디나 비밀번호가 틀렸습니다.")
                    }

                    Toast.makeText(this, sb.toString(), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 로그인 성공시 화면 이동 메소드
    // auth에 user 데이터가 null이 아니면 다음 메인 화면으로 전환
    private fun updateUI(user : FirebaseUser?) {
        user?.let {
            val editPreference = sharePreference.edit()
            editPreference.putString("email", inputEmail.text.toString().trim())
            editPreference.putString("pw", inputPw.text.toString().trim())
            editPreference.apply()

            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
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