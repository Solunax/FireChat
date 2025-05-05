package com.example.firechat.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.firechat.model.data.ChattingListData
import com.example.firechat.model.data.CurrentUserData
import com.example.firechat.model.repository.ChattingListRepository

class ChattingListViewModel : ViewModel() {
    private val repository = ChattingListRepository()

    private val _chattingRoomData = MutableLiveData<List<ChattingListData>>()
    val chattingRoomData: LiveData<List<ChattingListData>> get() = _chattingRoomData

    fun getChattingRoomsData() {
        repository.getChattingRoomsData { data ->
            if (data.isEmpty()) {
                _chattingRoomData.postValue(emptyList())
            } else {
                val tempData = mutableListOf<ChattingListData>()
                var count = 0

                for ((key, chattingRoom) in data) {
                    repository.getOpponentUserData(chattingRoom.users?.keys?.firstOrNull { it != CurrentUserData.uid }
                        ?: "") { opponentUser ->
                        tempData.add(ChattingListData(key, opponentUser, chattingRoom))
                        count++

                        if (count == data.size) {
                            _chattingRoomData.postValue(tempData)
                        }
                    }
                }
            }
        }
    }

    fun quitChattingRoom(key: String) {
        repository.quitChattingRoom(key)
    }

    fun removeListener() {
        repository.removeListener()
    }
}