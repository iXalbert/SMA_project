package com.example.interviewtrainer.ui.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.interviewtrainer.data.model.InterviewEntity
import com.example.interviewtrainer.data.model.QuestionDto
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
    onBack: () -> Unit,
    onStartSpacedRepetition: (List<QuestionDto>) -> Unit = {}
) {
    var selectedInterviewId by remember { mutableStateOf<Long?>(null) }
    var editingInterview by remember { mutableStateOf<InterviewEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Sesiuni", "Grafice Progres", "Repetiție Spațiată")

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val selectedInterview = interviews.find { it.id == selectedInterviewId }

    // Extragem întrebările cu cuvinte-cheie ratate folosind extensia de pe InterviewEntity
    val weakQuestions = remember(interviews) {
        interviews
            .flatMap { it.toQuestionReviews() }
            .filter { it.missedKeywords.isNotEmpty() }
            .distinctBy { it.questionText }
            .mapIndexed { idx, rev ->
                QuestionDto(
                    id = 800 + idx,
                    questionRo = rev.questionText,
                    questionEn = rev.questionText,
                    referenceAnswerRo = rev.referenceAnswer,
                    referenceAnswerEn = rev.referenceAnswer,
                    keywordsRo = rev.missedKeywords + rev.matchedKeywords,
                    keywordsEn = rev.missedKeywords + rev.matchedKeywords,
                    hintRo = "Concentrează-te pe conceptele: ${rev.missedKeywords.joinToString(", ")}",
                    hintEn = "Focus on concepts: ${rev.missedKeywords.joinToString(", ")}"
                )
            }
    }

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
                Column {
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
                    TabRow(selectedTabIndex = selectedTab) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            when (selectedTab) {
                // TAB 1: Sesiuni (lista completă + statistici + CRUD)
                0 -> {
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

                // TAB 2: Grafice Progres (Compose Canvas)
                1 -> {
                    val chronological = remember(interviews) {
                        interviews.sortedBy { it.timestamp }.takeLast(10)
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
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
                                    text = "Evoluția Scorului CtCI (ultimele ${chronological.size} sesiuni)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                if (chronological.size < 2) {
                                    Text(
                                        text = "Finalizează cel puțin 2 interviuri pentru a vizualiza curba de evoluție.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    val lineColor = MaterialTheme.colorScheme.primary
                                    val gridColor = MaterialTheme.colorScheme.outlineVariant

                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .padding(8.dp)
                                    ) {
                                        val w = size.width
                                        val h = size.height

                                        listOf(0f, 0.5f, 1f).forEach { ratio ->
                                            val y = h * ratio
                                            drawLine(
                                                color = gridColor,
                                                start = Offset(0f, y),
                                                end = Offset(w, y),
                                                strokeWidth = 1.dp.toPx()
                                            )
                                        }

                                        val stepX = w / (chronological.size - 1).coerceAtLeast(1)
                                        val path = Path()

                                        chronological.forEachIndexed { i, entity ->
                                            val x = i * stepX
                                            val y = h * (1f - (entity.score.coerceIn(0, 100) / 100f))
                                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)

                                            drawCircle(
                                                color = lineColor,
                                                radius = 5.dp.toPx(),
                                                center = Offset(x, y)
                                            )
                                        }

                                        drawPath(
                                            path = path,
                                            color = lineColor,
                                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                    }

                                    val avgScore = chronological.map { it.score }.average().toInt()
                                    val bestScore = chronological.maxOf { it.score }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("$avgScore%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Text("Medie", style = MaterialTheme.typography.labelSmall)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("$bestScore%", fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                                            Text("Record", style = MaterialTheme.typography.labelSmall)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("${interviews.size}", fontWeight = FontWeight.Bold)
                                            Text("Total Sesiuni", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // TAB 3: Repetiție Spațiată (antrenament țintit pe întrebările cu lacune)
                2 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Antrenament Repetiție Spațiată",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Aplicația extrage automat întrebările din istoricul tău unde ai omis cuvinte cheie sau analiza Big O.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Button(
                                    onClick = { onStartSpacedRepetition(weakQuestions.take(5)) },
                                    enabled = weakQuestions.isNotEmpty(),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Exersează ${weakQuestions.take(5).size} Întrebări Slabe")
                                }
                            }
                        }

                        if (weakQuestions.isEmpty()) {
                            Text(
                                text = "Nu există încă întrebări cu lacune detectate în sesiunile tale.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(weakQuestions) { q ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = q.questionRo,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Concepte de revizuit: ${q.keywordsRo.joinToString(", ")}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
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