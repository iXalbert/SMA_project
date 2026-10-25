package com.example.interviewtrainer.data.model

enum class MessageRole {
    USER,
    INTERVIEWER,
    HINT
}

data class ChatMessage(
    val text: String,
    val role: MessageRole,
    val questionData: QuestionDto? = null
)