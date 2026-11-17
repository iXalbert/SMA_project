package com.example.interviewtrainer.ui.chat

import android.app.Activity
import android.content.ActivityNotFoundException
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.interviewtrainer.data.model.ChatMessage
import com.example.interviewtrainer.data.model.MessageRole
import com.example.interviewtrainer.utils.VoiceHelper
import com.example.interviewtrainer.viewmodel.InterviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: InterviewViewModel,
    onFinishSession: () -> Unit
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val config by viewModel.config.collectAsState()
    val isEn = config.language == "Engleză"

    var userText by remember { mutableStateOf("") }

    val voiceHelper = remember {
        VoiceHelper(context) {}
    }

    // Launcher recunoaștere vocală cu fallback pentru Emulator
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val spokenText = matches[0]
                userText = if (userText.isBlank()) spokenText else "$userText $spokenText"
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceHelper.shutdown()
        }
    }

    // Citire prin TTS a întrebării intervievatorului (Text-To-Speech funcționează și pe emulator!)
    LaunchedEffect(messages.size) {
        val lastMsg = messages.lastOrNull()
        if (lastMsg != null && lastMsg.role == MessageRole.INTERVIEWER) {
            voiceHelper.speak(lastMsg.text, config.language)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${config.role} (${config.experienceLevel})", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        voiceHelper.shutdown()
                        viewModel.finishInterview(onFinishSession)
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Încheie Interviul")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    ChatBubble(message = message)
                }

                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }

            // CHAT BAR
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 💡 Hint
                    IconButton(
                        onClick = { viewModel.requestHint() },
                        enabled = !isLoading,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = "Indiciu")
                    }

                    // 🎙️ Microfon (Încearcă STT nativ, iar dacă e emulator fără serviciu vocal, simulează dictarea)
                    IconButton(
                        onClick = {
                            try {
                                val intent = voiceHelper.createSpeechIntent(config.language)
                                speechLauncher.launch(intent)
                            } catch (e: ActivityNotFoundException) {
                                // Fallback pentru emulator fără servicii vocale
                                val simulatedText = if (isEn)
                                    "I would use a HashMap with O(1) time complexity and a bit vector for memory optimization."
                                else
                                    "Aș folosi un HashMap cu complexitate O(1) și un bit vector pentru optimizarea memoriei."

                                userText = if (userText.isBlank()) simulatedText else "$userText $simulatedText"
                                Toast.makeText(context, "Simulare dictare vocală (Emulator Mode)", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Dictare Vocală")
                    }

                    OutlinedTextField(
                        value = userText,
                        onValueChange = { userText = it },
                        placeholder = {
                            Text(if (isEn) "Type or speak answer..." else "Scrie sau dictează răspunsul...")
                        },
                        modifier = Modifier.weight(1f),
                        maxLines = 3
                    )

                    // Send
                    IconButton(
                        onClick = {
                            viewModel.sendMessage(userText)
                            userText = ""
                        },
                        enabled = userText.isNotBlank() && !isLoading
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Trimite")
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    val isHint = message.role == MessageRole.HINT

    val alignment = when {
        isUser -> Alignment.CenterEnd
        else -> Alignment.CenterStart
    }

    val containerColor = when {
        isUser -> MaterialTheme.colorScheme.primaryContainer
        isHint -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

    val textColor = when {
        isUser -> MaterialTheme.colorScheme.onPrimaryContainer
        isHint -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = containerColor,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                color = textColor,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}