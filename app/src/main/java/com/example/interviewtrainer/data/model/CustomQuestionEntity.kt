package com.example.interviewtrainer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "custom_questions")
data class CustomQuestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val domain: String,
    val specialization: String,
    val role: String,
    val questionText: String,
    val referenceAnswer: String,
    val keywordsCsv: String,
    val hint: String = ""
) {
    fun toQuestionDto(): QuestionDto {
        val kwList = keywordsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return QuestionDto(
            id = 10000 + id,
            questionRo = questionText,
            questionEn = questionText,
            referenceAnswerRo = referenceAnswer,
            referenceAnswerEn = referenceAnswer,
            keywordsRo = kwList,
            keywordsEn = kwList,
            hintRo = hint.ifBlank { "Concentrează-te pe cuvintele cheie: $keywordsCsv" },
            hintEn = hint.ifBlank { "Focus on keywords: $keywordsCsv" },
            interviewerTipRo = "Întrebare adăugată în banca ta personală.",
            interviewerTipEn = "Question from your personal question bank."
        )
    }
}