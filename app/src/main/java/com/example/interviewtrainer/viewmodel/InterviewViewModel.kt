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

class InterviewViewModel(application: Application) : AndroidViewModel(application) {

    val repository = InterviewRepository()
    private val dao = AppDatabase.getDatabase(application).interviewDao()

    val savedInterviews: StateFlow<List<InterviewEntity>> = MutableStateFlow(emptyList())

    private val _config = MutableStateFlow(InterviewConfig())
    val config: StateFlow<InterviewConfig> = _config.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _report = MutableStateFlow<InterviewReport?>(null)
    val report: StateFlow<InterviewReport?> = _report.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            dao.getAllInterviews().collect { list ->
                (savedInterviews as MutableStateFlow).value = list
            }
        }
    }

    fun updateConfig(
        domain: String,
        specialization: String,
        role: String,
        experienceLevel: String,
        language: String
    ) {
        _config.value = InterviewConfig(
            domain = domain,
            specialization = specialization,
            role = role,
            experienceLevel = experienceLevel,
            language = language
        )
    }

    fun startInterview(rawGistUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _messages.value = emptyList()

            val (firstText, qData) = repository.startInterview(_config.value, rawGistUrl)

            _messages.value = listOf(
                ChatMessage(text = firstText, role = MessageRole.INTERVIEWER, questionData = qData)
            )
            _isLoading.value = false
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank() || _isLoading.value) return

        val currentList = _messages.value.toMutableList()
        currentList.add(ChatMessage(text = userText, role = MessageRole.USER))
        _messages.value = currentList

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
                    summary = generatedReport.summary
                )
            )

            _isLoading.value = false
            withContext(Dispatchers.Main) {
                onFinished()
            }
        }
    }
}