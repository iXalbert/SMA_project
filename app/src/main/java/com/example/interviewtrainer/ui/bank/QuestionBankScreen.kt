package com.example.interviewtrainer.ui.bank

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.interviewtrainer.data.model.BookmarkedQuestionEntity
import com.example.interviewtrainer.data.model.CustomQuestionEntity
import com.example.interviewtrainer.ui.setup.CustomQuestionFormDialog
import com.example.interviewtrainer.viewmodel.InterviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionBankScreen(
    viewModel: InterviewViewModel
) {
    val bookmarks by viewModel.bookmarks.collectAsState()
    val customQuestions by viewModel.customQuestions.collectAsState()
    val config by viewModel.config.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Salvate (Bookmarks), 1 = Întrebările Mele
    var isFlashcardMode by remember { mutableStateOf(false) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingCustomQuestion by remember { mutableStateOf<CustomQuestionEntity?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Column {
                        Text(
                            text = "Bancă & Recapitulare",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${bookmarks.size} salvate • ${customQuestions.size} proprii",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (selectedTab == 0 && bookmarks.isNotEmpty()) {
                        FilledTonalButton(
                            onClick = { isFlashcardMode = !isFlashcardMode },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Style,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFlashcardMode) "Listă" else "Flashcards",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (selectedTab == 1) {
                        FilledTonalIconButton(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Adaugă Întrebare")
                        }
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
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Salvate (${bookmarks.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        isFlashcardMode = false
                    },
                    text = { Text("Întrebările Mele (${customQuestions.size})", fontWeight = FontWeight.SemiBold) }
                )
            }

            if (selectedTab == 0) {
                if (bookmarks.isEmpty()) {
                    EmptyBankState(
                        title = "Nicio întrebare salvată încă",
                        subtitle = "Apasă pe iconița de salvare în timpul interviului sau în raport pentru a recapitula întrebările dificile."
                    )
                } else if (isFlashcardMode) {
                    FlashcardDeckView(
                        bookmarks = bookmarks,
                        onExitFlashcards = { isFlashcardMode = false }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(bookmarks, key = { it.questionId }) { item ->
                            BookmarkCard(
                                bookmark = item,
                                onRemove = { viewModel.removeBookmark(item.questionId) }
                            )
                        }
                    }
                }
            } else {
                if (customQuestions.isEmpty()) {
                    EmptyBankState(
                        title = "Nu ai întrebări proprii",
                        subtitle = "Adaugă întrebări personalizate cu cuvinte cheie, iar simulatorul le va include automat în interviuri.",
                        actionLabel = "Creează prima întrebare",
                        onAction = { showCreateDialog = true }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(customQuestions, key = { it.id }) { q ->
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
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = q.role,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                        Row {
                                            IconButton(onClick = { editingCustomQuestion = q }, modifier = Modifier.size(34.dp)) {
                                                Icon(Icons.Default.Edit, contentDescription = "Editează", modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(onClick = { viewModel.deleteCustomQuestion(q) }, modifier = Modifier.size(34.dp)) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Șterge",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = q.questionText,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (q.keywordsCsv.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Cuvinte cheie: ${q.keywordsCsv}",
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
        }

        if (showCreateDialog) {
            CustomQuestionFormDialog(
                title = "Întrebare Nouă",
                initialQuestion = "",
                initialAnswer = "",
                initialKeywords = "",
                initialHint = "",
                onDismiss = { showCreateDialog = false },
                onSave = { qText, refAns, kw, hint ->
                    viewModel.addCustomQuestion(
                        CustomQuestionEntity(
                            domain = config.domain,
                            specialization = config.specialization,
                            role = config.role.ifBlank { "Software Engineer" },
                            questionText = qText,
                            referenceAnswer = refAns,
                            keywordsCsv = kw,
                            hint = hint
                        )
                    )
                    showCreateDialog = false
                }
            )
        }

        editingCustomQuestion?.let { current ->
            CustomQuestionFormDialog(
                title = "Editează Întrebarea",
                initialQuestion = current.questionText,
                initialAnswer = current.referenceAnswer,
                initialKeywords = current.keywordsCsv,
                initialHint = current.hint,
                onDismiss = { editingCustomQuestion = null },
                onSave = { qText, refAns, kw, hint ->
                    viewModel.updateCustomQuestion(
                        current.copy(
                            questionText = qText,
                            referenceAnswer = refAns,
                            keywordsCsv = kw,
                            hint = hint
                        )
                    )
                    editingCustomQuestion = null
                }
            )
        }
    }
}

@Composable
fun BookmarkCard(
    bookmark: BookmarkedQuestionEntity,
    onRemove: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = bookmark.role,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.BookmarkRemove,
                        contentDescription = "Elimină din salvate",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = bookmark.questionRo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Răspuns ideal:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = bookmark.referenceAnswerRo,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Apasă pentru a vedea răspunsul ideal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun FlashcardDeckView(
    bookmarks: List<BookmarkedQuestionEntity>,
    onExitFlashcards: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    val safeIndex = currentIndex.coerceIn(0, bookmarks.lastIndex)
    val currentCard = bookmarks[safeIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Card ${safeIndex + 1} din ${bookmarks.size} • ${currentCard.role}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )

        AnimatedContent(targetState = isFlipped, label = "flashcard_flip") { showAnswer ->
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (showAnswer) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    width = 1.5.dp,
                    color = if (showAnswer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { isFlipped = !isFlipped }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (showAnswer) "RĂSPUNS DE REFERINȚĂ" else "ÎNTREBARE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (showAnswer) currentCard.referenceAnswerRo else currentCard.questionRo,
                        style = if (showAnswer) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleLarge,
                        fontWeight = if (showAnswer) FontWeight.Normal else FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flip,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (showAnswer) "Apasă pentru a reveni la întrebare" else "Apasă pentru a dezvălui răspunsul",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    if (safeIndex > 0) {
                        currentIndex = safeIndex - 1
                        isFlipped = false
                    }
                },
                enabled = safeIndex > 0,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Anterior")
            }

            Button(
                onClick = {
                    if (safeIndex < bookmarks.lastIndex) {
                        currentIndex = safeIndex + 1
                        isFlipped = false
                    } else {
                        onExitFlashcards()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Text(if (safeIndex < bookmarks.lastIndex) "Următorul" else "Finalizează")
                if (safeIndex < bookmarks.lastIndex) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun EmptyBankState(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onAction, shape = RoundedCornerShape(12.dp)) {
                    Text(actionLabel)
                }
            }
        }
    }
}