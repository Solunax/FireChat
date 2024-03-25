package com.example.firechat.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.example.firechat.databinding.ChattingRoomDrawerListItemBinding

class DrawerUserListViewAdapter(private val userList: ArrayList<String>) :
    BaseAdapter(){

    override fun getCount(): Int {
        return userList.size
    }

    override fun getItem(p0: Int): Any {
        return userList[p0]
    }

    override fun getItemId(p0: Int): Long {
        return p0.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding = if (convertView == null) {
            val tempBinding = ChattingRoomDrawerListItemBinding.inflate(LayoutInflater.from(parent.context))
            tempBinding.root.tag = tempBinding
            tempBinding
        } else {
            convertView.tag
        } as ChattingRoomDrawerListItemBinding

        bind(binding, userList[position])

        return binding.root
    }

    private fun bind(binding: ChattingRoomDrawerListItemBinding, data: String) {
        binding.listItemUserName.text = data
    }
}