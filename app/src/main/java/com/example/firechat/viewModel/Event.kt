package com.example.firechat.viewModel

// 이벤트 핸들링을 위한 이벤트 객체
open class Event<out T>(private val content: T) {
    private var hasBeenHandle = false
        private set

    // 이벤트 발생 후 이 이벤트가 처리되었는지 확인함
    // 만약 처리되었다면 null을 반환함
    // 처리되지 않았다면 hasBeenHandle의 값을 true로 변경하고 content를 반환함
    // 해당 앱에선 Firebase 사용중 발생하는 오류 핸들링에 이 객체를 사용하고 있음
    // content는 오류 코드(String)를 사용
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandle) {
            null
        } else {
            hasBeenHandle = true
            content
        }
    }
}