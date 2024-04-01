package com.example.firechat.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.example.firechat.databinding.ChattingRoomDrawerListItemBinding

// 채팅방 드로어에 사용할 ListViewAdapter
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
        // 채팅방에 참여중인 유저의 이름값 설정
        binding.listItemUserName.text = data
    }
}