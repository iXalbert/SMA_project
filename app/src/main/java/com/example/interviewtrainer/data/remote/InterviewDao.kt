package com.example.interviewtrainer.data.remote

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.interviewtrainer.data.model.BookmarkedQuestionEntity
import com.example.interviewtrainer.data.model.CustomQuestionEntity
import com.example.interviewtrainer.data.model.InterviewEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InterviewDao {

    // CRUD Interviews
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInterview(interview: InterviewEntity)

    @Query("SELECT * FROM interviews ORDER BY timestamp DESC")
    fun getAllInterviews(): Flow<List<InterviewEntity>>

    @Update
    suspend fun updateInterview(interview: InterviewEntity)

    @Delete
    suspend fun deleteInterview(interview: InterviewEntity)

    // CRUD Custom Questions (Banca personală de întrebări)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomQuestion(question: CustomQuestionEntity)

    @Query("SELECT * FROM custom_questions ORDER BY id DESC")
    fun getAllCustomQuestions(): Flow<List<CustomQuestionEntity>>

    @Query("SELECT * FROM custom_questions WHERE role = :role")
    suspend fun getCustomQuestionsByRole(role: String): List<CustomQuestionEntity>

    @Update
    suspend fun updateCustomQuestion(question: CustomQuestionEntity)

    @Delete
    suspend fun deleteCustomQuestion(question: CustomQuestionEntity)

    // Bookmarks
    @Query("SELECT * FROM bookmarked_questions")
    fun getAllBookmarks(): Flow<List<BookmarkedQuestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkedQuestionEntity)

    @Query("DELETE FROM bookmarked_questions WHERE questionId = :questionId")
    suspend fun deleteBookmark(questionId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarked_questions WHERE questionId = :questionId)")
    suspend fun isBookmarked(questionId: Int): Boolean
}