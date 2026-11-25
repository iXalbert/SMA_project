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
    val feedback: String,
    val followUpQuestion: String?
)

object GeminiAiService {

    private const val MODEL_ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="

    suspend fun evaluateResponseSemantically(
        apiKey: String,
        question: String,
        referenceAnswer: String,
        userAnswer: String,
        language: String
    ): SemanticEvalResult? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null

        val isEn = language == "Engleză"
        val prompt = """
            You are a senior technical interviewer using the "Cracking the Coding Interview" rubric.
            Evaluate the candidate's answer semantically (including any pseudocode/code provided).
            Language for output: ${if (isEn) "English" else "Romanian"}.
            
            Question: $question
            Ideal Reference: $referenceAnswer
            Candidate Answer: $userAnswer
            
            Return ONLY valid JSON with exact keys:
            {
              "score": <integer 0-100>,
              "feedback": "<concise CtCI feedback covering correctness, Big O, edge cases, and clarity>",
              "followUp": "<optional short follow-up question if answer is incomplete, or empty string>"
            }
        """.trimIndent()

        try {
            val rawText = callGemini(apiKey, prompt) ?: return@withContext null
            val cleanJson = extractJsonObject(rawText)
            val obj = JSONObject(cleanJson)
            SemanticEvalResult(
                score = obj.optInt("score", 70).coerceIn(0, 100),
                feedback = obj.optString("feedback", ""),
                followUpQuestion = obj.optString("followUp", "").ifBlank { null }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun generateQuestionsFromCvOrJob(
        apiKey: String,
        cvOrJobText: String,
        role: String,
        level: String
    ): List<QuestionDto> = withContext(Dispatchers.IO) {
        if (cvOrJobText.isBlank()) return@withContext emptyList()

        if (apiKey.isNotBlank()) {
            val prompt = """
                Generate 5 technical and behavioral interview questions tailored to this CV or Job Description.
                Target Role: $role | Experience Level: $level.
                Text:
                $cvOrJobText
                
                Return ONLY a valid JSON array of 5 objects with exact keys:
                [
                  {
                    "id": 901,
                    "questionRo": "...",
                    "questionEn": "...",
                    "referenceAnswerRo": "...",
                    "referenceAnswerEn": "...",
                    "keywordsRo": ["kw1", "kw2", "O(N)"],
                    "keywordsEn": ["kw1", "kw2", "O(N)"],
                    "hintRo": "...",
                    "hintEn": "...",
                    "interviewerTipRo": "...",
                    "interviewerTipEn": "..."
                  }
                ]
            """.trimIndent()

            try {
                val rawText = callGemini(apiKey, prompt)
                if (!rawText.isNullOrBlank()) {
                    val jsonArr = JSONArray(extractJsonArray(rawText))
                    val result = mutableListOf<QuestionDto>()
                    for (i in 0 until jsonArr.length()) {
                        val item = jsonArr.getJSONObject(i)
                        val kwRoArr = item.optJSONArray("keywordsRo") ?: JSONArray()
                        val kwEnArr = item.optJSONArray("keywordsEn") ?: JSONArray()
                        val kwsRo = (0 until kwRoArr.length()).map { kwRoArr.getString(it) }
                        val kwsEn = (0 until kwEnArr.length()).map { kwEnArr.getString(it) }

                        result.add(
                            QuestionDto(
                                id = item.optInt("id", 900 + i),
                                questionRo = item.optString("questionRo"),
                                questionEn = item.optString("questionEn"),
                                referenceAnswerRo = item.optString("referenceAnswerRo"),
                                referenceAnswerEn = item.optString("referenceAnswerEn"),
                                keywordsRo = kwsRo,
                                keywordsEn = kwsEn,
                                hintRo = item.optString("hintRo", "Gândește-te la complexitatea Big O și cazurile limită."),
                                hintEn = item.optString("hintEn", "Think about Big O complexity and edge cases."),
                                interviewerTipRo = item.optString("interviewerTipRo").ifBlank { null },
                                interviewerTipEn = item.optString("interviewerTipEn").ifBlank { null }
                            )
                        )
                    }
                    if (result.isNotEmpty()) return@withContext result
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        buildOfflineTailoredQuestions(cvOrJobText)
    }

    private fun buildOfflineTailoredQuestions(text: String): List<QuestionDto> {
        val lower = text.lowercase()
        val detectedSkills = listOf(
            "kotlin", "android", "jetpack compose", "c++", "c", "embedded", "rtos",
            "python", "java", "spring", "sql", "docker", "git", "fpga", "verilog", "spi", "i2c"
        ).filter { lower.contains(it) }.ifEmpty { listOf("arhitectură software", "structuri de date") }

        val mainSkill = detectedSkills.first().uppercase()
        val secondSkill = detectedSkills.getOrElse(1) { "optimizare" }.uppercase()

        return listOf(
            QuestionDto(
                id = 951,
                questionRo = "Din experiența ta menționată cu $mainSkill, care a fost cea mai dificilă problemă de performanță sau memorie rezolvată și ce complexitate avea soluția?",
                questionEn = "Based on your $mainSkill experience, what was the hardest performance or memory bottleneck you solved and what was its complexity?",
                referenceAnswerRo = "Candidatul explică contextul tehnic, profilarea problemei, structura de date/algoritmul ales, compromisul O(N)/O(1) și rezultatul măsurat.",
                referenceAnswerEn = "Candidate explains technical context, profiling, chosen data structure/algorithm, O(N)/O(1) trade-offs, and measured impact.",
                keywordsRo = listOf(mainSkill.lowercase(), "O(", "memorie", "performanță", "rezultat"),
                keywordsEn = listOf(mainSkill.lowercase(), "O(", "memory", "performance", "impact"),
                hintRo = "Folosește structura STAR și menționează explicit complexitatea Big O înainte și după optimizare.",
                hintEn = "Use STAR structure and explicitly state Big O complexity before and after optimization.",
                interviewerTipRo = "Rubrica CtCI punctează măsurătorile concrete și compromisul timp vs. spațiu.",
                interviewerTipEn = "The CtCI rubric rewards concrete metrics and time vs. space trade-offs."
            ),
            QuestionDto(
                id = 952,
                questionRo = "Cum gestionezi concurența, sincronizarea și cazurile limită (edge cases) într-un proiect care combină $mainSkill și $secondSkill?",
                questionEn = "How do you handle concurrency, synchronization, and edge cases in a project combining $mainSkill and $secondSkill?",
                referenceAnswerRo = "Descrie mecanismele de sincronizare (mutex, coroutines, ISR/volatile), prevenirea race conditions și tratarea intrărilor nule sau invalide.",
                referenceAnswerEn = "Describes synchronization primitives (mutex, coroutines, ISR/volatile), preventing race conditions, and handling null/invalid inputs.",
                keywordsRo = listOf("sincronizare", "edge", "null", "concurență", "race"),
                keywordsEn = listOf("synchronization", "edge", "null", "concurrency", "race"),
                hintRo = "Gândește-te la ce se întâmplă când două fluxuri accesează aceeași resursă simultan.",
                hintEn = "Think about what happens when two execution flows access the same shared resource simultaneously.",
                interviewerTipRo = "Menționează explicit cum previi deadlock-urile și race conditions.",
                interviewerTipEn = "Explicitly explain how you prevent deadlocks and race conditions."
            ),
            QuestionDto(
                id = 953,
                questionRo = "Cum ai proiecta arhitectura modulară a unei componente critice folosind $mainSkill astfel încât să fie ușor de testat?",
                questionEn = "How would you design the modular architecture of a critical component using $mainSkill so that it remains easily testable?",
                referenceAnswerRo = "Separarea responsabilităților (Clean Architecture / MVVM / HAL), dependency injection și izolarea dependențelor hardware sau de rețea.",
                referenceAnswerEn = "Separation of concerns (Clean Architecture / MVVM / HAL), dependency injection, and mocking external hardware or network layers.",
                keywordsRo = listOf("arhitectură", "test", "modular", "dependențe", "interfață"),
                keywordsEn = listOf("architecture", "test", "modular", "dependencies", "interface"),
                hintRo = "Menționează decuplarea logicii de business față de stratul I/O sau UI.",
                hintEn = "Mention decoupling business logic from the I/O or UI layer.",
                interviewerTipRo = "Separarea straturilor și testabilitatea arată maturitate inginerească.",
                interviewerTipEn = "Layer separation and testability demonstrate strong engineering maturity."
            )
        )
    }

    private fun callGemini(apiKey: String, prompt: String): String? {
        val url = URL(MODEL_ENDPOINT + apiKey.trim())
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            doOutput = true
            connectTimeout = 12000
            readTimeout = 18000
        }

        val payload = JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", prompt))
                    )
                )
            )
        }

        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(payload.toString()) }

        if (conn.responseCode !in 200..299) return null
        val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
        val root = JSONObject(responseStr)
        return root.optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text")
    }

    private fun extractJsonObject(raw: String): String {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        return if (start >= 0 && end > start) raw.substring(start, end + 1) else raw
    }

    private fun extractJsonArray(raw: String): String {
        val start = raw.indexOf('[')
        val end = raw.lastIndexOf(']')
        return if (start >= 0 && end > start) raw.substring(start, end + 1) else raw
    }
}