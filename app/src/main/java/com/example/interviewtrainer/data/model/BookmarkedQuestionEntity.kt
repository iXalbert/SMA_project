package com.example.interviewtrainer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarked_questions")
data class BookmarkedQuestionEntity(
    @PrimaryKey val questionId: Int,
    val domain: String,
    val specialization: String,
    val role: String,
    val questionRo: String,
    val questionEn: String,
    val referenceAnswerRo: String,
    val referenceAnswerEn: String,
    val hintRo: String,
    val hintEn: String
)