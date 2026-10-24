package com.example.interviewtrainer.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "interviews")
data class InterviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    val role: String,
    val level: String,
    val score: Int,
    val summary: String,
    val timestamp: Long = System.currentTimeMillis()
)