package com.example.interviewtrainer.data.remote

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.interviewtrainer.data.model.InterviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InterviewDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInterview(interview: InterviewEntity)

    @Query("SELECT * FROM interviews ORDER BY timestamp DESC")
    fun getAllInterviews(): Flow<List<InterviewEntity>>
}