package com.example.interviewtrainer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.interviewtrainer.data.model.ChatMessage
import com.example.interviewtrainer.data.model.InterviewConfig
import com.example.interviewtrainer.data.model.InterviewReport
import com.example.interviewtrainer.data.model.MessageRole
import com.example.interviewtrainer.data.repository.InterviewRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InterviewViewModel(
    val repository: InterviewRepository = InterviewRepository()
) : ViewModel() {

    private val _config = MutableStateFlow(InterviewConfig())
    val config: StateFlow<InterviewConfig> = _config.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _report = MutableStateFlow<InterviewReport?>(null)
    val report: StateFlow<InterviewReport?> = _report.asStateFlow()

    fun updateConfig(domain: String, role: String, experienceLevel: String, language: String) {
        _config.value = InterviewConfig(
            domain = domain,
            role = role,
            experienceLevel = experienceLevel,
            language = language
        )
    }

    fun startInterview() {
        viewModelScope.launch {
            _isLoading.value = true
            _messages.value = emptyList()

            val (firstText, qData) = repository.startInterview(_config.value)

            _messages.value = listOf(
                ChatMessage(text = firstText, role = MessageRole.INTERVIEWER, questionData = qData)
            )
            _isLoading.value = false
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank() || _isLoading.value) return

        val currentList = _messages.value.toMutableList()
        val userMsg = ChatMessage(text = userText, role = MessageRole.USER)
        currentList.add(userMsg)
        _messages.value = currentList

        viewModelScope.launch {
            _isLoading.value = true
            val (aiText, qData) = repository.sendMessage(currentList, userText, _config.value)

            val updatedList = _messages.value.toMutableList()
            updatedList.add(ChatMessage(text = aiText, role = MessageRole.INTERVIEWER, questionData = qData))
            _messages.value = updatedList
            _isLoading.value = false
        }
    }

    fun finishInterview(onFinished: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _report.value = repository.generateReport(_messages.value)
            _isLoading.value = false
            onFinished()
        }
    }
}