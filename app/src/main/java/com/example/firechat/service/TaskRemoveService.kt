package com.example.firechat.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.google.firebase.auth.FirebaseAuth

// Application에 서비스를 연결하여 앱이 강제 종료되었을 때 작업을 수행함
class TaskRemoveService : Service() {
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    // 강제종료시 auth 로그아웃
    override fun onTaskRemoved(rootIntent: Intent?) {
        FirebaseAuth.getInstance().signOut()
    }
}