package com.example.interviewtrainer.ui.feedback

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.interviewtrainer.data.model.QuestionReview
import com.example.interviewtrainer.viewmodel.InterviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    viewModel: InterviewViewModel,
    onBackToSetup: () -> Unit
) {
    val report by viewModel.report.collectAsState()
    val isEn = viewModel.config.collectAsState().value.language == "Engleză"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEn) "Interview Report" else "Raport Evaluare Interviu", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        val currentReport = report
        if (currentReport == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                currentReport.score >= 80 -> Color(0xFFE8F5E9)
                                currentReport.score >= 50 -> Color(0xFFFFF3E0)
                                else -> Color(0xFFFFEBEE)
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${currentReport.score}%",
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    currentReport.score >= 80 -> Color(0xFF2E7D32)
                                    currentReport.score >= 50 -> Color(0xFFE65100)
                                    else -> Color(0xFFC62828)
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = currentReport.summary,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (currentReport.reviews.isNotEmpty()) {
                    item {
                        Text(
                            text = if (isEn) "Detailed Question Analysis" else "Analiză Detaliată per Întrebare",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(currentReport.reviews) { review ->
                        QuestionReviewCard(review = review, isEn = isEn)
                    }
                }

                item {
                    Button(
                        onClick = onBackToSetup,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(if (isEn) "Start New Session" else "Interviu Nou")
                    }
                }
            }
        }
    }
}

@Composable
fun QuestionReviewCard(review: QuestionReview, isEn: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = review.questionText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()

            Text(
                text = "${if (isEn) "Your Answer: " else "Răspunsul Tău: "}${review.userResponse}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "${if (isEn) "Ideal Reference Answer: " else "Răspuns Ideal: "}${review.referenceAnswer}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )

            if (review.matchedKeywords.isNotEmpty()) {
                Text(
                    text = "${if (isEn) "Keywords hit: " else "Cuvinte cheie atinse: "}${review.matchedKeywords.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (review.missedKeywords.isNotEmpty()) {
                Text(
                    text = "${if (isEn) "Keywords missed: " else "Cuvinte cheie omise: "}${review.missedKeywords.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFC62828),
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Card distinct pentru sfaturile din CtCI / TensorFlow
            if (!review.interviewerTip.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = if (isEn) "💡 Interviewer Criteria & Tips (CtCI):" else "💡 La ce se uită intervievatorul (CtCI):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = review.interviewerTip,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}