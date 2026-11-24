package com.example.interviewtrainer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.interviewtrainer.data.model.*
import com.example.interviewtrainer.data.remote.AppDatabase
import com.example.interviewtrainer.data.repository.InterviewRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.interviewtrainer.utils.VideoSessionReport

class InterviewViewModel(application: Application) : AndroidViewModel(application) {

    val repository = InterviewRepository()
    private val dao = AppDatabase.getDatabase(application).interviewDao()

    // 1. Starea Temei Grafice (Dark / Light)
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    // 2. Favorite (Bookmarks)
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
    val isTimedMode: StateFlow<Boolean> = _isTimedMode

    private val _sessionAnalytics = MutableStateFlow<SessionAnalytics?>(null)
    val sessionAnalytics: StateFlow<SessionAnalytics?> = _sessionAnalytics

    private val _videoReport = MutableStateFlow<VideoSessionReport?>(null)
    val videoReport: StateFlow<VideoSessionReport?> = _videoReport

    fun setVideoReport(report: VideoSessionReport?) {
        _videoReport.value = report
    }

    private var questionAskedTimestamp: Long = System.currentTimeMillis()
    private val answerDurationsSec = mutableListOf<Int>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            dao.getAllInterviews().collect { list ->
                (savedInterviews as MutableStateFlow).value = list
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            dao.getAllBookmarks().collect { list ->
                (bookmarkedQuestions as MutableStateFlow).value = list
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            dao.getAllCustomQuestions().collect { list ->
                (customQuestions as MutableStateFlow).value = list
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            dao.getAllBookmarks().collect { list ->
                (bookmarks as MutableStateFlow).value = list
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
        isTimedMode: Boolean = false
    ) {
        _config.value = InterviewConfig(
            domain,
            specialization,
            role,
            experienceLevel,
            language
        )
        _isTimedMode.value = isTimedMode
    }

    fun startInterview(rawGistUrl: String) {

        _videoReport.value = null

        answerDurationsSec.clear()
        _sessionAnalytics.value = null
        questionAskedTimestamp = System.currentTimeMillis()

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _messages.value = emptyList()

            val customForRole = dao.getCustomQuestionsByRole(_config.value.role).map { it.toQuestionDto() }
            val (firstText, qData) = repository.startInterview(_config.value, rawGistUrl, customForRole)

            _messages.value = listOf(
                ChatMessage(text = firstText, role = MessageRole.INTERVIEWER, questionData = qData)
            )
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
            val (aiText, qData) = repository.sendMessage(userText, _config.value)

            val updatedList = _messages.value.toMutableList()
            updatedList.add(ChatMessage(text = aiText, role = MessageRole.INTERVIEWER, questionData = qData))
            _messages.value = updatedList
            _isLoading.value = false
        }
    }

    fun requestHint() {
        val hintText = repository.getHintForCurrentQuestion(_config.value.language)
        if (hintText.isNotBlank()) {
            val currentList = _messages.value.toMutableList()
            val hintPrefix = if (_config.value.language == "Engleză") "💡 Hint: " else "💡 Indiciu: "
            currentList.add(ChatMessage(text = hintPrefix + hintText, role = MessageRole.HINT))
            _messages.value = currentList
        }
    }

    fun finishInterview(onFinished: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val generatedReport = repository.generateReport(_messages.value, _config.value.language)
            _report.value = generatedReport

            dao.insertInterview(
                InterviewEntity(
                    domain = "${_config.value.domain} - ${_config.value.specialization}",
                    role = _config.value.role,
                    level = _config.value.experienceLevel,
                    score = generatedReport.score,
                    summary = generatedReport.summary,
                    detailsJson = generatedReport.reviews.toDetailsJson()
                )
            )

            val userResponses = _messages.value
                .filter { it.role == MessageRole.USER }
                .map { it.text }

            val allWords = userResponses
                .flatMap { it.lowercase().split(Regex("[^a-zA-ZăâîșțĂÂÎȘȚ0-9]+")) }
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

    // CRUD: CREATE
    fun addInterviewRecord(interview: InterviewEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertInterview(interview)
        }
    }

    // CRUD: UPDATE
    fun updateInterviewRecord(interview: InterviewEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateInterview(interview)
        }
    }

    // CRUD: DELETE
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

