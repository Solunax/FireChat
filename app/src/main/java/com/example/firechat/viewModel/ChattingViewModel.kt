package com.example.firechat.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firechat.model.data.ChattingState
import com.example.firechat.model.data.CurrentUserData
import com.example.firechat.model.data.Message
import com.example.firechat.model.repository.ChattingRoomRepository
import kotlinx.coroutines.launch

class ChattingViewModel: ViewModel() {
    private val repository = ChattingRoomRepository()

    private val _chatRoomKey = MutableLiveData<String>()
    val chatRoomKey: LiveData<String> get() = _chatRoomKey

    private val _opponentOnlineState = MutableLiveData<Boolean>()
    val opponentOnlineState: LiveData<Boolean> get() = _opponentOnlineState

    fun setChattingRoomKey(uid: String, opponentUid: String) {
        viewModelScope.launch {
            repository.getChattingRoomKey(uid, opponentUid) { key ->
                _chatRoomKey.postValue(key!!)
            }
        }
    }

    fun getOpponentUserOnlineState(chatRoomKey: String, opponentUid: String) {
        repository.getOpponentUserOnlineState(chatRoomKey, opponentUid) { state ->
            _opponentOnlineState.postValue(state)
        }
    }

    fun removeListener() {
        repository.removeListener()
    }

    fun changeOnlineState(chatRoomKey: String, state: ChattingState) {
        repository.changeOnlineState(chatRoomKey, CurrentUserData.uid!!, state)
    }

    fun sendMessage(chatRoomKey: String, message: Message, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        repository.sendMessage(chatRoomKey, message, onSuccess, onFailure)
    }

    fun chattingRoomAvailableCheck(chatRoomKey: String) {
        repository.chattingRoomAvailableCheck(chatRoomKey)
    }
}