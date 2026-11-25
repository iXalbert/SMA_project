package com.example.interviewtrainer.ui.feedback

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.interviewtrainer.data.model.QuestionReview
import com.example.interviewtrainer.viewmodel.InterviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    viewModel: InterviewViewModel,
    onBackToSetup: () -> Unit
) {
    val context = LocalContext.current
    val report by viewModel.report.collectAsState()
    val analytics by viewModel.sessionAnalytics.collectAsState()
    val videoReport by viewModel.videoReport.collectAsState()
    val voiceMetrics by viewModel.voiceMetricsReport.collectAsState()
    val config by viewModel.config.collectAsState()
    val isEn = config.language == "Engleză"

    var selectedReviewPair by remember { mutableStateOf<Pair<Int, QuestionReview>?>(null) }

    selectedReviewPair?.let { (index, review) ->
        BackHandler { selectedReviewPair = null }
        QuestionDetailScreen(
            questionNumber = index + 1,
            review = review,
            isEn = isEn,
            onBack = { selectedReviewPair = null }
        )
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Text(
                        text = if (isEn) "Interview Report" else "Raport Evaluare",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    report?.let { rep ->
                        IconButton(
                            onClick = {
                                val shareText = buildString {
                                    appendLine("📋 Raport Interview Trainer — ${config.role} (${config.experienceLevel})")
                                    appendLine("🏆 Scor CtCI: ${rep.score}%")
                                    appendLine("📝 Rezumat: ${rep.summary}")
                                    analytics?.let { a ->
                                        appendLine("⏱ Medie răspuns: ${a.averageSecondsPerAnswer}s • Ritm: ${a.wordsPerMinute} cuv/min • Cuvinte umplutură: ${a.fillerWordsCount}")
                                    }
                                    videoReport?.let { v ->
                                        appendLine("👁 Contact vizual: ${v.eyeContactScore}% • Postură umeri: ${v.postureStabilityScore}% • Gesticulație: ${v.handGestureScore}%")
                                    }
                                    voiceMetrics?.let { vm ->
                                        appendLine("🎙 Încredere voce: ${vm.confidenceScore}% • Inflexiune: ${vm.inflectionScore}%")
                                    }
                                }
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Trimite raportul"))
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Trimite raport")
                        }
                    }
                }
            )
        }
    ) { padding ->
        val currentReport = report
        if (currentReport == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val scoreColor = when {
                currentReport.score >= 80 -> Color(0xFF16A34A)
                currentReport.score >= 50 -> Color(0xFFD97706)
                else -> Color(0xFFDC2626)
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Card 1: Scor General CtCI
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isEn) "OVERALL SCORE (CtCI RUBRIC)" else "SCOR GENERAL (RUBRICĂ CtCI)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${currentReport.score}%",
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = scoreColor
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentReport.summary,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Card 2: Speech & Pace Analytics
                analytics?.let { stats ->
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MetricMiniItem(
                                    value = "${stats.averageSecondsPerAnswer}s",
                                    label = if (isEn) "Avg / Answer" else "Medie / Răspuns"
                                )
                                MetricMiniItem(
                                    value = "${stats.wordsPerMinute}",
                                    label = if (isEn) "Words / Min" else "Cuvinte / Min"
                                )
                                MetricMiniItem(
                                    value = "${stats.fillerWordsCount}",
                                    label = if (isEn) "Filler Words" else "Cuv. Umplutură"
                                )
                            }
                        }
                    }
                }

                // Card 3: Analiză Vocală (Inflexiune & Încredere)
                voiceMetrics?.let { vm ->
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = if (isEn) "VOCAL CONFIDENCE & INFLECTION" else "ANALIZĂ VOCALĂ (INFLEXIUNE & ÎNCREDERE)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    MetricMiniItem(
                                        value = "${vm.confidenceScore}%",
                                        label = if (isEn) "Voice Confidence" else "Încredere Voce"
                                    )
                                    MetricMiniItem(
                                        value = "${vm.inflectionScore}%",
                                        label = if (isEn) "Inflection" else "Inflexiune"
                                    )
                                }
                                Text(
                                    text = if (isEn) vm.feedbackEn else vm.feedbackRo,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Card 4: Analiză Video Nonverbală (TensorFlow Lite)
                videoReport?.let { vReport ->
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = if (isEn) "NON-VERBAL & BODY LANGUAGE (TENSORFLOW LITE)" else "LIMBAJ NONVERBAL & POSTURĂ (TENSORFLOW LITE)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    MetricMiniItem(
                                        value = "${vReport.eyeContactScore}%",
                                        label = if (isEn) "Eye Contact" else "Contact Vizual"
                                    )
                                    MetricMiniItem(
                                        value = "${vReport.postureStabilityScore}%",
                                        label = if (isEn) "Shoulders" else "Postură Umeri"
                                    )
                                    MetricMiniItem(
                                        value = "${vReport.handGestureScore}%",
                                        label = if (isEn) "Gestures" else "Gesticulație"
                                    )
                                    MetricMiniItem(
                                        value = "${vReport.engagementScore}%",
                                        label = if (isEn) "Engagement" else "Expresivitate"
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                Text(
                                    text = if (isEn) vReport.feedbackEn else vReport.feedbackRo,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (currentReport.reviews.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(top = 4.dp)) {
                            Text(
                                text = if (isEn) "Questions & Answers" else "Întrebări și Răspunsuri",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isEn) "Tap a question to inspect CtCI criteria & ideal answer"
                                else "Apasă pe o întrebare pentru criteriile CtCI și răspunsul ideal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    itemsIndexed(currentReport.reviews) { index, review ->
                        CompactQuestionCard(
                            questionNumber = index + 1,
                            review = review,
                            isEn = isEn,
                            onClick = { selectedReviewPair = index to review }
                        )
                    }
                }

                item {
                    Button(
                        onClick = onBackToSetup,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .height(52.dp)
                    ) {
                        Text(
                            text = if (isEn) "Start New Session" else "Interviu Nou",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricMiniItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CompactQuestionCard(
    questionNumber: Int,
    review: QuestionReview,
    isEn: Boolean,
    onClick: () -> Unit
) {
    val totalKeywords = review.matchedKeywords.size + review.missedKeywords.size

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isEn) "Question #$questionNumber" else "Întrebarea #$questionNumber",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    if (totalKeywords > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${review.matchedKeywords.size}/$totalKeywords ${if (isEn) "keywords" else "cuvinte cheie"}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Text(
                    text = review.questionText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "${if (isEn) "Your answer: " else "Răspunsul tău: "}${review.userResponse}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Vezi detalii",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuestionDetailScreen(
    questionNumber: Int,
    review: QuestionReview,
    isEn: Boolean,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Text(
                        text = if (isEn) "Question #$questionNumber Analysis" else "Detalii Întrebarea #$questionNumber",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isEn) "Back" else "Înapoi"
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isEn) "QUESTION" else "ÎNTREBARE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = review.questionText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isEn) "YOUR ANSWER" else "RĂSPUNSUL TĂU",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = review.userResponse,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (review.matchedKeywords.isNotEmpty() || review.missedKeywords.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isEn) "KEYWORDS EVALUATION" else "EVALUARE CUVINTE CHEIE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )

                        if (review.matchedKeywords.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                review.matchedKeywords.forEach { kw ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFDCFCE7)
                                    ) {
                                        Text(
                                            text = "✓ $kw",
                                            color = Color(0xFF166534),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (review.missedKeywords.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                review.missedKeywords.forEach { kw ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFFEE2E2)
                                    ) {
                                        Text(
                                            text = "✕ $kw",
                                            color = Color(0xFF991B1B),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isEn) "IDEAL REFERENCE ANSWER" else "RĂSPUNS IDEAL DE REFERINȚĂ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = review.referenceAnswer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (!review.interviewerTip.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isEn) "💡 INTERVIEWER CRITERIA" else "💡 LA CE SE UITĂ INTERVIEVATORUL",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = review.interviewerTip,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}