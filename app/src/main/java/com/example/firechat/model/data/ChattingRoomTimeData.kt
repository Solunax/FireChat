package com.example.firechat.model.data

import java.text.SimpleDateFormat
import java.util.Date

// 채팅방 시간 데이터를 저장하는 객체
// 형식없는 문자열 형태로 DB에 저장된 데이터를 나눠서
// 년 월 일 시 분의 필드로 저장
class ChattingRoomTimeData(timeString: String) {
    val dateTime: Date
    val year: Int
    val month: Int
    val date: Int
    val hour: Int
    val minute: Int

    init {
        year = timeString.substring(0, 4).toInt()
        month = timeString.substring(4, 6).toInt()
        date = timeString.substring(6, 8).toInt()
        hour = timeString.substring(8, 10).toInt()
        minute = timeString.substring(10, 12).toInt()
        dateTime = SimpleDateFormat("yyyyMMddHHmm").parse(timeString)!!
    }
}