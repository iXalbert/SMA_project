package com.example.interviewtrainer.data.model

data class QuestionReview(
    val questionId: Int,
    val questionText: String,
    val userResponse: String,
    val referenceAnswer: String,
    val matchedKeywords: List<String>,
    val missedKeywords: List<String>,
    val hintUsed: Boolean = false,
    val interviewerTip: String? = null
)

data class InterviewReport(
    val score: Int,
    val summary: String,
    val strengths: List<String>,
    val improvements: List<String>,
    val reviews: List<QuestionReview> = emptyList()
)