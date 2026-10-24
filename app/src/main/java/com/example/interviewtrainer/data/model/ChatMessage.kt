package com.example.interviewtrainer.data.model

enum class MessageRole {
    USER,
    INTERVIEWER
}

data class QuestionData(
    val questionText: String,
    val referenceAnswer: String,
    val keywords: List<String>
)

data class ChatMessage(
    val text: String,
    val role: MessageRole,
    val questionData: QuestionData? = null,
    val timestamp: Long = System.currentTimeMillis()
)