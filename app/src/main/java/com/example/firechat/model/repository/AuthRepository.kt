package com.example.firechat.model.repository

import android.util.Log
import com.example.firechat.model.data.CurrentUserData
import com.example.firechat.model.data.User
import com.example.firechat.viewModel.AuthError
import com.example.firechat.viewModel.AuthResultCallback
import com.example.firechat.viewModel.AuthSuccessResult
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.lang.Exception

class AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance()

    // 로그인 메소드
    fun tryLogin(id: String, pw: String, callback: AuthResultCallback) {
        auth.signInWithEmailAndPassword(id, pw)
            .addOnCompleteListener { task ->
                // 로그인 시도 성공 여부에 따라 분기
                if (task.isSuccessful) {
                    db.getReference("User")
                        .child(auth.currentUser!!.uid)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                // Singleton 객체에 User Name 값 설정
                                // 이 Name 변수는 드로어에서 사용됨
                                CurrentUserData.userName = snapshot.child("name").value.toString()

                                // 로그인 성공시 현재 유저의 uid와 함께 성공 코드를 반환
                                callback.onSuccess(
                                    AuthSuccessResult(
                                        "login success",
                                        auth.currentUser?.uid
                                    )
                                )
                            }

                            override fun onCancelled(error: DatabaseError) {
                            }
                        })
                } else {
                    // 로그인 실패시 발생한 문제에 따라서 에러 코드를 반환
                    callback.onFailure(getLoginError(task.exception))
                }
            }
    }

    // 회원가입 메소드, addOnCompleteListener를 통해 task의 성공 여부에 따라 분기
    fun attemptRegister(
        name: String,
        email: String,
        pw: String,
        callback: AuthResultCallback
    ) {
        auth.createUserWithEmailAndPassword(email, pw).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                val uid = user?.uid.toString()
                val userData = User(name, email, uid)

                // 유저의 이름과 아이디(이메일)를 DB에 저장
                // 경로는 DB -> User -> UID(고유 아이디)
                db.getReference("User").child(uid).setValue(userData)
                // 회원가입 성공시
                callback.onSuccess(AuthSuccessResult("register success"))
            } else {
                // 회원가입 실패시 발생한 문제에 따라서 에러 코드를 반환
                callback.onFailure(getRegisterError(task.exception))
            }
        }
    }

    private fun getLoginError(exception: Exception?): AuthError {
        return when (exception) {
            is FirebaseNetworkException -> AuthError("network error", "login network error")
            else -> AuthError("invalid", "login id/pw mismatch")
        }
    }

    private fun getRegisterError(exception: Exception?): AuthError {
        return when (exception) {
            is FirebaseAuthWeakPasswordException -> AuthError(
                "weak_password",
                "Password is too weak"
            )

            is FirebaseAuthUserCollisionException -> AuthError(
                "email_already_in_use",
                "Email is already in use"
            )

            is FirebaseAuthInvalidCredentialsException -> AuthError(
                "invalid_email",
                "Invalid email format"
            )

            is FirebaseNetworkException -> AuthError("network_error", "Network error occurred")
            else -> {
                Log.d("Register Debug", "${exception?.message}")
                AuthError("unknown_error", "An unknown error occurred")
            }
        }
    }

    // 로그아웃 메소드
    fun logout() {
        auth.signOut()
    }
}