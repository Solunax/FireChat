package com.example.firechat.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.firechat.model.data.CurrentUserData
import com.example.firechat.model.data.User
import com.example.firechat.model.repository.NewChatRepository

class NewChatViewModel : ViewModel() {
    private val repository = NewChatRepository()

    private val _filterUserList = MutableLiveData<List<User>>()
    val filterUserList: LiveData<List<User>> get() = _filterUserList

    private var allUserList: List<User> = emptyList()

    fun getAllUsers() {
        repository.getAllUserList { users ->
            allUserList = users.filter { it.uid != CurrentUserData.uid }
            _filterUserList.postValue(allUserList)
        }
    }

    // 초기 상태 즉, 아무것도 입력되지 않은 상태일 때 모든 유저의 목록을 표시
    // 사용자가 글자 입력 시, 사용자 정보 배열에서 해당 글자가 포함돼 있는지 확인 후
    // 해당 글자가 포함된 경우로 필터링하여 결과를 표시함
    fun searchUser(query: String) {
        if (query.isBlank()) {
            _filterUserList.postValue(allUserList)
        } else {
            val result = allUserList.filter { it.name?.contains(query, ignoreCase = true) == true }
            _filterUserList.postValue(result)
        }
    }

    // 채팅방을 새롭게 생성하는 메소드
    // 사용자의 정보와 상대방의 정보를 바탕으로 채팅방을 구성함
    // 채팅방 구성시 만약 이미 생성된 채팅방이 존재하면
    // 채팅방을 생성하지 않고 기존 채팅방으로 이동함
    fun createChattingRoom(opponentUser: User, result: (Pair<String, Boolean>) -> Unit) {
        repository.createChattingRoom(opponentUser, result)
    }
}