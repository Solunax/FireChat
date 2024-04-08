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

        setCancelable(false)
        window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }
}