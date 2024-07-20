package com.example.firechat.view.activity

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.firechat.databinding.RegisterActivityBinding
import com.example.firechat.util.*
import com.example.firechat.viewModel.AuthViewModel

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: RegisterActivityBinding
    private lateinit var inputName: EditText
    private lateinit var inputEmail: EditText
    private lateinit var inputPw: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var buttonBackground: RelativeLayout
    private lateinit var statusText: TextView
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RegisterActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initProperty()
        initView()
        initListener()
    }

    // 필드 초기화 메소드
    private fun initProperty() {
        // ViewModel에서 회원가입 시도시 결과에 따라 Event를 발생시킴
        // 해당 observer는 ViewModel에서 발생한 Event 내용에 따라서 분기함
        viewModel.event.observe(this) { event ->
            progressBar.visibility = View.INVISIBLE
            event.getContentIfNotHandled()?.let { code ->
                when (code) {
                    "register success" -> {
                        statusText.text = "회원가입 성공"
                        showText(this, "회원가입에 성공했습니다.")
                        finish()
                    }

                    "register weak password" -> {
                        statusText.text = "회원가입"
                        showText(this, "비밀번호 강도가 너무 약합니다.")
                    }

                    "register email already use" -> {
                        statusText.text = "회원가입"
                        showText(this, "이미 사용중인 이메일 주소입니다.")
                    }

                    "register invalid email" -> {
                        statusText.text = "회원가입"
                        showText(this, "이메일 주소 형식에 맞지 않습니다.")
                    }

                    "register network error" -> {
                        statusText.text = "회원가입"
                        showText(this, "비밀번호 강도가 너무 약합니다.")
                    }
                }
            }
        }
    }

    // View 초기화 메소드
    private fun initView() {
        inputName = binding.name
        inputEmail = binding.email
        inputPw = binding.pw

        val progressButtonBinding = binding.buttonRegister
        progressBar = progressButtonBinding.progressBar
        statusText = progressButtonBinding.statusMessage
        statusText.text = "회원가입"
        buttonBackground = progressButtonBinding.progressBackground

    }

    private fun initListener() {
        buttonBackground.setOnClickListener {
            attemptRegister()
        }
    }

    // 회원가입 시도 메소드
    private fun attemptRegister() {
        val name = inputName.text.toString().trim()
        val email = inputEmail.text.toString().trim()
        val pw = inputPw.text.toString().trim()

        // 입력 정보에 이상이 없으면 ViewModel의 회원가입 시도 메소드 호출
        if (infoValidationCheck(name, email, pw)) {
            statusText.text = "회원가입 중..."
            progressBar.visibility = View.VISIBLE
            viewModel.attemptRegister(name, email, pw)
        }
    }

    // 회원 가입시 정보를 제대로 입력했는지 확인하기 위한 유효성 검사용 메소드
    // 조건을 충족하지 못할시 Toast Message를 사용자에게 표시함
    private fun infoValidationCheck(name: String, email: String, pw: String): Boolean {
        if (name.isBlank()) {
            inputName.requestFocus()
            showText(this, "이름이 입력되지 않았습니다.")
            return false
        }

        if (email.isBlank()) {
            inputEmail.requestFocus()
            showText(this, "이메일이 입력되지 않았습니다.")
            return false
        }

        if (pw.isBlank()) {
            inputPw.requestFocus()
            showText(this, "비밀번호가 입력되지 않았습니다.")
            return false
        }

        if (pw.length < 6) {
            inputPw.requestFocus()
            showText(this, "비밀번호는 6글자 이상으로 설정하세요.")
            return false
        }

        return true
    }
}