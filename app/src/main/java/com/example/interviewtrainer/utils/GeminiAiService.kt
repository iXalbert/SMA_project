package com.example.interviewtrainer.utils

import com.example.interviewtrainer.data.model.QuestionDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class SemanticEvalResult(
    val score: Int,
    val feedback: String
)

object GeminiAiService {

    private const val MODEL_NAME = "gemini-2.5-flash"

    suspend fun generateQuestionsFromCvOrJob(
        apiKey: String,
        cvOrJobText: String,
        role: String,
        level: String
    ): List<QuestionDto> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank() || cleanKey.startsWith("cheia_ta")) {
            return@withContext generateLocalFallbackQuestions(cvOrJobText, role, level)
        }

        try {
            val prompt = """
                You are a senior technical interviewer. Generate 5 tailored interview questions for a $level "$role" candidate based on the following CV or Job Description:
                ---
                $cvOrJobText
                ---
                Return ONLY a valid JSON array (no markdown fences, no extra text) where each object has exact keys:
                "questionRo", "questionEn", "referenceAnswerRo", "referenceAnswerEn", "keywordsRo" (array of strings), "keywordsEn" (array of strings), "hintRo", "hintEn", "interviewerTipRo", "interviewerTipEn".
            """.trimIndent()

            val rawResponse = callGeminiApi(cleanKey, prompt)
                ?: return@withContext generateLocalFallbackQuestions(cvOrJobText, role, level)

            val cleanedJson = rawResponse
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val jsonArray = JSONArray(cleanedJson)
            val result = mutableListOf<QuestionDto>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                result.add(
                    QuestionDto(
                        id = 5000 + i,
                        questionRo = obj.optString("questionRo", "Explică arhitectura unui proiect relevant din CV-ul tău."),
                        questionEn = obj.optString("questionEn", "Explain the architecture of a relevant project from your CV."),
                        referenceAnswerRo = obj.optString("referenceAnswerRo", "Un răspuns structurat cu arhitectură, compromisuri și complexitate."),
                        referenceAnswerEn = obj.optString("referenceAnswerEn", "A structured answer covering architecture, trade-offs, and complexity."),
                        keywordsRo = obj.optJSONArray("keywordsRo").toStringList(),
                        keywordsEn = obj.optJSONArray("keywordsEn").toStringList(),
                        hintRo = obj.optString("hintRo", "Structurează răspunsul: Context -> Soluție -> Trade-offs."),
                        hintEn = obj.optString("hintEn", "Structure your answer: Context -> Solution -> Trade-offs."),
                        interviewerTipRo = obj.optString("interviewerTipRo", "Se urmărește claritatea tehnică și deciziile de arhitectură."),
                        interviewerTipEn = obj.optString("interviewerTipEn", "Looking for technical clarity and architectural trade-offs.")
                    )
                )
            }

            if (result.isNotEmpty()) result else generateLocalFallbackQuestions(cvOrJobText, role, level)
        } catch (e: Exception) {
            generateLocalFallbackQuestions(cvOrJobText, role, level)
        }
    }

    suspend fun evaluateResponseSemantically(
        apiKey: String,
        question: String,
        referenceAnswer: String,
        userAnswer: String,
        language: String
    ): SemanticEvalResult? = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank() || cleanKey.startsWith("cheia_ta") || userAnswer.isBlank()) {
            return@withContext null
        }

        try {
            val langInstruction = if (language == "Engleză") "English" else "Romanian"
            val prompt = """
                Evaluate the candidate's interview answer objectively.
                Question: $question
                Reference Answer: $referenceAnswer
                Candidate Answer: $userAnswer

                Respond ONLY with a valid JSON object (no markdown fences) with exact keys:
                "score": integer from 0 to 100,
                "feedback": concise 1-2 sentence constructive feedback in $langInstruction.
            """.trimIndent()

            val rawResponse = callGeminiApi(cleanKey, prompt) ?: return@withContext null
            val cleanedJson = rawResponse
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val obj = JSONObject(cleanedJson)
            val score = obj.optInt("score", -1)
            val feedback = obj.optString("feedback", "")
            if (score in 0..100 && feedback.isNotBlank()) {
                SemanticEvalResult(score = score, feedback = feedback)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun callGeminiApi(apiKey: String, prompt: String): String? {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent?key=$apiKey")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connectTimeout = 5000
            readTimeout = 7000
            doOutput = true
        }

        val requestBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
        }

        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(requestBody.toString())
        }

        if (conn.responseCode !in 200..299) {
            conn.disconnect()
            return null
        }

        val responseText = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        conn.disconnect()

        val root = JSONObject(responseText)
        return root.optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text")
    }

    private fun generateLocalFallbackQuestions(
        cvOrJobText: String,
        role: String,
        level: String
    ): List<QuestionDto> {
        val knownTechs = listOf(
            "Kotlin", "Java", "Python", "C++", "C#", "Compose", "Jetpack", "Room",
            "Coroutines", "Spring", "SQL", "PostgreSQL", "Docker", "Kubernetes",
            "REST", "GraphQL", "React", "TypeScript", "Git", "CI/CD", "AWS", "Firebase"
        )
        val detectedTechs = knownTechs.filter { tech ->
            cvOrJobText.contains(tech, ignoreCase = true)
        }.ifEmpty { listOf("arhitectură software", "structuri de date", "API-uri") }

        val mainTech = detectedTechs.first()
        val secondTech = detectedTechs.getOrElse(1) { mainTech }

        return listOf(
            QuestionDto(
                id = 5001,
                questionRo = "Pentru rolul de $role ($level), descrie cea mai complexă problemă tehnică rezolvată folosind $mainTech.",
                questionEn = "For the $level $role position, describe the most complex technical challenge you solved using$mainTech.",
                referenceAnswerRo = "Candidatul trebuie să explice contextul, constrângerile, soluția aleasă în $mainTech, alternativele analizate și impactul măsurabil.",
                referenceAnswerEn = "Candidate should explain context, constraints, chosen $mainTech solution, evaluated trade-offs, and measurable impact.",
                keywordsRo = listOf(mainTech, "arhitectură", "performanță", "trade-offs"),
                keywordsEn = listOf(mainTech, "architecture", "performance", "trade-offs"),
                hintRo = "Folosește structura STAR: Situație, Sarcină, Acțiune tehnică, Rezultat.",
                hintEn = "Use the STAR structure: Situation, Task, Technical Action, Result.",
                interviewerTipRo = "Se urmărește asumarea deciziilor tehnice și înțelegerea compromisurilor.",
                interviewerTipEn = "Looking for ownership of technical decisions and understanding of trade-offs."
            ),
            QuestionDto(
                id = 5002,
                questionRo = "Cum gestionezi optimizarea performanței, memoriei și cazurilor limită (edge cases) atunci când lucrezi cu $secondTech?",
                questionEn = "How do you handle performance optimization, memory management, and edge cases when working with $secondTech?",
                referenceAnswerRo = "Menționarea profilării, complexității algoritmice Big O, gestionării erorilor, concurenței și testării unitare.",
                referenceAnswerEn = "Mention profiling, Big O complexity, error handling, concurrency, and unit testing.",
                keywordsRo = listOf(secondTech, "O(", "memorie", "edge cases", "testare"),
                keywordsEn = listOf(secondTech, "O(", "memory", "edge cases", "testing"),
                hintRo = "Dă un exemplu concret de bottleneck și cum l-ai măsurat.",
                hintEn = "Give a concrete bottleneck example and how you measured it.",
                interviewerTipRo = "Se evaluează rigoarea inginerească și atenția la scalabilitate.",
                interviewerTipEn = "Evaluates engineering rigor and attention to scalability."
            ),
            QuestionDto(
                id = 5003,
                questionRo = "Cum ai structura și testa o funcționalitate critică de la zero pentru o echipă de $role, astfel încât codul să fie ușor de întreținut?",
                questionEn = "How would you architect and test a critical feature from scratch for a $role team to keep it maintainable?",
                referenceAnswerRo = "Separarea responsabilităților (Clean Architecture / SOLID), injectarea dependențelor, teste automate și code review.",
                referenceAnswerEn = "Separation of concerns (Clean Architecture / SOLID), dependency injection, automated tests, and code reviews.",
                keywordsRo = listOf("SOLID", "arhitectură", "teste", "modularitate"),
                keywordsEn = listOf("SOLID", "architecture", "tests", "modularity"),
                hintRo = "Vorbește despre separarea straturilor (UI, Business Logic, Data).",
                hintEn = "Discuss layer separation (UI, Business Logic, Data).",
                interviewerTipRo = "Se verifică maturitatea în scrierea codului curat și testabil.",
                interviewerTipEn = "Checks maturity in writing clean, testable code."
            )
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until length()) {
            val item = optString(i)
            if (item.isNotBlank()) list.add(item)
        }
        return list
    }
}