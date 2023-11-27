package com.example.firechat.model.data

import java.io.Serializable

data class ChattingState(
    val joinState: Boolean = false,
    val onlineState: Boolean = false
) : Serializable
