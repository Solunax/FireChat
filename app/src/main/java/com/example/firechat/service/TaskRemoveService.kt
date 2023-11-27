package com.example.firechat.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.google.firebase.auth.FirebaseAuth

class TaskRemoveService : Service() {
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    // 강제종료시 auth 로그아웃
    override fun onTaskRemoved(rootIntent: Intent?) {
        FirebaseAuth.getInstance().signOut()
    }
}