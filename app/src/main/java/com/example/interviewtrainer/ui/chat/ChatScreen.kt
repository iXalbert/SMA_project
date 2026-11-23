package com.example.interviewtrainer.ui.chat

import android.app.Activity
import android.content.ActivityNotFoundException
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SupportAgent
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
    var editingMessageIndex by remember { mutableStateOf<Int?>(null) }
    var editingMessageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val voiceHelper = remember {
        VoiceHelper(context) {}
    }

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

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
        val lastMsg = messages.lastOrNull()
        if (lastMsg != null && lastMsg.role == MessageRole.INTERVIEWER) {
            voiceHelper.speak(lastMsg.text, config.language)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                title = {
                    Column {
                        Text(
                            text = config.role,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${config.experienceLevel} • ${config.language}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = {
                            voiceHelper.shutdown()
                            viewModel.finishInterview(onFinishSession)
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEn) "Finish" else "Încheie",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Zona principală de mesaje (aici vom putea suprapune ulterior mini-preview-ul CameraX)
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(messages) { index, message ->
                        ChatBubble(
                            message = message,
                            isEn = isEn,
                            onEditUserMessage = if (message.role == MessageRole.USER) {
                                {
                                    editingMessageIndex = index
                                    editingMessageText = message.text
                                }
                            } else null
                        )
                    }

                    if (isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Text(
                                            text = if (isEn) "Interviewer is thinking..." else "Intervievatorul analizează...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bara de input modernă
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Buton Indiciu
                        FilledTonalIconButton(
                            onClick = { viewModel.requestHint() },
                            enabled = !isLoading,
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = "Indiciu", modifier = Modifier.size(20.dp))
                        }

                        // Buton Dictare Vocală
                        FilledTonalIconButton(
                            onClick = {
                                try {
                                    val intent = voiceHelper.createSpeechIntent(config.language)
                                    speechLauncher.launch(intent)
                                } catch (e: ActivityNotFoundException) {
                                    val simulatedText = if (isEn)
                                        "I would use a HashMap with O(1) time complexity."
                                    else
                                        "Aș folosi un HashMap cu complexitate O(1)."

                                    userText = if (userText.isBlank()) simulatedText else "$userText $simulatedText"
                                    Toast.makeText(context, "Simulare dictare vocală (Emulator Mode)", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Dictare Vocală", modifier = Modifier.size(20.dp))
                        }

                        OutlinedTextField(
                            value = userText,
                            onValueChange = { userText = it },
                            placeholder = {
                                Text(
                                    text = if (isEn) "Type or dictate answer..." else "Scrie sau dictează...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f),
                            maxLines = 4
                        )

                        // Buton Trimite
                        FilledIconButton(
                            onClick = {
                                viewModel.sendMessage(userText)
                                userText = ""
                            },
                            enabled = userText.isNotBlank() && !isLoading,
                            shape = CircleShape,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Trimite", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // Dialog UPDATE pentru corectarea transcrierii vocale
        if (editingMessageIndex != null) {
            AlertDialog(
                onDismissRequest = { editingMessageIndex = null },
                title = {
                    Text(
                        text = if (isEn) "Edit Transcribed Answer" else "Corectează răspunsul",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    OutlinedTextField(
                        value = editingMessageText,
                        onValueChange = { editingMessageText = it },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            editingMessageIndex?.let { idx ->
                                viewModel.updateUserMessage(idx, editingMessageText)
                            }
                            editingMessageIndex = null
                        }
                    ) {
                        Text(if (isEn) "Save" else "Salvează")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingMessageIndex = null }) {
                        Text(if (isEn) "Cancel" else "Anulează")
                    }
                }
            )
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    isEn: Boolean = false,
    onEditUserMessage: (() -> Unit)? = null
) {
    val isUser = message.role == MessageRole.USER
    val isHint = message.role == MessageRole.HINT

    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart

    val bubbleShape = when {
        isUser -> RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
        isHint -> RoundedCornerShape(14.dp)
        else -> RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    }

    val containerColor = when {
        isUser -> MaterialTheme.colorScheme.primary
        isHint -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val textColor = when {
        isUser -> MaterialTheme.colorScheme.onPrimary
        isHint -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val borderStroke = if (!isUser && !isHint) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    } else null

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Surface(
            shape = bubbleShape,
            color = containerColor,
            border = borderStroke,
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (!isUser && !isHint) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isEn) "AI Interviewer" else "Intervievator AI",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = message.text,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium
                )

                if (isUser && onEditUserMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f),
                            onClick = onEditUserMessage
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editează răspunsul",
                                    tint = textColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = if (isEn) "Edit" else "Editează",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}