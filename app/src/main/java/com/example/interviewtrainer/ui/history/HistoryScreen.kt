package com.example.interviewtrainer.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.interviewtrainer.data.model.InterviewEntity
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
    var editingInterview by remember { mutableStateOf<InterviewEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Istoric Interviuri") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Înapoi")
                    }
                },
                actions = {
                    // Buton CREATE manual pentru demo rapid
                    IconButton(onClick = { showCreateDialog = true }) {
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
                    Text("Nu ai salvat încă niciun interviu.")
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { showCreateDialog = true }) {
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
                items(interviews, key = { it.id }) { item ->
                    HistoryCard(
                        interview = item,
                        onEdit = { editingInterview = item },
                        onDelete = {
                            onDeleteInterview(item)
                            scope.launch {
                                snackbarHostState.showSnackbar("Sesiunea a fost ștearsă din baza de date.")
                            }
                        }
                    )
                }
            }
        }

        // Dialog CREATE
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

        // Dialog UPDATE
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
}

@Composable
fun HistoryCard(
    interview: InterviewEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(interview.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${interview.role} (${interview.level})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${interview.score} / 100",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (interview.score >= 70) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Domeniu: ${interview.domain}", style = MaterialTheme.typography.bodyMedium)
            Text(text = dateStr, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = interview.summary, style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editează", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Editează")
                }
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Șterge", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Șterge")
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
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = role,
                    onValueChange = { role = it },
                    label = { Text("Rol") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = level,
                    onValueChange = { level = it },
                    label = { Text("Nivel (Junior / Mid / Senior)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = scoreText,
                    onValueChange = { scoreText = it },
                    label = { Text("Scor (0-100)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Rezumat / Notițe") },
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedScore = scoreText.toIntOrNull()?.coerceIn(0, 100) ?: 0
                    onConfirm(domain.ifBlank { "IT & Software" }, role.ifBlank { "Developer" }, level.ifBlank { "Junior" }, parsedScore, summary)
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