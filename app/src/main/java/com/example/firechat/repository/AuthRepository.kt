package com.example.firechat.repository

import android.util.Log
import com.example.firechat.data.User
import com.example.firechat.viewModel.LoginResultCallBack
import com.example.firechat.viewModel.RegisterResultCallback
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.database.FirebaseDatabase

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance()

    // 로그인 메소드
    fun tryLogin(id : String, pw : String, resultCallBack : LoginResultCallBack){
        auth.signInWithEmailAndPassword(id, pw)
            .addOnCompleteListener { task ->
            // 로그인 시도 성공 여부에 따라 분기
                if(task.isSuccessful){
                    // 로그인 성공시 현재 유저의 uid와 함께 성공 코드를 반환
                    resultCallBack.onLoginSuccess("login success", auth.currentUser!!.uid)
                } else {
                    // 로그인 실패시 발생한 문제에 따라서 에러 코드를 반환
                    Log.d("Login Debug", "${task.exception?.message}")
                    val error = when(task.exception){
                        is FirebaseNetworkException -> "login network error"
                        else -> "login id/pw mismatch"
                    }
                    resultCallBack.onLoginFailed(error)
                }
        }
    }

    // 회원가입 메소드, addOnCompleteListener를 통해 task의 성공 여부에 따라 분기
    fun attemptRegister(name : String, email : String, pw : String, registerResultCallback: RegisterResultCallback) {
        auth.createUserWithEmailAndPassword(email, pw).addOnCompleteListener { task ->
            if(task.isSuccessful){
                val user = auth.currentUser
                val uid = user?.uid.toString()
                val userData = User(name, email, uid)

                // 유저의 이름과 아이디(이메일)를 DB에 저장
                // 경로는 DB -> User -> UID(고유 아이디)
                db.getReference("User").child(uid).setValue(userData)

                // 회원가입 성공시
                registerResultCallback.returnRegisterResult("register success")
            } else {
                // 회원가입 실패시 발생한 문제에 따라서 에러 코드를 반환
                val error = when (task.exception) {
                    is FirebaseAuthWeakPasswordException -> "register weak password"
                    is FirebaseAuthUserCollisionException -> "register email already use"
                    is FirebaseAuthInvalidCredentialsException -> "register invalid email"
                    is FirebaseNetworkException -> "register network error"
                    else -> {
                        Log.d("Register Debug", "${task.exception?.message}")
                        ""
                    }
                }

                registerResultCallback.returnRegisterResult(error)
            }
        }
    }

    // 로그아웃 메소드
    fun logout(){
        auth.signOut()
    }
}