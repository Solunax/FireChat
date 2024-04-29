package com.example.firechat.view.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.firechat.R
import com.example.firechat.databinding.ChattingRoomDrawerListItemBinding
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

// 채팅방 드로어에 사용할 ListViewAdapter
class DrawerUserListViewAdapter(private val context: Context, private val userList: ArrayList<Pair<String, String>>) :
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

    private fun bind(binding: ChattingRoomDrawerListItemBinding, data: Pair<String, String>) {
        // 채팅방에 참여중인 유저의 이름 및 프로필 이미지 설정
        binding.listItemUserName.text = data.first
        getProfileImage(FirebaseStorage.getInstance().getReference("${data.second}/profile.jpg"), binding.profileImage)
    }

    // 드로어에 현재 유저의 프로필 이미지를 불러오는 메소드
    // 이미지를 원형으로 잘라서(RequestOptions().circleCrop()) 삽입
    private fun getProfileImage(reference: StorageReference, profile: ImageView) {
        Glide.with(context)
            .load(reference)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .placeholder(R.drawable.baseline_person_24)
            .apply(RequestOptions().circleCrop())
            .into(profile)
    }
}