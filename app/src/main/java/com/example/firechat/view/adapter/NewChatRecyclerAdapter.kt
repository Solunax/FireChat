package com.example.firechat.view.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.firechat.R
import com.example.firechat.databinding.NewChatUserSearchResultItemBinding
import com.example.firechat.model.data.User
import com.example.firechat.view.dialog.LoadingDialog

// ListAdapter를 사용하면 AsyncDiffer(스레드 처리)를 더 편하게 사용할 수 있음
// submitList로 데이터를 갱신, currentList로 현재 데이터를 참조할 수 있음
class NewChatRecyclerAdapter(
    private val onUserSelected: (User) -> Unit
) : ListAdapter<User, NewChatRecyclerAdapter.ViewHolder>(DataComparator) {
    private lateinit var context: Context
    private lateinit var loadingDialog: LoadingDialog

    // 데이터 셋을 받아 차이를 계산
    // areItemsTheSame은 두 객체가 동일객체인지 확인
    // areContentsTheSame은 두 아이템이 동일한 데이터를 가지는지 확인함
    companion object DataComparator : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }

        // 사용자 UID 비교
        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.uid == newItem.uid
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        context = parent.context
        loadingDialog = LoadingDialog(context)
        val view = LayoutInflater.from(context)
            .inflate(R.layout.new_chat_user_search_result_item, parent, false)
        return ViewHolder(NewChatUserSearchResultItemBinding.bind(view))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemCount(): Int {
        return currentList.size
    }

    inner class ViewHolder(private val binding: NewChatUserSearchResultItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.searchName.text = user.name
            binding.searEmail.text = user.email
            binding.background.setOnClickListener {
                onUserSelected(user)
            }
        }
    }
}