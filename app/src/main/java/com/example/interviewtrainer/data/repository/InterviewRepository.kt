package com.example.interviewtrainer.data.repository

import com.example.interviewtrainer.data.model.ChatMessage
import com.example.interviewtrainer.data.model.InterviewConfig
import com.example.interviewtrainer.data.model.InterviewReport
import com.example.interviewtrainer.data.model.MessageRole
import com.example.interviewtrainer.data.model.QuestionData
import kotlinx.coroutines.delay

class InterviewRepository {

    private var questionIndex = 0

    // Baza de date extinsă cu Domenii -> Roluri -> Întrebări + Răspunsuri de referință + Keywords
    private val domainsData = mapOf(
        "IT & Software Development" to mapOf(
            "Android Developer" to listOf(
                QuestionData(
                    questionText = "Care este diferența dintre o Activity și un Fragment în Android?",
                    referenceAnswer = "Activity reprezintă un ecran independent cu propriul ciclu de viață, în timp ce Fragment reprezintă o porțiune reutilizabilă de interfață găzduită în interiorul unei Activity.",
                    keywords = listOf("ciclu de viață", "reutilizabil", "ecran", "găzduit", "lifecycle", "ui")
                ),
                QuestionData(
                    questionText = "Ce este Jetpack Compose și de ce este preferat în locul XML?",
                    referenceAnswer = "Jetpack Compose este un toolkit modern declarativ pentru construirea interfeței grafice în Kotlin, eliminând fișierele XML separate și reducând codul boilerplate.",
                    keywords = listOf("declarativ", "kotlin", "xml", "ui", "boilerplate", "state")
                ),
                QuestionData(
                    questionText = "Ce rol joacă arhitectura MVVM și StateFlow în Android?",
                    referenceAnswer = "MVVM separă logica de business de interfață, iar StateFlow este un flux reactiv de date care notifică UI-ul când starea se modifică, asigurând rezistența la rotirea ecranului.",
                    keywords = listOf("mvvm", "stateflow", "viewmodel", "reactiv", "stare", "separare")
                )
            ),
            "Backend Java (Spring Boot)" to listOf(
                QuestionData(
                    questionText = "Ce este Inversia Controlului (IoC) și Dependency Injection (DI) în Spring?",
                    referenceAnswer = "IoC transferă gestionarea obiectelor către framework-ul Spring, iar DI este tehnica prin care dependențele sunt injectate automat în clase prin adnotări precum @Autowired.",
                    keywords = listOf("ioc", "di", "dependency injection", "spring", "autowired", "bean")
                ),
                QuestionData(
                    questionText = "Care este diferența dintre JPA și Hibernate?",
                    referenceAnswer = "JPA este doar o specificație / interfață standard Java pentru ORM, în timp ce Hibernate este o implementare concretă a acestei specificații.",
                    keywords = listOf("jpa", "hibernate", "orm", "specificatie", "implementare", "baza de date")
                )
            )
        ),
        "Sisteme Embeddable & Hardware" to mapOf(
            "C++ / Embedded Systems Engineer" to listOf(
                QuestionData(
                    questionText = "Care este rolul cuvântului cheie `volatile` în C/C++?",
                    referenceAnswer = "Cuvântul `volatile` informează compilatorul că valoarea unei variabile se poate modifica neașteptat din exterior (ex: registru hardware, ISR), prevenind optimizările incorecte.",
                    keywords = listOf("compilator", "optimizare", "hardware", "isr", "registru", "modificare")
                ),
                QuestionData(
                    questionText = "Ce este o întrerupere (ISR) și ce reguli de bază trebuie să respecți într-un ISR?",
                    referenceAnswer = "Un ISR este o funcție executată de procesor la un eveniment hardware. Trebuie să fie cât mai scurtă, să nu folosească alocare dinamică de memorie sau funcții blocate (ex: printf, delay).",
                    keywords = listOf("intrerupere", "scurt", "alocare", "delay", "eveniment", "hardware")
                )
            )
        ),
        "Cyber Security" to mapOf(
            "Security Analyst" to listOf(
                QuestionData(
                    questionText = "Explică diferența dintre criptarea simetrică și cea asimetrică.",
                    referenceAnswer = "Criptarea simetrică folosește o singură cheie secretă pentru criptare și decriptare, în timp ce criptarea asimetrică folosește o pereche de chei: una publică și una privată.",
                    keywords = listOf("simetrica", "asimetrica", "cheie publica", "cheie privata", "secreta")
                )
            )
        )
    )

    fun getAvailableDomains(): List<String> = domainsData.keys.toList()

