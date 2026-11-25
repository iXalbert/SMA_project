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
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.interviewtrainer.data.model.MessageRole
import com.example.interviewtrainer.utils.BodyLanguageAnalyzer
import com.example.interviewtrainer.utils.LiveVideoMetrics
import com.example.interviewtrainer.utils.VoiceHelper
import com.example.interviewtrainer.viewmodel.InterviewViewModel
import java.util.concurrent.Executors

@Composable
fun VideoCallScreen(
    viewModel: InterviewViewModel,
    onFinishSession: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val config by viewModel.config.collectAsState()
    val interviewerStyle by viewModel.interviewerStyle.collectAsState()
    val isEn = config.language == "Engleză"

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var liveMetrics by remember { mutableStateOf(LiveVideoMetrics()) }
    var showQuickTextInput by remember { mutableStateOf(false) }
    var draftAnswer by remember { mutableStateOf("") }
    var isAmaliaSpeaking by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val videoAnalyzer = remember {
        BodyLanguageAnalyzer(context) { metrics ->
            liveMetrics = metrics
        }
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    val voiceHelper = remember {
        VoiceHelper(context) {}
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull().orEmpty().trim()
            if (spoken.isNotBlank()) {
                viewModel.sendMessage(spoken)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceHelper.shutdown()
            videoAnalyzer.close()
            analysisExecutor.shutdown()
        }
    }

    val lastInterviewerMessage = messages.lastOrNull { it.role == MessageRole.INTERVIEWER }
    val lastHintMessage = messages.lastOrNull { it.role == MessageRole.HINT }

    LaunchedEffect(lastInterviewerMessage?.text) {
        lastInterviewerMessage?.text?.let { text ->
            voiceHelper.speak(text, config.language)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "amaliaPulse")
    val avatarScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isAmaliaSpeaking) 1.12f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val statusBadgeColor = when {
        !liveMetrics.faceDetected || liveMetrics.nervousFaceTouch -> Color(0xFFEF4444)
        !liveMetrics.eyeContactOk || !liveMetrics.shouldersAligned -> Color(0xFFF59E0B)
        else -> Color(0xFF22C55E)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        if (hasCameraPermission) {
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
                                    it.setAnalyzer(analysisExecutor, videoAnalyzer)
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
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.72f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.88f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .scale(avatarScale)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SupportAgent,
                                contentDescription = "Amalia",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Amalia",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "$interviewerStyle • ${config.role}",
                                color = Color(0xFFCBD5E1),
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            lastInterviewerMessage?.text?.let { voiceHelper.speak(it, config.language) }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Repetă întrebarea",
                            tint = Color.White
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.68f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 165.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (isLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = if (isEn) "Amalia is evaluating your answer..." else "Amalia îți analizează răspunsul...",
                                color = Color(0xFFE2E8F0),
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        Text(
                            text = lastInterviewerMessage?.text ?: "",
                            color = Color.White,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                        if (lastHintMessage != null && messages.indexOf(lastHintMessage) > messages.indexOf(lastInterviewerMessage)) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = lastHintMessage.text,
                                color = Color(0xFFFDE047),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.72f),
                border = BorderStroke(1.5.dp, statusBadgeColor)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusBadgeColor)
                    )
                    Text(
                        text = if (isEn) liveMetrics.statusLabelEn else liveMetrics.statusLabelRo,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (showQuickTextInput) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = draftAnswer,
                        onValueChange = { draftAnswer = it },
                        placeholder = {
                            Text(
                                text = if (isEn) "Type answer..." else "Scrie răspunsul...",
                                color = Color.LightGray
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.9f),
                            unfocusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.9f)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                        maxLines = 3
                    )
                    FilledIconButton(
                        onClick = {
                            if (draftAnswer.isNotBlank()) {
                                viewModel.sendMessage(draftAnswer)
                                draftAnswer = ""
                                showQuickTextInput = false
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Trimite")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = { viewModel.requestHint() },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.18f),
                        contentColor = Color(0xFFFDE047)
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = "Indiciu")
                }

                FilledIconButton(
                    onClick = {
                        try {
                            val intent = voiceHelper.createSpeechIntent(config.language)
                            speechLauncher.launch(intent)
                        } catch (e: ActivityNotFoundException) {
                            val simulated = if (isEn)
                                "I would use a HashMap for O(1) lookup and validate edge cases first."
                            else
                                "Aș folosi un HashMap pentru căutare în O(1) și aș valida întâi cazurile limită."
                            viewModel.sendMessage(simulated)
                            Toast.makeText(context, "Răspuns vocal simulat (Emulator)", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Răspunde vocal",
                        modifier = Modifier.size(30.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = { showQuickTextInput = !showQuickTextInput },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Color.White.copy(alpha = 0.18f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Keyboard, contentDescription = "Tastatură")
                }

                FilledIconButton(
                    onClick = {
                        voiceHelper.shutdown()
                        viewModel.setVideoReport(videoAnalyzer.buildSessionReport())
                        viewModel.finishInterview(onFinishSession)
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFFDC2626),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.size(54.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Încheie apelul",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}