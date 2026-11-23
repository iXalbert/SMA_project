package com.example.interviewtrainer.utils

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceHelper(
    private val context: Context,
    private val onSpeechResult: (String) -> Unit
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isTtsReady = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
        }
    }

    fun speak(text: String, language: String) {
        if (!isTtsReady) return
        val locale = if (language == "Engleză") Locale.ENGLISH else Locale("ro", "RO")
        tts?.language = locale
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "InterviewTTS")
    }

    fun createSpeechIntent(language: String): Intent {
        val langCode = if (language == "Engleză") "en-US" else "ro-RO"
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langCode)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langCode)
            putExtra(RecognizerIntent.EXTRA_PROMPT, if (language == "Engleză") "Speak your answer..." else "Răspunde vocal...")
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}