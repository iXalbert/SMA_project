package com.example.interviewtrainer.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import kotlin.math.sqrt

data class VoiceConfidenceMetrics(
    val confidenceScore: Int = 0,
    val inflectionScore: Int = 0,
    val averageDb: Float = 0f,
    val feedbackRo: String = "",
    val feedbackEn: String = ""
)

class HandsFreeVoiceManager(
    private val context: Context,
    private val onSpeakingStateChanged: (Boolean) -> Unit,
    private val onListeningStateChanged: (Boolean) -> Unit,
    private val onPartialResult: (String) -> Unit,
    private val onFinalSpeechResult: (String, VoiceConfidenceMetrics) -> Unit,
    private val onError: (String) -> Unit
) {
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var speechRecognizer: SpeechRecognizer? = null
    private var currentLanguage: String = "Română"
    var isHandsFreeEnabled: Boolean = true

    private val rmsSamples = mutableListOf<Float>()

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        onSpeakingStateChanged(true)
                    }

                    override fun onDone(utteranceId: String?) {
                        onSpeakingStateChanged(false)
                        if (isHandsFreeEnabled) {
                            android.os.Handler(context.mainLooper).post {
                                startListening(currentLanguage)
                            }
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        onSpeakingStateChanged(false)
                    }
                })
            }
        }

        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        rmsSamples.clear()
                        onListeningStateChanged(true)
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {
                        if (rmsdB > -2f) {
                            rmsSamples.add(rmsdB)
                        }
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        onListeningStateChanged(false)
                    }

                    override fun onError(error: Int) {
                        onListeningStateChanged(false)
                        if (error != SpeechRecognizer.ERROR_CLIENT) {
                            onError("Audio code: $error")
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        onListeningStateChanged(false)
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull().orEmpty().trim()
                        if (text.isNotBlank()) {
                            val metrics = computeVocalMetrics()
                            onFinalSpeechResult(text, metrics)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partial = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            .orEmpty()
                        if (partial.isNotBlank()) {
                            onPartialResult(partial)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    fun speak(text: String, language: String) {
        currentLanguage = language
        stopListening()
        if (!isTtsReady) return

        val locale = if (language == "Engleză") Locale.US else Locale("ro", "RO")
        tts?.language = locale
        val cleanText = text.replace(Regex("""[*#💡✓✕]"""), "")
        val params = Bundle()
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, "AMALIA_UTTERANCE")
    }

    fun startListening(language: String) {
        currentLanguage = language
        val recognizer = speechRecognizer ?: run {
            onError("SpeechRecognizer indisponibil")
            return
        }
        val localeTag = if (language == "Engleză") "en-US" else "ro-RO"
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2200L)
        }
        try {
            recognizer.startListening(intent)
        } catch (_: Exception) {
            onListeningStateChanged(false)
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            onListeningStateChanged(false)
        } catch (_: Exception) {}
    }

    private fun computeVocalMetrics(): VoiceConfidenceMetrics {
        if (rmsSamples.isEmpty()) {
            return VoiceConfidenceMetrics(
                confidenceScore = 75,
                inflectionScore = 72,
                averageDb = 5.0f,
                feedbackRo = "Voce clară.",
                feedbackEn = "Clear voice."
            )
        }

        val meanDb = rmsSamples.average().toFloat()
        val variance = rmsSamples.map { (it - meanDb) * (it - meanDb) }.average().toFloat()
        val stdDev = sqrt(variance)

        val confidence = ((meanDb + 2f) * 9.5f).toInt().coerceIn(45, 98)
        val inflection = (stdDev * 28f + 42f).toInt().coerceIn(40, 96)

        val fbRo = when {
            confidence >= 80 && inflection >= 75 -> "Proiecție vocală sigură și intonație dinamică."
            confidence < 65 -> "Vorbește puțin mai tare și mai hotărât pentru a proiecta siguranță."
            inflection < 60 -> "Variază mai mult intonația pe cuvintele cheie pentru a evita tonul monoton."
            else -> "Ton vocal echilibrat și ritm constant."
        }

        val fbEn = when {
            confidence >= 80 && inflection >= 75 -> "Confident vocal projection and dynamic inflection."
            confidence < 65 -> "Project your voice slightly louder to convey stronger confidence."
            inflection < 60 -> "Add more vocal inflection on key technical terms to avoid sounding flat."
            else -> "Balanced vocal tone and steady pacing."
        }

        return VoiceConfidenceMetrics(
            confidenceScore = confidence,
            inflectionScore = inflection,
            averageDb = meanDb,
            feedbackRo = fbRo,
            feedbackEn = fbEn
        )
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
    }
}