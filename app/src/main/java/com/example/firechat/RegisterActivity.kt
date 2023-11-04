package com.example.firechat

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.firechat.data.User
import com.example.firechat.databinding.RegisterActivityBinding
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding : RegisterActivityBinding
    private lateinit var inputName : EditText
    private lateinit var inputEmail : EditText
    private lateinit var inputPw : EditText
    private lateinit var registerButton : Button
    private lateinit var auth : FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RegisterActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initView()
        initProperty()

        registerButton.setOnClickListener {
            attemptRegister()
        }
    }

    // 필드 초기화 메소드
    // Firebase auth를 회원가입에 사용해야 하기 때문에 인스턴스를 불러옴
    private fun initProperty() {
        auth = FirebaseAuth.getInstance()
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

        if (infoValidationCheck(name, email, pw)) {
            // auth의 회원가입 메소드, addOnCompleteListener를 통해 task의 성공 여부에 따라 분기
            auth.createUserWithEmailAndPassword(email, pw).addOnCompleteListener { task ->
                if(task.isSuccessful){
                    val user = auth.currentUser
                    val uid = user?.uid.toString()
                    val userData = User(name, email, uid)

                    // 유저의 이름과 아이디를 DB에 저장
                    // 경로는 DB -> User -> UID(고유 아이디)
                    FirebaseDatabase.getInstance().getReference("User").child(uid)
                        .setValue(userData)

                    // 회원가입 성공시 현재 Activity를 종료함
                    Toast.makeText(this, "회원가입에 성공했습니다.", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val sb = StringBuilder("회원가입에 실패했습니다.")

                    // 회원 가입 실패시 Exception의 error code에 따라서 사용자에게 메시지를 보여줌
                    when (task.exception) {
                        is FirebaseAuthWeakPasswordException -> sb.append("\n비밀번호가 너무 약합니다.")
                        is FirebaseAuthUserCollisionException -> sb.append("\n이미 사용중인 이메일 입니다.")
                        is FirebaseAuthInvalidCredentialsException -> sb.append("\n유효하지 않은 이메일 형식입니다")
                        is FirebaseNetworkException -> sb.append("\n네트워크 연결에 실패했습니다.")
                        else -> sb.append("\n${task.exception?.message}")
                    }

                    Toast.makeText(this, sb.toString(), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 회원 가입시 정보를 제대로 입력했는지 확인하기 위한 유효성 검사용 메소드
    // 조건을 충족하지 못할시 Toast Message를 사용자에게 표시함
    private fun infoValidationCheck(name : String, email : String, pw : String) : Boolean {
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

        if(pw.isBlank()){
            inputPw.requestFocus()
            Toast.makeText(this, "비밀번호가 입력되지 않았습니다.", Toast.LENGTH_SHORT).show()
            return false
        }

        if(pw.length < 6){
            inputPw.requestFocus()
            Toast.makeText(this, "비밀번호는 6글자 이상으로 설정하세요.", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }
}