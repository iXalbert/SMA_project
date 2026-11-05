package com.example.interviewtrainer.data.remote

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.interviewtrainer.data.model.BookmarkedQuestionEntity
import com.example.interviewtrainer.data.model.InterviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InterviewDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInterview(interview: InterviewEntity)

    @Query("SELECT * FROM interviews ORDER BY timestamp DESC")
    fun getAllInterviews(): Flow<List<InterviewEntity>>

    @Query("SELECT * FROM bookmarked_questions")
    fun getAllBookmarks(): kotlinx.coroutines.flow.Flow<List<BookmarkedQuestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkedQuestionEntity)

    @Query("DELETE FROM bookmarked_questions WHERE questionId = :questionId")
    suspend fun deleteBookmark(questionId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarked_questions WHERE questionId = :questionId)")
    suspend fun isBookmarked(questionId: Int): Boolean
}