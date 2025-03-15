package com.example.firechat.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.firechat.model.data.ChattingState
import com.example.firechat.model.data.CurrentUserData
import com.example.firechat.model.data.Message
import com.example.firechat.model.repository.ChattingRoomRepository

class ChattingViewModel : ViewModel() {
    private val repository = ChattingRoomRepository()

    private val _opponentOnlineState = MutableLiveData<Boolean>()
    val opponentOnlineState: LiveData<Boolean> get() = _opponentOnlineState

    private val _messages = MutableLiveData<List<Pair<String, Message>>>()
    val messages: LiveData<List<Pair<String, Message>>> get() = _messages

    init {
        repository.messageLiveData.observeForever { messageMap ->
            _messages.postValue(messageMap.toList())
        }
    }

    fun loadMessage(chatRoomKey: String) {
        repository.getChattingRoomMessage(chatRoomKey)
    }

    fun sendMessage(
        chatRoomKey: String,
        message: Message,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        repository.sendMessage(chatRoomKey, message, onSuccess, onFailure)
    }

    fun changeReadState(chatRoomKey: String, messageKey: String) {
        repository.changeReadState(chatRoomKey, messageKey)
    }

    fun deleteMessage(chatRoomKey: String, messageKey: String) {
        repository.deleteMessage(chatRoomKey, messageKey)
    }

    fun getOpponentUserOnlineState(chatRoomKey: String, opponentUid: String) {
        repository.getOpponentUserOnlineState(chatRoomKey, opponentUid) { state ->
            _opponentOnlineState.postValue(state)
        }
    }

    fun changeOnlineState(chatRoomKey: String, state: ChattingState) {
        repository.changeOnlineState(chatRoomKey, CurrentUserData.uid!!, state)
    }

    fun chattingRoomAvailableCheck(chatRoomKey: String) {
        repository.chattingRoomAvailableCheck(chatRoomKey)
    }

    fun removeListener(chatRoomKey: String) {
        repository.removeListener(chatRoomKey)
    }
}