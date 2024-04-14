package com.example.firechat.view.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import com.example.firechat.databinding.LoadingDialogBinding

class LoadingDialog(context: Context): Dialog(context){
    private lateinit var binding: LoadingDialogBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoadingDialogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 로딩 다이얼로그를 사용자 임의로 취소할 수 없게 설정함
        setCancelable(false)
        // 로딩 다이얼로그의 배경을 투명으로 설정
        window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }
}