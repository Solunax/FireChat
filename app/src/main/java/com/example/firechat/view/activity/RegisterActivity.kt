package com.example.firechat.view.activity

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.firechat.databinding.RegisterActivityBinding
import com.example.firechat.viewModel.AuthViewModel

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: RegisterActivityBinding
    private lateinit var inputName: EditText
    private lateinit var inputEmail: EditText
    private lateinit var inputPw: EditText
    private lateinit var registerButton: Button
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RegisterActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initProperty()
        initView()

        registerButton.setOnClickListener {
            attemptRegister()
        }
    }

    // 필드 초기화 메소드
    // Firebase auth를 회원가입에 사용해야 하기 때문에 인스턴스를 불러옴
    private fun initProperty() {
        viewModel.event.observe(this) { event ->
            event.getContentIfNotHandled()?.let { code ->
                when (code) {
                    "register success" -> {
                        Log.d("activity", "Y")
                        Toast.makeText(this, "회원가입에 성공했습니다.", Toast.LENGTH_SHORT).show()
                        finish()
                    }

                    "register weak password" -> Toast.makeText(
                        this,
                        "비밀번호 강도가 너무 약합니다.",
                        Toast.LENGTH_SHORT
                    ).show()

                    "register email already use" -> Toast.makeText(
                        this,
                        "이미 사용중인 이메일 주소입니다.",
                        Toast.LENGTH_SHORT
                    ).show()

                    "register invalid email" -> Toast.makeText(
                        this,
                        "이메일 주소 형식에 맞지 않습니다.",
                        Toast.LENGTH_SHORT
                    ).show()

                    "register network error" -> Toast.makeText(
                        this,
                        "비밀번호 강도가 너무 약합니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // View 초기화 메소드
    private fun initView() {
        inputName = binding.name
        inputEmail = binding.email
        inputPw = binding.pw
        registerButton = binding.buttonRegister
    }

    // 회원가입 시도 메소드
    private fun attemptRegister() {
        val name = inputName.text.toString().trim()
        val email = inputEmail.text.toString().trim()
        val pw = inputPw.text.toString().trim()

        if (infoValidationCheck(name, email, pw))
            viewModel.attemptRegister(name, email, pw)
    }

    // 회원 가입시 정보를 제대로 입력했는지 확인하기 위한 유효성 검사용 메소드
    // 조건을 충족하지 못할시 Toast Message를 사용자에게 표시함
    private fun infoValidationCheck(name: String, email: String, pw: String): Boolean {
        if (name.isBlank()) {
            inputName.requestFocus()
            Toast.makeText(this, "이름이 입력되지 않았습니다.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (email.isBlank()) {
            inputEmail.requestFocus()
            Toast.makeText(this, "이메일이 입력되지 않았습니다.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (pw.isBlank()) {
            inputPw.requestFocus()
            Toast.makeText(this, "비밀번호가 입력되지 않았습니다.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (pw.length < 6) {
            inputPw.requestFocus()
            Toast.makeText(this, "비밀번호는 6글자 이상으로 설정하세요.", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }
}