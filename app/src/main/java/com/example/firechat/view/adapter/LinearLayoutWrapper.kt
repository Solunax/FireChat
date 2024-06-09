package com.example.firechat.view.adapter

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager

// 채팅방 내부 Recycler View를 메소드로 스크롤할 때 발생하는 오류를 제거하기 위한 클래스
class LinearLayoutWrapper(context: Context?) : LinearLayoutManager(context) {
    override fun supportsPredictiveItemAnimations(): Boolean {
        return false
    }
}