package com.example.interviewtrainer.data.repository

import com.example.interviewtrainer.data.model.*
import com.example.interviewtrainer.data.remote.QuestionApiService
import kotlinx.coroutines.delay

class InterviewRepository {

    private val apiService = QuestionApiService.create()
    private var cachedQuestionBank: QuestionBankResponse? = null

    private var currentSessionQuestions: List<QuestionDto> = emptyList()
    private var questionIndex = 0

    suspend fun fetchQuestionBank(rawGistUrl: String): QuestionBankResponse {
        if (cachedQuestionBank == null) {
            cachedQuestionBank = apiService.getQuestionBank(rawGistUrl)
        }
        return cachedQuestionBank!!
    }

    suspend fun startInterview(config: InterviewConfig, rawGistUrl: String): Pair<String, QuestionDto?> {
        delay(400)
        val bank = fetchQuestionBank(rawGistUrl)

        val domain = bank.domains.find { it.name == config.domain }
        val spec = domain?.specializations?.find { it.name == config.specialization }
        val role = spec?.roles?.find { it.title == config.role }

        val allQuestions = role?.questions ?: emptyList()

        currentSessionQuestions = allQuestions.shuffled().take(config.questionLimit)
        questionIndex = 0

        return if (currentSessionQuestions.isNotEmpty()) {
            val q0 = currentSessionQuestions[0]
            val qText = if (config.language == "Engleză") q0.questionEn else q0.questionRo
            val greeting = if (config.language == "Engleză")
                "Hello! I will be your interviewer for ${config.role} (${config.experienceLevel}).\n\nQuestion #1: $qText"
            else
                "Salut! Voi fi intervievatorul tău pentru poziția de ${config.role} (${config.experienceLevel}).\n\nÎntrebarea #1: $qText"

            Pair(greeting, q0)
        } else {
            Pair("Nu s-au găsit întrebări pentru această poziție.", null)
        }
    }

    suspend fun sendMessage(userMessage: String, config: InterviewConfig): Pair<String, QuestionDto?> {
        delay(500)
        questionIndex++

        val isEn = config.language == "Engleză"

        return if (questionIndex < currentSessionQuestions.size) {
            val qNext = currentSessionQuestions[questionIndex]
            val qText = if (isEn) qNext.questionEn else qNext.questionRo
            val text = if (isEn) "Thank you.\n\nQuestion #${questionIndex + 1}: $qText"
            else "Am înregistrat răspunsul tău.\n\nÎntrebarea #${questionIndex + 1}: $qText"
            Pair(text, qNext)
        } else {
            val text = if (isEn) "You have completed all questions! Click 'Finish' to view your report."
            else "Ai parcurs toate întrebările! Apasă pe 'Încheie' pentru a genera raportul evaluat."
            Pair(text, null)
        }
    }

    fun getHintForCurrentQuestion(language: String): String {
        val q = currentSessionQuestions.getOrNull(questionIndex) ?: return ""
        return if (language == "Engleză") q.hintEn else q.hintRo
    }

    fun generateReport(messages: List<ChatMessage>, language: String): InterviewReport {
        val userMessages = messages.filter { it.role == MessageRole.USER }
        val isEn = language == "Engleză"

        if (userMessages.isEmpty()) {
            return InterviewReport(
                score = 0,
                summary = if (isEn) "No user responses were recorded." else "Nu au fost înregistrate răspunsuri.",
                strengths = emptyList(),
                improvements = listOf(if (isEn) "Try answering the questions in detail." else "Răspunde mai detaliat la întrebări.")
            )
        }

        var matchedKeywordsCount = 0
        var totalKeywords = 0

        currentSessionQuestions.forEachIndexed { idx, question ->
            val userAns = userMessages.getOrNull(idx)?.text?.lowercase() ?: ""
            val keywords = if (isEn) question.keywordsEn else question.keywordsRo
            totalKeywords += keywords.size

            keywords.forEach { kw ->
                if (userAns.contains(kw.lowercase())) {
                    matchedKeywordsCount++
                }
            }
        }

        val calculatedScore = if (totalKeywords > 0) {
            ((matchedKeywordsCount.toDouble() / totalKeywords) * 100).toInt().coerceIn(20, 95)
        } else 50

        val summaryText = when {
            calculatedScore >= 80 -> if (isEn) "Excellent technical knowledge!" else "Cunoștințe tehnice excelente!"
            calculatedScore >= 50 -> if (isEn) "Good overall response, but needs more depth." else "Răspunsuri bune, dar este loc de mai multe detalii tehnice."
            else -> if (isEn) "Basic understanding. Review fundamental concepts." else "Necesită mai multă pregătire pe concepte fundamentale."
        }

        val strengths = mutableListOf<String>()
        val improvements = mutableListOf<String>()

        if (calculatedScore >= 70) {
            strengths.add(if (isEn) "Clear understanding of key terminology" else "Înțelegere clară a terminologiei cheie")
            strengths.add(if (isEn) "Structured and direct answers" else "Răspunsuri structurate și la obiect")
        } else {
            improvements.add(if (isEn) "Include more specific technical terms" else "Folosește mai mulți termeni tehnici specifici")
            improvements.add(if (isEn) "Elaborate more on practical scenarios" else "Detaliază mai mult exemplele practice")
        }

        return InterviewReport(
            score = calculatedScore,
            summary = summaryText,
            strengths = strengths,
            improvements = improvements
        )
    }
}