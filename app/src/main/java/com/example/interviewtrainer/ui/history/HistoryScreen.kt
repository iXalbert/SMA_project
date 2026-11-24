package com.example.interviewtrainer.ui.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.interviewtrainer.data.model.InterviewEntity
import com.example.interviewtrainer.data.model.QuestionReview
import com.example.interviewtrainer.ui.feedback.CompactQuestionCard
import com.example.interviewtrainer.ui.feedback.QuestionDetailScreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    interviews: List<InterviewEntity>,
    onAddInterview: (InterviewEntity) -> Unit,
    onUpdateInterview: (InterviewEntity) -> Unit,
    onDeleteInterview: (InterviewEntity) -> Unit,
    onBack: () -> Unit
) {
    var selectedInterviewId by remember { mutableStateOf<Long?>(null) }
    var editingInterview by remember { mutableStateOf<InterviewEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val selectedInterview = interviews.find { it.id == selectedInterviewId }

    if (selectedInterview != null) {
        BackHandler { selectedInterviewId = null }
        HistoryDetailScreen(
            interview = selectedInterview,
            onBack = { selectedInterviewId = null },
            onEdit = { editingInterview = selectedInterview },
            onDelete = {
                onDeleteInterview(selectedInterview)
                selectedInterviewId = null
                scope.launch {
                    snackbarHostState.showSnackbar("Sesiunea a fost ștearsă din baza de date.")
                }
            }
        )
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    title = { Text("Istoric & Progres", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Înapoi")
                        }
                    },
                    actions = {
                        FilledTonalIconButton(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Adaugă Sesiune")
                        }
                    }
                )
            }
        ) { padding ->
            if (interviews.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Nu ai salvat încă niciun interviu.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { showCreateDialog = true }, shape = RoundedCornerShape(12.dp)) {
                            Text("Adaugă o sesiune de test")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Dashboard de Statistici calculate din Room
                    item {
                        val avgScore = interviews.map { it.score }.average().toInt()
                        val bestScore = interviews.maxOf { it.score }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${interviews.size}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Sesiuni",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$avgScore%",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Scor Mediu",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$bestScore%",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF16A34A)
                                    )
                                    Text(
                                        text = "Record",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Apasă pe un interviu pentru a vedea întrebările și răspunsurile",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    items(interviews, key = { it.id }) { item ->
                        CompactHistoryCard(
                            interview = item,
                            onClick = { selectedInterviewId = item.id }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        InterviewFormDialog(
            title = "Adaugă Sesiune Nouă",
            initialDomain = "IT & Software",
            initialRole = "Software Engineer",
            initialLevel = "Junior",
            initialScore = "85",
            initialSummary = "Simulare practică CtCI - HashMap și LinkedList.",
            onDismiss = { showCreateDialog = false },
            onConfirm = { domain, role, level, score, summary ->
                onAddInterview(
                    InterviewEntity(
                        domain = domain,
                        role = role,
                        level = level,
                        score = score,
                        summary = summary
                    )
                )
                showCreateDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Sesiune adăugată în Room DB.")
                }
            }
        )
    }

    editingInterview?.let { current ->
        InterviewFormDialog(
            title = "Editează Sesiunea",
            initialDomain = current.domain,
            initialRole = current.role,
            initialLevel = current.level,
            initialScore = current.score.toString(),
            initialSummary = current.summary,
            onDismiss = { editingInterview = null },
            onConfirm = { domain, role, level, score, summary ->
                onUpdateInterview(
                    current.copy(
                        domain = domain,
                        role = role,
                        level = level,
                        score = score,
                        summary = summary
                    )
                )
                editingInterview = null
                scope.launch {
                    snackbarHostState.showSnackbar("Modificările au fost salvate.")
                }
            }
        )
    }
}

@Composable
fun CompactHistoryCard(
    interview: InterviewEntity,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(interview.timestamp))
    val scoreColor = when {
        interview.score >= 80 -> Color(0xFF16A34A)
        interview.score >= 50 -> Color(0xFFD97706)
        else -> Color(0xFFDC2626)
    }

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
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${interview.role} (${interview.level})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = scoreColor.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "${interview.score}%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = interview.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Deschide",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(
    interview: InterviewEntity,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val reviews = remember(interview.detailsJson) { interview.toQuestionReviews() }
    var selectedReviewPair by remember { mutableStateOf<Pair<Int, QuestionReview>?>(null) }

    selectedReviewPair?.let { (index, review) ->
        BackHandler { selectedReviewPair = null }
        QuestionDetailScreen(
            questionNumber = index + 1,
            review = review,
            isEn = false,
            onBack = { selectedReviewPair = null }
        )
        return
    }

    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(interview.timestamp))
    val scoreColor = when {
        interview.score >= 80 -> Color(0xFF16A34A)
        interview.score >= 50 -> Color(0xFFD97706)
        else -> Color(0xFFDC2626)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = { Text("${interview.role} (${interview.level})", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Înapoi")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editează")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Șterge",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Scor obținut",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${interview.score} / 100",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = scoreColor
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Domeniu: ${interview.domain}", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Data: $dateStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = interview.summary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (reviews.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(top = 4.dp)) {
                        Text(
                            text = "Întrebări și Răspunsuri",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Apasă pe o întrebare pentru cuvinte cheie și răspunsul ideal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                itemsIndexed(reviews) { index, review ->
                    CompactQuestionCard(
                        questionNumber = index + 1,
                        review = review,
                        isEn = false,
                        onClick = { selectedReviewPair = index to review }
                    )
                }
            } else {
                item {
                    Text(
                        text = "Această sesiune nu are întrebări detaliate salvate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun InterviewFormDialog(
    title: String,
    initialDomain: String,
    initialRole: String,
    initialLevel: String,
    initialScore: String,
    initialSummary: String,
    onDismiss: () -> Unit,
    onConfirm: (domain: String, role: String, level: String, score: Int, summary: String) -> Unit
) {
    var domain by remember { mutableStateOf(initialDomain) }
    var role by remember { mutableStateOf(initialRole) }
    var level by remember { mutableStateOf(initialLevel) }
    var scoreText by remember { mutableStateOf(initialScore) }
    var summary by remember { mutableStateOf(initialSummary) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Rol") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = level,
                    onValueChange = { level = it },
                    label = { Text("Nivel (Junior / Mid / Senior)") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = scoreText,
                    onValueChange = { scoreText = it },
                    label = { Text("Scor (0-100)") },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Rezumat / Notițe") },
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedScore = scoreText.toIntOrNull()?.coerceIn(0, 100) ?: 0
                    onConfirm(
                        domain.ifBlank { "IT & Software" },
                        role.ifBlank { "Developer" },
                        level.ifBlank { "Junior" },
                        parsedScore,
                        summary
                    )
                }
            ) {
                Text("Salvează")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anulează")
            }
        }
    )
}