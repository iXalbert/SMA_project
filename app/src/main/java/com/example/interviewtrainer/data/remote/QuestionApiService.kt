package com.example.interviewtrainer.data.remote

import com.example.interviewtrainer.data.model.QuestionBankResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url

interface QuestionApiService {

    @GET
    suspend fun getQuestionBank(@Url fullUrl: String): QuestionBankResponse

    companion object {
        fun create(): QuestionApiService {
            return Retrofit.Builder()
                .baseUrl("https://gist.githubusercontent.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(QuestionApiService::class.java)
        }
    }
}