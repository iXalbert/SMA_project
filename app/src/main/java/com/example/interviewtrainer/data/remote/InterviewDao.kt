package com.example.interviewtrainer.data.remote

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.interviewtrainer.data.model.BookmarkedQuestionEntity
import com.example.interviewtrainer.data.model.InterviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InterviewDao {

    // CREATE
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInterview(interview: InterviewEntity)

    // READ
    @Query("SELECT * FROM interviews ORDER BY timestamp DESC")
    fun getAllInterviews(): Flow<List<InterviewEntity>>

    // UPDATE
    @Update
    suspend fun updateInterview(interview: InterviewEntity)

    // DELETE
    @Delete
    suspend fun deleteInterview(interview: InterviewEntity)

    @Query("SELECT * FROM bookmarked_questions")
    fun getAllBookmarks(): Flow<List<BookmarkedQuestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkedQuestionEntity)

    @Query("DELETE FROM bookmarked_questions WHERE questionId = :questionId")
    suspend fun deleteBookmark(questionId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarked_questions WHERE questionId = :questionId)")
    suspend fun isBookmarked(questionId: Int): Boolean
}