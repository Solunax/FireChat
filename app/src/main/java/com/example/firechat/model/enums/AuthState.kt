package com.example.firechat.model.enums

enum class AuthState(val statusText: String, val message: String) {
    // 로그인
    LOGIN_SUCCESS("로그인 성공", "로그인에 성공했습니다."),
    LOGIN_NO_ACCOUNT("로그인", "해당하는 아이디를 찾을 수 없습니다."),
    LOGIN_INVALID_INFORMATION("로그인", "아이디 혹은 비밀번호가 일치하지 않습니다."),
    LOGIN_NETWORK_ERROR("로그인", "네트워크 상태를 확인해 주세요"),
    LOGIN_UNKNOWN_ERROR("로그인", "알 수 없는 오류로 인해 로그인 할 수 없습니다. 관리자에게 문의해주세요."),

    // 회원가입
    REGISTER_SUCCESS("회원가입 성공", "회원가입에 성공했습니다."),
    REGISTER_WEAK_PASSWORD("회원가입", "비밀번호 강도가 너무 약합니다."),
    REGISTER_EMAIL_IN_USE("회원가입", "이미 사용중인 이메일 주소입니다."),
    REGISTER_INVALID_EMAIL_FORMAT("회원가입", "이메일 주소 형식에 맞지 않습니다."),
    REGISTER_TOO_MANY_ATTEMPTS("회원가입", "회원가입 시도 횟수가 너무 많습니다. 잠시 후 시도해주세요."),
    REGISTER_NETWORK_ERROR("회원가입", "네트워크 상태를 확인해 주세요"),
    REGISTER_UNKNOWN_ERROR("회원가입", "알 수 없는 오류가 발생했습니다. 관리자에게 문의해주세요.")
}