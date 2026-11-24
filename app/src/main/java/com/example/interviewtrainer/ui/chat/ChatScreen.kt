package com.example.interviewtrainer.ui.chat

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.interviewtrainer.data.model.ChatMessage
import com.example.interviewtrainer.data.model.MessageRole
import com.example.interviewtrainer.utils.BodyLanguageAnalyzer
import com.example.interviewtrainer.utils.LiveVideoMetrics
import com.example.interviewtrainer.utils.VoiceHelper
import com.example.interviewtrainer.viewmodel.InterviewViewModel
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: InterviewViewModel,
    onFinishSession: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val config by viewModel.config.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val isTimedMode by viewModel.isTimedMode.collectAsState()
    val isEn = config.language == "Engleză"

    var userText by remember { mutableStateOf("") }
    var editingMessageIndex by remember { mutableStateOf<Int?>(null) }
    var editingMessageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    var isCameraEnabled by remember { mutableStateOf(false) }
    var liveMetrics by remember { mutableStateOf(LiveVideoMetrics()) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) isCameraEnabled = true
    }

    val videoAnalyzer = remember {
        BodyLanguageAnalyzer(context) { metrics ->
            liveMetrics = metrics
        }
    }

    var remainingSeconds by remember { mutableIntStateOf(120) }

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
            videoAnalyzer.close()
        }
    }

    LaunchedEffect(messages.size, isTimedMode) {
        if (isTimedMode) {
            remainingSeconds = 120
            while (remainingSeconds > 0) {
                delay(1000L)
                remainingSeconds--
            }
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

    val timerMinutes = remainingSeconds / 60
    val timerSecs = remainingSeconds % 60
    val timerFormatted = String.format("%02d:%02d", timerMinutes, timerSecs)
    val timerUrgent = remainingSeconds <= 20
    val cameraActive = hasCameraPermission && isCameraEnabled

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
                    IconButton(
                        onClick = {
                            if (!hasCameraPermission) {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            } else {
                                isCameraEnabled = !isCameraEnabled
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (cameraActive) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = "Analiză Video",
                            tint = if (cameraActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isTimedMode) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (timerUrgent) Color(0xFFFEE2E2) else MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = if (timerUrgent) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = timerFormatted,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (timerUrgent) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    FilledTonalButton(
                        onClick = {
                            voiceHelper.shutdown()
                            viewModel.setVideoReport(videoAnalyzer.buildSessionReport())
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

            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(messages) { index, message ->
                        val qData = message.questionData
                        val isBookmarked = qData != null && bookmarks.any { it.questionId == qData.id }

                        ChatBubble(
                            message = message,
                            isEn = isEn,
                            isCameraActive = cameraActive,
                            isBookmarked = isBookmarked,
                            onToggleBookmark = if (qData != null) {
                                { viewModel.toggleBookmark(qData) }
                            } else null,
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

                if (cameraActive) {
                    CameraPipPreview(
                        lifecycleOwner = lifecycleOwner,
                        analyzer = videoAnalyzer,
                        liveMetrics = liveMetrics,
                        isEn = isEn,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                    )
                }
            }

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
                        if (!isTimedMode) {
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
                        }

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
fun CameraPipPreview(
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    analyzer: BodyLanguageAnalyzer,
    liveMetrics: LiveVideoMetrics,
    isEn: Boolean,
    modifier: Modifier = Modifier
) {
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
        }
    }

    val badgeColor = when {
        !liveMetrics.faceDetected || liveMetrics.nervousFaceTouch -> Color(0xFFDC2626)
        !liveMetrics.eyeContactOk || !liveMetrics.shouldersAligned -> Color(0xFFD97706)
        else -> Color(0xFF16A34A)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, badgeColor),
        shadowElevation = 6.dp,
        modifier = modifier.size(width = 112.dp, height = 152.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    it.setAnalyzer(analysisExecutor, analyzer)
                                }

                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_FRONT_CAMERA,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isEn) liveMetrics.statusLabelEn else liveMetrics.statusLabelRo,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    isEn: Boolean = false,
    isCameraActive: Boolean = false,
    isBookmarked: Boolean = false,
    onToggleBookmark: (() -> Unit)? = null,
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = if (isCameraActive) 124.dp else 0.dp),
        contentAlignment = alignment
    ) {
        Surface(
            shape = bubbleShape,
            color = containerColor,
            border = borderStroke,
            modifier = Modifier.widthIn(max = 285.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (!isUser && !isHint) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
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

                        if (onToggleBookmark != null) {
                            IconButton(
                                onClick = onToggleBookmark,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Salvează întrebarea",
                                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
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