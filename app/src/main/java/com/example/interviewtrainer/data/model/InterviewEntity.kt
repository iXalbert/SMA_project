package com.example.interviewtrainer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "interviews")
data class InterviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    val role: String,
    val level: String,
    val score: Int,
    val summary: String,
    val timestamp: Long = System.currentTimeMillis(),
    val detailsJson: String = ""
) {
    fun toQuestionReviews(): List<QuestionReview> {
        if (detailsJson.isBlank()) return emptyList()
        return try {
            val array = JSONArray(detailsJson)
            List(array.length()) { i ->
                val obj = array.getJSONObject(i)
                QuestionReview(
                    questionId = obj.optInt("questionId", i),
                    questionText = obj.optString("questionText"),
                    userResponse = obj.optString("userResponse"),
                    referenceAnswer = obj.optString("referenceAnswer"),
                    matchedKeywords = obj.optJSONArray("matchedKeywords").toStringList(),
                    missedKeywords = obj.optJSONArray("missedKeywords").toStringList(),
                    interviewerTip = obj.optString("interviewerTip").ifBlank { null }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

fun List<QuestionReview>.toDetailsJson(): String {
    val array = JSONArray()
    forEach { review ->
        val obj = JSONObject().apply {
            put("questionId", review.questionId)
            put("questionText", review.questionText)
            put("userResponse", review.userResponse)
            put("referenceAnswer", review.referenceAnswer)
            put("matchedKeywords", JSONArray(review.matchedKeywords))
            put("missedKeywords", JSONArray(review.missedKeywords))
            put("interviewerTip", review.interviewerTip ?: "")
        }
        array.put(obj)
    }
    return array.toString()
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return List(length()) { i -> optString(i) }.filter { it.isNotBlank() }
}