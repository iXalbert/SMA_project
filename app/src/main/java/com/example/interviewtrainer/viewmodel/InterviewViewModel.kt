package com.example.interviewtrainer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.interviewtrainer.data.model.*
import com.example.interviewtrainer.data.remote.AppDatabase
import com.example.interviewtrainer.data.repository.InterviewRepository
import com.example.interviewtrainer.utils.CtciRubricEvaluator
import com.example.interviewtrainer.utils.VideoSessionReport
import kotlinx.coroutines.Dispatchers
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

    private val _sessionAnalytics = MutableStateFlow<SessionAnalytics?>(null)
    val sessionAnalytics: StateFlow<SessionAnalytics?> = _sessionAnalytics.asStateFlow()

    private val _videoReport = MutableStateFlow<VideoSessionReport?>(null)
    val videoReport: StateFlow<VideoSessionReport?> = _videoReport.asStateFlow()

    fun setVideoReport(report: VideoSessionReport?) {
        _videoReport.value = report
    }

    private var questionAskedTimestamp: Long = System.currentTimeMillis()
    private val answerDurationsSec = mutableListOf<Int>()
    private val hintedQuestionIds = mutableSetOf<Int>()
    private var followUpAskedForCurrentQuestion = false

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
                        questionRo = question.questionRo,
                        questionEn = question.questionEn,
                        referenceAnswerRo = question.referenceAnswerRo,
                        referenceAnswerEn = question.referenceAnswerEn,
                        hintRo = question.hintRo,
                        hintEn = question.hintEn
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

    fun setInterviewMode(videoCall: Boolean, style: String = _interviewerStyle.value) {
        _isVideoCallMode.value = videoCall
        _interviewerStyle.value = style
    }

    fun startInterview(rawGistUrl: String) {
        hintedQuestionIds.clear()
        followUpAskedForCurrentQuestion = false
        _videoReport.value = null
        answerDurationsSec.clear()
        _sessionAnalytics.value = null
        questionAskedTimestamp = System.currentTimeMillis()

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
            val (aiText, qData) = repository.sendMessage(userText, _config.value)

            val updatedList = _messages.value.toMutableList()
            updatedList.add(ChatMessage(text = aiText, role = MessageRole.INTERVIEWER, questionData = qData))
            _messages.value = updatedList
            questionAskedTimestamp = System.currentTimeMillis()
            _isLoading.value = false
        }
    }

    fun requestHint() {
        val hintText = repository.getHintForCurrentQuestion(_config.value.language)
        if (hintText.isNotBlank()) {
            val lastQuestionId = _messages.value
                .lastOrNull { it.role == MessageRole.INTERVIEWER && it.questionData != null }
                ?.questionData?.id
            if (lastQuestionId != null) {
                hintedQuestionIds.add(lastQuestionId)
            }

            val currentList = _messages.value.toMutableList()
            val hintPrefix = if (_config.value.language == "Engleză") "💡 CtCI Hint: " else "💡 Indiciu CtCI: "
            currentList.add(ChatMessage(text = hintPrefix + hintText, role = MessageRole.HINT))
            _messages.value = currentList
        }
    }

    fun finishInterview(onFinished: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val isEn = _config.value.language == "Engleză"

            val baseReport = repository.generateReport(_messages.value, _config.value.language)

            val askedQuestionIds = _messages.value
                .filter { it.role == MessageRole.INTERVIEWER && it.questionData != null }
                .mapNotNull { it.questionData?.id }

            var ctciTotalScore = 0
            val enhancedReviews = baseReport.reviews.mapIndexed { idx, rev ->
                val allKeywords = rev.matchedKeywords + rev.missedKeywords
                val qId = askedQuestionIds.getOrNull(idx)
                val wasHintUsed = qId != null && hintedQuestionIds.contains(qId)

                val ctciEval = CtciRubricEvaluator.evaluateAnswer(
                    userAnswer = rev.userResponse,
                    referenceAnswer = rev.referenceAnswer,
                    keywords = allKeywords,
                    usedHint = wasHintUsed
                )

                ctciTotalScore += ctciEval.score

                val rubricHeader = if (isEn) ctciEval.rubricSummaryEn else ctciEval.rubricSummaryRo
                val updatedTip = buildString {
                    append(rubricHeader)
                    if (!rev.interviewerTip.isNullOrBlank()) {
                        append("\n\n")
                        append(rev.interviewerTip)
                    }
                }

                rev.copy(
                    matchedKeywords = ctciEval.matchedKeywords,
                    missedKeywords = ctciEval.missedKeywords,
                    interviewerTip = updatedTip
                )
            }

            val finalScore = if (enhancedReviews.isNotEmpty()) {
                (ctciTotalScore / enhancedReviews.size).coerceIn(0, 100)
            } else {
                baseReport.score
            }

            val videoNote = _videoReport.value?.let { v ->
                if (isEn) " | Eye contact: ${v.eyeContactScore}%, Shoulders: ${v.postureStabilityScore}%, Gestures: ${v.handGestureScore}%"
                else " | Contact vizual: ${v.eyeContactScore}%, Umeri: ${v.postureStabilityScore}%, Gesticulație: ${v.handGestureScore}%"
            } ?: ""

            val finalSummary = baseReport.summary + videoNote

            val finalReport = baseReport.copy(
                score = finalScore,
                summary = finalSummary,
                reviews = enhancedReviews
            )
            _report.value = finalReport

            dao.insertInterview(
                InterviewEntity(
                    domain = "${_config.value.domain} - ${_config.value.specialization}",
                    role = _config.value.role,
                    level = _config.value.experienceLevel,
                    score = finalReport.score,
                    summary = finalReport.summary,
                    detailsJson = finalReport.reviews.toDetailsJson()
                )
            )

            val userResponses = _messages.value
                .filter { it.role == MessageRole.USER }
                .map { it.text }

            val allWords = userResponses
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

            _isLoading.value = false
            withContext(Dispatchers.Main) {
                onFinished()
            }
        }
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