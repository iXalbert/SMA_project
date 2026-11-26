package com.example.interviewtrainer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.interviewtrainer.BuildConfig
import com.example.interviewtrainer.data.model.*
import com.example.interviewtrainer.data.remote.AppDatabase
import com.example.interviewtrainer.data.repository.InterviewRepository
import com.example.interviewtrainer.utils.CtciRubricEvaluator
import com.example.interviewtrainer.utils.GeminiAiService
import com.example.interviewtrainer.utils.VideoSessionReport
import com.example.interviewtrainer.utils.VoiceConfidenceMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InterviewViewModel(application: Application) : AndroidViewModel(application) {

    val repository = InterviewRepository()
    private val dao = AppDatabase.getDatabase(application).interviewDao()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    val bookmarkedQuestions: StateFlow<List<BookmarkedQuestionEntity>> = MutableStateFlow(emptyList())
    val savedInterviews: StateFlow<List<InterviewEntity>> = MutableStateFlow(emptyList())
    val customQuestions: StateFlow<List<CustomQuestionEntity>> = MutableStateFlow(emptyList())
    val bookmarks: StateFlow<List<BookmarkedQuestionEntity>> = MutableStateFlow(emptyList())

    private val _config = MutableStateFlow(InterviewConfig())
    val config: StateFlow<InterviewConfig> = _config.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _report = MutableStateFlow<InterviewReport?>(null)
    val report: StateFlow<InterviewReport?> = _report.asStateFlow()

    private val _isTimedMode = MutableStateFlow(false)
    val isTimedMode: StateFlow<Boolean> = _isTimedMode.asStateFlow()

    private val _isVideoCallMode = MutableStateFlow(false)
    val isVideoCallMode: StateFlow<Boolean> = _isVideoCallMode.asStateFlow()

    private val _interviewerStyle = MutableStateFlow("Friendly HR")
    val interviewerStyle: StateFlow<String> = _interviewerStyle.asStateFlow()

    private val _geminiApiKey = MutableStateFlow(BuildConfig.GEMINI_API_KEY)
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _sessionAnalytics = MutableStateFlow<SessionAnalytics?>(null)
    val sessionAnalytics: StateFlow<SessionAnalytics?> = _sessionAnalytics.asStateFlow()

    private val _videoReport = MutableStateFlow<VideoSessionReport?>(null)
    val videoReport: StateFlow<VideoSessionReport?> = _videoReport.asStateFlow()

    fun setVideoReport(report: VideoSessionReport?) {
        _videoReport.value = report
    }

    private val _voiceMetricsReport = MutableStateFlow<VoiceConfidenceMetrics?>(null)
    val voiceMetricsReport: StateFlow<VoiceConfidenceMetrics?> = _voiceMetricsReport.asStateFlow()

    private val recordedVoiceSamples = mutableListOf<VoiceConfidenceMetrics>()

    fun recordVoiceMetrics(metrics: VoiceConfidenceMetrics) {
        recordedVoiceSamples.add(metrics)
    }

    private var questionAskedTimestamp: Long = System.currentTimeMillis()
    private val answerDurationsSec = mutableListOf<Int>()
    private val hintedQuestionIds = mutableSetOf<Int>()
    private var followUpAskedForCurrentQuestion = false

    private var overrideQuestionsQueue: MutableList<QuestionDto> = mutableListOf()
    private var overrideIndex = 0

    init {
        viewModelScope.launch(Dispatchers.IO) {
            dao.getAllInterviews().collect { list ->
                (savedInterviews as MutableStateFlow).value = list
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            dao.getAllBookmarks().collect { list ->
                (bookmarkedQuestions as MutableStateFlow).value = list
                (bookmarks as MutableStateFlow).value = list
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            dao.getAllCustomQuestions().collect { list ->
                (customQuestions as MutableStateFlow).value = list
            }
        }
    }

    fun toggleBookmark(question: QuestionDto) {
        viewModelScope.launch(Dispatchers.IO) {
            if (dao.isBookmarked(question.id)) {
                dao.deleteBookmark(question.id)
            } else {
                dao.insertBookmark(
                    BookmarkedQuestionEntity(
                        questionId = question.id,
                        domain = _config.value.domain,
                        specialization = _config.value.specialization,
                        role = _config.value.role,
                        questionRo = question.questionRo.orEmpty(),
                        questionEn = question.questionEn.orEmpty(),
                        referenceAnswerRo = question.referenceAnswerRo.orEmpty(),
                        referenceAnswerEn = question.referenceAnswerEn.orEmpty(),
                        hintRo = question.hintRo.orEmpty(),
                        hintEn = question.hintEn.orEmpty()
                    )
                )
            }
        }
    }

    fun removeBookmark(questionId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteBookmark(questionId)
        }
    }

    fun updateConfig(
        domain: String,
        specialization: String,
        role: String,
        experienceLevel: String,
        language: String,
        isTimedMode: Boolean = false,
        isVideoCallMode: Boolean = _isVideoCallMode.value,
        interviewerStyle: String = _interviewerStyle.value
    ) {
        _config.value = InterviewConfig(
            domain,
            specialization,
            role,
            experienceLevel,
            language
        )
        _isTimedMode.value = isTimedMode
        _isVideoCallMode.value = isVideoCallMode
        _interviewerStyle.value = interviewerStyle
    }

    fun startInterview(rawGistUrl: String) {
        overrideQuestionsQueue.clear()
        overrideIndex = 0
        resetSessionTracking()

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _messages.value = emptyList()

            val customForRole = dao.getCustomQuestionsByRole(_config.value.role).map { it.toQuestionDto() }
            val (firstText, qData) = repository.startInterview(_config.value, rawGistUrl, customForRole)

            val isEn = _config.value.language == "Engleză"
            val introPrefix = if (_isVideoCallMode.value) {
                if (isEn) "Hi, I'm Amalia, your AI recruiter today. Let's get started!\n\n"
                else "Bună! Sunt Amalia și voi conduce interviul tău astăzi. Hai să începem!\n\n"
            } else ""

            _messages.value = listOf(
                ChatMessage(
                    text = introPrefix + firstText,
                    role = MessageRole.INTERVIEWER,
                    questionData = qData
                )
            )
            questionAskedTimestamp = System.currentTimeMillis()
            _isLoading.value = false
        }
    }

    fun startCustomCvInterview(
        cvOrJobText: String,
        targetRole: String,
        onReady: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            resetSessionTracking()

            val updatedCfg = _config.value.copy(
                domain = "CV / Job Tailored",
                specialization = "Custom Profile",
                role = targetRole.ifBlank { _config.value.role }
            )
            _config.value = updatedCfg

            val generated = GeminiAiService.generateQuestionsFromCvOrJob(
                apiKey = _geminiApiKey.value,
                cvOrJobText = cvOrJobText,
                role = updatedCfg.role,
                level = updatedCfg.experienceLevel
            )

            overrideQuestionsQueue = generated.toMutableList()
            overrideIndex = 0

            val isEn = updatedCfg.language == "Engleză"
            val firstQ = overrideQuestionsQueue.firstOrNull()
            if (firstQ != null) {
                val qText = (if (isEn) firstQ.questionEn else firstQ.questionRo).orEmpty()
                val intro = if (_isVideoCallMode.value) {
                    if (isEn) "Hi, I'm Amalia! I reviewed your profile for ${updatedCfg.role}. Question #1:\n\n$qText"
                    else "Bună! Sunt Amalia și am analizat profilul tău pentru ${updatedCfg.role}. Întrebarea #1:\n\n$qText"
                } else {
                    if (isEn) "Tailored Interview (${updatedCfg.role}) — Question #1:\n\n$qText"
                    else "Interviu Personalizat (${updatedCfg.role}) — Întrebarea #1:\n\n$qText"
                }

                _messages.value = listOf(
                    ChatMessage(text = intro, role = MessageRole.INTERVIEWER, questionData = firstQ)
                )
            }

            questionAskedTimestamp = System.currentTimeMillis()
            _isLoading.value = false
            withContext(Dispatchers.Main) {
                onReady()
            }
        }
    }

    fun startSpacedRepetitionInterview(weakQuestions: List<QuestionDto>, onReady: () -> Unit) {
        if (weakQuestions.isEmpty()) return
        resetSessionTracking()
        overrideQuestionsQueue = weakQuestions.toMutableList()
        overrideIndex = 0

        val isEn = _config.value.language == "Engleză"
        val firstQ = overrideQuestionsQueue.first()
        val qText = (if (isEn) firstQ.questionEn else firstQ.questionRo).orEmpty()
        val intro = if (isEn)
            "Spaced Repetition Practice — Let's master your missed topics. Question #1:\n\n$qText"
        else
            "Antrenament Repetiție Spațiată — Reluăm conceptele unde ai ezitat. Întrebarea #1:\n\n$qText"

        _messages.value = listOf(
            ChatMessage(text = intro, role = MessageRole.INTERVIEWER, questionData = firstQ)
        )
        questionAskedTimestamp = System.currentTimeMillis()
        onReady()
    }

    private fun resetSessionTracking() {
        _report.value = null
        hintedQuestionIds.clear()
        followUpAskedForCurrentQuestion = false
        _videoReport.value = null
        _voiceMetricsReport.value = null
        recordedVoiceSamples.clear()
        answerDurationsSec.clear()
        _sessionAnalytics.value = null
        questionAskedTimestamp = System.currentTimeMillis()
    }

    fun addCustomQuestion(question: CustomQuestionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertCustomQuestion(question)
        }
    }

    fun updateCustomQuestion(question: CustomQuestionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateCustomQuestion(question)
        }
    }

    fun deleteCustomQuestion(question: CustomQuestionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteCustomQuestion(question)
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank() || _isLoading.value) return

        val currentList = _messages.value.toMutableList()
        currentList.add(ChatMessage(text = userText, role = MessageRole.USER))
        _messages.value = currentList

        val elapsedSec = ((System.currentTimeMillis() - questionAskedTimestamp) / 1000L).toInt().coerceAtLeast(1)
        answerDurationsSec.add(elapsedSec)
        questionAskedTimestamp = System.currentTimeMillis()

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val isEn = _config.value.language == "Engleză"
            val wordCount = userText.trim().split(Regex("""\s+""")).size
            val lower = userText.lowercase()
            val mentionsComplexity = listOf("o(", "complexit", "timp", "memorie", "time", "space").any { lower.contains(it) }

            val needsFollowUp = !followUpAskedForCurrentQuestion &&
                    (wordCount < 10 || (!mentionsComplexity && _interviewerStyle.value == "Tech Lead"))

            if (needsFollowUp) {
                followUpAskedForCurrentQuestion = true
                val followUpText = when {
                    wordCount < 10 && isEn ->
                        "Could you elaborate a bit more on your reasoning and edge cases before we move on?"
                    wordCount < 10 ->
                        "Poți detalia puțin mai mult raționamentul tău și eventualele cazuri limită (edge cases) înainte să trecem mai departe?"
                    isEn ->
                        "Good approach. What would be the time and space complexity (Big O) of your solution?"
                    else ->
                        "Bună abordare. Care ar fi complexitatea ca timp și memorie (Big O) pentru soluția ta?"
                }

                val updatedList = _messages.value.toMutableList()
                updatedList.add(ChatMessage(text = followUpText, role = MessageRole.INTERVIEWER))
                _messages.value = updatedList
                questionAskedTimestamp = System.currentTimeMillis()
                _isLoading.value = false
                return@launch
            }

            followUpAskedForCurrentQuestion = false

            if (overrideQuestionsQueue.isNotEmpty()) {
                overrideIndex++
                val updatedList = _messages.value.toMutableList()
                if (overrideIndex < overrideQuestionsQueue.size) {
                    val nextQ = overrideQuestionsQueue[overrideIndex]
                    val qText = (if (isEn) nextQ.questionEn else nextQ.questionRo).orEmpty()
                    val prompt = if (isEn)
                        "Got it. Moving to Question #${overrideIndex + 1}:\n\n$qText"
                    else
                        "Am înțeles. Trecem la Întrebarea #${overrideIndex + 1}:\n\n$qText"
                    updatedList.add(ChatMessage(text = prompt, role = MessageRole.INTERVIEWER, questionData = nextQ))
                } else {
                    val endMsg = if (isEn)
                        "We completed all tailored questions! Tap Finish to inspect your report."
                    else
                        "Am parcurs toate întrebările personalizate! Apasă pe Încheie pentru raport."
                    updatedList.add(ChatMessage(text = endMsg, role = MessageRole.INTERVIEWER))
                }
                _messages.value = updatedList
                questionAskedTimestamp = System.currentTimeMillis()
                _isLoading.value = false
                return@launch
            }

            val (aiText, qData) = repository.sendMessage(userText, _config.value)
            val updatedList = _messages.value.toMutableList()
            updatedList.add(ChatMessage(text = aiText, role = MessageRole.INTERVIEWER, questionData = qData))
            _messages.value = updatedList
            questionAskedTimestamp = System.currentTimeMillis()
            _isLoading.value = false
        }
    }

    fun requestHint() {
        val lastQuestion = _messages.value
            .lastOrNull { it.role == MessageRole.INTERVIEWER && it.questionData != null }
            ?.questionData

        val isEn = _config.value.language == "Engleză"
        val hintText = if (lastQuestion != null) {
            hintedQuestionIds.add(lastQuestion.id)
            (if (isEn) lastQuestion.hintEn else lastQuestion.hintRo).orEmpty()
        } else {
            repository.getHintForCurrentQuestion(_config.value.language)
        }

        if (hintText.isNotBlank()) {
            val currentList = _messages.value.toMutableList()
            val hintPrefix = if (isEn) "💡 CtCI Hint: " else "💡 Indiciu CtCI: "
            currentList.add(ChatMessage(text = hintPrefix + hintText, role = MessageRole.HINT))
            _messages.value = currentList
        }
    }

    fun sendMessageAndFinish(userText: String, onFinished: () -> Unit) {
        if (userText.isNotBlank()) {
            val currentList = _messages.value.toMutableList()
            currentList.add(ChatMessage(text = userText.trim(), role = MessageRole.USER))
            _messages.value = currentList

            val elapsedSec = ((System.currentTimeMillis() - questionAskedTimestamp) / 1000L).toInt().coerceAtLeast(1)
            answerDurationsSec.add(elapsedSec)
        }
        finishInterview(onFinished)
    }

    fun finishInterview(onFinished: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            var hasNavigated = false

            suspend fun navigateOnce() {
                if (!hasNavigated) {
                    hasNavigated = true
                    _isLoading.value = false
                    withContext(Dispatchers.Main) {
                        onFinished()
                    }
                }
            }

            try {
                val isEn = _config.value.language == "Engleză"
                val allMsgs = _messages.value
                val userMsgs = allMsgs.filter { it.role == MessageRole.USER }

                val pairedFromChat = mutableListOf<Pair<QuestionDto, String>>()
                var currentQ: QuestionDto? = null
                val currentAnswers = mutableListOf<String>()

                for (msg in allMsgs) {
                    if (msg.role == MessageRole.INTERVIEWER && msg.questionData != null) {
                        if (currentQ != null && currentAnswers.isNotEmpty()) {
                            pairedFromChat.add(currentQ to currentAnswers.joinToString("\n"))
                        }
                        currentQ = msg.questionData
                        currentAnswers.clear()
                    } else if (msg.role == MessageRole.USER && currentQ != null) {
                        currentAnswers.add(msg.text.trim())
                    }
                }
                if (currentQ != null && currentAnswers.isNotEmpty()) {
                    pairedFromChat.add(currentQ to currentAnswers.joinToString("\n"))
                }

                val finalPairs: List<Pair<QuestionDto, String>> = when {
                    pairedFromChat.isNotEmpty() -> pairedFromChat
                    overrideQuestionsQueue.isNotEmpty() -> {
                        overrideQuestionsQueue.mapIndexedNotNull { idx, q ->
                            val ans = userMsgs.getOrNull(idx)?.text ?: return@mapIndexedNotNull null
                            q to ans
                        }
                    }
                    else -> emptyList()
                }

                val reviewsToEvaluate: List<QuestionReview> = if (finalPairs.isNotEmpty()) {
                    finalPairs.map { (q, ans) ->
                        val safeQuestionText = (if (isEn) q.questionEn else q.questionRo).orEmpty()
                            .ifBlank { q.questionRo.orEmpty().ifBlank { q.questionEn.orEmpty() } }
                        val safeRefAnswer = (if (isEn) q.referenceAnswerEn else q.referenceAnswerRo).orEmpty()
                            .ifBlank { q.referenceAnswerRo.orEmpty().ifBlank { q.referenceAnswerEn.orEmpty() } }
                        val safeKeywords: List<String> = ((if (isEn) q.keywordsEn else q.keywordsRo)
                            ?: q.keywordsRo
                            ?: q.keywordsEn
                            ?: emptyList()).filterNotNull()
                        val safeTip = (if (isEn) q.interviewerTipEn else q.interviewerTipRo)
                            ?: q.interviewerTipRo
                            ?: q.interviewerTipEn

                        QuestionReview(
                            questionId = q.id,
                            questionText = safeQuestionText,
                            userResponse = ans,
                            referenceAnswer = safeRefAnswer,
                            matchedKeywords = emptyList(),
                            missedKeywords = safeKeywords,
                            interviewerTip = safeTip
                        )
                    }
                } else if (userMsgs.isNotEmpty()) {
                    runCatching {
                        repository.generateReport(allMsgs, _config.value.language).reviews
                    }.getOrDefault(emptyList())
                } else {
                    emptyList()
                }

                var instantScoreSum = 0
                val scoredReviews = reviewsToEvaluate.map { rev ->
                    val safeMatched = rev.matchedKeywords.orEmpty()
                    val safeMissed = rev.missedKeywords.orEmpty()
                    val allKeywords = (safeMatched + safeMissed).distinct()
                    val wasHintUsed = hintedQuestionIds.contains(rev.questionId)

                    val evalResult = runCatching {
                        CtciRubricEvaluator.evaluateAnswer(
                            userAnswer = rev.userResponse,
                            referenceAnswer = rev.referenceAnswer,
                            keywords = allKeywords,
                            usedHint = wasHintUsed
                        )
                    }.getOrNull()

                    val qScore: Int
                    val matched: List<String>
                    val missed: List<String>
                    val rubricHeader: String

                    if (evalResult != null) {
                        qScore = evalResult.score
                        matched = evalResult.matchedKeywords.orEmpty()
                        missed = evalResult.missedKeywords.orEmpty()
                        rubricHeader = (if (isEn) evalResult.rubricSummaryEn else evalResult.rubricSummaryRo).orEmpty()
                    } else {
                        val lowerAns = rev.userResponse.lowercase()
                        matched = allKeywords.filter { lowerAns.contains(it.lowercase()) }
                        missed = allKeywords.filterNot { lowerAns.contains(it.lowercase()) }
                        val kwRatio = if (allKeywords.isNotEmpty()) {
                            (matched.size.toDouble() / allKeywords.size.toDouble()) * 70.0
                        } else 50.0
                        val lenBonus = (rev.userResponse.length.coerceAtMost(120) / 120.0) * 30.0
                        val penalty = if (wasHintUsed) 10 else 0
                        qScore = (kwRatio + lenBonus - penalty).toInt().coerceIn(15, 100)
                        rubricHeader = if (isEn) "CtCI Evaluation: $qScore%" else "Evaluare CtCI: $qScore%"
                    }

                    instantScoreSum += qScore

                    val tip = buildString {
                        append(rubricHeader)
                        if (!rev.interviewerTip.isNullOrBlank()) {
                            append("\n\n")
                            append(rev.interviewerTip)
                        }
                    }

                    rev.copy(
                        matchedKeywords = matched,
                        missedKeywords = missed,
                        interviewerTip = tip
                    ) to qScore
                }

                val instantReviews = scoredReviews.map { it.first }
                val instantScore = if (instantReviews.isNotEmpty()) {
                    (instantScoreSum / instantReviews.size).coerceIn(0, 100)
                } else 0

                if (recordedVoiceSamples.isNotEmpty()) {
                    val avgConf = recordedVoiceSamples.map { it.confidenceScore }.average().toInt()
                    val avgInfl = recordedVoiceSamples.map { it.inflectionScore }.average().toInt()
                    val avgDb = recordedVoiceSamples.map { it.averageDb }.average().toFloat()
                    val lastSample = recordedVoiceSamples.last()
                    _voiceMetricsReport.value = VoiceConfidenceMetrics(
                        confidenceScore = avgConf,
                        inflectionScore = avgInfl,
                        averageDb = avgDb,
                        feedbackRo = lastSample.feedbackRo,
                        feedbackEn = lastSample.feedbackEn
                    )
                }

                val instantReport = buildReportObject(
                    score = instantScore,
                    reviews = instantReviews,
                    isEn = isEn
                )
                _report.value = instantReport

                val allWords = userMsgs
                    .map { it.text }
                    .flatMap { it.lowercase().split(Regex("""[^a-zA-ZăâîșțĂÂÎȘȚ0-9]+""")) }
                    .filter { it.isNotBlank() }

                val fillerDictionary = setOf(
                    "ăăă", "aaa", "gen", "practic", "deci", "cumva", "efectiv",
                    "like", "basically", "actually", "literally", "um", "uh"
                )
                val foundFillers = allWords.filter { it in fillerDictionary }

                val totalSeconds = answerDurationsSec.sum().coerceAtLeast(1)
                val avgSeconds = if (answerDurationsSec.isNotEmpty()) totalSeconds / answerDurationsSec.size else 0
                val wpm = ((allWords.size.toDouble() / totalSeconds.toDouble()) * 60.0).toInt().coerceIn(0, 300)

                _sessionAnalytics.value = SessionAnalytics(
                    averageSecondsPerAnswer = avgSeconds,
                    wordsPerMinute = wpm,
                    fillerWordsCount = foundFillers.size,
                    detectedFillers = foundFillers.distinct(),
                    isTimedMode = _isTimedMode.value
                )

                val sessionTimestamp = System.currentTimeMillis()
                runCatching {
                    dao.insertInterview(
                        InterviewEntity(
                            domain = "${_config.value.domain} - ${_config.value.specialization}",
                            role = _config.value.role,
                            level = _config.value.experienceLevel,
                            score = instantReport.score,
                            summary = instantReport.summary,
                            timestamp = sessionTimestamp,
                            detailsJson = instantReport.reviews.toDetailsJson()
                        )
                    )
                }

                navigateOnce()

                val apiKey = _geminiApiKey.value.trim()
                if (apiKey.isNotBlank() && !apiKey.startsWith("cheia_ta") && scoredReviews.isNotEmpty()) {
                    val llmResults = coroutineScope {
                        scoredReviews.map { (rev, _) ->
                            async(Dispatchers.IO) {
                                GeminiAiService.evaluateResponseSemantically(
                                    apiKey = apiKey,
                                    question = rev.questionText,
                                    referenceAnswer = rev.referenceAnswer,
                                    userAnswer = rev.userResponse,
                                    language = _config.value.language
                                )
                            }
                        }.awaitAll()
                    }

                    if (llmResults.any { it != null }) {
                        var refinedSum = 0
                        val refinedReviews = scoredReviews.mapIndexed { idx, (rev, baseScore) ->
                            val llm = llmResults[idx]
                            val combinedScore = if (llm != null) {
                                ((baseScore * 0.4) + (llm.score * 0.6)).toInt().coerceIn(0, 100)
                            } else baseScore
                            refinedSum += combinedScore

                            val updatedTip = buildString {
                                append(rev.interviewerTip ?: "")
                                if (llm != null && llm.feedback.isNotBlank()) {
                                    append("\n\n🤖 Gemini Semantic Review (${llm.score}%): ")
                                    append(llm.feedback)
                                }
                            }
                            rev.copy(interviewerTip = updatedTip)
                        }

                        val refinedFinalScore = (refinedSum / refinedReviews.size).coerceIn(0, 100)
                        val refinedReport = buildReportObject(
                            score = refinedFinalScore,
                            reviews = refinedReviews,
                            isEn = isEn
                        )
                        _report.value = refinedReport

                        val savedRecord = savedInterviews.value.find { it.timestamp == sessionTimestamp }
                        if (savedRecord != null) {
                            runCatching {
                                dao.updateInterview(
                                    savedRecord.copy(
                                        score = refinedReport.score,
                                        summary = refinedReport.summary,
                                        detailsJson = refinedReport.reviews.toDetailsJson()
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (_report.value == null) {
                    _report.value = InterviewReport(
                        score = 0,
                        summary = "Raport generat local.",
                        strengths = emptyList(),
                        improvements = emptyList(),
                        reviews = emptyList()
                    )
                }
                navigateOnce()
            }
        }
    }

    private fun buildReportObject(
        score: Int,
        reviews: List<QuestionReview>,
        isEn: Boolean
    ): InterviewReport {
        val baseSummary = when {
            reviews.isEmpty() -> if (isEn)
                "Session ended before answering any questions."
            else
                "Sesiune încheiată fără răspunsuri înregistrate."
            score >= 80 -> if (isEn)
                "Strong performance! Solid technical depth, Big O awareness, and clear communication."
            else
                "Performanță excelentă! Profunzime tehnică, analiză Big O și comunicare clară."
            score >= 50 -> if (isEn)
                "Good foundation. Focus more on edge cases and explicit time/space complexity."
            else
                "Bază bună. Insistă mai mult pe cazurile limită și complexitatea timp/spațiu."
            else -> if (isEn)
                "Needs more practice on core concepts and structured explanations."
            else
                "Necesită exercițiu suplimentar pe conceptele de bază și structurarea răspunsului."
        }

        val videoNote = if (_isVideoCallMode.value) {
            _videoReport.value?.let { v ->
                if (isEn) " | Eye contact: ${v.eyeContactScore}%, Shoulders: ${v.postureStabilityScore}%, Gestures: ${v.handGestureScore}%"
                else " | Contact vizual: ${v.eyeContactScore}%, Umeri: ${v.postureStabilityScore}%, Gesticulație: ${v.handGestureScore}%"
            } ?: ""
        } else ""

        val voiceNote = _voiceMetricsReport.value?.let { vm ->
            if (isEn) " | Voice confidence: ${vm.confidenceScore}%, Inflection: ${vm.inflectionScore}%"
            else " | Încredere voce: ${vm.confidenceScore}%, Inflexiune: ${vm.inflectionScore}%"
        } ?: ""

        val derivedStrengths = reviews
            .flatMap { it.matchedKeywords }
            .distinct()
            .ifEmpty { listOf(if (isEn) "Structured explanation" else "Structurarea răspunsului") }

        val derivedImprovements = reviews
            .flatMap { it.missedKeywords }
            .distinct()
            .ifEmpty { listOf(if (isEn) "Explicit Big O & edge cases" else "Analiză explicită Big O și edge cases") }

        return InterviewReport(
            score = score,
            summary = baseSummary + videoNote + voiceNote,
            strengths = derivedStrengths,
            improvements = derivedImprovements,
            reviews = reviews
        )
    }

    fun addInterviewRecord(interview: InterviewEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertInterview(interview)
        }
    }

    fun updateInterviewRecord(interview: InterviewEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateInterview(interview)
        }
    }

    fun deleteInterviewRecord(interview: InterviewEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteInterview(interview)
        }
    }

    fun updateUserMessage(index: Int, newText: String) {
        if (newText.isBlank()) return
        val currentList = _messages.value.toMutableList()
        if (index in currentList.indices && currentList[index].role == MessageRole.USER) {
            currentList[index] = currentList[index].copy(text = newText.trim())
            _messages.value = currentList
        }
    }
}

data class SessionAnalytics(
    val averageSecondsPerAnswer: Int,
    val wordsPerMinute: Int,
    val fillerWordsCount: Int,
    val detectedFillers: List<String>,
    val isTimedMode: Boolean
)