    fun getRolesForDomain(domain: String): List<String> {
        return domainsData[domain]?.keys?.toList() ?: listOf("General Developer")
    }

    private fun getQuestions(config: InterviewConfig): List<QuestionData> {
        val domainMap = domainsData[config.domain] ?: domainsData.values.first()
        return domainMap[config.role] ?: domainMap.values.first()
    }

    suspend fun startInterview(config: InterviewConfig): Pair<String, QuestionData> {
        delay(600)
        questionIndex = 0
        val questions = getQuestions(config)
        val q0 = questions[0]
        val text = "Salut! Voi fi intervievatorul tău pentru poziția de ${config.role} (${config.experienceLevel}).\n\nÎntrebarea #1: ${q0.questionText}"
        return Pair(text, q0)
    }

    suspend fun sendMessage(history: List<ChatMessage>, userMessage: String, config: InterviewConfig): Pair<String, QuestionData?> {
        delay(800)
        questionIndex++
        val questions = getQuestions(config)

        return if (questionIndex < questions.size) {
            val qNext = questions[questionIndex]
            val text = "Am înregistrat răspunsul tău.\n\nÎntrebarea #${questionIndex + 1}: ${qNext.questionText}"
            Pair(text, qNext)
        } else {
            val text = "Ai parcurs toate întrebările din acest modul! Apasă pe 'Încheie' pentru a genera raportul evaluat."
            Pair(text, null)
        }
    }

    // Algoritmul real de evaluare a răspunsurilor
    suspend fun generateReport(history: List<ChatMessage>): InterviewReport {
        delay(1200)

        // Filtram perechile (Intrebare AI, Raspuns Utilizator)
        val qaPairs = mutableListOf<Pair<QuestionData, String>>()
        for (i in 0 until history.size - 1) {
            val currentMsg = history[i]
            val nextMsg = history[i + 1]
            if (currentMsg.role == MessageRole.INTERVIEWER && currentMsg.questionData != null && nextMsg.role == MessageRole.USER) {
                qaPairs.add(Pair(currentMsg.questionData, nextMsg.text))
            }
        }

        if (qaPairs.isEmpty()) {
            return InterviewReport(0, "Nu au fost detectate răspunsuri valide pentru evaluare.", emptyList(), listOf("Răspunde la mai multe întrebări."))
        }

        var totalScoreSum = 0
        val strengths = mutableListOf<String>()
        val improvements = mutableListOf<String>()

        qaPairs.forEachIndexed { idx, pair ->
            val question = pair.first
            val userAnswer = pair.second.lowercase()

            var questionScore = 0
            var matchedKeywords = 0

            // 1. Verificam cuvintele cheie
            question.keywords.forEach { kw ->
                if (userAnswer.contains(kw.lowercase())) {
                    matchedKeywords++
                }
            }

            val keywordCoverage = if (question.keywords.isNotEmpty()) matchedKeywords.toDouble() / question.keywords.size else 0.5
            questionScore += (keywordCoverage * 70).toInt() // Până la 70 de puncte din keywords

            // 2. Evaluăm detalierea răspunsului (lungimea/complexitatea)
            val wordCount = userAnswer.split("\\s+".toRegex()).size
            if (wordCount >= 15) questionScore += 30
            else if (wordCount >= 8) questionScore += 15
            else questionScore += 5

            questionScore = questionScore.coerceIn(0, 100)
            totalScoreSum += questionScore

            if (questionScore >= 70) {
                strengths.add("Q${idx + 1}: Explicație bună legată de '${question.keywords.firstOrNull() ?: "domeniu"}'")
            } else {
                improvements.add("Q${idx + 1}: Detaliază mai mult. Răspunsul ideal conține: ${question.referenceAnswer}")
            }
        }

        val finalScore = (totalScoreSum / qaPairs.size).coerceIn(0, 100)

        val summary = when {
            finalScore >= 85 -> "Excelent! Ai o stăpânire tehnică foarte bună a noțiunilor."
            finalScore >= 60 -> "Performanță medie. Ai cunoștințe de bază, dar răspunsurile pot fi mai aprofundate."
            else -> "Necesită revizuire. Răspunsurile au fost prea scurte sau au omis concepte cheie."
        }

        return InterviewReport(
            score = finalScore,
            summary = summary,
            strengths = strengths.ifEmpty { listOf("Punctualitate în trimiterea răspunsurilor.") },
            improvements = improvements.ifEmpty { listOf("Continuă să exersezi pentru a menține consistența.") }
        )
    }
}