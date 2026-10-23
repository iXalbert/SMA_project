package com.example.interviewtrainer.data.model

enum class MessageRole {
    USER,
    INTERVIEWER
}

data class ChatMessage(
    val text: String,
    val role: MessageRole,
    val timestamp: Long = System.currentTimeMillis()
)