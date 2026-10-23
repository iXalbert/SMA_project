package com.example.interviewtrainer.data.repository

import com.example.interviewtrainer.data.model.ChatMessage
import com.example.interviewtrainer.data.model.InterviewConfig
import kotlinx.coroutines.delay

class InterviewRepository {

    private var questionIndex = 0

    private val questionsPool = mapOf(
        "Android Developer" to listOf(
            "Care este diferența dintre o activitate (Activity) și un Fragment în Android?",
            "Poți explică cum funcționează Jetpack Compose și prin ce se diferențiază de arhitectura clasică bazată pe XML?",
            "Ce este arhitectura MVVM și ce rol joacă StateFlow / LiveData în gestionarea stării?",
            "Cum gestionezi operațiile asincrone și apelurile de rețea în Kotlin (Coroutines vs Threads)?"
        ),
        "C++ / Embedded" to listOf(
            "Care este diferența dintre un pointer și o referință în C++?",
            "Explică conceptul de RAII (Resource Acquisition Is Initialization) și cum ajută la evitarea memory leak-urilor.",
            "Ce este un intrerupere (Interrupt Service Routine - ISR) și la ce trebuie să fii atent când scrii cod în ea?",
            "Cum funcționează cuvântul cheie `volatile` în contextul microcontrolerelor?"
        ),
        "General IT" to listOf(
            "Descrie un proiect tehnic recent la care ai lucrat și cea mai mare provocare întâmpinată.",
            "Care sunt principiile OOP (Programare Orientată pe Obiect) și cum le aplici în codul tău?",
            "Ce este un API RESTful și ce metode HTTP cunoști?",
            "Cum gestionezi versiunile de cod și colaborarea în echipă folosind Git?"
        )
    )

    suspend fun startInterview(config: InterviewConfig): String {
        delay(800) // Simulăm procesarea instantanee a agentului
        questionIndex = 0

        val questions = getQuestionsForRole(config.role)
        return "Salut! Sunt intervievatorul tău pentru poziția de ${config.role} (${config.experienceLevel}).\n\nPrima întrebare: ${questions[0]}"
    }

    suspend fun sendMessage(history: List<ChatMessage>, userMessage: String, config: InterviewConfig): String {
        delay(1000)
        questionIndex++

        val questions = getQuestionsForRole(config.role)

        return if (questionIndex < questions.size) {
            "Mulțumesc pentru răspuns!\n\nÎntrebarea #${questionIndex + 1}: ${questions[questionIndex]}"
        } else {
            "Îți mulțumesc pentru răspunsuri! Am parcurs toate întrebările din această sesiune de interviu.\n\nSesiunea este completă!"
        }
    }

    private fun getQuestionsForRole(role: String): List<String> {
        return questionsPool.entries
            .firstOrNull { role.contains(it.key, ignoreCase = true) }?.value
            ?: questionsPool["General IT"]!!
    }
}